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
package io.github.muyitech.common.spring.validation.telephone;

import io.github.muyitech.common.spring.validation.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * 电话号码校验器。
 *
 * <p>
 * 支持校验中国大陆固定电话（0 开头的区号 + 7-8 位号码）与手机号。
 *
 * @author keep simple
 * @since 2025/5/21
 */
public class TelephoneValidator implements ConstraintValidator<Telephone, String> {

    @Override
    public void initialize(Telephone annotation) {}

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空/空白值默认不校验，即校验通过（非空约束由 @NotBlank 负责；
        // lang3 isBlank 与 ValidationUtils 的 hasText 语义等价）
        if (StringUtils.isBlank(value)) {
            return true;
        }
        // 校验固定电话或手机号
        return ValidationUtils.isTel(value) || ValidationUtils.isMobile(value);
    }
}
