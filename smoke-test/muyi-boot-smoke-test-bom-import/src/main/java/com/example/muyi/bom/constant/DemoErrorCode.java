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
package com.example.muyi.bom.constant;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;

/**
 * 示例业务错误码常量（BOM 导入示例应用）。
 *
 * <p>
 * 本应用未配置错误码 YAML 注册，常量默认文案即最终响应文案；
 * {@link #MODULE_ERROR} 由 {@code DemoCustomModuleExceptionHandler} 拦截并转换为自定义响应。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public final class DemoErrorCode {

    /**
     * 模块演示错误：由 {@code DemoCustomModuleExceptionHandler} 拦截并转换为自定义响应。
     */
    public static final ErrorCode MODULE_ERROR =
            new ErrorCode("1001001001", "模块级异常处理器演示：业务专有异常已被自定义拦截");

    private DemoErrorCode() {}
}
