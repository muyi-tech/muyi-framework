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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * {@link ServiceException} 单元测试。
 */
class ServiceExceptionTest {

    @Test
    void noArgsConstructor_createsNullFields() {
        ServiceException ex = new ServiceException();
        assertNull(ex.getCode());
        assertNull(ex.getMessage());
    }

    @Test
    void errorCodeConstructor_setsFields() {
        ServiceException ex = new ServiceException(new ErrorCode("1001", "用户不存在"));
        assertEquals("1001", ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void codeMessageConstructor_setsFields() {
        ServiceException ex = new ServiceException("1002", "手机号已存在");
        assertEquals("1002", ex.getCode());
        assertEquals("手机号已存在", ex.getMessage());
    }

    @Test
    void varargsConstructor_storesArgs() {
        ServiceException ex = new ServiceException("1002", "手机号 138 已存在", "138");
        assertEquals("1002", ex.getCode());
        assertEquals("手机号 138 已存在", ex.getMessage());
        assertEquals(1, ex.getArgs().length);
        assertEquals("138", ex.getArgs()[0]);
    }

    @Test
    void codeMessageConstructor_argsRemainNull() {
        ServiceException ex = new ServiceException("1002", "手机号已存在");
        assertNull(ex.getArgs());
    }

    @Test
    void getMessage_returnsPlainMessage_evenWithCause() {
        IOException cause = new IOException("db down");
        ServiceException ex = new ServiceException("1003", "库存不足", cause);
        assertEquals("库存不足", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void extendsNestedRuntimeException_supportsCauseChain() {
        RuntimeException root = new RuntimeException("root");
        IOException middle = new IOException("wrap", root);
        ServiceException ex = new ServiceException("1004", "下单失败", middle);
        // 异常链可用：getMostSpecificCause 取到根因，contains 匹配链上类型
        assertSame(root, ex.getMostSpecificCause());
        assertTrue(ex.contains(IOException.class));
        assertTrue(ex.contains(RuntimeException.class));
        // 无 cause 时，最具体原因即自身
        ServiceException noCause = new ServiceException("1004", "下单失败");
        assertSame(noCause, noCause.getMostSpecificCause());
    }

    @Test
    void errorCodeWithCauseConstructor_preservesChainAndArgs() {
        IllegalStateException cause = new IllegalStateException("lock failed");
        ServiceException ex = new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST, cause);
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    void causeAndArgsConstructor_keepsBoth() {
        IOException cause = new IOException("io");
        ServiceException ex = new ServiceException("1007", "手机号 {} 冲突", cause, "138");
        assertSame(cause, ex.getCause());
        assertEquals(1, ex.getArgs().length);
        assertEquals("138", ex.getArgs()[0]);
    }

    @Test
    void argsDefensiveCopies_isolateInternalState() {
        ServiceException ex = new ServiceException("1005", "手机号 {} 已存在", "138");
        Object[] first = ex.getArgs();
        first[0] = "tampered";
        // 改动返回值不影响内部状态；两次读取亦为不同数组
        assertEquals("138", ex.getArgs()[0]);
        assertNotSame(ex.getArgs(), ex.getArgs());
    }

    @Test
    void contract_noPublicSetters_exceptionIsFinalState() {
        // 终态契约守护：无公开 setter（构造后不可变，可安全跨线程传播）
        assertThat(
                        Arrays.stream(ServiceException.class.getDeclaredMethods())
                                .filter(m -> m.getName().startsWith("set"))
                                .toList())
                .isEmpty();
    }

    @Test
    void varargsConstructor_emptyArgs_normalizedToNull() {
        // args.length == 0 归一为 null：空参数不产生空数组状态
        ServiceException ex = new ServiceException("1009", "演示消息", new Object[0]);
        assertNull(ex.getArgs());
    }

    @Test
    void varargsConstructor_nullArgs_normalizedToNull() {
        // 显式 (Object[]) null：args == null 侧同样归一为 null（4 参构造直传 null 数组）
        ServiceException ex =
                new ServiceException("1011", "演示消息", (Throwable) null, (Object[]) null);
        assertNull(ex.getArgs());
    }

    @Test
    void toString_containsCodeMessageAndArgs() {
        ServiceException ex = new ServiceException("1010", "手机号 {} 已存在", "138");
        String text = ex.toString();
        assertTrue(text.contains("code=1010"));
        assertTrue(text.contains("message=手机号 {} 已存在"));
        assertTrue(text.contains("args=[138]"));
    }

    @Test
    void serializable_roundTrip_preservesFields() throws Exception {
        ServiceException original = new ServiceException("1006", "演示模式，禁止写操作");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ServiceException restored = (ServiceException) in.readObject();
            assertEquals(original.getCode(), restored.getCode());
            assertEquals(original.getMessage(), restored.getMessage());
        }
    }

    @Test
    void serializable_roundTrip_preservesArgsAndCauseChain() throws Exception {
        IOException cause = new IOException("db down");
        // 构造器承载模版原文与 args（格式化由 ServiceExceptionUtil 完成），
        // 序列化回读后模版、args、cause 链均保真
        ServiceException original = new ServiceException("1008", "用户 {} 下单失败", cause, "muyi");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ServiceException restored = (ServiceException) in.readObject();
            assertEquals("1008", restored.getCode());
            assertEquals("用户 {} 下单失败", restored.getMessage());
            assertEquals(1, restored.getArgs().length);
            assertEquals("muyi", restored.getArgs()[0]);
            assertInstanceOf(IOException.class, restored.getCause());
            assertEquals("db down", restored.getCause().getMessage());
        }
    }
}
