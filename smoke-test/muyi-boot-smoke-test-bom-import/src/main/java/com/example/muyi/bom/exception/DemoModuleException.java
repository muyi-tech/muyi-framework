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
package com.example.muyi.bom.exception;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;

/**
 * 业务专有异常示例：非 {@code ServiceException} 体系（{@code ServiceException} 为 final 类，
 * 不可继承），核心处理器不识别该类型——若无人拦截将落入核心兜底；由
 * {@code DemoCustomModuleExceptionHandler} 在核心兜底前拦截并翻译为自定义响应。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public class DemoModuleException extends RuntimeException {

    /**
     * 业务错误码。
     */
    private final ErrorCode errorCode;

    /**
     * 根据错误码构造业务专有异常。
     * @param errorCode 错误码
     */
    public DemoModuleException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    /**
     * 获取业务错误码。
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
