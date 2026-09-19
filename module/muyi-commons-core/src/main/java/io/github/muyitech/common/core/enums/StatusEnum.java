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
 * 通用状态枚举。
 *
 * <p>
 * 约定 {@code 0} 表示开启、{@code 1} 表示关闭，用于实体启停状态的存储与展示。
 *
 * @author keep simple
 * @since 2025/4/7
 */
public enum StatusEnum implements ArrayValuable<Integer> {

    /**
     * 开启.
     */
    ENABLE(0, "开启"),
    /**
     * 关闭.
     */
    DISABLE(1, "关闭");

    private static final Integer[] ARRAYS =
            Arrays.stream(values()).map(StatusEnum::getCode).toArray(Integer[]::new);

    /**
     * 编码.
     */
    private final Integer code;

    /**
     * 描述.
     */
    private final String desc;

    StatusEnum(final Integer code, final String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取编码，与 {@code SexEnum#getCode()} / {@code DeletedEnum#getCode()} 命名契约一致。
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
     * @param code 状态编码
     * @return code 对应的枚举
     */
    public static StatusEnum getEnum(final Integer code) {
        if (Objects.isNull(code)) {
            throw new IllegalArgumentException(
                    StatusEnum.class.getSimpleName() + " code must not be null.");
        }
        for (StatusEnum statusEnum : StatusEnum.values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException(
                StatusEnum.class.getSimpleName() + " of code " + code + " is non existent.");
    }

    @Override
    public Integer[] array() {
        return ARRAYS.clone();
    }

    @Override
    public String toString() {
        return "StatusEnum." + name() + "(code=" + code + ", desc=" + desc + ")";
    }

    /**
     * 判断是否处于开启状态。
     * @param code 状态编码
     * @return 开启返回 true；为空或非开启返回 false
     */
    public static boolean isEnable(Integer code) {
        return Objects.equals(ENABLE.code, code);
    }

    /**
     * 判断是否处于关闭状态。
     * @param code 状态编码
     * @return 关闭返回 true；为空或非关闭返回 false
     */
    public static boolean isDisable(Integer code) {
        return Objects.equals(DISABLE.code, code);
    }
}
