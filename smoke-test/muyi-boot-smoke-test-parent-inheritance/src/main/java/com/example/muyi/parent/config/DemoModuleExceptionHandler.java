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
package com.example.muyi.parent.config;

import io.github.muyitech.boot.webmvc.exception.ModuleExceptionHandler;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 模块级异常处理器示例（空实现，全部放行）。
 *
 * <p>
 * 演示框架扩展点：实现 {@link ModuleExceptionHandler} 并注册为 Bean，核心处理器
 * {@code CoreExceptionHandler} 会优先调用它；返回 {@code null} 表示不处理，交回核心兜底。
 *
 * <p>
 * 声明方式与 {@link DemoCustomModuleExceptionHandler} 一致：类上以
 * {@code @RestControllerAdvice} 声明（同时满足 SPI 收集与 advice 注册两条通道）。
 * 排序 {@code @Order(Ordered.LOWEST_PRECEDENCE - 1)}：业务扩展处理器统一排在框架核心
 * 兜底（{@code CoreExceptionHandler}，{@code LOWEST_PRECEDENCE}）之前一格，保证扩展优先于
 * 兜底；本示例为纯放行实现，无专有异常可拦截，因此不声明通道一（@ExceptionHandler）方法，
 * 仅走通道二（SPI）。
 *
 * <p>
 * 业务方可在此处理自己模块的专有异常，例如：
 *
 * <pre>{@code
 * if (ex instanceof OrderNotFoundException) {
 *     return ApiResult.error(ORDER_NOT_FOUND);
 * }
 * return null;
 * }</pre>
 *
 * @author keep simple
 * @since 2026/9/12
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DemoModuleExceptionHandler implements ModuleExceptionHandler {

    @Override
    public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        // 示例：不拦截任何异常，全部交给核心处理器兜底
        return null;
    }
}
