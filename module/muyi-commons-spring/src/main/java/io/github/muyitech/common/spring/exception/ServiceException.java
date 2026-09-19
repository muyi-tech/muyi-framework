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
package io.github.muyitech.common.spring.exception;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.springframework.core.NestedRuntimeException;

/**
 * 业务逻辑异常，业务层到 ApiResult 之间数据传递的通道类。
 *
 * <p>
 * 继承 {@link NestedRuntimeException}，以获得完整的异常原因链能力（如
 * {@link #getMostSpecificCause()}、{@link #contains(Class)}）；message 由父类承载，
 * {@link #getMessage()} 返回构造时传入的业务错误提示，不拼接 "nested exception" 后缀。
 *
 * <p>
 * <b>构造后终态</b>：所有字段 final、无 setter；{@code args} 进出做防御性拷贝，
 * 异常一旦抛出即为不可变快照，可安全跨线程传播。空构造仅为满足序列化框架构造约定，
 * Java 序列化回读经反射直填字段、不破坏终态。
 *
 * @author keep simple
 * @since 2025/5/21
 */
public final class ServiceException extends NestedRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码（字符串形态，数字码或字符串码）；空构造时为 null。
     */
    private final @Nullable String code;

    /**
     * 消息格式化参数（替换模版中的 {} 占位符），供响应端 i18n 重新格式化；可为 null。
     */
    private final Object @Nullable [] args;

    /**
     * 空构造方法，避免反序列化问题（字段回读由序列化机制直填）。
     */
    public ServiceException() {
        super(null);
        this.code = null;
        this.args = null;
    }

    /**
     * 根据错误码构造业务异常。
     * @param errorCode 错误码
     */
    public ServiceException(ErrorCode errorCode) {
        this(errorCode, (Throwable) null);
    }

    /**
     * 根据错误码与原因构造业务异常。
     * @param errorCode 错误码
     * @param cause 原始异常（保留异常链，供 {@link #getMostSpecificCause()} / {@link #contains(Class)}）
     */
    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
        this.args = null;
    }

    /**
     * 根据错误码与提示构造业务异常。
     * @param code 业务错误码（字符串形态）
     * @param message 错误提示
     */
    public ServiceException(String code, String message) {
        this(code, message, (Throwable) null);
    }

    /**
     * 根据错误码、提示与原因构造业务异常。
     * @param code 业务错误码（字符串形态）
     * @param message 错误提示
     * @param cause 原始异常（保留异常链）
     */
    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = null;
    }

    /**
     * 根据错误码、提示与格式化参数构造业务异常。
     * @param code 业务错误码（字符串形态）
     * @param message 错误提示（已格式化或模版原文）
     * @param args 格式化参数（构造时做防御性拷贝；可为空）
     */
    public ServiceException(String code, String message, Object @Nullable ... args) {
        this(code, message, (Throwable) null, args);
    }

    /**
     * 根据错误码、提示、原因与格式化参数构造业务异常。
     * @param code 业务错误码（字符串形态）
     * @param message 错误提示（已格式化或模版原文）
     * @param cause 原始异常（保留异常链）
     * @param args 格式化参数（构造与读取时各做防御性拷贝；可为空）
     */
    public ServiceException(
            String code, String message, Throwable cause, Object @Nullable ... args) {
        super(message, cause);
        this.code = code;
        this.args = args == null || args.length == 0 ? null : args.clone();
    }

    /**
     * 获取业务错误码。
     * @return 业务错误码（字符串形态），未设置时为 null
     */
    public @Nullable String getCode() {
        return code;
    }

    /**
     * 获取消息格式化参数。
     * @return 格式化参数副本（可为 null）；对返回数组的修改不影响异常内部状态
     */
    public Object @Nullable [] getArgs() {
        return args == null ? null : args.clone();
    }

    @Override
    public String toString() {
        return "ServiceException(code="
                + code
                + ", message="
                + getMessage()
                + ", args="
                + Arrays.deepToString(args)
                + ")";
    }
}
