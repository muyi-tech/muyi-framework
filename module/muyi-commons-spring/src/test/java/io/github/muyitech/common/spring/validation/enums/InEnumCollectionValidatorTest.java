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
package io.github.muyitech.common.spring.validation.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.muyitech.common.core.enums.StatusEnum;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link InEnumCollectionValidator} 单元测试。
 *
 * <p>
 * 重点覆盖 null/空集合短路（NPE 修复点）与非法值时的自定义消息模板替换逻辑。
 */
class InEnumCollectionValidatorTest {

    private InEnumCollectionValidator validator;

    private ConstraintValidatorContext context;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        validator = new InEnumCollectionValidator();
        InEnum annotation = mock(InEnum.class);
        when(annotation.value()).thenReturn((Class) StatusEnum.class);
        validator.initialize(annotation);
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void initialize_extractsValuesFromEnum() {
        // StatusEnum 的合法值为 {0, 1}
        assertTrue(validator.isValid(Arrays.asList(0, 1), context));
    }

    @Test
    void isValid_nullCollection_returnsTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_emptyCollection_returnsTrue() {
        assertTrue(validator.isValid(Collections.emptyList(), context));
    }

    @Test
    void isValid_allValuesPresent_returnsTrue() {
        assertTrue(validator.isValid(Arrays.asList(0), context));
    }

    @Test
    void isValid_invalidValue_returnsFalseAndReplacesTemplate() {
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.getDefaultConstraintMessageTemplate()).thenReturn("{value} 不是合法的枚举值");
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        boolean valid = validator.isValid(List.of(999), context);

        assertFalse(valid);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("999 不是合法的枚举值");
        verify(builder).addConstraintViolation();
    }

    @Test
    void isValid_collectionWithNullElement_returnsFalseAndReplacesTemplate() {
        // 合法值 + null 混合集合：合法值通过 allMatch，null 元素在 noneMatch(Objects::isNull) 处拦截
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.getDefaultConstraintMessageTemplate()).thenReturn("{value} 不是合法的枚举值");
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        boolean valid = validator.isValid(Arrays.asList(0, null), context);

        assertFalse(valid);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
        verify(builder).addConstraintViolation();
    }
}
