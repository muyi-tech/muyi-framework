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
package io.github.muyitech.common.core.beans;

/**
 * 可生成 T 数组的接口。
 *
 * <p>
 * 枚举类实现该接口，可将枚举常量统一暴露为值数组，便于下拉列表、参数校验、批量查询等场景使用。 例如
 * {@code DeletedEnum}、{@code StatusEnum} 均实现了本接口。
 *
 * @param <T> 数组元素类型
 * @author keep simple
 * @since 2025/5/21
 */
public interface ArrayValuable<T> {

    /**
     * 返回枚举全部常量对应的值数组。
     * @return 值数组
     */
    T[] array();
}
