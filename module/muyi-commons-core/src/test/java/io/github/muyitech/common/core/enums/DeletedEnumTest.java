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
 * {@link DeletedEnum} 单元测试。
 */
class DeletedEnumTest {

    @Test
    void values_containsYesAndNoInOrder() {
        assertArrayEquals(
                new DeletedEnum[] {DeletedEnum.YES, DeletedEnum.NO}, DeletedEnum.values());
    }

    @Test
    void codeAndDesc_matchesDefinition() {
        assertEquals(1, DeletedEnum.YES.getCode());
        assertEquals("已删除", DeletedEnum.YES.getDesc());
        assertEquals(0, DeletedEnum.NO.getCode());
        assertEquals("未删除", DeletedEnum.NO.getDesc());
    }

    @Test
    void array_matchesCodeOrder() {
        assertArrayEquals(new Integer[] {1, 0}, DeletedEnum.YES.array());
    }

    @Test
    void array_returnsDefensiveCopy() {
        Integer[] first = DeletedEnum.YES.array();
        first[0] = 99;
        assertArrayEquals(new Integer[] {1, 0}, DeletedEnum.YES.array());
    }

    @Test
    void getEnum_found() {
        assertEquals(DeletedEnum.YES, DeletedEnum.getEnum(1));
        assertEquals(DeletedEnum.NO, DeletedEnum.getEnum(0));
    }

    @Test
    void getEnum_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> DeletedEnum.getEnum(null));
    }

    @Test
    void getEnum_notExist_throws() {
        assertThrows(IllegalArgumentException.class, () -> DeletedEnum.getEnum(99));
    }

    @Test
    void toString_containsNameCodeAndDesc() {
        String text = DeletedEnum.NO.toString();
        assertTrue(text.contains("DeletedEnum.NO"));
        assertTrue(text.contains("code=0"));
        assertTrue(text.contains("desc=未删除"));
    }
}
