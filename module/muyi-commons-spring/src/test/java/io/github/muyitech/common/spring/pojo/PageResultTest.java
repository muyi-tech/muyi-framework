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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link PageResult} 单元测试。
 */
class PageResultTest {

    @Test
    void noArgsConstructor_listNeverNull() {
        // 非 null 契约：空构造 list 为空集合（for-each 直接可用），total 未设置时为 null
        PageResult<String> result = new PageResult<>();
        assertEquals(null, result.getTotal());
        assertEquals(java.util.Collections.emptyList(), result.getList());
        result.setList(null);
        assertEquals(java.util.Collections.emptyList(), result.getList());
    }

    @Test
    void listTotalConstructor_setsFields() {
        List<String> list = Arrays.asList("a", "b");
        PageResult<String> result = new PageResult<>(list, 2L);
        assertEquals(list, result.getList());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void totalOnlyConstructor_createsEmptyList() {
        PageResult<String> result = new PageResult<>(5L);
        assertEquals(Collections.emptyList(), result.getList());
        assertEquals(5L, result.getTotal());
    }

    @Test
    void empty_createsZeroTotalEmptyList() {
        PageResult<String> result = PageResult.empty();
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void listTotalConstructor_nullList_normalizedToEmpty() {
        // null 归一契约：list 入参 null 时落为空集合，永不为 null
        PageResult<String> result = new PageResult<>(null, 3L);
        assertEquals(Collections.emptyList(), result.getList());
        assertEquals(3L, result.getTotal());
    }

    @Test
    void setTotal_updatesTotal() {
        PageResult<String> result = new PageResult<>();
        result.setTotal(9L);
        assertEquals(9L, result.getTotal());
        result.setTotal(null);
        assertEquals(null, result.getTotal());
    }

    @Test
    void setList_nonNullList_preserved() {
        // 非 null 侧契约：setList 有值时原样保留（与 null 归一为空集合互补）
        PageResult<String> result = new PageResult<>();
        result.setList(Arrays.asList("a", "b"));
        assertEquals(Arrays.asList("a", "b"), result.getList());
    }

    @Test
    void equals_sameValues_true() {
        PageResult<String> a = new PageResult<>(Arrays.asList("a", "b"), 2L);
        PageResult<String> b = new PageResult<>(Arrays.asList("a", "b"), 2L);
        assertEquals(a, b);
        // 对称性
        assertEquals(b, a);
        // hashCode 一致性
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstance_true() {
        PageResult<String> result = PageResult.empty();
        assertEquals(result, result);
    }

    @Test
    void equals_nullAndDifferentType_false() {
        PageResult<String> result = PageResult.empty();
        assertFalse(result.equals(null));
        assertFalse(result.equals(new Object()));
    }

    @Test
    void equals_differentTotalOrList_false() {
        PageResult<String> base = new PageResult<>(List.of("a"), 1L);
        assertNotEquals(base, new PageResult<>(List.of("a"), 2L));
        assertNotEquals(base, new PageResult<>(List.of("b"), 1L));
        assertNotEquals(base, new PageResult<>(null, 1L));
    }

    @Test
    void equals_nullFields_symmetric() {
        PageResult<String> a = new PageResult<>();
        PageResult<String> b = new PageResult<>();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsValues() {
        PageResult<String> result = new PageResult<>(List.of("a"), 1L);
        String text = result.toString();
        assertTrue(text.contains("total=1"));
        assertTrue(text.contains("a"));
    }
}
