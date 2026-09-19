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
package io.github.muyitech.common.spring.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

/**
 * {@link ValidationUtils} 单元测试。
 */
class ValidationUtilsTest {

    @Test
    void isMobile_acceptsValid() {
        assertTrue(ValidationUtils.isMobile("13800138000"));
        assertTrue(ValidationUtils.isMobile("19912345678"));
        // 各真实号段回归：4[014-9] / 5[0-35-9] / 6[2567] / 7[0-8] / 9[0-35-9]
        assertTrue(ValidationUtils.isMobile("14700001111"));
        assertTrue(ValidationUtils.isMobile("15512345678"));
        assertTrue(ValidationUtils.isMobile("16612345678"));
        assertTrue(ValidationUtils.isMobile("16212345678"));
        assertTrue(ValidationUtils.isMobile("17666666666"));
        assertTrue(ValidationUtils.isMobile("18812345678"));
        assertTrue(ValidationUtils.isMobile("+8613800138000"));
        assertTrue(ValidationUtils.isMobile("008613800138000"));
    }

    @Test
    void isMobile_rejectsInvalid() {
        assertFalse(ValidationUtils.isMobile("123456"));
        assertFalse(ValidationUtils.isMobile("23800138000"));
        assertFalse(ValidationUtils.isMobile(""));
        assertFalse(ValidationUtils.isMobile(null));
        // 未分配号段（第二位/第三位组合不存在）
        assertFalse(ValidationUtils.isMobile("14212345678"));
        assertFalse(ValidationUtils.isMobile("14312345678"));
        assertFalse(ValidationUtils.isMobile("15412345678"));
        assertFalse(ValidationUtils.isMobile("16112345678"));
        assertFalse(ValidationUtils.isMobile("16412345678"));
        assertFalse(ValidationUtils.isMobile("17912345678"));
        assertFalse(ValidationUtils.isMobile("19412345678"));
    }

    @Test
    void isMobile_rejectsCommaLiterals() {
        // 历史 bug 回归：号段字符类混入逗号字面量时以下畸形串被误放行
        assertFalse(ValidationUtils.isMobile("14,12345678"));
        assertFalse(ValidationUtils.isMobile("15,12345678"));
        assertFalse(ValidationUtils.isMobile("16,23456789"));
        assertFalse(ValidationUtils.isMobile("19,12345678"));
    }

    @Test
    void isTel_acceptsValid() {
        assertTrue(ValidationUtils.isTel("010-12345678"));
        assertTrue(ValidationUtils.isTel("0755-1234567"));
        assertTrue(ValidationUtils.isTel("02112345678"));
    }

    @Test
    void isTel_rejectsInvalid() {
        assertFalse(ValidationUtils.isTel("110"));
        assertFalse(ValidationUtils.isTel("1234567890123456"));
        assertFalse(ValidationUtils.isTel(null));
    }

    @Test
    void isURL_acceptsValid() {
        assertTrue(ValidationUtils.isURL("https://example.com"));
        assertTrue(ValidationUtils.isURL("http://localhost:8080/api"));
    }

    @Test
    void isURL_rejectsInvalid() {
        assertFalse(ValidationUtils.isURL("example"));
        assertFalse(ValidationUtils.isURL(""));
    }

    @Test
    void isXmlNCName_acceptsValid() {
        assertTrue(ValidationUtils.isXmlNCName("order_item"));
        assertTrue(ValidationUtils.isXmlNCName("_private1"));
    }

    @Test
    void isXmlNCName_rejectsInvalid() {
        assertFalse(ValidationUtils.isXmlNCName("1abc"));
        assertFalse(ValidationUtils.isXmlNCName("a b"));
        assertFalse(ValidationUtils.isXmlNCName(""));
        assertFalse(ValidationUtils.isXmlNCName(null));
    }

    static class DemoBean {

        @NotNull(message = "id 不能为空")
        private Integer id;

        @NotBlank(message = "name 不能为空")
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void validate_passesWhenConstraintsSatisfied() {
        DemoBean bean = new DemoBean();
        bean.setId(1);
        bean.setName("张三");
        ValidationUtils.validate(bean);
    }

    @Test
    void validate_throwsWhenConstraintsViolated() {
        DemoBean bean = new DemoBean();
        assertThrows(ConstraintViolationException.class, () -> ValidationUtils.validate(bean));
    }

    @Test
    void validate_withGroup_passes() {
        DemoBean bean = new DemoBean();
        bean.setId(1);
        bean.setName("张三");
        ValidationUtils.validate(bean);
        assertEquals(1, bean.getId());
    }
}
