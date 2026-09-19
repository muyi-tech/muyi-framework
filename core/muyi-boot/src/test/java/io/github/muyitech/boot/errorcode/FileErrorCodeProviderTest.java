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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * {@link FileErrorCodeProvider} 单元测试。
 *
 * <p>
 * 覆盖：正常加载（数字码/字符串码/多语言/覆盖全局码）、缺 code、未知字段、
 * 非法码格式（前导零/小写蛇形）、文件不存在跳过、空 locations。
 *
 * @author keep simple
 * @since 2026/9/14
 */
class FileErrorCodeProviderTest {

    @Test
    @DisplayName("正常加载：数字码规范化、字符串码原样、多语言、全局码覆盖全部生效")
    void load_parsesAllForms() throws Exception {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/valid-error-codes.yaml"));

        List<ErrorCode> codes = List.copyOf(provider.load());

        assertThat(codes).hasSize(4);
        // 数字写法：YAML 标量原文 "1001001000" 注册为字符串
        ErrorCode biz =
                codes.stream()
                        .filter(c -> "1001001000".equals(c.getCode()))
                        .findFirst()
                        .orElseThrow();
        assertThat(biz.getMsg()).isEqualTo("用户 {} 不存在");
        assertThat(biz.getMessages()).containsEntry("en-US", "User {} does not exist");
        // 字符串写法：原样注册
        ErrorCode stringCode =
                codes.stream()
                        .filter(c -> "WECHAT_API_ERROR".equals(c.getCode()))
                        .findFirst()
                        .orElseThrow();
        assertThat(stringCode.getMsg()).isEqualTo("微信接口调用失败: {}");
        // 全局码覆盖文案
        ErrorCode overridden =
                codes.stream().filter(c -> "500".equals(c.getCode())).findFirst().orElseThrow();
        assertThat(overridden.getMsg()).isEqualTo("系统开小差了,请稍后重试");
    }

    @Test
    @DisplayName("缺 code：启动期 fail-fast，报文件与行号")
    void load_missingCode_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/missing-code.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing-code.yaml")
                .hasMessageContaining("code 缺失");
    }

    @Test
    @DisplayName("未知字段：fail-fast 并提示允许的字段")
    void load_unknownField_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/unknown-field.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知字段")
                .hasMessageContaining("code / msg / messages");
    }

    @Test
    @DisplayName("数字码带前导零：fail-fast（八进制歧义防护）")
    void load_leadingZeroCode_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/leading-zero.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("前导零");
    }

    @Test
    @DisplayName("字符串码非法（小写蛇形）：fail-fast")
    void load_invalidStringCode_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/invalid-string-code.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SCREAMING_SNAKE_CASE");
    }

    @Test
    @DisplayName("文件不存在：warn 跳过，不阻塞启动")
    void load_missingFile_skipsGracefully() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/not-exist.yaml"));

        assertThat(provider.load()).isEmpty();
    }

    @Test
    @DisplayName("locations 为空：不加载任何文件")
    void load_emptyLocations_returnsEmpty() {
        assertThat(new FileErrorCodeProvider(List.of()).load()).isEmpty();
        assertThat(new FileErrorCodeProvider(null).load()).isEmpty();
    }

    @Test
    @DisplayName("order：晚于常量兜底源（值大者后加载，LAST_WINS 下可覆盖框架文案）")
    void order_isAfterConstantsProvider() {
        ConstantsErrorCodeProvider constants = new ConstantsErrorCodeProvider();
        FileErrorCodeProvider file = new FileErrorCodeProvider(List.of());

        assertThat(file.getOrder()).isGreaterThan(constants.getOrder());
    }

    @Test
    @DisplayName("测试资源文件存在且可读（防止资源目录改名导致假绿）")
    void testResources_exist() throws Exception {
        assertThat(
                        FileCopyUtils.copyToString(
                                new java.io.InputStreamReader(
                                        new ClassPathResource("errorcodes/valid-error-codes.yaml")
                                                .getInputStream(),
                                        java.nio.charset.StandardCharsets.UTF_8)))
                .contains("error-codes:");
    }

    @Test
    @DisplayName("空 YAML 文档（仅注释）：warn 跳过，不阻塞启动")
    void load_emptyDocument_skipsGracefully() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/empty-doc.yaml"));

        assertThat(provider.load()).isEmpty();
    }

    @Test
    @DisplayName("location 指向目录：读取失败 fail-fast（IllegalStateException）")
    void load_directoryLocation_throwsIllegalState() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("file:src/test/resources/errorcodes"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("读取失败")
                .hasMessageContaining("errorcodes");
    }

    @Test
    @DisplayName("根节点是列表：fail-fast（YAML 根节点必须是键值映射）")
    void load_rootList_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/root-list.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YAML 根节点必须是键值映射");
    }

    @Test
    @DisplayName("根节点是标量：fail-fast（YAML 根节点必须是键值映射）")
    void load_rootScalar_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/root-scalar.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YAML 根节点必须是键值映射");
    }

    @Test
    @DisplayName("缺少 error-codes 键：warn 跳过，不阻塞启动")
    void load_missingErrorCodesKey_skipsGracefully() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/no-error-codes.yaml"));

        assertThat(provider.load()).isEmpty();
    }

    @Test
    @DisplayName("error-codes 不是列表：fail-fast")
    void load_errorCodesNotSequence_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(
                        List.of("classpath:errorcodes/error-codes-not-list.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("error-codes 必须是列表");
    }

    @Test
    @DisplayName("error-codes 条目不是映射：fail-fast")
    void load_entryNotMapping_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/item-not-mapping.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("error-codes 条目必须是键值映射");
    }

    @Test
    @DisplayName("messages 不是映射：fail-fast（必须是语言标签 → 文案映射）")
    void load_messagesNotMapping_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(
                        List.of("classpath:errorcodes/messages-not-mapping.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages 必须是语言标签 → 文案映射");
    }

    @Test
    @DisplayName("code 值不是标量：fail-fast")
    void load_nonScalarCode_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/non-scalar-code.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code 必须是标量值");
    }

    @Test
    @DisplayName("键不是标量（复杂键）：fail-fast")
    void load_nonScalarKey_throwsIllegalArgument() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/non-scalar-key.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("键 必须是标量键");
    }

    // ==================== 资源访问路径补测（mock resolver 注入） ====================

    /**
     * 将 provider 内部的资源解析器替换为 mock（JDK17 实例 final 字段反射可写），模拟
     * jar 内 classpath 资源等非本地文件场景。
     * @param provider 被注入的 provider
     * @param mockResolver mock 的资源解析器
     * @throws Exception 反射失败时抛出
     */
    private void injectResolver(FileErrorCodeProvider provider, Object mockResolver)
            throws Exception {
        Field field = FileErrorCodeProvider.class.getDeclaredField("resourceResolver");
        field.setAccessible(true);
        field.set(provider, mockResolver);
    }

    @Test
    @DisplayName("getFile() 抛 IO 异常（jar 内资源场景）：回退流读取，正常加载")
    void load_getFileThrowsIOException_fallsBackToStreamRead() throws Exception {
        PathMatchingResourcePatternResolver mockResolver =
                mock(PathMatchingResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        when(mockResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getFile()).thenThrow(new IOException("not a local file"));
        byte[] yaml =
                FileCopyUtils.copyToByteArray(
                        new ClassPathResource("errorcodes/valid-error-codes.yaml")
                                .getInputStream());
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(yaml));

        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/valid-error-codes.yaml"));
        injectResolver(provider, mockResolver);

        assertThat(provider.load()).hasSize(4);
    }

    @Test
    @DisplayName("getInputStream() 也抛 IO 异常：读取失败 fail-fast（IllegalStateException）")
    void load_getInputStreamThrows_failsFast() throws Exception {
        PathMatchingResourcePatternResolver mockResolver =
                mock(PathMatchingResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        when(mockResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getFile()).thenThrow(new IOException("not a local file"));
        when(resource.getInputStream()).thenThrow(new IOException("stream closed"));

        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/valid-error-codes.yaml"));
        injectResolver(provider, mockResolver);

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("读取失败")
                .hasMessageContaining("valid-error-codes.yaml");
    }

    @Test
    @DisplayName("getInputStream() 正常但 close() 抛 IO 异常：资源关闭失败同样 fail-fast")
    void load_closeThrowsIOException_failsFast() throws Exception {
        PathMatchingResourcePatternResolver mockResolver =
                mock(PathMatchingResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        when(mockResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getFile()).thenThrow(new IOException("not a local file"));
        byte[] yaml =
                FileCopyUtils.copyToByteArray(
                        new ClassPathResource("errorcodes/valid-error-codes.yaml")
                                .getInputStream());
        // 流内容正常可读，仅 close() 抛异常——覆盖 try-with-resources 的资源关闭失败路径
        when(resource.getInputStream())
                .thenReturn(
                        new ByteArrayInputStream(yaml) {
                            @Override
                            public void close() throws IOException {
                                throw new IOException("close failed");
                            }
                        });

        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/valid-error-codes.yaml"));
        injectResolver(provider, mockResolver);

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("读取失败")
                .hasMessageContaining("valid-error-codes.yaml");
    }

    @Test
    @DisplayName("读流中途抛 IO 异常（close 正常）：读取失败 fail-fast，不被 SnakeYAML 包装")
    void load_readAllBytesThrows_failsFast() throws Exception {
        PathMatchingResourcePatternResolver mockResolver =
                mock(PathMatchingResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        when(mockResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getFile()).thenThrow(new IOException("not a local file"));
        // 第一次读就抛 IO 异常、close 正常——覆盖 try 块异常经资源关闭区重抛进入 catch 的路径
        when(resource.getInputStream())
                .thenReturn(
                        new java.io.InputStream() {
                            @Override
                            public int read() throws IOException {
                                throw new IOException("read failed");
                            }
                        });

        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/valid-error-codes.yaml"));
        injectResolver(provider, mockResolver);

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("读取失败")
                .hasMessageContaining("valid-error-codes.yaml")
                .hasRootCauseMessage("read failed");
    }

    @Test
    @DisplayName("code 为空白字符串：与缺失同判，fail-fast")
    void load_blankCode_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/blank-code.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank-code.yaml")
                .hasMessageContaining("code 缺失");
    }

    @Test
    @DisplayName("msg 字段整体缺失：fail-fast")
    void load_missingMsg_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/missing-msg.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing-msg.yaml")
                .hasMessageContaining("msg 缺失");
    }

    @Test
    @DisplayName("msg 为空白字符串：与缺失同判，fail-fast")
    void load_blankMsg_failsFast() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/blank-msg.yaml"));

        assertThatThrownBy(provider::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank-msg.yaml")
                .hasMessageContaining("msg 缺失");
    }

    @Test
    @DisplayName("根节点键不是标量（复杂键）：findValue 跳过后按缺 error-codes 处理，warn 跳过")
    void load_complexRootKey_skipsGracefully() {
        FileErrorCodeProvider provider =
                new FileErrorCodeProvider(List.of("classpath:errorcodes/complex-root-key.yaml"));

        assertThat(provider.load()).isEmpty();
    }

    @Test
    @DisplayName("lineOf：无 StartMark 的手工节点回退行号 1（防御 null）")
    void lineOf_nullStartMark_returnsOne() throws Exception {
        FileErrorCodeProvider provider = new FileErrorCodeProvider(List.of());
        ScalarNode node = new ScalarNode(Tag.STR, "x", null, null, DumperOptions.ScalarStyle.PLAIN);

        Method lineOf = FileErrorCodeProvider.class.getDeclaredMethod("lineOf", Node.class);
        lineOf.setAccessible(true);

        assertThat((int) lineOf.invoke(provider, node)).isEqualTo(1);
    }
}
