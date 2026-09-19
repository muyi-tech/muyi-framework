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
package io.github.muyitech.boot.webflux.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link MuyiNotFoundWebExceptionHandler} 单元测试。
 *
 * <p>
 * 覆盖：404 的 {@link ResponseStatusException} 被包装为 {@code ApiResult} JSON 响应（HTTP 404 +
 * application/json + 统一 NOT_FOUND 文案，不携带路径）；非 404 异常原样放行；非
 * {@link ResponseStatusException} 异常放行。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class MuyiNotFoundWebExceptionHandlerTest {

    private final MuyiNotFoundWebExceptionHandler handler = new MuyiNotFoundWebExceptionHandler();

    @Test
    void notFoundStatusException_wrappedAsApiResultJson() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/missing/path").build());
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.NOT_FOUND, (String) null);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body)
                .contains("\"code\":\"404\"")
                .contains("\"msg\":\"请求未找到\"")
                .doesNotContain("/api/missing/path");
    }

    @Test
    void otherStatusException_passesThrough() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, (String) null);

        StepVerifier.create(handler.handle(exchange, ex))
                .expectError(ResponseStatusException.class)
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void nonResponseStatusException_passesThrough() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        IllegalStateException ex = new IllegalStateException("boom");

        StepVerifier.create(handler.handle(exchange, ex))
                .expectError(IllegalStateException.class)
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void singleArgConstructor_wrapsNotFoundAsApiResultJson() {
        // 单参构造：仅 JsonMapper、不传 ObjectMapper（未启用 objectMapper 增强的场景）
        MuyiNotFoundWebExceptionHandler customHandler =
                new MuyiNotFoundWebExceptionHandler(JsonMapper.builder().build());
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/missing/path").build());
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.NOT_FOUND, (String) null);

        StepVerifier.create(customHandler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"code\":\"404\"").contains("\"msg\":\"请求未找到\"");
    }
}
