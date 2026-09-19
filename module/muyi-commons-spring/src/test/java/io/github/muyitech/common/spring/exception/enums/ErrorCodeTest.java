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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link ErrorCode} 单元测试。
 */
class ErrorCodeTest {

    @Test
    void constructor_setsCodeAndMsg() {
        ErrorCode errorCode = new ErrorCode("400", "请求参数不正确");
        assertEquals("400", errorCode.getCode());
        assertEquals("请求参数不正确", errorCode.getMsg());
    }

    @Test
    void messagesConstructor_normalizesNullToEmptyMap() {
        ErrorCode withMessages =
                new ErrorCode("1001001000", "用户不存在", Map.of("en-US", "User not found"));
        assertEquals("User not found", withMessages.getMessages().get("en-US"));

        ErrorCode nullMessages = new ErrorCode("1001001001", "用户不存在", null);
        assertTrue(nullMessages.getMessages().isEmpty());
    }

    @Test
    void equals_comparesByCode() {
        // 码即身份：同 code 不同 msg 仍然相等
        ErrorCode a = new ErrorCode("400", "请求参数不正确");
        ErrorCode b = new ErrorCode("400", "请求非法");
        ErrorCode c = new ErrorCode("500", "系统异常");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, new Object());
    }

    @Test
    void equals_sameInstance_shortCircuitsTrue() {
        // 同引用短路：this == o 直接返回 true（不进入 instanceof / code 比较）
        ErrorCode errorCode = new ErrorCode("400", "请求参数不正确");
        assertTrue(errorCode.equals(errorCode));
    }

    @Test
    void toString_containsCodeAndMsg() {
        String str = new ErrorCode("404", "请求未找到").toString();
        assertTrue(str.contains("404"));
        assertTrue(str.contains("请求未找到"));
    }
}
