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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link SexEnum} 单元测试。
 */
class SexEnumTest {

    @Test
    void values_containsAllInOrder() {
        assertArrayEquals(
                new SexEnum[] {SexEnum.MALE, SexEnum.FEMALE, SexEnum.UNKNOWN}, SexEnum.values());
    }

    @Test
    void codeAndDesc_matchesDefinition() {
        assertEquals(1, SexEnum.MALE.getCode());
        assertEquals("男", SexEnum.MALE.getDesc());
        assertEquals(2, SexEnum.FEMALE.getCode());
        assertEquals("女", SexEnum.FEMALE.getDesc());
        assertEquals(0, SexEnum.UNKNOWN.getCode());
        assertEquals("未知", SexEnum.UNKNOWN.getDesc());
    }

    @Test
    void array_matchesCodeOrder() {
        assertArrayEquals(new Integer[] {1, 2, 0}, SexEnum.MALE.array());
    }

    @Test
    void array_returnsDefensiveCopy() {
        Integer[] first = SexEnum.MALE.array();
        first[0] = 99;
        assertArrayEquals(new Integer[] {1, 2, 0}, SexEnum.MALE.array());
    }

    @Test
    void getEnum_found() {
        assertEquals(SexEnum.MALE, SexEnum.getEnum(1));
        assertEquals(SexEnum.FEMALE, SexEnum.getEnum(2));
        assertEquals(SexEnum.UNKNOWN, SexEnum.getEnum(0));
    }

    @Test
    void getEnum_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SexEnum.getEnum(null));
    }

    @Test
    void getEnum_notExist_throws() {
        assertThrows(IllegalArgumentException.class, () -> SexEnum.getEnum(99));
    }

    @Test
    void toString_containsFieldValues() {
        String text = SexEnum.MALE.toString();
        assertTrue(text.contains("MALE"));
        assertTrue(text.contains("code=1"));
    }
}
