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
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServiceException} 工具类。
 *
 * @author keep simple
 * @since 2025/5/21
 */
public class ServiceExceptionUtil {

    private static final Logger log = LoggerFactory.getLogger(ServiceExceptionUtil.class);

    private ServiceExceptionUtil() {}

    // ========== 和 ServiceException 的集成 ==========

    /**
     * 根据错误码创建业务异常。
     * @param errorCode 错误码
     * @return 业务异常
     */
    public static ServiceException exception(ErrorCode errorCode) {
        return exception0(errorCode.getCode(), errorCode.getMsg());
    }

    /**
     * 根据错误码与格式化参数创建业务异常。
     * @param errorCode 错误码
     * @param params 格式化参数，替换消息模版中的 {} 占位符
     * @return 业务异常
     */
    public static ServiceException exception(ErrorCode errorCode, Object... params) {
        return exception0(errorCode.getCode(), errorCode.getMsg(), params);
    }

    /**
     * 根据错误码与原因创建业务异常（保留异常链）。
     * @param errorCode 错误码
     * @param cause 原始异常
     * @return 业务异常
     */
    public static ServiceException exception(ErrorCode errorCode, Throwable cause) {
        return new ServiceException(errorCode, cause);
    }

    /**
     * 创建业务异常。
     * @param code 业务错误码（字符串形态）
     * @param messagePattern 消息模版
     * @param params 格式化参数
     * @return 业务异常
     */
    public static ServiceException exception0(
            String code, String messagePattern, Object... params) {
        String message = doFormat(code, messagePattern, params);
        return new ServiceException(code, message, params);
    }

    /**
     * 创建携带原始异常的业务异常（保留异常链，供排查）。
     * @param code 业务错误码（字符串形态）
     * @param messagePattern 消息模版
     * @param cause 原始异常
     * @param params 格式化参数
     * @return 业务异常
     */
    public static ServiceException exception0(
            String code, String messagePattern, Throwable cause, Object... params) {
        String message = doFormat(code, messagePattern, params);
        return new ServiceException(code, message, cause, params);
    }

    /**
     * 创建参数不正确的业务异常（错误码为 {@link GlobalErrorCodeConstants#BAD_REQUEST}）。
     * @param messagePattern 消息模版
     * @param params 格式化参数
     * @return 业务异常
     */
    public static ServiceException invalidParamException(String messagePattern, Object... params) {
        return exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), messagePattern, params);
    }

    // ========== 格式化方法 ==========

    /**
     * 将错误编号对应的消息使用 params 进行格式化。
     *
     * <p>
     * 防御约定：{@code messagePattern} 为 null 时返回 null（不抛 NPE，交由上层消息语义兜底）；
     * 无参数（null 或空数组）时原样返回模版、不做占位符检查——默认消息携带
     * {@code {}} 占位符属正常形态（占位符由响应端 i18n 阶段填充），不视为"参数过少"。
     *
     * @param code 错误编号（字符串形态）
     * @param messagePattern 消息模版（可为 null，返回 null）
     * @param params 参数（可为 null 或空数组——原样返回模版）
     * @return 格式化后的提示；模版为 null 时返回 null
     */
    public static @Nullable String doFormat(
            String code, @Nullable String messagePattern, Object @Nullable ... params) {
        if (messagePattern == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            return messagePattern;
        }
        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
        int i = 0;
        int j;
        int l;
        for (l = 0; l < params.length; l++) {
            j = messagePattern.indexOf("{}", i);
            if (j == -1) {
                log.warn("[doFormat][参数过多：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
                if (i == 0) {
                    return messagePattern;
                } else {
                    sbuf.append(messagePattern.substring(i));
                    return sbuf.toString();
                }
            } else {
                sbuf.append(messagePattern, i, j);
                sbuf.append(params[l]);
                i = j + 2;
            }
        }
        if (messagePattern.indexOf("{}", i) != -1) {
            log.warn("[doFormat][参数过少：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
        }
        sbuf.append(messagePattern.substring(i));
        return sbuf.toString();
    }
}
