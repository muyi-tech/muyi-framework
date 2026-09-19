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

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.pojo.ApiResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ReactiveModuleExceptionHandler} 单元测试。
 *
 * <p>
 * 覆盖模块扩展点约定：实现类对不认识的异常返回 null（回落核心处理器）、返回非空结果短路。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class ReactiveModuleExceptionHandlerTest {

    private final MockServerWebExchange exchange =
            MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());

    @Test
    void unknownException_returnsNull_fallsBackToCore() {
        ReactiveModuleExceptionHandler handler =
                new ReactiveModuleExceptionHandler() {
                    @Override
                    public ApiResult<?> allExceptionHandler(
                            ServerWebExchange exchange, Throwable ex) {
                        return null;
                    }
                };

        ApiResult<?> result = handler.allExceptionHandler(exchange, new RuntimeException("x"));

        assertThat(result).isNull();
    }

    @Test
    void knownException_returnsResult_shortCircuitsCore() {
        ReactiveModuleExceptionHandler handler =
                new ReactiveModuleExceptionHandler() {
                    @Override
                    public ApiResult<?> allExceptionHandler(
                            ServerWebExchange exchange, Throwable ex) {
                        if (ex instanceof IllegalStateException) {
                            // ApiResult.error 按 {} 占位符格式化参数
                            return ApiResult.error(
                                    new ErrorCode("1001", "模块异常 {}"), ex.getMessage());
                        }
                        return null;
                    }
                };

        ApiResult<?> result =
                handler.allExceptionHandler(exchange, new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo("1001");
        assertThat(result.getMsg()).isEqualTo("模块异常 boom");
    }
}
