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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link GlobalErrorCodeConstants} 单元测试。
 *
 * <p>
 * 校验全局错误码的取值契约：成功码为字符串 "0"，所有错误码落在 [0, 999] 区间内且彼此不重复；
 * 同时校验常量类契约（final class + 私有构造，消除常量接口反模式）。
 */
class GlobalErrorCodeConstantsTest {

    @Test
    void success_isZeroString() {
        assertEquals("0", GlobalErrorCodeConstants.SUCCESS.getCode());
        assertEquals("成功", GlobalErrorCodeConstants.SUCCESS.getMsg());
    }

    @Test
    void keyErrorCodes_haveExpectedValues() {
        assertEquals("400", GlobalErrorCodeConstants.BAD_REQUEST.getCode());
        assertEquals("401", GlobalErrorCodeConstants.UNAUTHORIZED.getCode());
        assertEquals("403", GlobalErrorCodeConstants.FORBIDDEN.getCode());
        assertEquals("404", GlobalErrorCodeConstants.NOT_FOUND.getCode());
        assertEquals("405", GlobalErrorCodeConstants.METHOD_NOT_ALLOWED.getCode());
        assertEquals("500", GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void allErrorCodes_withinRangeAndUnique() throws IllegalAccessException {
        Set<Integer> codes = new HashSet<>();
        Field[] fields = GlobalErrorCodeConstants.class.getDeclaredFields();
        int errorCodeCount = 0;
        for (Field field : fields) {
            if (field.getType() != ErrorCode.class) {
                continue;
            }
            ErrorCode errorCode = (ErrorCode) field.get(null);
            errorCodeCount++;
            int code = Integer.parseInt(errorCode.getCode());
            // 成功码 0 与错误码均在 [0, 999] 保留区间内
            assertTrue(code >= 0 && code <= 999, "错误码越界: " + code);
            // 错误码不可重复
            assertTrue(codes.add(code), "错误码重复: " + code);
        }
        // 至少覆盖成功码 + 客户端段 + 服务端段（12 个）
        assertTrue(errorCodeCount >= 12, "全局错误码数量异常: " + errorCodeCount);
    }

    @Test
    void constantClassContract_finalClassWithPrivateConstructor() {
        // final class：禁止 implements / 继承（消除常量接口反模式）
        assertTrue(Modifier.isFinal(GlobalErrorCodeConstants.class.getModifiers()));
        // 私有构造：禁止实例化
        for (Constructor<?> constructor :
                GlobalErrorCodeConstants.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        }
    }

    @Test
    void instantiation_throwsIllegalAccess() {
        assertThrows(
                IllegalAccessException.class,
                () -> GlobalErrorCodeConstants.class.getDeclaredConstructor().newInstance());
    }
}
