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
package io.github.muyitech.common.spring.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link SortField} 单元测试。
 */
class SortFieldTest {

    @Test
    void orderConstants_haveExpectedValues() {
        assertEquals("asc", SortField.ORDER_ASC);
        assertEquals("desc", SortField.ORDER_DESC);
    }

    @Test
    void allArgsConstructor_setsFields() {
        SortField field = new SortField("createTime", SortField.ORDER_DESC);
        assertEquals("createTime", field.getField());
        assertEquals("desc", field.getOrder());
    }

    @Test
    void noArgsConstructor_createsNullFields() {
        SortField field = new SortField();
        assertNull(field.getField());
        assertNull(field.getOrder());
    }

    @Test
    void setters_roundTrip() {
        SortField field = new SortField();
        field.setField("updatedAt");
        field.setOrder(SortField.ORDER_ASC);
        assertEquals("updatedAt", field.getField());
        assertEquals(SortField.ORDER_ASC, field.getOrder());
    }

    @Test
    void equals_sameValues_true() {
        SortField a = new SortField("createTime", SortField.ORDER_DESC);
        SortField b = new SortField("createTime", SortField.ORDER_DESC);
        assertEquals(a, b);
        // 对称性
        assertEquals(b, a);
        // hashCode 一致性
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstance_true() {
        SortField field = new SortField("id", SortField.ORDER_ASC);
        assertEquals(field, field);
    }

    @Test
    void equals_nullAndDifferentType_false() {
        SortField field = new SortField("id", SortField.ORDER_ASC);
        assertFalse(field.equals(null));
        assertFalse(field.equals(new Object()));
    }

    @Test
    void equals_differentFieldOrOrder_false() {
        SortField base = new SortField("createTime", SortField.ORDER_DESC);
        assertNotEquals(base, new SortField("updatedAt", SortField.ORDER_DESC));
        assertNotEquals(base, new SortField("createTime", SortField.ORDER_ASC));
    }

    @Test
    void equals_nullFields_symmetric() {
        SortField a = new SortField();
        SortField b = new SortField();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new SortField("id", SortField.ORDER_ASC));
    }

    @Test
    void toString_containsFieldValues() {
        SortField field = new SortField("createTime", SortField.ORDER_DESC);
        String text = field.toString();
        assertTrue(text.contains("createTime"));
        assertTrue(text.contains("desc"));
    }
}
