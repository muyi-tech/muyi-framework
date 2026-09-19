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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@link ServiceExceptionUtil} 单元测试。
 */
class ServiceExceptionUtilTest {

    @Test
    void exception_usesErrorCode() {
        ServiceException ex = ServiceExceptionUtil.exception(new ErrorCode("1001", "用户不存在"));
        assertEquals("1001", ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void exception_formatsParams() {
        ServiceException ex =
                ServiceExceptionUtil.exception(new ErrorCode("1002", "用户 {} 不存在"), "张三");
        assertEquals("1002", ex.getCode());
        assertEquals("用户 张三 不存在", ex.getMessage());
        // 格式化参数随异常携带，供响应端 i18n 重新格式化
        assertEquals(1, ex.getArgs().length);
        assertEquals("张三", ex.getArgs()[0]);
    }

    @Test
    void invalidParamException_usesBadRequestCode() {
        ServiceException ex = ServiceExceptionUtil.invalidParamException("参数 {} 不正确", "pageNo");
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("参数 pageNo 不正确", ex.getMessage());
    }

    @Test
    void doFormat_replacesPlaceholdersInOrder() {
        String message = ServiceExceptionUtil.doFormat("1003", "{} 和 {} 冲突", "A", "B");
        assertEquals("A 和 B 冲突", message);
    }

    @Test
    void doFormat_extraParams_returnsPattern() {
        String message = ServiceExceptionUtil.doFormat("1004", "无占位符消息", "多余参数");
        assertEquals("无占位符消息", message);
    }

    @Test
    void doFormat_missingParams_appendsRemainder() {
        String message = ServiceExceptionUtil.doFormat("1005", "有 {} 但缺少 {} 参数", "第一个");
        assertEquals("有 第一个 但缺少 {} 参数", message);
    }

    @Test
    void doFormat_excessParamsAfterSubstitution_appendsRemainder() {
        // 占位符替换过程中，剩余参数多于占位符，触发 i != 0 的截断分支
        String message = ServiceExceptionUtil.doFormat("1006", "{} 已存在", "A", "B");
        assertEquals("A 已存在", message);
    }

    @Test
    void doFormat_emptyParams_returnsPatternAsIs() {
        // 无参数时原样返回模版：默认消息携带 {} 占位符属正常形态（占位符由响应端 i18n 填充），
        // 不做占位符检查、不记"参数过少"日志
        assertEquals("默认消息 {} 保持原样", ServiceExceptionUtil.doFormat("1008", "默认消息 {} 保持原样"));
        assertEquals(
                "默认消息 {} 保持原样",
                ServiceExceptionUtil.doFormat("1008", "默认消息 {} 保持原样", (Object[]) null));
    }

    @Test
    void doFormat_nullPattern_returnsNull() {
        assertNull(ServiceExceptionUtil.doFormat("1009", null, "参数"));
        assertNull(ServiceExceptionUtil.doFormat("1009", null));
    }

    @Test
    void exception0_buildsServiceExceptionWithCode() {
        ServiceException ex = ServiceExceptionUtil.exception0("1007", "余额 {} 不足", 5);
        assertSame(ServiceException.class, ex.getClass());
        assertEquals("1007", ex.getCode());
        assertEquals("余额 5 不足", ex.getMessage());
    }

    @Test
    void exception_withCause_preservesChain() {
        IOException cause = new IOException("db down");
        ServiceException ex =
                ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, cause);
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    void exception0_withCauseAndParams_formatsMessageAndKeepsChain() {
        IllegalStateException cause = new IllegalStateException("lock");
        ServiceException ex = ServiceExceptionUtil.exception0("1008", "用户 {} 下单失败", cause, "muyi");
        assertEquals("用户 muyi 下单失败", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertEquals(1, ex.getArgs().length);
        assertEquals("muyi", ex.getArgs()[0]);
    }
}
