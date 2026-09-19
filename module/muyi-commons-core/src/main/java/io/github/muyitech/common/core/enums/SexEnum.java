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
package io.github.muyitech.common.core.enums;

import io.github.muyitech.common.core.beans.ArrayValuable;
import java.util.Arrays;
import java.util.Objects;

/**
 * 性别枚举。
 *
 * <p>
 * 约定 {@code 1} 男、{@code 2} 女、{@code 0} 未知，作为用户性别数据字典。
 *
 * @author keep simple
 * @since 2024/8/2
 */
public enum SexEnum implements ArrayValuable<Integer> {

    /**
     * 男.
     */
    MALE(1, "男"),
    /**
     * 女.
     */
    FEMALE(2, "女"),

    /**
     * 未知.
     */
    UNKNOWN(0, "未知");

    private static final Integer[] ARRAYS =
            Arrays.stream(values()).map(SexEnum::getCode).toArray(Integer[]::new);

    /**
     * 编码.
     */
    private final Integer code;

    /**
     * 描述.
     */
    private final String desc;

    SexEnum(final Integer code, final String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取编码.
     * @return 编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取描述.
     * @return 描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取枚举，未命中或入参为空时抛出 {@link IllegalArgumentException}。
     * @param code 编码
     * @return code 对应的枚举
     */
    public static SexEnum getEnum(final Integer code) {
        if (Objects.isNull(code)) {
            throw new IllegalArgumentException(
                    SexEnum.class.getSimpleName() + " code must not be null.");
        }
        for (SexEnum sexEnum : SexEnum.values()) {
            if (sexEnum.getCode().equals(code)) {
                return sexEnum;
            }
        }
        throw new IllegalArgumentException(
                SexEnum.class.getSimpleName() + " of code " + code + " is non existent.");
    }

    @Override
    public Integer[] array() {
        return ARRAYS.clone();
    }

    @Override
    public String toString() {
        return "SexEnum." + name() + "(code=" + code + ", desc=" + desc + ")";
    }
}
