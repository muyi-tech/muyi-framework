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
package io.github.muyitech.boot.exception.support;

import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;

import io.github.muyitech.boot.errorcode.ErrorCodeMessages;
import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.pojo.ApiResult;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异常处理共享扩展接口（core，无 Web 栈依赖）。
 *
 * <p>
 * WebMVC 与 WebFlux 两端异常处理器的共享逻辑上移至此：ServiceException 日志降噪
 * （名单与栈帧数由各栈 Properties 注入，框架零内置名单）、错误码本地化（按请求
 * Locale 解析，注册表命中则用注册表文案 + i18n）、cause 链兜底与 cause 穿透查找工具。
 *
 * <p>
 * 两端的 {@code @ExceptionHandler} 方法退化为薄壳，委托本接口的 default 方法，
 * 消除双栈重复逻辑且可独立单元测试。
 *
 * @author keep simple
 * @since 2026/9/13
 */
public interface ExceptionHandlerSupport {

    /**
     * 共享日志。
     */
    Logger LOG = LoggerFactory.getLogger(ExceptionHandlerSupport.class);

    /**
     * ServiceException 统一处理：日志降噪后构造错误结果。
     *
     * <p>
     * 降噪行为全部配置化（框架零内置名单）：ignore-messages 命中则不打印日志；
     * 打印时只取前 stack-trace-frames 个非工具类栈帧（0 = 静默）。响应构造按注册表
     * 命中与否本地化：命中 → 按请求 Locale 解析注册表文案（i18n/热更新即时生效）；
     * 未命中 → 原样输出异常自带 message。
     *
     * @param ex 业务异常
     * @param ignoreMessages 不打印日志的消息名单（null 视为空；List/Set 均可）
     * @param stackTraceFrames 打印栈帧数（0 = 静默，负数同 0）
     * @param locale 请求 Locale（响应构造时解析；可为 null，null 时用默认消息）
     * @param registry 错误码注册表（可为 null，null 时原样输出异常消息）
     * @return 错误结果
     */
    default ApiResult<?> handleServiceException(
            ServiceException ex,
            @Nullable Collection<String> ignoreMessages,
            int stackTraceFrames,
            @Nullable Locale locale,
            @Nullable ErrorCodeRegistry registry) {
        logServiceException(ex, ignoreMessages, stackTraceFrames);

        // 响应构造：注册表命中则按 Locale 解析（文案热更新与 i18n 即时生效），未命中原样输出
        String message = ex.getMessage();
        if (registry != null) {
            ErrorCode definition = registry.find(ex.getCode()).orElse(null);
            if (definition != null) {
                message = ErrorCodeMessages.resolve(definition, locale, ex.getArgs());
            }
        }
        return ApiResult.error(ex.getCode(), message);
    }

    /**
     * 兜底处理未知异常：cause 链上存在 {@link ServiceException} 时委托业务异常处理，
     * 否则按系统异常返回 500（注册表命中时同样本地化）。
     *
     * @param ex 未知异常
     * @param ignoreMessages 不打印日志的消息名单（null 视为空；委托业务异常时透传；List/Set 均可）
     * @param stackTraceFrames 打印栈帧数（委托业务异常时透传）
     * @param locale 请求 Locale（可为 null，null 时用默认消息）
     * @param registry 错误码注册表（可为 null）
     * @return 错误结果
     */
    default ApiResult<?> handleUnexpectedException(
            Throwable ex,
            @Nullable Collection<String> ignoreMessages,
            int stackTraceFrames,
            @Nullable Locale locale,
            @Nullable ErrorCodeRegistry registry) {
        // 特殊：cause 链上包含 ServiceException 时，视为业务异常直接返回
        ServiceException cause = findCause(ex.getCause(), ServiceException.class);
        if (Objects.nonNull(cause)) {
            return handleServiceException(
                    cause, ignoreMessages, stackTraceFrames, locale, registry);
        }

        // 处理异常
        LOG.error("[handleUnexpectedException]", ex);
        // 返回 ERROR ApiResult：注册表命中则本地化 500 文案，未命中用默认消息
        return ApiResult.error(
                INTERNAL_SERVER_ERROR.getCode(),
                resolveErrorCodeMessage(INTERNAL_SERVER_ERROR, locale, registry));
    }

    /**
     * 404 统一出口：WebMVC / WebFlux 两栈的 advice 分支与 WebFlux 独立
     * {@code MuyiNotFoundWebExceptionHandler} 共用，保证同一契约——
     * 消息恒为注册表解析的 NOT_FOUND 文案（默认"请求未找到"或业务翻译，随请求
     * Locale 与注册表热更新生效），<b>不携带路径与容器原始细节</b>；定位信息由各
     * 入口降级写入日志（warn 级），避免异常细节（如 {@code No static resource ...}
     * 容器消息、请求路径）透传给终端用户。
     *
     * @param locale 请求 Locale（可为 null，null 时用默认消息）
     * @param registry 错误码注册表（可为 null，null 时用错误码默认消息）
     * @return 404 错误结果
     */
    default ApiResult<?> handleNotFound(
            @Nullable Locale locale, @Nullable ErrorCodeRegistry registry) {
        return ApiResult.error(
                NOT_FOUND.getCode(), resolveErrorCodeMessage(NOT_FOUND, locale, registry));
    }

    /**
     * 解析错误码的展示消息：注册表命中则按请求 Locale 解析注册表文案（i18n/热更新即时
     * 生效），未命中（或无注册表）回退错误码默认消息。
     *
     * @param errorCode 错误码
     * @param locale 请求 Locale（可为 null，null 时用默认消息）
     * @param registry 错误码注册表（可为 null）
     * @return 展示消息
     */
    static String resolveErrorCodeMessage(
            ErrorCode errorCode, @Nullable Locale locale, @Nullable ErrorCodeRegistry registry) {
        if (registry == null) {
            return errorCode.getMsg();
        }
        return registry.find(errorCode.getCode())
                .map(definition -> ErrorCodeMessages.resolve(definition, locale, null))
                .orElse(errorCode.getMsg());
    }

    /**
     * ServiceException 日志降噪：名单命中不打印；打印时跳过工具类栈帧并限制帧数。
     * @param ex 业务异常
     * @param ignoreMessages 名单（null 视为空）
     * @param stackTraceFrames 帧数（0 或负数 = 静默）
     */
    private void logServiceException(
            ServiceException ex,
            @Nullable Collection<String> ignoreMessages,
            int stackTraceFrames) {
        if (stackTraceFrames <= 0) {
            return;
        }
        // 名单命中则不打印，避免 ex 堆栈过多（名单由业务方在 yaml 配置，框架零内置）
        if (ignoreMessages != null
                && ex.getMessage() != null
                && ignoreMessages.contains(ex.getMessage())) {
            return;
        }
        // 只打印前 N 个非工具类栈帧，并且使用 warn 在控制台输出，更容易看到
        try {
            StackTraceElement[] stackTraces = ex.getStackTrace();
            int printed = 0;
            for (StackTraceElement stackTrace : stackTraces) {
                if (Objects.equals(
                        stackTrace.getClassName(), ServiceExceptionUtil.class.getName())) {
                    continue;
                }
                LOG.warn("[handleServiceException]\n\t{}", stackTrace);
                printed++;
                if (printed >= stackTraceFrames) {
                    break;
                }
            }
        } catch (Exception ignored) {
            // 忽略日志，避免影响主流程
        }
    }

    /**
     * 沿 cause 链（含自身）查找指定类型的异常，防止循环引用。
     *
     * <p>
     * WebFlux 的 JSON 解码失败需要穿透
     * {@code ServerWebInputException → DecodingException → InvalidFormatException}
     * 两层包装，统一用本方法处理。
     *
     * @param <T> 目标异常类型
     * @param ex 起始异常
     * @param type 目标异常类型
     * @return 链上第一个匹配的异常；未匹配或入参为 null 时返回 null
     */
    static <T extends Throwable> @Nullable T findCause(
            @Nullable Throwable ex, @Nullable Class<T> type) {
        if (ex == null || type == null) {
            return null;
        }
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = ex;
        while (current != null && seen.add(current)) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
