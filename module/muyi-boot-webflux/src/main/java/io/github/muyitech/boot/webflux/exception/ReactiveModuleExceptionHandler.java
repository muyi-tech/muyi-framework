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

import io.github.muyitech.boot.exception.support.ExceptionHandlerSupport;
import io.github.muyitech.common.spring.pojo.ApiResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * 定义响应式模块异常处理器标记接口（WebFlux）。
 *
 * <p>
 * 各模块实现该接口并注册为 Bean，响应式核心处理器
 * {@link ReactiveCoreExceptionHandler} 会优先调用，若返回非空结果则直接返回。
 *
 * <p>
 * 实现示例：
 *
 * <pre>{@code
 * @Component
 * @Order(Ordered.LOWEST_PRECEDENCE - 1) // 通用模块处理器置核心兜底前一格；专拦业务异常可用 HIGHEST_PRECEDENCE
 * public class ModuleSampleWebFluxExceptionHandler implements ReactiveModuleExceptionHandler {
 *
 *     @Override
 *     public ApiResult<?> allExceptionHandler(ServerWebExchange exchange, Throwable ex) {
 *         if (ex instanceof ModuleSampleWebException webEx) {
 *             return ApiResult.error(webEx.getErrorCode(), webEx.getArgs());
 *         }
 *         return null; // 不处理，交回核心兜底
 *     }
 * }
 * }</pre>
 *
 * @author keep simple
 * @since 2026/9/13
 */
public interface ReactiveModuleExceptionHandler extends ExceptionHandlerSupport {

    /**
     * 模块异常处理入口。
     * @param exchange 当前请求交换
     * @param ex 异常
     * @return 通用返回结果；返回 null 表示不处理，交由后续处理器
     */
    ApiResult<?> allExceptionHandler(ServerWebExchange exchange, Throwable ex);
}
