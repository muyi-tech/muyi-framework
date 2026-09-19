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
package io.github.muyitech.boot.webmvc.exception;

import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.METHOD_NOT_ALLOWED;

import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.exception.support.ExceptionHandlerSupport;
import io.github.muyitech.boot.webmvc.autoconfigure.MuyiWebMvcProperties;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * 核心模块异常处理基类（WebMVC）。
 *
 * <p>
 * 作为兜底的 {@link RestControllerAdvice}，处理 SpringMVC、参数校验、业务异常与系统异常；
 * 通过 {@link ModuleExceptionHandler} 支持各模块自定义异常处理的扩展。
 *
 * <p>
 * 业务异常日志降噪与系统异常 cause 链兜底等共享逻辑上移至
 * {@link ExceptionHandlerSupport}，本类方法为薄壳委托。
 *
 * <p>
 * 经 {@code MuyiWebMvcAutoConfiguration} 自动装配（业务方自定义同类型 Bean 时自动 back off）。
 *
 * <p>
 * 排序契约：本类以 {@code @Order(Ordered.LOWEST_PRECEDENCE)} 兜底殿后。业务方以
 * {@code @RestControllerAdvice} 声明的模块处理器应使用更高优先级——专有异常拦截用
 * {@code HIGHEST_PRECEDENCE}，一般业务扩展用 {@code LOWEST_PRECEDENCE - 1}，确保扩展
 * 优先于兜底。SPI 通道（{@link ModuleExceptionHandler} 收集分发）的顺序独立于 advice
 * 排序，实际装载结果以启动日志为准。
 *
 * @author keep simple
 * @since 2025/7/11
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE) // 最低优先级
public class CoreExceptionHandler implements ExceptionHandlerSupport {

    private static final Logger log = LoggerFactory.getLogger(CoreExceptionHandler.class);

    /**
     * 模块级异常处理器（可选依赖）。
     *
     * <p>
     * 经 {@link ObjectProvider} 延迟收集：容器内没有模块处理器 Bean 时正常注入空流，
     * 不影响应用启动（{@code @Resource List} 在无候选 Bean 时会抛
     * NoSuchBeanDefinitionException 导致启动失败）。
     */
    @Autowired private ObjectProvider<ModuleExceptionHandler> moduleExceptionHandlers;

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
        List<ModuleExceptionHandler> handlers = moduleExceptionHandlers.orderedStream().toList();
        if (handlers.isEmpty()) {
            log.debug("未装载模块级异常处理器，所有异常由核心处理器兜底");
            return;
        }
        List<String> descriptions = handlers.stream().map(CoreExceptionHandler::describe).toList();
        log.info("已装载 {} 个模块级异常处理器（按优先级排序，首个非 null 结果短路生效）: {}", handlers.size(), descriptions);
    }

    private static String describe(ModuleExceptionHandler handler) {
        int order =
                handler instanceof Ordered ordered
                        ? ordered.getOrder()
                        : OrderUtils.getOrder(handler.getClass(), Ordered.LOWEST_PRECEDENCE);
        return handler.getClass().getName() + "(order=" + order + ")";
    }

    /**
     * 异常处理配置项：业务异常日志降噪（ignore-messages / stack-trace-frames）。
     */
    @Autowired private MuyiWebMvcProperties properties;

    /**
     * 统一异常处理入口，按类型分发到具体的异常处理方法。
     *
     * <p>
     * 定位为<b>编程式统一入口</b>：不承载 {@code @ExceptionHandler} 注解，Spring 请求主链路
     * 走本类各精确匹配方法与 {@link #defaultExceptionHandler} 兜底；本入口供显式调用
     * （过滤器错误页、批处理重试等需要"一个异常换一份 ApiResult"的场景）。
     *
     * <p>
     * ⚠️ 头部 SPI 分发与回落 {@link #defaultExceptionHandler} 的兜底分发叠加时，同一异常会
     * 询问模块处理器两次（直接走 advice 主链路则为一次）；模块处理器应无副作用状态。
     *
     * @param request 请求
     * @param ex 异常
     * @return 通用返回结果
     */
    public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        ApiResult<?> moduleResult = firstModuleResult(request, ex);
        if (moduleResult != null) {
            return moduleResult;
        }
        if (ex instanceof MissingServletRequestParameterException) {
            return missingServletRequestParameterExceptionHandler(
                    (MissingServletRequestParameterException) ex);
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return methodArgumentTypeMismatchExceptionHandler(
                    (MethodArgumentTypeMismatchException) ex);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            return methodArgumentNotValidExceptionExceptionHandler(
                    (MethodArgumentNotValidException) ex);
        }
        if (ex instanceof BindException) {
            return bindExceptionHandler((BindException) ex);
        }
        if (ex instanceof ConstraintViolationException) {
            return constraintViolationExceptionHandler((ConstraintViolationException) ex);
        }
        if (ex instanceof ValidationException) {
            return validationException((ValidationException) ex);
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return maxUploadSizeExceededExceptionHandler((MaxUploadSizeExceededException) ex);
        }
        if (ex instanceof NoHandlerFoundException) {
            return noHandlerFoundExceptionHandler(request, (NoHandlerFoundException) ex);
        }
        if (ex instanceof NoResourceFoundException) {
            return noResourceFoundExceptionHandler(request, (NoResourceFoundException) ex);
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return httpRequestMethodNotSupportedExceptionHandler(
                    (HttpRequestMethodNotSupportedException) ex);
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return httpMediaTypeNotSupportedExceptionHandler(
                    (HttpMediaTypeNotSupportedException) ex);
        }
        if (ex instanceof ServiceException) {
            return serviceExceptionHandler(request, (ServiceException) ex);
        }
        return defaultExceptionHandler(request, ex);
    }

    /**
     * 处理 SpringMVC 请求参数缺失。
     *
     * <p>
     * 例如说，接口上设置了 @RequestParam("xx") 参数，结果并未传递 xx 参数。
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ApiResult<?> missingServletRequestParameterExceptionHandler(
            MissingServletRequestParameterException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return ApiResult.error(
                BAD_REQUEST.getCode(), String.format("请求参数缺失:%s", ex.getParameterName()));
    }

    /**
     * 处理 SpringMVC 请求参数类型错误。
     *
     * <p>
     * 例如说，接口上设置了 @RequestParam("xx") 参数为 Integer，结果传递 xx 参数类型为 String。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<?> methodArgumentTypeMismatchExceptionHandler(
            MethodArgumentTypeMismatchException ex) {
        log.warn("[methodArgumentTypeMismatchExceptionHandler]", ex);
        return ApiResult.error(
                BAD_REQUEST.getCode(), String.format("请求参数类型错误:%s", ex.getMessage()));
    }

    /**
     * 处理 SpringMVC 参数校验不正确。
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
        // 转换 CommonResult
        if (StringUtils.isEmpty(errorMessage)) {
            return ApiResult.error(BAD_REQUEST);
        }
        return ApiResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", errorMessage));
    }

    /**
     * 处理 SpringMVC 参数绑定不正确，本质上也是通过 Validator 校验。
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<?> bindExceptionHandler(BindException ex) {
        log.warn("[handleBindException]", ex);
        FieldError fieldError = ex.getFieldError();
        Assert.state(fieldError != null, "fieldError 不能为空"); // 断言，避免告警
        return ApiResult.error(
                BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
    }

    /**
     * 处理 SpringMVC 请求参数类型错误。
     *
     * <p>
     * 例如说，接口上设置了 @RequestBody 实体中 xx 属性类型为 Integer，结果传递 xx 参数类型为 String。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @SuppressWarnings("PatternVariableCanBeUsed")
    public ApiResult<?> methodArgumentTypeInvalidFormatExceptionHandler(
            HttpMessageNotReadableException ex) {
        log.warn("[methodArgumentTypeInvalidFormatExceptionHandler]", ex);
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) ex.getCause();
            return ApiResult.error(
                    BAD_REQUEST.getCode(),
                    String.format("请求参数类型错误:%s", invalidFormatException.getValue()));
        }
        if (Strings.CS.startsWith(ex.getMessage(), "Required request body is missing")) {
            return ApiResult.error(BAD_REQUEST.getCode(), "请求参数类型错误: request body 缺失");
        }
        return defaultExceptionHandler(getRequest(), ex);
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
    public ApiResult<?> validationException(ValidationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        return ApiResult.error(BAD_REQUEST);
    }

    /**
     * 处理上传文件过大异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException ex) {
        log.warn("[maxUploadSizeExceededExceptionHandler]", ex);
        return ApiResult.error(BAD_REQUEST.getCode(), "上传文件过大，请调整后重试");
    }

    /**
     * 处理 SpringMVC 请求地址不存在。
     *
     * <p>
     * 注意，它需要设置如下两个配置项：1. spring.mvc.throw-exception-if-no-handler-found 为 true 2.
     * spring.mvc.static-path-pattern 为 /statics/**
     *
     * <p>
     * 消息走 {@link ExceptionHandlerSupport#handleNotFound(Locale, ErrorCodeRegistry)} 统一出口：
     * 恒为注册表解析的 NOT_FOUND 文案，不携带请求 URL；URL 只降级写入日志（warn 级）。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResult<?> noHandlerFoundExceptionHandler(
            HttpServletRequest req, NoHandlerFoundException ex) {
        log.warn("[noHandlerFoundExceptionHandler]", ex);
        return handleNotFound(resolveLocale(req), findRegistry());
    }

    /**
     * 处理 SpringMVC 请求地址不存在。
     *
     * <p>
     * 消息走 {@link ExceptionHandlerSupport#handleNotFound(Locale, ErrorCodeRegistry)} 统一出口：
     * 恒为注册表解析的 NOT_FOUND 文案，不携带资源路径；路径只降级写入日志（warn 级）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    private ApiResult<?> noResourceFoundExceptionHandler(
            HttpServletRequest req, NoResourceFoundException ex) {
        log.warn("[noResourceFoundExceptionHandler]", ex);
        return handleNotFound(resolveLocale(req), findRegistry());
    }

    /**
     * 处理 SpringMVC 请求方法不正确。
     *
     * <p>
     * 例如说，A 接口的方法为 GET 方式，结果请求方法为 POST 方式，导致不匹配。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<?> httpRequestMethodNotSupportedExceptionHandler(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("[httpRequestMethodNotSupportedExceptionHandler]", ex);
        return ApiResult.error(
                METHOD_NOT_ALLOWED.getCode(), String.format("请求方法不正确:%s", ex.getMessage()));
    }

    /**
     * 处理 SpringMVC 请求的 Content-Type 不正确。
     *
     * <p>
     * 例如说，A 接口的 Content-Type 为 application/json，结果请求的 Content-Type 为
     * application/octet-stream，导致不匹配。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ApiResult<?> httpMediaTypeNotSupportedExceptionHandler(
            HttpMediaTypeNotSupportedException ex) {
        log.warn("[httpMediaTypeNotSupportedExceptionHandler]", ex);
        return ApiResult.error(BAD_REQUEST.getCode(), String.format("请求类型不正确:%s", ex.getMessage()));
    }

    /**
     * 处理业务异常 ServiceException。
     *
     * <p>
     * 例如说，商品库存不足，用户手机号已存在。日志降噪、本地化与结果构造上移至
     * {@link ExceptionHandlerSupport#handleServiceException}，双栈共享。
     */
    @ExceptionHandler(value = ServiceException.class)
    public ApiResult<?> serviceExceptionHandler(HttpServletRequest req, ServiceException ex) {
        MuyiWebMvcProperties.ServiceExceptionLog noise = properties.getServiceExceptionLog();
        return handleServiceException(
                ex,
                noise.getIgnoreMessages(),
                noise.getStackTraceFrames(),
                resolveLocale(req),
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
    public ApiResult<?> defaultExceptionHandler(HttpServletRequest req, Throwable ex) {
        ApiResult<?> moduleResult = firstModuleResult(req, ex);
        if (moduleResult != null) {
            return moduleResult;
        }
        MuyiWebMvcProperties.ServiceExceptionLog noise = properties.getServiceExceptionLog();
        return handleUnexpectedException(
                ex,
                noise.getIgnoreMessages(),
                noise.getStackTraceFrames(),
                resolveLocale(req),
                findRegistry());
    }

    /**
     * 依 {@code @Order} 优先级遍历 SPI 模块级处理器，返回首个非 null 结果。
     * @param request 请求
     * @param ex 异常
     * @return 首个处理结果；全部处理器放行（返回 null）时为 null
     */
    private ApiResult<?> firstModuleResult(HttpServletRequest request, Throwable ex) {
        if (Objects.isNull(moduleExceptionHandlers)) {
            return null;
        }
        return moduleExceptionHandlers
                .orderedStream()
                .map(
                        moduleExceptionHandler ->
                                moduleExceptionHandler.allExceptionHandler(request, ex))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
     * 从当前请求上下文获取请求对象。
     * @return 请求对象
     */
    private HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Assert.state(
                requestAttributes instanceof ServletRequestAttributes,
                "未找到 ServletRequestAttributes");
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 解析请求 Locale：优先请求作用域（{@code Accept-Language}），回退
     * {@link LocaleContextHolder} / 默认值——与 WebFlux 侧
     * {@code ReactiveCoreExceptionHandler#resolveLocale(ServerWebExchange)} 双栈语义对称。
     *
     * <p>
     * {@code LocaleContextHolder} 是线程绑定的静态上下文，异步线程或应用显式写入时可能
     * 偏离当前请求，故仅作回退使用。
     *
     * @param req 当前请求
     * @return 请求 Locale
     */
    private static Locale resolveLocale(HttpServletRequest req) {
        Locale locale = req != null ? req.getLocale() : null;
        if (locale != null) {
            return locale;
        }
        // LocaleContextHolder#getLocale 契约保证非 null（无上下文时内部回退 Locale.getDefault()），
        // 无需再判空
        return LocaleContextHolder.getLocale();
    }
}
