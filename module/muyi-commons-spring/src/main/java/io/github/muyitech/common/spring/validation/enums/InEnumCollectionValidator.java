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

import io.github.muyitech.common.core.beans.ArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 自定义枚举校验注解验证器（集合）。
 *
 * <p>
 * 取值集合与元素统一按字符串形态比对，兼容 Integer/Long/String 等 code 类型。
 *
 * @author keep simple
 * @since 2025/4/7
 */
public class InEnumCollectionValidator implements ConstraintValidator<InEnum, Collection<?>> {

    /** 枚举声明的合法取值集合（字符串形态，全部命中才通过） */
    private Set<String> values;

    @Override
    public void initialize(InEnum annotation) {
        ArrayValuable<?>[] constants = annotation.value().getEnumConstants();
        this.values =
                Arrays.stream(constants)
                        .flatMap(constant -> Arrays.stream(constant.array()))
                        .map(String::valueOf)
                        .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(Collection<?> list, ConstraintValidatorContext context) {
        // null / 空集合视为校验通过（非空约束由 @NotNull 负责）
        if (CollectionUtils.isEmpty(list)) {
            return true;
        }
        // 全部元素命中取值集合才算通过（null 元素不放行）
        boolean passed =
                list.stream()
                                .filter(Objects::nonNull)
                                .allMatch(item -> values.contains(String.valueOf(item)))
                        && list.stream().noneMatch(Objects::isNull);
        if (passed) {
            return true;
        }
        // 校验不通过，自定义提示语句（因为，注解上的 value 是枚举类，无法获得枚举类的实际值）
        context.disableDefaultConstraintViolation(); // 禁用默认的 message 的值
        context.buildConstraintViolationWithTemplate(
                        context.getDefaultConstraintMessageTemplate()
                                .replaceAll("\\{value}", StringUtils.join(list, ",")))
                .addConstraintViolation(); // 重新添加错误提示语句
        return false;
    }
}
