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
package com.example.muyi.parent.constant;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;

/**
 * 示例业务错误码常量。
 *
 * <p>
 * code 与默认文案与 {@code muyi-error-codes.yaml} 注册的定义保持一致；运行时注册表
 * （LAST_WINS 策略）以文件定义为准（含多语言 messages），常量仅提供无注册表时的兜底。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public final class DemoErrorCode {

    /**
     * 用户不存在：YAML 中注册多语言文案（zh-CN / en-US），运行时按请求 Locale 解析。
     */
    public static final ErrorCode USER_NOT_FOUND = new ErrorCode("1001001000", "用户 {} 不存在");

    /**
     * 模块演示错误：未注册进 {@code muyi-error-codes.yaml}，演示注册表缺项时回退常量默认
     * 文案；由 {@code DemoCustomModuleExceptionHandler} 拦截并转换为自定义响应。
     */
    public static final ErrorCode MODULE_ERROR =
            new ErrorCode("1001001001", "模块级异常处理器演示：业务专有异常已被自定义拦截");

    private DemoErrorCode() {}
}
