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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.muyitech.common.spring.validation.mobile.MobileValidator;
import io.github.muyitech.common.spring.validation.telephone.TelephoneValidator;
import org.junit.jupiter.api.Test;

/**
 * {@link MobileValidator} 与 {@link TelephoneValidator} 单元测试。
 */
class MobileTelephoneValidatorTest {

    private final MobileValidator mobileValidator = new MobileValidator();

    private final TelephoneValidator telephoneValidator = new TelephoneValidator();

    @Test
    void mobile_initialize_executesWithoutError() {
        // initialize 为空实现，验证其可被正常调用（jakarta 校验框架会调用该方法）
        mobileValidator.initialize(null);
    }

    @Test
    void telephone_initialize_executesWithoutError() {
        telephoneValidator.initialize(null);
    }

    @Test
    void mobile_validatesCorrectly() {
        assertTrue(mobileValidator.isValid("13800138000", null));
        assertTrue(mobileValidator.isValid("", null)); // 空值不校验
        assertTrue(mobileValidator.isValid(null, null));
        assertFalse(mobileValidator.isValid("123456", null));
        // 逗号字面量负例（历史正则 bug 回归）
        assertFalse(mobileValidator.isValid("14,12345678", null));
        assertFalse(mobileValidator.isValid("15,12345678", null));
    }

    @Test
    void telephone_acceptsMobileAndLandline() {
        assertTrue(telephoneValidator.isValid("13800138000", null)); // 手机号
        assertTrue(telephoneValidator.isValid("010-12345678", null)); // 固定电话
        assertTrue(telephoneValidator.isValid("", null)); // 空值不校验
        assertTrue(telephoneValidator.isValid(null, null));
        assertFalse(telephoneValidator.isValid("abc", null));
    }
}
