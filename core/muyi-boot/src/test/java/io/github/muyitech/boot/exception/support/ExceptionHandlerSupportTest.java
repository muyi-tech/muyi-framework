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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.muyitech.boot.errorcode.ConstantsErrorCodeProvider;
import io.github.muyitech.boot.errorcode.DefaultErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.ErrorCodeProvider;
import io.github.muyitech.boot.errorcode.OverridePolicy;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import io.github.muyitech.common.spring.pojo.ApiResult;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ExceptionHandlerSupport} 单元测试。
 *
 * <p>
 * 覆盖：{@code handleServiceException}
 * （普通、降噪名单、注册表 i18n 本地化与参数重格式化、无注册表回退）；{@code handleUnexpectedException}
 * （cause 链业务异常委托、兜底 500 含本地化）；{@code handleNotFound}（404 统一出口：无注册表默认
 * 文案、注册表命中回退与覆盖本地化）；{@code findCause}（直接命中、多层穿透、
 * 无匹配 null、null 入参、自引用环防护）。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class ExceptionHandlerSupportTest {

    /** 测试用静态来源工厂：固定返回给定错误码集合（Ordered 顺序 0）。 */
    private static ErrorCodeProvider staticProvider(Collection<ErrorCode> codes) {
        return new ErrorCodeProvider() {
            @Override
            public Collection<ErrorCode> load() {
                return codes;
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

    /** 接口 default 方法可直接经匿名实现调用。 */
    private final ExceptionHandlerSupport support = new ExceptionHandlerSupport() {};

    /** 测试注册表：框架全局码 + 业务码 1001 的中英文案。 */
    private final DefaultErrorCodeRegistry registry =
            new DefaultErrorCodeRegistry(
                    List.<ErrorCodeProvider>of(
                            new ConstantsErrorCodeProvider(),
                            staticProvider(
                                    List.of(
                                            new ErrorCode(
                                                    "1001",
                                                    "用户不存在",
                                                    Map.of(
                                                            "zh-CN",
                                                            "用户不存在",
                                                            "en-US",
                                                            "User {} does not exist"))))),
                    OverridePolicy.LAST_WINS,
                    null);

    // ==================== handleServiceException ====================

    @Test
    void handleServiceException_normal_returnsCodeAndMessage() {
        ApiResult<?> result =
                support.handleServiceException(
                        new ServiceException("3001", "订单不存在"),
                        List.of(),
                        1,
                        Locale.SIMPLIFIED_CHINESE,
                        registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("降噪名单命中走静默分支，仍返回正确结果且不抛出")
    void handleServiceException_ignoredMessage_returnsWithoutThrowing() {
        // 用注册表未登记的码，验证返回的 msg 来自异常本身而非注册表兜底文案
        ServiceException ex = new ServiceException("3003", "会话已过期");

        ApiResult<?> result =
                support.handleServiceException(
                        ex, List.of("会话已过期"), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("3003");
        assertThat(result.getMsg()).isEqualTo("会话已过期");
    }

    @Test
    @DisplayName("frames=0 时静默分支生效，仍返回正确结果")
    void handleServiceException_zeroFrames_silent() {
        ApiResult<?> result =
                support.handleServiceException(
                        new ServiceException("3002", "库存不足"), List.of(), 0, null, registry);

        assertThat(result.getCode()).isEqualTo("3002");
        assertThat(result.getMsg()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("ServiceExceptionUtil 创建的异常，栈帧跳过工具类后仍能正确返回")
    void handleServiceException_exceptionFromUtil_skipsUtilStackFrame() {
        ServiceException ex = ServiceExceptionUtil.exception(new ErrorCode("3003", "库存不足"));

        ApiResult<?> result =
                support.handleServiceException(
                        ex, List.of(), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("3003");
        assertThat(result.getMsg()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("注册表命中时按请求 Locale 解析，并用异常携带参数重新格式化")
    void handleServiceException_registryHit_resolvesLocaleAndReformats() {
        ServiceException ex =
                ServiceExceptionUtil.exception(new ErrorCode("1001", "用户不存在"), "zhang");

        ApiResult<?> result = support.handleServiceException(ex, List.of(), 1, Locale.US, registry);

        assertThat(result.getCode()).isEqualTo("1001");
        assertThat(result.getMsg()).isEqualTo("User zhang does not exist");
    }

    @Test
    @DisplayName("注册表命中但无语言匹配时回退默认文案")
    void handleServiceException_registryHit_withoutLocaleMatch_fallsBackToDefault() {
        ServiceException ex = new ServiceException("1001", "用户不存在");

        ApiResult<?> result =
                support.handleServiceException(ex, List.of(), 1, Locale.FRANCE, registry);

        assertThat(result.getCode()).isEqualTo("1001");
        assertThat(result.getMsg()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("无注册表时原样输出异常自带消息")
    void handleServiceException_nullRegistry_returnsExceptionMessage() {
        ApiResult<?> result =
                support.handleServiceException(
                        new ServiceException("9999", "自定义消息"), List.of(), 1, Locale.US, null);

        assertThat(result.getCode()).isEqualTo("9999");
        assertThat(result.getMsg()).isEqualTo("自定义消息");
    }

    @Test
    @DisplayName("栈帧读取抛异常：防御性 catch 静默忽略，不影响 ApiResult 组装")
    void handleServiceException_stackTraceReadThrows_ignored() {
        // ServiceException 是 final 类，用 Mockito inline mock 让 getStackTrace() 抛异常，
        // 覆盖 logServiceException 的防御性 catch 分支
        ServiceException bomb = org.mockito.Mockito.mock(ServiceException.class);
        org.mockito.Mockito.when(bomb.getCode()).thenReturn("3001");
        org.mockito.Mockito.when(bomb.getMessage()).thenReturn("订单不存在");
        org.mockito.Mockito.when(bomb.getArgs()).thenReturn(null);
        org.mockito.Mockito.when(bomb.getStackTrace()).thenThrow(new IllegalStateException("boom"));

        ApiResult<?> result =
                support.handleServiceException(
                        bomb, List.of(), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("ignore-messages 名单为 null 时跳过名单比对，照常打印并返回正确结果")
    void handleServiceException_nullIgnoreMessages_stillLogsAndReturns() {
        ApiResult<?> result =
                support.handleServiceException(
                        new ServiceException("3001", "订单不存在"),
                        null,
                        1,
                        Locale.SIMPLIFIED_CHINESE,
                        registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("异常 message 为 null 时名单比对安全短路，照常打印并返回正确结果")
    void handleServiceException_nullMessage_stillLogsAndReturns() {
        ServiceException ex = new ServiceException("3001", null);

        ApiResult<?> result =
                support.handleServiceException(
                        ex, List.of("会话已过期"), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isNull();
    }

    @Test
    @DisplayName("栈帧为空时循环自然退出（不进循环体），返回正确结果")
    void handleServiceException_emptyStackTrace_loopEndsNaturally() {
        ServiceException ex = new ServiceException("3001", "订单不存在");
        ex.setStackTrace(new StackTraceElement[0]);

        ApiResult<?> result =
                support.handleServiceException(
                        ex, List.of(), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("frames 大于实际非工具类栈帧数时循环自然结束（打印全部而非 break 截断）")
    void handleServiceException_framesExceedStackTrace_loopEndsNaturally() {
        ApiResult<?> result =
                support.handleServiceException(
                        new ServiceException("3001", "订单不存在"),
                        List.of(),
                        Integer.MAX_VALUE,
                        Locale.SIMPLIFIED_CHINESE,
                        registry);

        assertThat(result.getCode()).isEqualTo("3001");
        assertThat(result.getMsg()).isEqualTo("订单不存在");
    }

    // ==================== handleUnexpectedException ====================

    @Test
    void handleUnexpectedException_withoutCause_returnsInternalServerError() {
        ApiResult<?> result =
                support.handleUnexpectedException(
                        new NullPointerException("npe"), List.of(), 1, null, registry);

        assertThat(result.getCode())
                .isEqualTo(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode());
        assertThat(result.getMsg())
                .isEqualTo(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg());
    }

    @Test
    @DisplayName("注册表覆盖 500 文案时，兜底响应使用本地化文案")
    void handleUnexpectedException_registryOverrides500_usesLocalizedMsg() {
        DefaultErrorCodeRegistry overriding =
                new DefaultErrorCodeRegistry(
                        List.<ErrorCodeProvider>of(
                                new ConstantsErrorCodeProvider(),
                                staticProvider(List.of(new ErrorCode("500", "系统开小差了")))),
                        OverridePolicy.LAST_WINS,
                        null);

        ApiResult<?> result =
                support.handleUnexpectedException(
                        new IllegalStateException("boom"),
                        List.of(),
                        1,
                        Locale.SIMPLIFIED_CHINESE,
                        overriding);

        assertThat(result.getCode()).isEqualTo("500");
        assertThat(result.getMsg()).isEqualTo("系统开小差了");
    }

    @Test
    @DisplayName("直接 cause 为 ServiceException 时委托业务异常处理")
    void handleUnexpectedException_causeIsServiceException_delegates() {
        ApiResult<?> result =
                support.handleUnexpectedException(
                        new RuntimeException("wrap", new ServiceException("4000", "内部业务异常")),
                        List.of(),
                        1,
                        Locale.SIMPLIFIED_CHINESE,
                        registry);

        assertThat(result.getCode()).isEqualTo("4000");
        assertThat(result.getMsg()).isEqualTo("内部业务异常");
    }

    @Test
    @DisplayName("cause 链多层（第二层）为 ServiceException 时同样委托")
    void handleUnexpectedException_nestedCauseIsServiceException_delegates() {
        RuntimeException middle =
                new RuntimeException("middle", new ServiceException("5001", "深层业务异常"));
        RuntimeException top = new RuntimeException("top", middle);

        ApiResult<?> result =
                support.handleUnexpectedException(
                        top, List.of(), 1, Locale.SIMPLIFIED_CHINESE, registry);

        assertThat(result.getCode()).isEqualTo("5001");
        assertThat(result.getMsg()).isEqualTo("深层业务异常");
    }

    // ==================== handleNotFound（404 统一出口） ====================

    @Test
    @DisplayName("无注册表时返回 NOT_FOUND 默认文案")
    void handleNotFound_withoutRegistry_returnsDefaultMessage() {
        ApiResult<?> result = support.handleNotFound(Locale.SIMPLIFIED_CHINESE, null);

        assertThat(result.getCode()).isEqualTo(GlobalErrorCodeConstants.NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(GlobalErrorCodeConstants.NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("注册表命中但无翻译时回退错误码默认文案")
    void handleNotFound_registryHit_withoutTranslation_fallsBackToDefault() {
        ApiResult<?> result = support.handleNotFound(Locale.US, registry);

        assertThat(result.getCode()).isEqualTo("404");
        assertThat(result.getMsg()).isEqualTo(GlobalErrorCodeConstants.NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("注册表覆盖 404 文案时，按请求 Locale 返回本地化文案")
    void handleNotFound_registryOverrides404_resolvesLocalizedMessage() {
        DefaultErrorCodeRegistry overriding =
                new DefaultErrorCodeRegistry(
                        List.<ErrorCodeProvider>of(
                                new ConstantsErrorCodeProvider(),
                                staticProvider(
                                        List.of(
                                                new ErrorCode(
                                                        "404",
                                                        "请求未找到",
                                                        Map.of(
                                                                "zh-CN",
                                                                "请求未找到",
                                                                "en-US",
                                                                "Resource not found"))))),
                        OverridePolicy.LAST_WINS,
                        null);

        ApiResult<?> enResult = support.handleNotFound(Locale.US, overriding);
        assertThat(enResult.getCode()).isEqualTo("404");
        assertThat(enResult.getMsg()).isEqualTo("Resource not found");

        ApiResult<?> zhResult = support.handleNotFound(Locale.SIMPLIFIED_CHINESE, overriding);
        assertThat(zhResult.getMsg()).isEqualTo("请求未找到");
    }

    // ==================== findCause ====================

    @Test
    void findCause_directMatch_returnsSelf() {
        ServiceException ex = new ServiceException("1", "x");

        ServiceException found = ExceptionHandlerSupport.findCause(ex, ServiceException.class);

        assertThat(found).isSameAs(ex);
    }

    @Test
    void findCause_nestedCause_returnsMatch() {
        InvalidFormatSimulator inner = new InvalidFormatSimulator("bad value");
        RuntimeException outer = new RuntimeException("wrap", inner);

        InvalidFormatSimulator found =
                ExceptionHandlerSupport.findCause(outer, InvalidFormatSimulator.class);

        assertThat(found).isSameAs(inner);
    }

    @Test
    void findCause_multiLayerPenetration_returnsMatch() {
        // 模拟 WebFlux JSON 解码三层包装：top → middle → inner
        // 搜索类型须与 top/middle 不同类，否则顶层自身匹配（directMatch 语义）而非穿透
        RuntimeException inner = new IllegalStateException("inner");
        RuntimeException middle = new RuntimeException("middle", inner);
        RuntimeException top = new RuntimeException("top", middle);

        IllegalStateException found =
                ExceptionHandlerSupport.findCause(top, IllegalStateException.class);

        assertThat(found).isSameAs(inner);
    }

    @Test
    void findCause_noMatch_returnsNull() {
        RuntimeException ex = new RuntimeException("wrap", new IllegalStateException("cause"));

        ServiceException found = ExceptionHandlerSupport.findCause(ex, ServiceException.class);

        assertThat(found).isNull();
    }

    @Test
    void findCause_nullException_returnsNull() {
        ServiceException found = ExceptionHandlerSupport.findCause(null, ServiceException.class);

        assertThat(found).isNull();
    }

    @Test
    void findCause_nullType_returnsNull() {
        ServiceException found = ExceptionHandlerSupport.findCause(new RuntimeException("x"), null);

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("cause 链成环时防环退出，不死循环")
    void findCause_cyclicCauseChain_terminatesWithoutLoop() {
        CyclicThrowable a = new CyclicThrowable(null);
        CyclicThrowable b = new CyclicThrowable(a);
        a.cause = b; // 构造环：a → b → a

        ServiceException found = ExceptionHandlerSupport.findCause(a, ServiceException.class);

        assertThat(found).isNull();
    }

    /** 用于 findCause 类型匹配的模拟异常。 */
    private static final class InvalidFormatSimulator extends RuntimeException {

        private InvalidFormatSimulator(String message) {
            super(message);
        }
    }

    /** cause 链可手动成环的异常，用于验证 findCause 防环。 */
    private static final class CyclicThrowable extends RuntimeException {

        private Throwable cause;

        private CyclicThrowable(Throwable cause) {
            super("cyclic");
            this.cause = cause;
        }

        @Override
        public Throwable getCause() {
            return cause;
        }
    }
}
