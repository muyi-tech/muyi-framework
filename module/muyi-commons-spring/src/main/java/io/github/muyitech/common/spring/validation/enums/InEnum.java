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
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义枚举校验注解：字段值必须命中枚举 {@link ArrayValuable#array()} 声明的取值集合。
 *
 * <p>
 * 支持任意 code 类型（Integer/Long/String 等）：校验时统一按字符串形态比对，
 * 避免包装类型不一致（如 {@code Long[]} 枚举数组 vs {@code Integer} 字段）导致静默失败。
 *
 * @author keep simple
 * @since 2025/4/7
 */
@Target({
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.PARAMETER,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InEnumValidator.class, InEnumCollectionValidator.class})
public @interface InEnum {

    /**
     * @return 实现 {@link ArrayValuable} 接口的枚举类
     */
    Class<? extends ArrayValuable<?>> value();

    /**
     * 校验失败时的提示消息。
     * @return 提示消息
     */
    String message() default "{value} 不是合法的枚举值";

    /**
     * 验证组。
     * @return 验证组
     */
    Class<?>[] groups() default {};

    /**
     * 负载。
     * @return 负载
     */
    Class<? extends Payload>[] payload() default {};
}
