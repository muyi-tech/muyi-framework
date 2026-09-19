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
package io.github.muyitech.boot.errorcode;

import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 错误码多语言解析器（静态工具，纯函数）。
 *
 * <p>
 * 解析链（三级回退）：
 *
 * <pre>
 * messages.get(locale.toLanguageTag())      // 精确匹配 zh-CN
 * └─ 未命中 → messages.get(locale.getLanguage())   // 语言级回退 zh
 *    └─ 未命中 → errorCode.getMsg()                // 默认消息兜底
 * → ServiceExceptionUtil.doFormat(...)      // 复用现有 {} 格式化
 * </pre>
 *
 * <p>
 * 分层约定：库层 API（{@code ApiResult.error(ErrorCode, args)} 等）保持默认
 * {@code msg}——无请求上下文就不猜语言；Web 层在响应构造时按请求 Locale 调用本类。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public final class ErrorCodeMessages {

    private ErrorCodeMessages() {}

    /**
     * 解析错误码的本地化消息（无格式化参数）。
     * @param errorCode 错误码定义（可为 null）
     * @param locale 请求 Locale（可为 null，null 时用默认消息）
     * @return 本地化消息；errorCode 为 null 返回 null
     */
    public static @Nullable String resolve(@Nullable ErrorCode errorCode, @Nullable Locale locale) {
        return resolve(errorCode, locale, null);
    }

    /**
     * 解析错误码的本地化消息并格式化 {} 占位符。
     * @param errorCode 错误码定义（可为 null）
     * @param locale 请求 Locale（可为 null，null 时用默认消息）
     * @param args 格式化参数（可为 null）
     * @return 本地化消息；errorCode 为 null 返回 null
     */
    public static @Nullable String resolve(
            @Nullable ErrorCode errorCode, @Nullable Locale locale, Object @Nullable [] args) {
        if (errorCode == null) {
            return null;
        }
        Map<String, String> messages = errorCode.getMessages();
        String template = null;
        // ErrorCode 构造器已归一 messages 恒非 null，无需判空
        if (locale != null && !messages.isEmpty()) {
            // 精确匹配 zh-CN；未命中回退语言级 zh
            template = messages.get(locale.toLanguageTag());
            if (template == null) {
                template = messages.get(locale.getLanguage());
            }
        }
        if (template == null) {
            template = errorCode.getMsg();
        }
        return format(errorCode.getCode(), template, args);
    }

    /**
     * 按 {@code {}} 占位符格式化消息（与 {@code ServiceExceptionUtil.doFormat} 一致）。
     * @param code 错误码（日志用）
     * @param template 消息模版
     * @param args 格式化参数（可为 null）
     * @return 格式化后的消息
     */
    public static String format(String code, String template, Object @Nullable [] args) {
        if (args == null || args.length == 0) {
            return template;
        }
        return ServiceExceptionUtil.doFormat(code, template, args);
    }
}
