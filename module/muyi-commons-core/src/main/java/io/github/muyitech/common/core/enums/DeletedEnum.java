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
 * 删除标记枚举。
 *
 * <p>
 * 约定 {@code 1} 表示已删除、{@code 0} 表示未删除，用于逻辑删除场景的查询条件与结果标记。
 *
 * @author keep simple
 * @since 2025/4/7
 */
public enum DeletedEnum implements ArrayValuable<Integer> {

    /**
     * 已删除.
     */
    YES(1, "已删除"),
    /**
     * 未删除.
     */
    NO(0, "未删除");

    private static final Integer[] ARRAYS =
            Arrays.stream(values()).map(DeletedEnum::getCode).toArray(Integer[]::new);

    /**
     * 编码.
     */
    private final Integer code;

    /**
     * 描述.
     */
    private final String desc;

    DeletedEnum(final Integer code, final String desc) {
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
    public static DeletedEnum getEnum(final Integer code) {
        if (Objects.isNull(code)) {
            throw new IllegalArgumentException(
                    DeletedEnum.class.getSimpleName() + " code must not be null.");
        }
        for (DeletedEnum deletedEnum : DeletedEnum.values()) {
            if (deletedEnum.getCode().equals(code)) {
                return deletedEnum;
            }
        }
        throw new IllegalArgumentException(
                DeletedEnum.class.getSimpleName() + " of code " + code + " is non existent.");
    }

    @Override
    public Integer[] array() {
        return ARRAYS.clone();
    }

    @Override
    public String toString() {
        return "DeletedEnum." + name() + "(code=" + code + ", desc=" + desc + ")";
    }
}
