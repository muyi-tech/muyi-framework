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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.muyitech.common.core.beans.ArrayValuable;
import io.github.muyitech.common.core.enums.StatusEnum;
import io.github.muyitech.common.spring.validation.enums.InEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link io.github.muyitech.common.spring.validation.enums.InEnum} 校验单元测试。
 */
class InEnumValidatorTest {

    private static Validator validator;

    static class SingleBean {

        @InEnum(StatusEnum.class)
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    static class CollectionBean {

        @InEnum(StatusEnum.class)
        private List<Integer> statuses;

        public List<Integer> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<Integer> statuses) {
            this.statuses = statuses;
        }
    }

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void single_acceptsValidValue() {
        SingleBean bean = new SingleBean();
        bean.setStatus(StatusEnum.ENABLE.getCode());
        assertTrue(validator.validate(bean).isEmpty());
    }

    @Test
    void single_rejectsInvalidValue() {
        SingleBean bean = new SingleBean();
        bean.setStatus(999);
        assertFalse(validator.validate(bean).isEmpty());
    }

    @Test
    void single_acceptsNull() {
        SingleBean bean = new SingleBean();
        assertTrue(validator.validate(bean).isEmpty());
    }

    @Test
    void collection_acceptsAllValidValues() {
        CollectionBean bean = new CollectionBean();
        bean.setStatuses(Arrays.asList(StatusEnum.ENABLE.getCode(), StatusEnum.DISABLE.getCode()));
        assertTrue(validator.validate(bean).isEmpty());
    }

    @Test
    void collection_rejectsAnyInvalidValue() {
        CollectionBean bean = new CollectionBean();
        bean.setStatuses(Arrays.asList(StatusEnum.ENABLE.getCode(), 999));
        assertFalse(validator.validate(bean).isEmpty());
        assertEquals(1, validator.validate(bean).size());
    }

    @Test
    void collection_acceptsEmptyOrNull() {
        CollectionBean bean = new CollectionBean();
        bean.setStatuses(Collections.emptyList());
        assertTrue(validator.validate(bean).isEmpty());
        assertTrue(validator.validate(new CollectionBean()).isEmpty());
    }

    /** Long 型 code 枚举（历史 bug 回归：Integer 字段曾与 Long[] 数组 contains 失配） */
    public enum LongCodeEnum implements ArrayValuable<Long> {
        FIRST(1L),
        SECOND(2L);

        public static final Long[] ARRAYS =
                Arrays.stream(values()).map(LongCodeEnum::getCode).toArray(Long[]::new);

        private final Long code;

        LongCodeEnum(Long code) {
            this.code = code;
        }

        public Long getCode() {
            return code;
        }

        @Override
        public Long[] array() {
            return ARRAYS;
        }
    }

    static class LongCodeBean {

        @InEnum(LongCodeEnum.class)
        private Integer intField;

        @InEnum(LongCodeEnum.class)
        private String stringField;

        public Integer getIntField() {
            return intField;
        }

        public void setIntField(Integer intField) {
            this.intField = intField;
        }

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }
    }

    @Test
    void crossTypeComparisons_acceptViaStringNormalization() {
        // Integer 字段命中 Long[] 枚举（修复前 List<Long>.contains(Integer) 恒 false）
        LongCodeBean bean = new LongCodeBean();
        bean.setIntField(1);
        bean.setStringField("2");
        assertTrue(validator.validate(bean).isEmpty());
    }

    @Test
    void crossTypeComparisons_stillRejectUnknown() {
        LongCodeBean bean = new LongCodeBean();
        bean.setIntField(3);
        bean.setStringField("x");
        assertFalse(validator.validate(bean).isEmpty());
        assertEquals(2, validator.validate(bean).size());
    }
}
