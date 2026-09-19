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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.webflux.autoconfigure.MuyiWebFluxProperties;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.pojo.ApiResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * {@link ReactiveCoreExceptionHandler} 单元测试。
 *
 * <p>
 * 覆盖映射表全分支：参数校验类（WebExchangeBindException / MethodArgumentNotValidException /
 * ConstraintViolationException / ValidationException）、输入类（ServerWebInputException 含
 * Jackson 3 InvalidFormatException 两层 cause 穿透 / body 缺失 / 未知 cause 兜底）、
 * HTTP 语义类（UnsupportedMediaTypeStatusException / ResponseStatusException 404/405/415/500）、
 * 上传限制（DataBufferLimitException）、业务异常与系统兜底；同时验证
 * {@link ReactiveModuleExceptionHandler} 模块扩展点的优先分发逻辑。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class ReactiveCoreExceptionHandlerTest {

    private ReactiveCoreExceptionHandler handler;

    private ReactiveModuleExceptionHandler moduleExceptionHandler;

    private ServerWebExchange exchange;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        handler = new ReactiveCoreExceptionHandler();
        moduleExceptionHandler = mock(ReactiveModuleExceptionHandler.class);
        // 反射注入 ObjectProvider 字段（mock 提供有序流），避免启动 Spring 容器；
        // thenAnswer 保证每次调用返回新流（真实 ObjectProvider 语义，allExceptionHandler
        // 与 defaultExceptionHandler 链式分发会消费两次）
        ObjectProvider<ReactiveModuleExceptionHandler> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(inv -> Stream.of(moduleExceptionHandler));
        Field field =
                ReactiveCoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, provider);
        // 反射注入配置项（降噪名单 / 堆栈帧数默认值），避免启动 Spring 容器
        Field props = ReactiveCoreExceptionHandler.class.getDeclaredField("properties");
        props.setAccessible(true);
        props.set(handler, new MuyiWebFluxProperties());
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
    }

    @AfterEach
    void tearDown() {
        // LocaleContextHolder 是 JVM 全局态：设置过默认 Locale 的用例必须复位，
        // 避免污染同 JVM 后续测试（与 WebMVC 侧 CoreExceptionHandlerTest 对称）
        LocaleContextHolder.setDefaultLocale(null);
    }

    /**
     * 构造一个非 null 的 {@link MethodParameter}，供校验类异常构造使用。
     * @return 基于 allExceptionHandler 方法构造的 MethodParameter
     * @throws Exception 反射获取方法失败时抛出
     */
    private MethodParameter methodParameter() throws Exception {
        return new MethodParameter(
                ReactiveCoreExceptionHandler.class.getMethod(
                        "allExceptionHandler", ServerWebExchange.class, Throwable.class),
                0);
    }

    // ==================== allExceptionHandler 模块分发 ====================

    @Test
    void allExceptionHandler_moduleHandlerReturnsResult_shortCircuit() {
        ApiResult<Object> moduleResult = ApiResult.error("1001", "模块异常");
        doReturn(moduleResult)
                .when(moduleExceptionHandler)
                .allExceptionHandler(eq(exchange), any(Throwable.class));

        ApiResult<?> result = handler.allExceptionHandler(exchange, new RuntimeException("x"));

        assertThat(result).isSameAs(moduleResult);
        verify(moduleExceptionHandler).allExceptionHandler(eq(exchange), any(Throwable.class));
    }

    @Test
    void allExceptionHandler_moduleHandlerReturnsNull_fallsThroughToDefault() {
        when(moduleExceptionHandler.allExceptionHandler(eq(exchange), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.allExceptionHandler(exchange, new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void allExceptionHandler_noModuleHandler_handlesByCore() throws Exception {
        Field field =
                ReactiveCoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, null);

        ApiResult<?> result =
                handler.allExceptionHandler(exchange, new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    // ==================== allExceptionHandler dispatch ====================

    @Test
    void allExceptionHandler_webExchangeBind_dispatch() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "名字不能为空"));
        WebExchangeBindException ex =
                new WebExchangeBindException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("名字不能为空");
    }

    @Test
    void allExceptionHandler_methodArgumentNotValid_dispatch() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "名字不能为空"));
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(
                        methodParameter(), bindingResult);

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("名字不能为空");
    }

    @Test
    void allExceptionHandler_constraintViolation_dispatch() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("姓名长度不合法");
        ConstraintViolationException ex =
                new ConstraintViolationException("校验失败", Set.of(violation));

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("姓名长度不合法");
    }

    @Test
    void allExceptionHandler_validationException_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(exchange, new ValidationException("本地校验失败"));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    @Test
    void allExceptionHandler_dataBufferLimit_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(
                        exchange,
                        new DataBufferLimitException("Exceeded limit on max bytes to buffer"));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("上传文件过大");
    }

    @Test
    void allExceptionHandler_serverWebInput_dispatch() {
        ServerWebInputException ex =
                new ServerWebInputException("Required request parameter 'name' is not present");

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("name");
    }

    @Test
    void allExceptionHandler_unsupportedMediaType_dispatch() {
        UnsupportedMediaTypeStatusException ex =
                new UnsupportedMediaTypeStatusException(
                        MediaType.APPLICATION_OCTET_STREAM, List.of(MediaType.APPLICATION_JSON));

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("请求类型不正确");
    }

    @Test
    void allExceptionHandler_responseStatus_notFound_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(
                        exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "路由未匹配"));

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
        assertThat(result.getMsg()).doesNotContain("路由未匹配");
    }

    @Test
    void allExceptionHandler_responseStatus_methodNotAllowed_dispatch() {
        ApiResult<?> result =
                handler.allExceptionHandler(
                        exchange,
                        new ResponseStatusException(
                                HttpStatus.METHOD_NOT_ALLOWED, "POST not allowed"));

        assertThat(result.getCode()).isEqualTo(METHOD_NOT_ALLOWED.getCode());
        assertThat(result.getMsg()).contains("POST not allowed");
    }

    @Test
    void allExceptionHandler_responseStatus_serviceException_dispatch() {
        ServiceException ex = new ServiceException("5001", "业务失败");

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo("5001");
        assertThat(result.getMsg()).isEqualTo("业务失败");
    }

    @Test
    void allExceptionHandler_unknownException_returnsInternalServerError() {
        when(moduleExceptionHandler.allExceptionHandler(eq(exchange), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.allExceptionHandler(exchange, new ArrayIndexOutOfBoundsException(2));

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    // ==================== 参数校验方法 ====================

    @Test
    void methodArgumentNotValidExceptionHandler_noFieldError_usesAllErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new org.springframework.validation.ObjectError("obj", "组合校验失败"));
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(
                        methodParameter(), bindingResult);

        ApiResult<?> result = handler.methodArgumentNotValidExceptionExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("组合校验失败");
    }

    @Test
    void methodArgumentNotValidExceptionHandler_emptyMessage_returnsBadRequestOnly()
            throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(
                        methodParameter(), bindingResult);

        ApiResult<?> result = handler.methodArgumentNotValidExceptionExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).isEqualTo(BAD_REQUEST.getMsg());
    }

    @Test
    void webExchangeBindExceptionHandler_noFieldError_usesAllErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new org.springframework.validation.ObjectError("obj", "组合校验失败"));
        WebExchangeBindException ex =
                new WebExchangeBindException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.webExchangeBindExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("组合校验失败");
    }

    @Test
    void webExchangeBindExceptionHandler_emptyErrors_returnsBadRequestOnly() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "obj");
        WebExchangeBindException ex =
                new WebExchangeBindException(methodParameter(), bindingResult);

        ApiResult<?> result = handler.webExchangeBindExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).isEqualTo(BAD_REQUEST.getMsg());
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
    void validationExceptionHandler_returnsBadRequest() {
        ApiResult<?> result = handler.validationExceptionHandler(new ValidationException("本地校验失败"));

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    // ==================== ServerWebInputException 三分支 ====================

    @Test
    void serverWebInputExceptionHandler_invalidFormat_penetratesTwoLayerCause() {
        // 模拟 WebFlux JSON 解码两层包装：ServerWebInputException → DecodingException →
        // InvalidFormatException
        InvalidFormatException invalidFormat = mock(InvalidFormatException.class);
        when(invalidFormat.getValue()).thenReturn("not-a-number");
        RuntimeException decodingException = new RuntimeException("decode failed", invalidFormat);
        ServerWebInputException ex =
                new ServerWebInputException("JSON decoding error", null, decodingException);

        ApiResult<?> result = handler.serverWebInputExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("not-a-number");
    }

    @Test
    void serverWebInputExceptionHandler_missingBody_returnsBadRequest() {
        ServerWebInputException ex = new ServerWebInputException("Request body is missing");

        ApiResult<?> result = handler.serverWebInputExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("request body 缺失");
    }

    @Test
    void serverWebInputExceptionHandler_unknownCause_returnsBadRequest() {
        ServerWebInputException ex =
                new ServerWebInputException(
                        "JSON decoding error", null, new IllegalArgumentException("x"));

        ApiResult<?> result = handler.serverWebInputExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("JSON decoding error");
    }

    @Test
    void serverWebInputExceptionHandler_nullReason_returnsBadRequestOnly() {
        ServerWebInputException ex = new ServerWebInputException(null);

        ApiResult<?> result = handler.serverWebInputExceptionHandler(ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).isEqualTo(BAD_REQUEST.getMsg());
    }

    // ==================== ResponseStatusException 状态码映射 ====================

    @Test
    void responseStatusExceptionHandler_badRequest_mapsBadRequest() {
        ApiResult<?> result =
                handler.responseStatusExceptionHandler(
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数不合法"), exchange);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("参数不合法");
    }

    @Test
    void responseStatusExceptionHandler_notFound_unifiedExit() {
        ApiResult<?> result =
                handler.responseStatusExceptionHandler(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "No static resource api/demo/missing"),
                        exchange);

        assertThat(result.getCode()).isEqualTo(NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(NOT_FOUND.getMsg());
        assertThat(result.getMsg()).doesNotContain("No static resource");
        assertThat(result.getMsg()).doesNotContain("/api/demo/missing");
    }

    @Test
    void responseStatusExceptionHandler_unsupportedMediaType_mapsBadRequest() {
        ApiResult<?> result =
                handler.responseStatusExceptionHandler(
                        new ResponseStatusException(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE, (String) null),
                        exchange);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
    }

    @Test
    void responseStatusExceptionHandler_serverError_mapsInternalServerError() {
        ApiResult<?> result =
                handler.responseStatusExceptionHandler(
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "boom"),
                        exchange);

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
        assertThat(result.getMsg()).contains("boom");
    }

    // ==================== 业务异常 / 兜底 ====================

    @Test
    void serviceExceptionHandler_returnsCodeAndMessage() {
        ServiceException ex = new ServiceException("3001", "订单不存在");

        ApiResult<?> result = handler.serviceExceptionHandler(ex, exchange);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    void serviceExceptionHandler_ignoreMessage_returnsWithoutThrowing() {
        ServiceException ex = new ServiceException("401", "无效的刷新令牌");

        ApiResult<?> result = handler.serviceExceptionHandler(ex, exchange);

        assertThat(result.getCode()).isEqualTo("401");
        assertThat(result.getMsg()).isEqualTo("无效的刷新令牌");
    }

    @Test
    void defaultExceptionHandler_returnsInternalServerError() {
        ApiResult<?> result =
                handler.defaultExceptionHandler(new NullPointerException("npe"), exchange);

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo(INTERNAL_SERVER_ERROR.getMsg());
    }

    @Test
    void defaultExceptionHandler_causeIsServiceException_delegates() {
        ServiceException cause = new ServiceException("4000", "内部业务异常");
        ApiResult<?> result =
                handler.defaultExceptionHandler(new RuntimeException("wrap", cause), exchange);

        assertThat(result.getCode()).isEqualTo("4000");
        assertThat(result.getMsg()).isEqualTo("内部业务异常");
    }

    @Test
    void defaultExceptionHandler_moduleHandlerReturnsResult_shortCircuit() {
        ApiResult<Object> custom = ApiResult.error("1001001001", "模块级自定义响应");
        doReturn(custom)
                .when(moduleExceptionHandler)
                .allExceptionHandler(eq(exchange), any(Throwable.class));

        ApiResult<?> result =
                handler.defaultExceptionHandler(new NullPointerException("npe"), exchange);

        assertThat(result).isSameAs(custom);
    }

    @Test
    void defaultExceptionHandler_moduleHandlerReturnsNull_fallsThroughToInternalServerError() {
        when(moduleExceptionHandler.allExceptionHandler(eq(exchange), any(Throwable.class)))
                .thenReturn(null);

        ApiResult<?> result =
                handler.defaultExceptionHandler(new NullPointerException("npe"), exchange);

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
    }

    // ==================== 兼容性说明 ====================

    /**
     * {@link MethodArgumentTypeMismatchException} 在 WebFlux 中由框架包装为
     * {@link ServerWebInputException}，不存在独立分支；此处仅验证类型本身可被兜底分支处理。
     */
    @Test
    void methodArgumentTypeMismatch_wrappedAsInputException() {
        MethodArgumentTypeMismatchException cause =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "age", null, null);
        ServerWebInputException ex =
                new ServerWebInputException("Type mismatch for argument 'age'", null, cause);

        ApiResult<?> result = handler.allExceptionHandler(exchange, ex);

        assertThat(result.getCode()).isEqualTo(BAD_REQUEST.getCode());
        assertThat(result.getMsg()).contains("age");
    }

    // ==================== logModuleHandlers / findRegistry / resolveLocale / errorCodeFor 补测
    // ====================

    @Test
    @SuppressWarnings("unchecked")
    void logModuleHandlers_emptyStream_logsDebugOnly() throws Exception {
        // 空模块 handler 流：debug 分支（不产生描述行），方法正常返回
        ObjectProvider<ReactiveModuleExceptionHandler> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.orderedStream()).thenAnswer(inv -> Stream.empty());
        Field field =
                ReactiveCoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, emptyProvider);

        Method method = ReactiveCoreExceptionHandler.class.getDeclaredMethod("logModuleHandlers");
        method.setAccessible(true);

        assertThatCode(() -> method.invoke(handler)).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void logModuleHandlers_nonEmptyStream_describesOrderedAndPlainHandlers() throws Exception {
        // 非空流：Ordered 实现走 getOrder()，普通实现走 OrderUtils 反射读取，两类描述都输出
        ObjectProvider<ReactiveModuleExceptionHandler> provider = mock(ObjectProvider.class);
        ReactiveModuleExceptionHandler orderedHandler =
                mock(
                        ReactiveModuleExceptionHandler.class,
                        withSettings().extraInterfaces(Ordered.class));
        when(((Ordered) orderedHandler).getOrder()).thenReturn(1);
        when(provider.orderedStream())
                .thenAnswer(inv -> Stream.of(moduleExceptionHandler, orderedHandler));
        Field field =
                ReactiveCoreExceptionHandler.class.getDeclaredField("moduleExceptionHandlers");
        field.setAccessible(true);
        field.set(handler, provider);

        Method method = ReactiveCoreExceptionHandler.class.getDeclaredMethod("logModuleHandlers");
        method.setAccessible(true);

        assertThatCode(() -> method.invoke(handler)).doesNotThrowAnyException();
        verify(orderedHandler, never()).allExceptionHandler(any(), any());
    }

    @Test
    void responseStatusExceptionHandler_serviceUnavailable_mapsInternalServerError() {
        // 5xx 非 500（如 503）：兜底为 INTERNAL_SERVER_ERROR，而不是 BAD_REQUEST
        ApiResult<?> result =
                handler.responseStatusExceptionHandler(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "服务维护中"),
                        exchange);

        assertThat(result.getCode()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
        assertThat(result.getMsg()).contains("服务维护中");
    }

    @Test
    @SuppressWarnings("unchecked")
    void serviceExceptionHandler_registryHit_resolvesRegistryMessage() throws Exception {
        // 注册表命中：优先注册表文案（Locale 按 exchange 解析），而不是异常原始消息
        ErrorCodeRegistry registry = mock(ErrorCodeRegistry.class);
        when(registry.find("5001")).thenReturn(Optional.of(new ErrorCode("5001", "注册表文案")));
        ObjectProvider<ErrorCodeRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        Field field = ReactiveCoreExceptionHandler.class.getDeclaredField("errorCodeRegistries");
        field.setAccessible(true);
        field.set(handler, provider);

        ApiResult<?> result =
                handler.serviceExceptionHandler(new ServiceException("5001", "异常原始消息"), exchange);

        assertThat(result.getCode()).isEqualTo("5001");
        assertThat(result.getMsg()).isEqualTo("注册表文案");
    }

    @Test
    void serviceExceptionHandler_nullExchange_fallsBackToDefaultLocale() {
        // exchange 为 null：LocaleContext 解析安全降级，业务结果不受影响
        ApiResult<?> result =
                handler.serviceExceptionHandler(new ServiceException("3001", "订单不存在"), null);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    void serviceExceptionHandler_nullLocaleContext_fallsBackToDefaultLocale() {
        // exchange.getLocaleContext() 返回 null：安全降级到 LocaleContextHolder
        ServerWebExchange noLocaleExchange = mock(ServerWebExchange.class);
        when(noLocaleExchange.getLocaleContext()).thenReturn(null);

        ApiResult<?> result =
                handler.serviceExceptionHandler(
                        new ServiceException("3001", "订单不存在"), noLocaleExchange);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    void serviceExceptionHandler_nullLocale_fallsBackToDefaultLocale() {
        // LocaleContext 存在但 getLocale() 为 null：安全降级到 LocaleContextHolder
        ServerWebExchange nullLocaleExchange = mock(ServerWebExchange.class);
        when(nullLocaleExchange.getLocaleContext()).thenReturn((LocaleContext) () -> null);

        ApiResult<?> result =
                handler.serviceExceptionHandler(
                        new ServiceException("3001", "订单不存在"), nullLocaleExchange);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    void responseStatusExceptionHandler_notFound_exchangeLocalePreferred_overContext()
            throws Exception {
        // resolveLocale 快乐路径（JaCoCo 此前唯一漏网分支）：exchange 的 LocaleContext 提供
        // 请求 Locale（Accept-Language: ja），优先于线程绑定上下文（zh-CN），404 文案随请求
        // Locale 本地化——与 WebMVC 侧 requestLocalePreferred_overContext 语义对称
        ErrorCodeRegistry registry = mock(ErrorCodeRegistry.class);
        when(registry.find("404"))
                .thenReturn(
                        Optional.of(new ErrorCode("404", "请求未找到", Map.of("ja", "リクエストが見つかりません"))));
        injectRegistry(registry);

        ServerWebExchange jaExchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/missing")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                                .build());
        LocaleContextHolder.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        try {
            ApiResult<?> result =
                    handler.responseStatusExceptionHandler(
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "No static resource x"),
                            jaExchange);

            assertThat(result.getMsg()).isEqualTo("リクエストが見つかりません");
        } finally {
            LocaleContextHolder.setDefaultLocale(null);
        }
    }

    @Test
    void responseStatusExceptionHandler_notFound_fallsBackToContextLocale_whenExchangeLocaleAbsent()
            throws Exception {
        // resolveLocale 回退取值语义：LocaleContext 的 locale 为 null 时回退
        // LocaleContextHolder（线程默认 US），404 文案本地化为 en-US——与 WebMVC 侧
        // fallsBackToContextLocale_whenRequestLocaleAbsent 语义对称
        ErrorCodeRegistry registry = mock(ErrorCodeRegistry.class);
        when(registry.find("404"))
                .thenReturn(
                        Optional.of(
                                new ErrorCode(
                                        "404", "请求未找到", Map.of("en-US", "Resource not found"))));
        injectRegistry(registry);

        ServerWebExchange noLocaleExchange = mock(ServerWebExchange.class);
        when(noLocaleExchange.getLocaleContext()).thenReturn((LocaleContext) () -> null);
        LocaleContextHolder.setDefaultLocale(Locale.US);
        try {
            ApiResult<?> result =
                    handler.responseStatusExceptionHandler(
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "No static resource x"),
                            noLocaleExchange);

            assertThat(result.getMsg()).isEqualTo("Resource not found");
        } finally {
            LocaleContextHolder.setDefaultLocale(null);
        }
    }

    /** 反射注入错误码注册表 ObjectProvider（容器内无注册表 Bean 时的单测替代）。 */
    @SuppressWarnings("unchecked")
    private void injectRegistry(ErrorCodeRegistry registry) throws Exception {
        ObjectProvider<ErrorCodeRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        Field field = ReactiveCoreExceptionHandler.class.getDeclaredField("errorCodeRegistries");
        field.setAccessible(true);
        field.set(handler, provider);
    }
}
