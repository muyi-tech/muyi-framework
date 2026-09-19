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
package io.github.muyitech.boot.webflux.exception;

import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.METHOD_NOT_ALLOWED;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;

import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.exception.support.ExceptionHandlerSupport;
import io.github.muyitech.boot.webflux.autoconfigure.MuyiWebFluxProperties;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * 核心模块异常处理基类（WebFlux 响应式）。
 *
 * <p>
 * 作为兜底的 {@link RestControllerAdvice}，处理参数校验、业务异常、HTTP 语义异常与系统异常；
 * 通过 {@link ReactiveModuleExceptionHandler} 支持各模块自定义异常处理的扩展。
 *
 * <p>
 * 与 Servlet 版的关键差异：
 *
 * <ul>
 *   <li>事件循环模型无 {@code RequestContextHolder}，方法签名统一携带 {@link ServerWebExchange}；
 *   <li>404 双出口——advice 的 {@code ResponseStatusException} 分支与 WebExceptionHandler 链兜底的
 *       {@link MuyiNotFoundWebExceptionHandler}——共用 {@link ExceptionHandlerSupport#handleNotFound}
 *       统一文案（注册表解析、不透传容器细节）；
 *   <li>缺失参数/类型错误/请求体解析失败统一映射为 {@link ServerWebInputException} 分支；
 *   <li>JSON 解码失败需穿透 {@code ServerWebInputException → DecodingException → InvalidFormatException}
 *       两层 cause，统一走 {@link ExceptionHandlerSupport#findCause}。
 * </ul>
 *
 * <p>
 * 业务异常日志降噪与系统异常 cause 链兜底等共享逻辑上移至
 * {@link ExceptionHandlerSupport}，本类方法为薄壳委托。
 *
 * <p>
 * 排序契约：本类以 {@code @Order(Ordered.LOWEST_PRECEDENCE)} 兜底殿后。业务方以
 * {@code @RestControllerAdvice} 声明的模块处理器应使用更高优先级——专有异常拦截用
 * {@code HIGHEST_PRECEDENCE}，一般业务扩展用 {@code LOWEST_PRECEDENCE - 1}，确保扩展
 * 优先于兜底。SPI 通道（{@link ReactiveModuleExceptionHandler} 收集分发）的顺序独立于
 * advice 排序，实际装载结果以启动日志为准。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE) // 最低优先级
public class ReactiveCoreExceptionHandler implements ExceptionHandlerSupport {

    private static final Logger log = LoggerFactory.getLogger(ReactiveCoreExceptionHandler.class);

    /**
     * 模块级异常处理器（可选依赖）。
     *
     * <p>
     * 经 {@link ObjectProvider} 延迟收集：容器内没有模块处理器 Bean 时正常注入空流，
     * 不影响应用启动（{@code @Resource List} 在无候选 Bean 时会抛
     * NoSuchBeanDefinitionException 导致启动失败）。
     */
    @Autowired private ObjectProvider<ReactiveModuleExceptionHandler> moduleExceptionHandlers;

    /**
     * 错误码注册表（可选依赖）：容器内无注册表 Bean 时为空流，响应回退异常自带消息。
     */
    @Autowired private ObjectProvider<ErrorCodeRegistry> errorCodeRegistries;

    /**
     * 启动时输出模块级异常处理器清单（集成方可观测性）。
     *
     * <p>
     * 多模块集成的"集成方的眼睛"：启动即暴露本次集成装载了哪些处理器及其生效顺序；
     * 首个返回非 null 结果的处理器短路生效。未标注 {@code @Order} 也不实现
     * {@link Ordered} 的处理器，顺序由 Spring 语义兜底为最低优先级（跨 jar 不保证），
     * 日志直接显示兜底值以便集成方识别遗漏。
     */
    @PostConstruct
    private void logModuleHandlers() {
        List<ReactiveModuleExceptionHandler> handlers =
                moduleExceptionHandlers.orderedStream().toList();
        if (handlers.isEmpty()) {
            log.debug("未装载模块级异常处理器，所有异常由核心处理器兜底");
            return;
        }
        List<String> descriptions =
                handlers.stream().map(ReactiveCoreExceptionHandler::describe).toList();
        log.info("已装载 {} 个模块级异常处理器（按优先级排序，首个非 null 结果短路生效）: {}", handlers.size(), descriptions);
    }

    private static String describe(ReactiveModuleExceptionHandler handler) {
        int order =
                handler instanceof Ordered ordered
                        ? ordered.getOrder()
                        : OrderUtils.getOrder(handler.getClass(), Ordered.LOWEST_PRECEDENCE);
        return handler.getClass().getName() + "(order=" + order + ")";
    }

    /**
     * 异常处理配置项：业务异常日志降噪（ignore-messages / stack-trace-frames）。
     */
    @Autowired private MuyiWebFluxProperties properties;

    /**
     * 统一异常处理入口，按类型分发到具体的异常处理方法。
     *
     * <p>
     * 定位为<b>编程式统一入口</b>：不承载 {@code @ExceptionHandler} 注解，Spring 请求主链路
     * 走本类各精确匹配方法与 {@link #defaultExceptionHandler} 兜底；本入口供显式调用场景。
     *
     * <p>
     * ⚠️ 头部 SPI 分发与回落 {@link #defaultExceptionHandler} 的兜底分发叠加时，同一异常会
     * 询问模块处理器两次（直接走 advice 主链路则为一次）；模块处理器应无副作用状态。
     *
     * @param exchange 当前请求交换
     * @param ex 异常
     * @return 通用返回结果
     */
    public ApiResult<?> allExceptionHandler(ServerWebExchange exchange, Throwable ex) {
        ApiResult<?> moduleResult = firstModuleResult(exchange, ex);
        if (moduleResult != null) {
            return moduleResult;
        }
        if (ex instanceof WebExchangeBindException webExchangeBindException) {
            return webExchangeBindExceptionHandler(webExchangeBindException);
        }
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidExceptionExceptionHandler(methodArgumentNotValidException);
        }
        if (ex instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationExceptionHandler(constraintViolationException);
        }
        if (ex instanceof ValidationException validationException) {
            return validationExceptionHandler(validationException);
        }
        if (ex instanceof DataBufferLimitException dataBufferLimitException) {
            return dataBufferLimitExceptionHandler(dataBufferLimitException);
        }
        if (ex instanceof ServerWebInputException serverWebInputException) {
            return serverWebInputExceptionHandler(serverWebInputException);
        }
        if (ex instanceof UnsupportedMediaTypeStatusException unsupportedMediaTypeStatusException) {
            return unsupportedMediaTypeStatusExceptionHandler(unsupportedMediaTypeStatusException);
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusExceptionHandler(responseStatusException, exchange);
        }
        if (ex instanceof ServiceException serviceException) {
            return serviceExceptionHandler(serviceException, exchange);
        }
        MuyiWebFluxProperties.ServiceExceptionLog noise = properties.getServiceExceptionLog();
        return handleUnexpectedException(
                ex,
                noise.getIgnoreMessages(),
                noise.getStackTraceFrames(),
                resolveLocale(exchange),
                findRegistry());
    }

    /**
     * 处理 WebFlux 表单绑定/参数校验不正确。
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ApiResult<?> webExchangeBindExceptionHandler(WebExchangeBindException ex) {
        log.warn("[webExchangeBindExceptionHandler]", ex);
        FieldError fieldError = ex.getFieldError();
        if (fieldError != null) {
            return ApiResult.error(
                    BAD_REQUEST.getCode(),
                    String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
        }
        List<ObjectError> allErrors = ex.getAllErrors();
        if (CollectionUtils.isNotEmpty(allErrors)) {
            return ApiResult.error(
                    BAD_REQUEST.getCode(),
                    String.format("请求参数不正确:%s", allErrors.get(0).getDefaultMessage()));
        }
        return ApiResult.error(BAD_REQUEST);
    }

    /**
     * 处理 WebFlux 参数校验不正确。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<?> methodArgumentNotValidExceptionExceptionHandler(
            MethodArgumentNotValidException ex) {
        log.warn("[methodArgumentNotValidExceptionExceptionHandler]", ex);
        // 获取 errorMessage
        String errorMessage = null;
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError == null) {
            // 组合校验
            List<ObjectError> allErrors = ex.getBindingResult().getAllErrors();
            if (CollectionUtils.isNotEmpty(allErrors)) {
                errorMessage = allErrors.get(0).getDefaultMessage();
            }
        } else {
            errorMessage = fieldError.getDefaultMessage();
        }
        // 转换 ApiResult
        if (StringUtils.isEmpty(errorMessage)) {
            return ApiResult.error(BAD_REQUEST);
        }
        return ApiResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", errorMessage));
    }

    /**
     * 处理 Validator 校验不通过产生的异常。
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ApiResult<?> constraintViolationExceptionHandler(ConstraintViolationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        ConstraintViolation<?> constraintViolation = ex.getConstraintViolations().iterator().next();
        return ApiResult.error(
                BAD_REQUEST.getCode(),
                String.format("请求参数不正确:%s", constraintViolation.getMessage()));
    }

    /**
     * 本地参数校验时，抛出的 ValidationException 异常。
     */
    @ExceptionHandler(value = ValidationException.class)
    public ApiResult<?> validationExceptionHandler(ValidationException ex) {
        log.warn("[validationExceptionHandler]", ex);
        return ApiResult.error(BAD_REQUEST);
    }

    /**
     * 处理上传文件过大异常（WebFlux 对应 {@link DataBufferLimitException}）。
     */
    @ExceptionHandler(DataBufferLimitException.class)
    public ApiResult<?> dataBufferLimitExceptionHandler(DataBufferLimitException ex) {
        log.warn("[dataBufferLimitExceptionHandler]", ex);
        return ApiResult.error(BAD_REQUEST.getCode(), "上传文件过大，请调整后重试");
    }

    /**
     * 处理请求输入异常（WebFlux 将缺失参数、类型转换失败、请求体解析失败统一映射为
     * {@link ServerWebInputException}）。
     *
     * <p>
     * JSON 解码失败通过 {@link ExceptionHandlerSupport#findCause} 穿透两层 cause：
     * {@code ServerWebInputException → DecodingException → InvalidFormatException}。
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ApiResult<?> serverWebInputExceptionHandler(ServerWebInputException ex) {
        log.warn("[serverWebInputExceptionHandler]", ex);
        InvalidFormatException invalidFormatException =
                ExceptionHandlerSupport.findCause(ex, InvalidFormatException.class);
        if (invalidFormatException != null) {
            return ApiResult.error(
                    BAD_REQUEST.getCode(),
                    String.format("请求参数类型错误:%s", invalidFormatException.getValue()));
        }
        String reason = ex.getReason();
        if (reason != null && reason.contains("body is missing")) {
            return ApiResult.error(BAD_REQUEST.getCode(), "请求参数类型错误: request body 缺失");
        }
        if (StringUtils.isNotEmpty(reason)) {
            return ApiResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", reason));
        }
        return ApiResult.error(BAD_REQUEST);
    }

    /**
     * 处理请求的 Content-Type 不正确。
     *
     * <p>
     * 例如说，A 接口的 Content-Type 为 application/json，结果请求的 Content-Type 为
     * application/octet-stream，导致不匹配。
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ApiResult<?> unsupportedMediaTypeStatusExceptionHandler(
            UnsupportedMediaTypeStatusException ex) {
        log.warn("[unsupportedMediaTypeStatusExceptionHandler]", ex);
        return ApiResult.error(
                BAD_REQUEST.getCode(), String.format("请求类型不正确:%s", ex.getContentType()));
    }

    /**
     * 处理 {@link ResponseStatusException}（404/405/415 等 HTTP 语义异常），按状态码映射业务错误码。
     *
     * <p>
     * 404 走 {@link ExceptionHandlerSupport#handleNotFound(Locale, ErrorCodeRegistry)} 统一出口：
     * 恒为注册表解析的 NOT_FOUND 文案，容器细节（如 {@code No static resource ...}）不透传给终端
     * 用户，只随 {@code log.warn} 入日志；其余状态保持"码文案:原因"模板。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ApiResult<?> responseStatusExceptionHandler(
            ResponseStatusException ex, ServerWebExchange exchange) {
        log.warn("[responseStatusExceptionHandler]", ex);
        ErrorCode errorCode = errorCodeFor(ex.getStatusCode());
        if (errorCode == NOT_FOUND) {
            return handleNotFound(resolveLocale(exchange), findRegistry());
        }
        String detail = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ApiResult.error(
                errorCode.getCode(), String.format("%s:%s", errorCode.getMsg(), detail));
    }

    /**
     * 处理业务异常 ServiceException。
     *
     * <p>
     * 日志降噪、本地化与结果构造上移至 {@link ExceptionHandlerSupport#handleServiceException}，
     * 双栈共享。
     */
    @ExceptionHandler(value = ServiceException.class)
    public ApiResult<?> serviceExceptionHandler(ServiceException ex, ServerWebExchange exchange) {
        MuyiWebFluxProperties.ServiceExceptionLog noise = properties.getServiceExceptionLog();
        return handleServiceException(
                ex,
                noise.getIgnoreMessages(),
                noise.getStackTraceFrames(),
                resolveLocale(exchange),
                findRegistry());
    }

    /**
     * 处理系统异常，兜底处理所有的一切。
     *
     * <p>
     * 模块级处理器优先：进入兜底前先经 {@link #firstModuleResult} 分发 SPI 模块处理器，
     * 首个非 null 结果短路生效——模块扩展点由此接入 Spring 异常主链路（历史遗留的
     * 分发未接线缺陷，自 muyi-boot 继承，此处首次兑现）。全部放行后才落到 cause 链
     * 检查与 500 兜底（{@link ExceptionHandlerSupport#handleUnexpectedException}，双栈共享）。
     */
    @ExceptionHandler(value = Exception.class)
    public ApiResult<?> defaultExceptionHandler(Throwable ex, ServerWebExchange exchange) {
        ApiResult<?> moduleResult = firstModuleResult(exchange, ex);
        if (moduleResult != null) {
            return moduleResult;
        }
        MuyiWebFluxProperties.ServiceExceptionLog noise = properties.getServiceExceptionLog();
        return handleUnexpectedException(
                ex,
                noise.getIgnoreMessages(),
                noise.getStackTraceFrames(),
                resolveLocale(exchange),
                findRegistry());
    }

    /**
     * 依 {@code @Order} 优先级遍历 SPI 模块级处理器，返回首个非 null 结果。
     * @param exchange 当前请求交换
     * @param ex 异常
     * @return 首个处理结果；全部处理器放行（返回 null）时为 null
     */
    private ApiResult<?> firstModuleResult(ServerWebExchange exchange, Throwable ex) {
        if (Objects.isNull(moduleExceptionHandlers)) {
            return null;
        }
        return moduleExceptionHandlers
                .orderedStream()
                .map(
                        moduleExceptionHandler ->
                                moduleExceptionHandler.allExceptionHandler(exchange, ex))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析请求 Locale：优先 exchange 的 LocaleContext，回退 LocaleContextHolder / 默认值。
     * @param exchange 当前请求交换
     * @return 请求 Locale
     */
    private static Locale resolveLocale(ServerWebExchange exchange) {
        LocaleContext localeContext = exchange != null ? exchange.getLocaleContext() : null;
        if (localeContext != null && localeContext.getLocale() != null) {
            return localeContext.getLocale();
        }
        // LocaleContextHolder#getLocale 契约保证非 null（无上下文时内部回退 Locale.getDefault()），
        // 无需再判空——与 WebMVC 侧 CoreExceptionHandler#resolveLocale 双栈语义对称
        return LocaleContextHolder.getLocale();
    }

    /**
     * 获取错误码注册表（可选，无 Bean 时返回 null）。
     * @return 注册表或 null
     */
    private ErrorCodeRegistry findRegistry() {
        if (errorCodeRegistries == null) {
            return null;
        }
        return errorCodeRegistries.getIfAvailable();
    }

    /**
     * HTTP 状态码映射业务错误码：404/405/5xx 对应专用错误码，其余 4xx 归为 BAD_REQUEST。
     * @param status HTTP 状态码
     * @return 业务错误码
     */
    private static ErrorCode errorCodeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 500 -> INTERNAL_SERVER_ERROR;
            default -> status.is5xxServerError() ? INTERNAL_SERVER_ERROR : BAD_REQUEST;
        };
    }
}
