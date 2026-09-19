/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.muyitech.boot.errorcode;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * 文件初始化错误码来源（YAML，classpath:/file: 混用，逗号分隔多 location）。
 *
 * <p>
 * 文件格式（{@code error-codes} 列表；{@code {}} 占位符与
 * {@code ServiceExceptionUtil.doFormat} 一致）：
 *
 * <pre>{@code
 * error-codes:
 *   - code: 1001001000          # 数字写法 → 注册为 "1001001000"
 *     msg: "用户 {} 不存在"
 *     messages:                  # 可选，多语言定义
 *       zh-CN: "用户 {} 不存在"
 *       en-US: "User {} does not exist"
 *   - code: WECHAT_API_ERROR    # 字符串写法，原样注册
 *     msg: "微信接口调用失败: {}"
 *   - code: 500                  # 覆盖全局 500 文案，无需发版
 *     msg: "系统开小差了,请稍后重试"
 * }</pre>
 *
 * <p>
 * 启动期 fail-fast：缺 code / 格式非法 / YAML 非法，抛出带文件名与行号的明确异常；
 * 默认 locations 为空——不建文件也能正常启动。数字写法经 YAML 标量原文校验
 * （禁止前导零，避免 {@code 0500} 八进制歧义）。
 *
 * <p>
 * order = {@code HIGHEST_PRECEDENCE + 200}：晚于常量兜底源加载
 * （Spring Ordered 约定，值小者先加载），LAST_WINS 下可覆盖框架默认文案。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public class FileErrorCodeProvider implements ErrorCodeProvider {

    /**
     * 共享日志。
     */
    private static final Logger LOG = LoggerFactory.getLogger(FileErrorCodeProvider.class);

    /**
     * 字符串码命名规则：SCREAMING_SNAKE_CASE，2~32 字符（4.1 辅轨）。
     */
    private static final Pattern STRING_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");

    /**
     * 数字码规则：无前导零的 1~10 位十进制（0 与业务 10 位码均合法）。
     */
    private static final Pattern NUMBER_CODE_PATTERN = Pattern.compile("^(0|[1-9][0-9]{0,9})$");

    /**
     * 文件内错误码列表的键名。
     */
    private static final String ROOT_KEY = "error-codes";

    /**
     * 错误码定义文件位置列表（classpath:/file: 混用）。
     */
    private final List<String> locations;

    /**
     * 资源解析器（classpath 与 file 统一处理）。
     */
    private final PathMatchingResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    /**
     * 构造文件来源。
     * @param locations 错误码定义文件位置（可空——不建文件也能正常启动）
     */
    public FileErrorCodeProvider(@Nullable List<String> locations) {
        this.locations = locations == null ? List.of() : List.copyOf(locations);
    }

    @Override
    public Collection<ErrorCode> load() {
        List<ErrorCode> result = new ArrayList<>();
        for (String location : locations) {
            result.addAll(loadLocation(location));
        }
        return result;
    }

    /**
     * 加载单个 location。
     * @param location 资源位置
     * @return 该文件的错误码列表
     */
    private List<ErrorCode> loadLocation(String location) {
        Resource resource = resourceResolver.getResource(location);
        if (!resource.exists()) {
            LOG.warn("[loadLocation][错误码定义文件不存在，跳过：{}]", location);
            return List.of();
        }
        // 目录 fail-fast：JDK 读目录 URL 会返回目录清单文本流，不拦截会被误当 YAML 解析出误导性报错
        File localFile;
        try {
            localFile = resource.getFile();
        } catch (IOException ex) {
            localFile = null; // 非本地文件（如 jar 内 classpath 资源），交给下方流读取
        }
        if (localFile != null && localFile.isDirectory()) {
            throw new IllegalStateException("[loadLocation][错误码定义文件读取失败：" + location + "（是目录）]");
        }
        Node root;
        try {
            // 全量读入内存再解析：读流失败不再被 SnakeYAML 包装为 YAMLException，
            // 统一 fail-fast 为"文件读取失败"（IOException），解析与 IO 职责解耦
            byte[] bytes = readResourceBytes(resource);
            Yaml yaml = new Yaml(new LoaderOptions());
            List<Node> documents = new ArrayList<>();
            yaml.composeAll(
                            new java.io.InputStreamReader(
                                    new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
                    .forEach(documents::add);
            if (documents.isEmpty()) {
                LOG.warn("[loadLocation][错误码定义文件为空，跳过：{}]", location);
                return List.of();
            }
            root = documents.get(0);
        } catch (IOException ex) {
            // 普通 try-catch（单异常表条目 → 单分支）：getInputStream 失败、读流失败、
            // close 失败统一经 readResourceBytes 以 IOException 传播至此 fail-fast。
            // 若 try-with-resources 与 catch 直接同层，javac 会生成"双条目 → 同一 catch"
            // 模板，JaCoCo 单 probe 无法区分来源条目，报告恒有一个分支无法覆盖。
            throw new IllegalStateException("[loadLocation][错误码定义文件读取失败：" + location + "]", ex);
        }
        if (!(root instanceof MappingNode rootMapping)) {
            throw new IllegalArgumentException(formatError(location, 1, "YAML 根节点必须是键值映射"));
        }
        Node listNode = findValue(rootMapping, ROOT_KEY);
        if (listNode == null) {
            LOG.warn("[loadLocation][文件缺少 error-codes 键，跳过：{}]", location);
            return List.of();
        }
        if (!(listNode instanceof SequenceNode sequence)) {
            throw new IllegalArgumentException(
                    formatError(location, lineOf(listNode), "error-codes 必须是列表"));
        }
        List<ErrorCode> result = new ArrayList<>();
        for (Node item : sequence.getValue()) {
            result.add(parseEntry(location, item));
        }
        return result;
    }

    /**
     * 全量读取资源字节（读流与关闭在独立 try-with-resources 内完成）。
     *
     * <p>
     * 独立封装使 {@link #loadLocation(String)} 的 catch 退化为普通 try-catch
     * 单异常表条目：getInputStream 失败、读流失败、close 失败统一以 IOException
     * 传播至调用方 fail-fast（close 失败经 try-with-resources 模板的 primaryExc
     * 重抛路径传播，fail-fast 语义不变）。
     * @param resource 资源
     * @return 资源完整字节
     * @throws IOException 读取或关闭失败
     */
    private byte[] readResourceBytes(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * 解析单条错误码定义。
     * @param location 资源位置（报错用）
     * @param item 条目节点
     * @return 错误码定义
     */
    private ErrorCode parseEntry(String location, Node item) {
        if (!(item instanceof MappingNode mapping)) {
            throw new IllegalArgumentException(
                    formatError(location, lineOf(item), "error-codes 条目必须是键值映射"));
        }
        String code = null;
        String msg = null;
        Map<String, String> messages = new LinkedHashMap<>();
        for (NodeTuple tuple : mapping.getValue()) {
            String key = scalarValue(location, tuple.getKeyNode(), "键");
            Node valueNode = tuple.getValueNode();
            switch (key) {
                case "code" -> code = rawScalar(location, valueNode, "code");
                case "msg" -> msg = rawScalar(location, valueNode, "msg");
                case "messages" -> parseMessages(location, valueNode, messages);
                default ->
                        throw new IllegalArgumentException(
                                formatError(
                                        location,
                                        lineOf(valueNode),
                                        "未知字段 " + key + "（允许：code / msg / messages）"));
            }
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(formatError(location, lineOf(item), "code 缺失"));
        }
        validateCode(location, item, code);
        if (msg == null || msg.isBlank()) {
            throw new IllegalArgumentException(formatError(location, lineOf(item), "msg 缺失"));
        }
        return new ErrorCode(code, msg, messages);
    }

    /**
     * 解析多语言文案节点。
     * @param location 资源位置（报错用）
     * @param valueNode messages 节点
     * @param messages 输出容器
     */
    private void parseMessages(String location, Node valueNode, Map<String, String> messages) {
        if (valueNode.getNodeId() != NodeId.mapping) {
            throw new IllegalArgumentException(
                    formatError(location, lineOf(valueNode), "messages 必须是语言标签 → 文案映射"));
        }
        MappingNode mapping = (MappingNode) valueNode;
        for (NodeTuple tuple : mapping.getValue()) {
            String tag = scalarValue(location, tuple.getKeyNode(), "语言标签");
            String text = rawScalar(location, tuple.getValueNode(), "messages." + tag);
            messages.put(tag, text);
        }
    }

    /**
     * 双轨码格式校验（4.1）：数字码（主轨，无前导零）或字符串码（辅轨，SCREAMING_SNAKE_CASE）。
     * @param location 资源位置（报错用）
     * @param node 码值节点（行号用）
     * @param code 码值原文
     */
    private void validateCode(String location, Node node, String code) {
        if (NUMBER_CODE_PATTERN.matcher(code).matches()) {
            return;
        }
        if (STRING_CODE_PATTERN.matcher(code).matches()) {
            return;
        }
        throw new IllegalArgumentException(
                formatError(
                        location,
                        lineOf(node),
                        "code "
                                + code
                                + " 非法：数字码须为无前导零的 1~10 位十进制，"
                                + "字符串码须符合 SCREAMING_SNAKE_CASE（2~32 字符）"));
    }

    /**
     * 在映射节点中查找指定键的值节点。
     * @param mapping 映射节点
     * @param key 键名
     * @return 值节点；未命中返回 null
     */
    private Node findValue(MappingNode mapping, String key) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode scalar && key.equals(scalar.getValue())) {
                return tuple.getValueNode();
            }
        }
        return null;
    }

    /**
     * 读取标量节点原文（不经过 YAML 类型解析——数字码保持标量原文，规避前导零歧义）。
     * @param location 资源位置（报错用）
     * @param node 值节点
     * @param field 字段名（报错用）
     * @return 标量原文
     */
    private String rawScalar(String location, Node node, String field) {
        if (!(node instanceof ScalarNode scalar)) {
            throw new IllegalArgumentException(
                    formatError(location, lineOf(node), field + " 必须是标量值"));
        }
        return scalar.getValue();
    }

    /**
     * 读取标量键名。
     * @param location 资源位置（报错用）
     * @param node 键节点
     * @param field 字段名（报错用）
     * @return 键名
     */
    private String scalarValue(String location, Node node, String field) {
        if (!(node instanceof ScalarNode scalar)) {
            throw new IllegalArgumentException(
                    formatError(location, lineOf(node), field + " 必须是标量键"));
        }
        return scalar.getValue();
    }

    /**
     * 节点起始行号（1 基）。
     * @param node 节点
     * @return 行号
     */
    private int lineOf(Node node) {
        return node.getStartMark() == null ? 1 : node.getStartMark().getLine() + 1;
    }

    /**
     * 格式化启动期校验错误（带文件名与行号）。
     * @param location 资源位置
     * @param line 行号
     * @param message 错误描述
     * @return 完整错误消息
     */
    private String formatError(String location, int line, String message) {
        return "[FileErrorCodeProvider][" + location + " 第 " + line + " 行] " + message;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}
