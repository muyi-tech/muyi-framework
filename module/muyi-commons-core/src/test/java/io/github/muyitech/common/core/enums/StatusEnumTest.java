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
package io.github.muyitech.common.core.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link StatusEnum} 单元测试。
 */
class StatusEnumTest {

    @Test
    void values_containsEnableAndDisableInOrder() {
        assertArrayEquals(
                new StatusEnum[] {StatusEnum.ENABLE, StatusEnum.DISABLE}, StatusEnum.values());
    }

    @Test
    void statusAndDesc_matchesDefinition() {
        assertEquals(0, StatusEnum.ENABLE.getCode());
        assertEquals("开启", StatusEnum.ENABLE.getDesc());
        assertEquals(1, StatusEnum.DISABLE.getCode());
        assertEquals("关闭", StatusEnum.DISABLE.getDesc());
    }

    @Test
    void array_matchesStatusOrder() {
        assertArrayEquals(new Integer[] {0, 1}, StatusEnum.ENABLE.array());
    }

    @Test
    void array_returnsDefensiveCopy() {
        Integer[] first = StatusEnum.ENABLE.array();
        first[0] = 99;
        assertArrayEquals(new Integer[] {0, 1}, StatusEnum.ENABLE.array());
    }

    @Test
    void getEnum_found() {
        assertEquals(StatusEnum.ENABLE, StatusEnum.getEnum(0));
        assertEquals(StatusEnum.DISABLE, StatusEnum.getEnum(1));
    }

    @Test
    void getEnum_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> StatusEnum.getEnum(null));
    }

    @Test
    void getEnum_notExist_throws() {
        assertThrows(IllegalArgumentException.class, () -> StatusEnum.getEnum(99));
    }

    @Test
    void isEnable() {
        assertTrue(StatusEnum.isEnable(0));
        assertFalse(StatusEnum.isEnable(1));
        assertFalse(StatusEnum.isEnable(null));
    }

    @Test
    void isDisable() {
        assertTrue(StatusEnum.isDisable(1));
        assertFalse(StatusEnum.isDisable(0));
        assertFalse(StatusEnum.isDisable(null));
    }

    @Test
    void toString_containsNameCodeAndDesc() {
        String text = StatusEnum.ENABLE.toString();
        assertTrue(text.contains("StatusEnum.ENABLE"));
        assertTrue(text.contains("code=0"));
        assertTrue(text.contains("desc=开启"));
    }
}
