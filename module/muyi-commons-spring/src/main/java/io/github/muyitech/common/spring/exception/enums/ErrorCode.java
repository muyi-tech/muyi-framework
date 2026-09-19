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
package io.github.muyitech.common.spring.exception.enums;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 错误码对象。
 *
 * <p>
 * 码值为字符串形态：数字码（主轨，如 {@code "0"}、{@code "500"}、10 位四段业务码
 * {@code "1010002000"}）与字符串码（辅轨，SCREAMING_SNAKE_CASE，如
 * {@code "WECHAT_API_ERROR"}）统一以 String 承载。全局错误码参见
 * {@link GlobalErrorCodeConstants}；业务错误码分段与登记规范见框架设计文档第 4 章。
 *
 * <p>
 * {@link #messages} 携带多语言文案（key 为 BCP 47 语言标签，如 {@code zh-CN} /
 * {@code en-US}），可为空——响应端按请求 Locale 解析，未命中回退默认 {@link #msg}。
 *
 * <p>
 * 值对象语义：equals/hashCode 按 {@link #code} 实现（码即身份）。
 *
 * @author keep simple
 * @since 2025/5/21
 */
public class ErrorCode {

    /**
     * 错误码（字符串形态）。
     */
    private final String code;

    /**
     * 错误提示（默认语言）。
     */
    private final String msg;

    /**
     * 多语言文案（key = BCP 47 语言标签），可为空 Map。
     */
    private final Map<String, String> messages;

    /**
     * 构造错误码对象（无多语言文案）。
     * @param code 错误码
     * @param message 错误提示
     */
    public ErrorCode(String code, String message) {
        this(code, message, Map.of());
    }

    /**
     * 构造错误码对象。
     * @param code 错误码
     * @param message 错误提示
     * @param messages 多语言文案（可为 null，归一为空 Map）
     */
    public ErrorCode(String code, String message, @Nullable Map<String, String> messages) {
        this.code = code;
        this.msg = message;
        this.messages = messages == null ? Map.of() : Map.copyOf(messages);
    }

    /**
     * 获取错误码。
     * @return 错误码（字符串形态）
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取错误提示（默认语言）。
     * @return 错误提示
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 获取多语言文案。
     * @return 多语言文案（key = BCP 47 语言标签），永不为 null
     */
    public Map<String, String> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorCode)) {
            return false;
        }
        ErrorCode that = (ErrorCode) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return "ErrorCode(code=" + code + ", msg=" + msg + ", messages=" + messages + ")";
    }
}
