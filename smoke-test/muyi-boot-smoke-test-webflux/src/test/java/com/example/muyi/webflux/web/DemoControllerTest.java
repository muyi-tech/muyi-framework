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
package com.example.muyi.webflux.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.muyi.webflux.WebFluxApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * {@link DemoController} 集成测试（响应式）。
 *
 * <p>
 * 使用 {@code WebTestClient.bindToApplicationContext} 绑定应用上下文（无真实服务器），
 * 覆盖统一成功响应、业务异常包装与方法不匹配（405）三类场景。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@SpringBootTest
@ContextConfiguration(classes = WebFluxApplication.class)
class DemoControllerTest {

    @Autowired private org.springframework.context.ApplicationContext applicationContext;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void hello_returnsUnifiedSuccessResult() {
        EntityExchangeResult<byte[]> result =
                client.get()
                        .uri("/api/demo/hello")
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody()
                        .jsonPath("$.code")
                        .isEqualTo("0")
                        .jsonPath("$.data")
                        .isEqualTo("Hello, Muyi Framework!")
                        .returnResult();

        assertThat(result.getResponseBody()).isNotNull();
    }

    @Test
    void sexEnums_returnsKeyValueList() {
        client.get()
                .uri("/api/demo/sex")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("0")
                .jsonPath("$.data")
                .isArray()
                .jsonPath("$.data[0].key")
                .isNumber()
                .jsonPath("$.data[0].value")
                .exists();
    }

    @Test
    void error_returnsBusinessErrorResult() {
        client.get()
                .uri("/api/demo/error")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("400")
                .jsonPath("$.msg")
                .isNotEmpty();
    }

    @Test
    void i18nError_zhCN_returnsChineseMessage() {
        client.get()
                .uri("/api/demo/i18n-error")
                .header("Accept-Language", "zh-CN")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("1001001000")
                .jsonPath("$.msg")
                .isEqualTo("用户 muyi 不存在");
    }

    @Test
    void i18nError_enUS_returnsEnglishMessage() {
        client.get()
                .uri("/api/demo/i18n-error")
                .header("Accept-Language", "en-US")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("1001001000")
                .jsonPath("$.msg")
                .isEqualTo("User muyi does not exist");
    }

    @Test
    void i18nError_unmatchedLocale_fallsBackToDefaultMessage() {
        client.get()
                .uri("/api/demo/i18n-error")
                .header("Accept-Language", "fr-FR")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("1001001000")
                .jsonPath("$.msg")
                .isEqualTo("用户 muyi 不存在");
    }

    @Test
    @DisplayName("POST 到 GET-only 端点：HttpRequestMethodNotSupportedException 映射为 METHOD_NOT_ALLOWED")
    void postToGetOnlyEndpoint_returnsMethodNotAllowed() {
        client.method(HttpMethod.POST)
                .uri("/api/demo/hello")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("405");
    }

    @Test
    void moduleError_interceptedByCustomModuleHandler() {
        client.get()
                .uri("/api/demo/module-error")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("1001001001")
                .jsonPath("$.msg")
                .isEqualTo("模块级异常处理器演示：业务专有异常已被自定义拦截");
    }
}
