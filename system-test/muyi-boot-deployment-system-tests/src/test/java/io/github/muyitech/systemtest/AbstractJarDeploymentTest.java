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
package io.github.muyitech.systemtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 部署系统测试基类：把示例应用的可执行 jar 以真实 JVM 进程（{@code java -jar}）启动，
 * 通过 HTTP 验证运行时行为（mock 型测试无法覆盖的最后一层：进程、真实端口、
 * 真实序列化与真实异常处理链）。
 *
 * <p>
 * 用例覆盖 {@code /api/demo} 的典型场景：统一响应包装（{@code ApiResult}）、
 * 数据字典（{@code KeyValue}）、业务异常（{@code ServiceException} 统一转换）、
 * 业务异常本地化（{@code Accept-Language}）、模块级自定义拦截（扩展点）。
 * 两栈（WebMVC / WebFlux）行为约定一致：业务异常返回 HTTP 200 + body 中业务码非 0。
 *
 * <p>
 * 子类只需声明被测 jar 前缀与应用名。测试类间顺序执行（surefire 默认非并行），
 * 每个类独立完成「启动 → 断言 → 停止」的完整生命周期。
 *
 * @author keep simple
 * @since 2026/9/14
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractJarDeploymentTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String jarNamePrefix;

    private final String appName;

    private Process application;

    private int port;

    private HttpClient client;

    private Path logFile;

    protected AbstractJarDeploymentTest(String jarNamePrefix, String appName) {
        this.jarNamePrefix = jarNamePrefix;
        this.appName = appName;
    }

    @BeforeAll
    void startApplication() throws IOException, InterruptedException {
        port = JarDeploymentSupport.freePort();
        logFile = JarDeploymentSupport.LOGS_DIR.resolve(appName + ".log");
        application = JarDeploymentSupport.startJar(jarNamePrefix, port, appName);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JarDeploymentSupport.awaitReady(client, baseUrl() + "/hello", logFile);
    }

    @AfterAll
    void stopApplication() {
        JarDeploymentSupport.stop(application);
    }

    /**
     * 统一响应：成功结果由 {@code ApiResult} 包装（code="0"、msg 为空串、data 为业务数据），
     * 验证响应式/Servlet 双栈在真实运行时的 JSON 序列化结构。
     */
    @Test
    void unifiedResponseWrapsData() {
        HttpResponse<String> response = JarDeploymentSupport.get(client, baseUrl() + "/hello");
        assertEquals(200, response.statusCode(), "HTTP 状态码");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("0", body.get("code").asText(), "成功业务码（字符串形态）");
        assertEquals("", body.get("msg").asText(), "成功时 msg 为空串");
        assertEquals("Hello, Muyi Framework!", body.get("data").asText(), "业务数据");
    }

    /**
     * 数据字典：{@code KeyValue} 容器序列化为 key/value 字段，
     * 验证 commons 通用组件在真实运行时的序列化行为。
     */
    @Test
    void keyValueSerialization() {
        HttpResponse<String> response = JarDeploymentSupport.get(client, baseUrl() + "/sex");
        assertEquals(200, response.statusCode(), "HTTP 状态码");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("0", body.get("code").asText(), "成功业务码（字符串形态）");
        JsonNode data = body.get("data");
        assertTrue(data.isArray(), "data 应为数组");
        assertTrue(data.size() > 0, "data 不应为空（SexEnum 至少一项）");
        assertTrue(data.get(0).has("key"), "元素应含 key 字段");
        assertTrue(data.get(0).has("value"), "元素应含 value 字段");
    }

    /**
     * 业务异常：{@code ServiceException}（BAD_REQUEST）被异常处理模块统一转换为
     * 错误 {@code ApiResult}——框架约定为 HTTP 200 + body 中业务码 400（错误码在 body），
     * 验证异常处理链在真实进程内生效。
     */
    @Test
    void serviceExceptionBecomesErrorResult() {
        HttpResponse<String> response = JarDeploymentSupport.get(client, baseUrl() + "/error");
        assertEquals(200, response.statusCode(), "HTTP 状态码（错误码在 body 的框架约定）");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("400", body.get("code").asText(), "BAD_REQUEST 业务码（字符串形态）");
        assertNotNull(body.get("msg"), "错误提示应存在");
    }

    /**
     * 业务异常本地化：{@code /i18n-error} 抛出的业务异常（YAML 注册多语言文案），
     * 按请求 {@code Accept-Language} 解析为对应语言文案——验证错误码 YAML 资源随
     * fat jar 分发、FileErrorCodeProvider 启动加载与真实进程内 Accept-Language
     * 本地化链路（Mock 冒烟测试已覆盖 zh-CN 与未匹配回退，此处仅验 en-US 主链路）。
     */
    @Test
    void i18nError_resolvesAcceptLanguage() {
        HttpResponse<String> response =
                JarDeploymentSupport.get(
                        client, baseUrl() + "/i18n-error", Map.of("Accept-Language", "en-US"));
        assertEquals(200, response.statusCode(), "HTTP 状态码");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("1001001000", body.get("code").asText(), "业务码（字符串形态）");
        assertEquals("User muyi does not exist", body.get("msg").asText(), "en-US 本地化文案");
    }

    /**
     * 模块级自定义拦截：{@code /module-error} 抛出的业务专有异常（非 {@code ServiceException}
     * 体系，核心处理器不识别）由 {@code DemoCustomModuleExceptionHandler} 在核心兜底前
     * 拦截并返回自定义错误响应——验证模块扩展点在真实进程内生效
     * （Mock 冒烟测试已覆盖同断言，此处验 fat jar 分发场景）。
     */
    @Test
    void moduleError_interceptedByCustomModuleHandler() {
        HttpResponse<String> response =
                JarDeploymentSupport.get(client, baseUrl() + "/module-error");
        assertEquals(200, response.statusCode(), "HTTP 状态码（错误码在 body 的框架约定）");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("1001001001", body.get("code").asText(), "模块处理器自定义业务码");
        assertEquals("模块级异常处理器演示：业务专有异常已被自定义拦截", body.get("msg").asText(), "自定义拦截文案");
    }

    /**
     * 404 统一转换：未匹配路由在真实进程内被核心处理器转为错误 {@code ApiResult}
     * （WebMVC 经 NoHandlerFound/NoResourceFound 转换，常开；WebFlux 经 advice 的
     * ResponseStatusException 分支或 handle-not-found 兜底——两栈三出口共用
     * {@code handleNotFound} 统一文案）。契约固化 code 形态与 message：应用未接
     * 注册表时恒为默认文案"请求未找到"，且不携带路径与容器细节。
     */
    @Test
    void notFound_convertedToErrorResult() {
        HttpResponse<String> response =
                JarDeploymentSupport.get(client, baseUrl() + "/no-such-endpoint");
        JsonNode body = JSON.readTree(response.body());
        assertEquals("404", body.get("code").asText(), "404 业务码（字符串形态）");
        assertEquals("请求未找到", body.get("msg").asText(), "NOT_FOUND 默认文案（404 统一出口，双栈一致且不透传细节）");
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api/demo";
    }
}
