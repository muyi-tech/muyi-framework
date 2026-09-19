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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link SortablePageParam} 单元测试。
 */
class SortablePageParamTest {

    @Test
    void inheritsPageParamDefaults() {
        SortablePageParam param = new SortablePageParam();
        assertEquals(1, param.getPageNo());
        assertEquals(10, param.getPageSize());
        assertNull(param.getSortingFields());
    }

    @Test
    void sortingFields_setAndGet() {
        SortablePageParam param = new SortablePageParam();
        List<SortField> fields = Arrays.asList(new SortField("createTime", SortField.ORDER_DESC));
        param.setSortingFields(fields);
        assertEquals(fields, param.getSortingFields());
    }

    @Test
    void inheritedSetters_roundTrip() {
        SortablePageParam param = new SortablePageParam();
        param.setPageNo(3);
        param.setPageSize(30);
        assertEquals(3, param.getPageNo());
        assertEquals(30, param.getPageSize());
    }

    @Test
    void equals_sameValues_true() {
        SortablePageParam a = new SortablePageParam();
        a.setSortingFields(List.of(new SortField("id", SortField.ORDER_ASC)));
        SortablePageParam b = new SortablePageParam();
        b.setSortingFields(List.of(new SortField("id", SortField.ORDER_ASC)));
        assertEquals(a, b);
        // 对称性
        assertEquals(b, a);
        // hashCode 一致性
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstance_true() {
        SortablePageParam param = new SortablePageParam();
        assertEquals(param, param);
    }

    @Test
    void equals_nullAndDifferentType_false() {
        SortablePageParam param = new SortablePageParam();
        assertFalse(param.equals(null));
        assertFalse(param.equals(new Object()));
        // 父类实例不等于子类（instanceof SortablePageParam 为 false）
        assertFalse(param.equals(new PageParam()));
    }

    @Test
    void equals_differentSuperFields_false() {
        // super.equals() 为 false 的分支：分页字段不同
        SortablePageParam a = new SortablePageParam();
        SortablePageParam b = new SortablePageParam();
        b.setPageNo(2);
        assertNotEquals(a, b);
    }

    @Test
    void equals_differentSortingFields_false() {
        SortablePageParam a = new SortablePageParam();
        a.setSortingFields(List.of(new SortField("id", SortField.ORDER_ASC)));
        SortablePageParam b = new SortablePageParam();
        assertNotEquals(a, b);
    }

    @Test
    void equals_nullSortingFields_symmetric() {
        SortablePageParam a = new SortablePageParam();
        SortablePageParam b = new SortablePageParam();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsSuperAndSortingFields() {
        SortablePageParam param = new SortablePageParam();
        param.setSortingFields(List.of(new SortField("id", SortField.ORDER_ASC)));
        String text = param.toString();
        assertTrue(text.contains("SortablePageParam"));
        assertTrue(text.contains("id"));
        assertEquals(SortField.ORDER_ASC, param.getSortingFields().get(0).getOrder());
    }
}
