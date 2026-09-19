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
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.METHOD_NOT_ALLOWED;
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.muyitech.boot.errorcode.ConstantsErrorCodeProvider;
import io.github.muyitech.boot.errorcode.DefaultErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.ErrorCodeProvider;
import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.OverridePolicy;
import io.github.muyitech.boot.webmvc.autoconfigure.MuyiWebMvcProperties;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * {@link CoreExceptionHandler} 单元测试。
 *
 * <p>
 * 覆盖全部异常处理分支：MVC 参数类异常、参数校验类异常、请求方法/类型类异常、
 * 业务异常 ServiceException、以及兜底系统异常；同时验证
 * {@link ModuleExceptionHandler} 模块扩展点的优先分发逻辑。
 *
 * @author keep simple
 * @since 2026/9/12
 */
class CoreExceptionHandlerTest {

    private CoreExceptionHandler handler;

    private ModuleExceptionHandler moduleExceptionHandler;

    private HttpServletRequest request;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        handler = new CoreExceptionHandler();
        moduleExceptionHandler = mock(ModuleExceptionHandler.class);
        // 反射注入 ObjectProvider 字段（mock 提供有序流），避免启动 Spring 容器；
        // thenAnswer 保证每次调用返回新流（真实 ObjectProvider 语义，allExceptionHandler
        // 与 defaultExceptionHandler 链式分发会消费两次）
        ObjectProvider<ModuleExceptionHandler> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(inv -> Stream.of(moduleExceptionHandler));
        Field field = CoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, provider);
        // 反射注入配置项（降噪名单 / 堆栈帧数默认值），避免启动 Spring 容器
        Field props = CoreExceptionHandler.class.getDeclaredField("properties");
        props.setAccessible(true);
        props.set(handler, new MuyiWebMvcProperties());
        request = mock(HttpServletRequest.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LocaleContextHolder.setDefaultLocale(null);
    }

    /**
     * 构造一个非 null 的 {@link MethodParameter}，供 {@link MethodArgumentNotValidException} 使用。
     *
     * <p>
     * 该异常在记录日志时会调用 {@code getMessage()}，内部访问 parameter，传 null 会触发 NPE。
     * @return 基于 allExceptionHandler 方法构造的 MethodParameter
     * @throws Exception 反射获取方法失败时抛出
     */
    private org.springframework.core.MethodParameter methodParameter() throws Exception {
        return new org.springframework.core.MethodParameter(
                CoreExceptionHandler.class.getMethod(
                        "allExceptionHandler", HttpServletRequest.class, Throwable.class),
                0);
    }

    /** 测试用静态来源工厂：固定返回给定错误码集合（Ordered 顺序 0）。 */
    private static ErrorCodeProvider staticProvider(java.util.Collection<ErrorCode> codes) {
        return new ErrorCodeProvider() {
            @Override
            public java.util.Collection<ErrorCode> load() {
                return codes;
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

    /** 反射注入错误码注册表（findRegistry 走 getIfAvailable 取值）。 */
    @SuppressWarnings("unchecked")
    private void injectRegistry(ErrorCodeRegistry registry) throws Exception {
        ObjectProvider<ErrorCodeRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        Field field = CoreExceptionHandler.class.getDeclaredField("errorCodeRegistries");
        field.setAccessible(true);
        field.set(handler, provider);
    }

    // ==================== allExceptionHandler 分发 ====================

    @Test
    void allExceptionHandler_moduleHandlerReturnsResult_shortCircuit() {
        ApiResult<Object> moduleResult = ApiResult.error("1001", "模块异常");
        doReturn(moduleResult)
                .when(moduleExceptionHandler)
                .allExceptionHandler(eq(request), any(Throwable.class));

        ApiResult<?> result = handler.allExceptionHandler(request, new RuntimeException("x"));

        assertThat(result).isSameAs(moduleResult);
        verify(moduleExceptionHandler).allExceptionHandler(eq(request), any(Throwable.class));
    }

    @Test
    void allExceptionHandler_moduleHandlerReturnsNull_fallsThroughToDefault() {
        when(moduleExceptionHandler.allExceptionHandler(eq(request), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.allExceptionHandler(request, new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void allExceptionHandler_noModuleHandler_handlesByCore() throws Exception {
        Field field = CoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, null);

        ApiResult<?> result =
                handler.allExceptionHandler(request, new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void allExceptionHandler_missingParameter_dispatch() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("name", "String");

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("name");
    }

    @Test
    void allExceptionHandler_typeMismatch_dispatch() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "age", null, null);

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("age");
    }

    @Test
    void allExceptionHandler_methodArgumentNotValid_dispatch() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "名字不能为空"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("名字不能为空");
    }

    @Test
    void allExceptionHandler_serviceException_dispatch() {
        ServiceException ex = new ServiceException("5001", "业务失败");

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo("5001");
        assertThat(result.getMsg()).isEqualTo("业务失败");
    }

    @Test
    void allExceptionHandler_bindException_dispatch() {
        BindException ex = new BindException(new Object(), "obj");
        ex.addError(new FieldError("obj", "age", "年龄不能为负"));

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("年龄不能为负");
    }

    @Test
    void allExceptionHandler_constraintViolation_dispatch() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("姓名长度不合法");
        ConstraintViolationException ex =
                new ConstraintViolationException("校验失败", Set.of(violation));

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("姓名长度不合法");
    }

    @Test
    void allExceptionHandler_validationException_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(request, new ValidationException("本地校验失败"));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    @Test
    void allExceptionHandler_maxUploadSize_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(request, new MaxUploadSizeExceededException(1024));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("上传文件过大");
    }

    @Test
    void allExceptionHandler_noHandlerFound_dispatch() {
        NoHandlerFoundException ex =
                new NoHandlerFoundException("GET", "/api/none", HttpHeaders.EMPTY);

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
        assertThat(result.getMsg()).doesNotContain("/api/none");
    }

    @Test
    void allExceptionHandler_httpMethodNotSupported_dispatch() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    void allExceptionHandler_httpMediaTypeNotSupported_dispatch() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(
                        MediaType.APPLICATION_JSON, List.of(MediaType.TEXT_PLAIN));

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    // ==================== 参数缺失 / 类型错误 ====================

    @Test
    void missingServletRequestParameterExceptionHandler_returnsBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("page", "Integer");

        ApiResult<?> result = handler.missingServletRequestParameterExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("page");
    }

    @Test
    void methodArgumentTypeMismatchExceptionHandler_returnsBadRequest() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

        ApiResult<?> result = handler.methodArgumentTypeMismatchExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("id");
    }

    // ==================== 参数校验 ====================

    @Test
    void methodArgumentNotValidExceptionHandler_fieldError() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "mobile", "手机号格式不正确"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.methodArgumentNotValidExceptionExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("手机号格式不正确");
    }

    @Test
    void methodArgumentNotValidExceptionHandler_noFieldError_usesAllErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new org.springframework.validation.ObjectError("obj", "组合校验失败"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.methodArgumentNotValidExceptionExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("组合校验失败");
    }

    @Test
    void methodArgumentNotValidExceptionHandler_emptyMessage_returnsBadRequestOnly()
            throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.methodArgumentNotValidExceptionExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).isEqualTo(BAD_REQUEST.getMsg());
    }

    @Test
    void bindExceptionHandler_returnsBadRequest() {
        BindException ex = new BindException(new Object(), "obj");
        ex.addError(new FieldError("obj", "age", "年龄不能为负"));

        ApiResult<?> result = handler.bindExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("年龄不能为负");
    }

    @Test
    void bindExceptionHandler_nullFieldError_throwsIllegalStateException() {
        // 无任何 FieldError 时 getFieldError() 返回 null，触发 Assert.state 失败分支
        BindException ex = new BindException(new Object(), "obj");

        assertThatThrownBy(() -> handler.bindExceptionHandler(ex))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constraintViolationExceptionHandler_returnsFirstViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("姓名长度不合法");
        ConstraintViolationException ex =
                new ConstraintViolationException("校验失败", Set.of(violation));

        ApiResult<?> result = handler.constraintViolationExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("姓名长度不合法");
    }

    @Test
    void validationException_returnsBadRequest() {
        ApiResult<?> result = handler.validationException(new ValidationException("本地校验失败"));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    // ==================== 请求体 / 上传 ====================

    @Test
    void methodArgumentTypeInvalidFormatExceptionHandler_invalidFormat() {
        InvalidFormatException cause = mock(InvalidFormatException.class);
        when(cause.getValue()).thenReturn("not-a-number");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException(
                        "json 解析失败", cause, mock(HttpInputMessage.class));

        ApiResult<?> result = handler.methodArgumentTypeInvalidFormatExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("not-a-number");
    }

    @Test
    void methodArgumentTypeInvalidFormatExceptionHandler_missingBody() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException(
                        "Required request body is missing", mock(HttpInputMessage.class));

        ApiResult<?> result = handler.methodArgumentTypeInvalidFormatExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("request body 缺失");
    }

    @Test
    void methodArgumentTypeInvalidFormatExceptionHandler_unknownCause_fallsThroughToDefault() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException(
                        "其他解析异常", new IllegalArgumentException("x"), mock(HttpInputMessage.class));

        ApiResult<?> result = handler.methodArgumentTypeInvalidFormatExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void maxUploadSizeExceededExceptionHandler_returnsBadRequest() {
        ApiResult<?> result =
                handler.maxUploadSizeExceededExceptionHandler(
                        new MaxUploadSizeExceededException(1024));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("上传文件过大");
    }

    // ==================== 请求地址 / 方法 / 类型 ====================

    @Test
    void noHandlerFoundExceptionHandler_returnsNotFound() {
        NoHandlerFoundException ex =
                new NoHandlerFoundException("GET", "/api/none", HttpHeaders.EMPTY);

        ApiResult<?> result = handler.noHandlerFoundExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
        assertThat(result.getMsg()).doesNotContain("/api/none");
    }

    @Test
    @DisplayName("404 出口：请求作用域 Locale 优先于 LocaleContextHolder（与 WebFlux 语义对称）")
    void noHandlerFound_requestLocalePreferred_overContext() throws Exception {
        ErrorCodeRegistry overriding =
                new DefaultErrorCodeRegistry(
                        List.<ErrorCodeProvider>of(
                                new ConstantsErrorCodeProvider(),
                                staticProvider(
                                        List.of(
                                                new ErrorCode(
                                                        "404",
                                                        "请求未找到",
                                                        Map.of(
                                                                "zh-CN", "请求未找到",
                                                                "ja", "リクエストが見つかりません"))))),
                        OverridePolicy.LAST_WINS,
                        null);
        injectRegistry(overriding);
        when(request.getLocale()).thenReturn(Locale.JAPAN);
        // 线程绑定上下文设为 zh-CN：验证请求作用域 ja 优先于 LocaleContextHolder
        LocaleContextHolder.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

        ApiResult<?> result =
                handler.noHandlerFoundExceptionHandler(
                        request,
                        new NoHandlerFoundException("GET", "/api/none", HttpHeaders.EMPTY));

        assertThat(result.getMsg()).isEqualTo("リクエストが見つかりません");
    }

    @Test
    @DisplayName("404 出口：请求无 Accept-Language 时回退 LocaleContextHolder")
    void noHandlerFound_fallsBackToContextLocale_whenRequestLocaleAbsent() throws Exception {
        ErrorCodeRegistry overriding =
                new DefaultErrorCodeRegistry(
                        List.<ErrorCodeProvider>of(
                                new ConstantsErrorCodeProvider(),
                                staticProvider(
                                        List.of(
                                                new ErrorCode(
                                                        "404",
                                                        "请求未找到",
                                                        Map.of(
                                                                "zh-CN", "请求未找到",
                                                                "en-US", "Resource not found"))))),
                        OverridePolicy.LAST_WINS,
                        null);
        injectRegistry(overriding);
        // mock 默认 getLocale() 返回 null：回退线程绑定 Locale（US）
        LocaleContextHolder.setDefaultLocale(Locale.US);

        ApiResult<?> result =
                handler.noHandlerFoundExceptionHandler(
                        request,
                        new NoHandlerFoundException("GET", "/api/none", HttpHeaders.EMPTY));

        assertThat(result.getMsg()).isEqualTo("Resource not found");
    }

    @Test
    void allExceptionHandler_noResourceFoundException_dispatch() {
        NoResourceFoundException ex =
                new NoResourceFoundException(HttpMethod.GET, "No static resource", "/static/a.js");

        ApiResult<?> result = handler.allExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
        assertThat(result.getMsg()).doesNotContain("/static/a.js");
    }

    @Test
    void httpRequestMethodNotSupportedExceptionHandler_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ApiResult<?> result = handler.httpRequestMethodNotSupportedExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(METHOD_NOT_ALLOWED.getCode());
        assertThat(result.getMsg()).contains("POST");
    }

    @Test
    void httpMediaTypeNotSupportedExceptionHandler_returnsBadRequest() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(
                        MediaType.APPLICATION_JSON, List.of(MediaType.TEXT_PLAIN));

        ApiResult<?> result = handler.httpMediaTypeNotSupportedExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("application/json");
    }

    // ==================== 业务异常 ====================

    @Test
    void serviceExceptionHandler_returnsCodeAndMessage() {
        ServiceException ex = new ServiceException("3001", "订单不存在");

        ApiResult<?> result = handler.serviceExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    void serviceExceptionHandler_ignoreMessage_returnsWithoutThrowing() {
        ServiceException ex = new ServiceException("401", "无效的刷新令牌");

        ApiResult<?> result = handler.serviceExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo("401");
        assertThat(result.getMsg()).isEqualTo("无效的刷新令牌");
    }

    @Test
    void serviceExceptionHandler_exceptionFromUtil_skipsUtilStackFrame() {
        // 通过 ServiceExceptionUtil 创建异常，栈帧第一层为 ServiceExceptionUtil，触发 for 循环的 continue 分支
        ServiceException ex = ServiceExceptionUtil.exception(new ErrorCode("3002", "库存不足"));

        ApiResult<?> result = handler.serviceExceptionHandler(request, ex);

        assertThat(result.getCode()).isEqualTo("3002");
        assertThat(result.getMsg()).isEqualTo("库存不足");
    }

    // ==================== 兜底系统异常 ====================

    @Test
    void defaultExceptionHandler_returnsInternalServerError() {
        ApiResult<?> result =
                handler.defaultExceptionHandler(request, new NullPointerException("npe"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo(INTERNAL_SERVER_ERROR.getMsg());
    }

    @Test
    void defaultExceptionHandler_causeIsServiceException_delegates() {
        ServiceException cause = new ServiceException("4000", "内部业务异常");
        ApiResult<?> result =
                handler.defaultExceptionHandler(request, new RuntimeException("wrap", cause));

        assertThat(result.getCode()).isEqualTo("4000");
        assertThat(result.getMsg()).isEqualTo("内部业务异常");
    }

    @Test
    void defaultExceptionHandler_moduleHandlerReturnsResult_shortCircuit() {
        ApiResult<Object> custom = ApiResult.error("1001001001", "模块级自定义响应");
        doReturn(custom)
                .when(moduleExceptionHandler)
                .allExceptionHandler(eq(request), any(Throwable.class));

        ApiResult<?> result =
                handler.defaultExceptionHandler(request, new NullPointerException("npe"));

        assertThat(result).isSameAs(custom);
    }

    @Test
    void defaultExceptionHandler_moduleHandlerReturnsNull_fallsThroughToInternalServerError() {
        when(moduleExceptionHandler.allExceptionHandler(eq(request), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.defaultExceptionHandler(request, new NullPointerException("npe"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void allExceptionHandler_unknownException_returnsInternalServerError() {
        when(moduleExceptionHandler.allExceptionHandler(eq(request), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.allExceptionHandler(request, new ArrayIndexOutOfBoundsException(2));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void logModuleHandlers_noHandlers_logsDebugAndSkips() throws Exception {
        // 未注册任何模块处理器：early-return 分支（不触 describe）
        @SuppressWarnings("unchecked")
        ObjectProvider<ModuleExceptionHandler> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.orderedStream()).thenReturn(Stream.empty());
        setField("moduleExceptionHandlers", emptyProvider);

        java.lang.reflect.Method method =
                CoreExceptionHandler.class.getDeclaredMethod("logModuleHandlers");
        method.setAccessible(true);
        assertThatCode(() -> method.invoke(handler)).doesNotThrowAnyException();
    }

    @Test
    void logModuleHandlers_orderedAndPlainHandlers_describesBothBranches() throws Exception {
        // Ordered 实现 → instanceof 分支；普通实现 → OrderUtils 兜底分支
        ModuleExceptionHandler ordered =
                mock(ModuleExceptionHandler.class, withSettings().extraInterfaces(Ordered.class));
        doReturn(7).when((Ordered) ordered).getOrder();
        ModuleExceptionHandler plain = mock(ModuleExceptionHandler.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<ModuleExceptionHandler> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(ordered, plain));
        setField("moduleExceptionHandlers", provider);

        java.lang.reflect.Method method =
                CoreExceptionHandler.class.getDeclaredMethod("logModuleHandlers");
        method.setAccessible(true);
        assertThatCode(() -> method.invoke(handler)).doesNotThrowAnyException();
    }

    @Test
    void noHandlerFoundExceptionHandler_nullRequest_fallsBackToContextLocale() {
        // resolveLocale 的 req == null 防御分支（LocaleContextHolder 契约保证非 null 回退）
        ApiResult<?> result =
                handler.noHandlerFoundExceptionHandler(
                        null, new NoHandlerFoundException("GET", "/api/none", HttpHeaders.EMPTY));

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
    }

    private void setField(String name, Object value) throws Exception {
        Field field = CoreExceptionHandler.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(handler, value);
    }
}
