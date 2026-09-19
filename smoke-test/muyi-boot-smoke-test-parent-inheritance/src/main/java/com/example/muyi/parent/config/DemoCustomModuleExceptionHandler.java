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

import com.example.muyi.parent.exception.DemoModuleException;
import io.github.muyitech.boot.webmvc.exception.ModuleExceptionHandler;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 模块级异常处理器自定义实现示例（双通道）。
 *
 * <p>
 * 与 {@link DemoModuleExceptionHandler}（空实现，全部放行）互补：拦截本模块专有的
 * {@link DemoModuleException} 并返回自定义响应；其余异常返回 {@code null} 放行，
 * 交回核心兜底。
 *
 * <p>
 * 同一实现注册两条通道：
 * <ul>
 * <li>通道一（@ExceptionHandler）：Spring MVC 全局异常处理直接分发，不依赖核心
 * 处理器接线，按 advice 顺序（HIGHEST_PRECEDENCE）抢在核心兜底前翻译专有异常；</li>
 * <li>通道二（SPI 扩展点）：实现 {@link ModuleExceptionHandler}，由核心处理器
 * 收集分发，返回 {@code null} 放行交回核心兜底。</li>
 * </ul>
 *
 * @author keep simple
 * @since 2026/9/14
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DemoCustomModuleExceptionHandler implements ModuleExceptionHandler {

    /**
     * 通道一：Spring MVC 全局异常处理（按异常类型匹配，不匹配不进入本方法）。
     * @param ex 业务专有异常
     * @return 错误结果
     */
    @ExceptionHandler(DemoModuleException.class)
    public ApiResult<?> handleDemoModuleException(DemoModuleException ex) {
        return ApiResult.error(ex.getErrorCode());
    }

    /**
     * 通道二：SPI 扩展点（核心处理器收集分发；其余异常返回 null 放行交回核心兜底）。
     */
    @Override
    public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        if (ex instanceof DemoModuleException moduleEx) {
            return ApiResult.error(moduleEx.getErrorCode());
        }
        return null;
    }
}
