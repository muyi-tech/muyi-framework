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

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link PageParam} 单元测试。
 *
 * <p>
 * 覆盖默认值与分页参数的 {@code @NotNull}/{@code @Min}/{@code @Max} 校验约束。
 */
class PageParamTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory =
                jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void defaultValues_arePageOneSizeTen() {
        PageParam param = new PageParam();
        assertEquals(1, param.getPageNo());
        assertEquals(10, param.getPageSize());
    }

    @Test
    void pageSizeNone_isMinusOne() {
        assertEquals(-1, PageParam.PAGE_SIZE_NONE);
    }

    @Test
    void settersAndGetters_work() {
        PageParam param = new PageParam();
        param.setPageNo(2);
        param.setPageSize(50);
        assertEquals(2, param.getPageNo());
        assertEquals(50, param.getPageSize());
    }

    @Test
    void defaultParam_passesValidation() {
        assertTrue(validator.validate(new PageParam()).isEmpty());
    }

    @Test
    void nullPageNo_failsValidation() {
        PageParam param = new PageParam();
        param.setPageNo(null);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void zeroPageNo_failsValidation() {
        PageParam param = new PageParam();
        param.setPageNo(0);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void pageSizeOverMax_failsValidation() {
        PageParam param = new PageParam();
        param.setPageSize(101);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void nullPageSize_failsValidation() {
        PageParam param = new PageParam();
        param.setPageSize(null);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void pageSizeNone_rejectedByBeanValidation() {
        // 契约：PAGE_SIZE_NONE(-1) 仅限服务端内部旁路；@Valid 请求绑定必须拒绝，
        // 防止外部调用方拉全量（见 PageParam#PAGE_SIZE_NONE javadoc）
        PageParam param = new PageParam();
        param.setPageSize(PageParam.PAGE_SIZE_NONE);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void equals_sameValues_true() {
        PageParam a = new PageParam();
        a.setPageNo(2);
        a.setPageSize(20);
        PageParam b = new PageParam();
        b.setPageNo(2);
        b.setPageSize(20);
        assertEquals(a, b);
        // 对称性
        assertEquals(b, a);
        // hashCode 一致性
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstance_true() {
        PageParam param = new PageParam();
        assertEquals(param, param);
    }

    @Test
    void equals_nullAndDifferentType_false() {
        PageParam param = new PageParam();
        assertFalse(param.equals(null));
        assertFalse(param.equals(new Object()));
    }

    @Test
    void equals_differentPageNoOrPageSize_false() {
        PageParam base = new PageParam();
        PageParam otherPageNo = new PageParam();
        otherPageNo.setPageNo(2);
        assertNotEquals(base, otherPageNo);
        PageParam otherPageSize = new PageParam();
        otherPageSize.setPageSize(20);
        assertNotEquals(base, otherPageSize);
    }

    @Test
    void toString_containsValues() {
        PageParam param = new PageParam();
        String text = param.toString();
        assertTrue(text.contains("pageNo=1"));
        assertTrue(text.contains("pageSize=10"));
    }
}
