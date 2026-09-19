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
package io.github.muyitech.boot.webmvc.exception;

import io.github.muyitech.boot.exception.support.ExceptionHandlerSupport;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 定义异常处理器标记接口（WebMVC）。
 *
 * <p>
 * 各模块实现该接口，注册自己的异常处理器；核心处理器会优先调用模块处理器，若返回非空结果则直接返回。
 *
 * <p>
 * 实现示例（双通道：@ExceptionHandler 全局异常处理 + SPI 扩展点，两个通道都要求实现）：
 *
 * <pre>{@code
 * @RestControllerAdvice
 * @Order(Ordered.LOWEST_PRECEDENCE - 1) // 通用模块处理器置核心兜底前一格；专拦业务异常可用 HIGHEST_PRECEDENCE
 * public class ModuleSampleWebExceptionHandler implements ModuleExceptionHandler {
 *
 *     @Override
 *     public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
 *         // SPI 通道：只处理本模块异常，其余返回 null 放行
 *         if (ex instanceof ModuleSampleWebException webEx) {
 *             return ApiResult.error(webEx.getErrorCode(), webEx.getArgs());
 *         }
 *         return null;
 *     }
 *
 *     @ExceptionHandler(ModuleSampleWebException.class)
 *     public ApiResult<?> handleModuleSampleWebException(ModuleSampleWebException ex) {
 *         // advice 通道：Spring 精确匹配优先于核心兜底
 *         return ApiResult.error(ex.getErrorCode(), ex.getArgs());
 *     }
 * }
 * }</pre>
 *
 * @author keep simple
 * @since 2025/7/11
 */
public interface ModuleExceptionHandler extends ExceptionHandlerSupport {

    /**
     * 模块异常处理入口。
     * @param request 请求
     * @param ex 异常
     * @return 通用返回结果；返回 null 表示不处理，交由后续处理器
     */
    ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex);
}
