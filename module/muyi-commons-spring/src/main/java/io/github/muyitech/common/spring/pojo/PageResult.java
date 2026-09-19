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
package io.github.muyitech.common.spring.pojo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 分页结果。
 *
 * @param <T> 数据泛型
 * @author keep simple
 * @since 2024/8/2
 */
public final class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总量。可空：无总量信息（如空构造）时为 null。
     */
    private @Nullable Long total;

    /**
     * 数据。契约：永不为 null（无数据时为空集合）。
     */
    private List<T> list = Collections.emptyList();

    /**
     * 空构造方法（list 为空集合、total 为 null，非 null 契约不破）。
     */
    public PageResult() {}

    /**
     * 构造分页结果。
     * @param list 数据（null 归一为空集合）
     * @param total 总量
     */
    public PageResult(@Nullable List<T> list, @Nullable Long total) {
        this.list = list == null ? Collections.emptyList() : list;
        this.total = total;
    }

    /**
     * 构造仅包含总量的分页结果。
     * @param total 总量
     */
    public PageResult(@Nullable Long total) {
        this.list = Collections.emptyList();
        this.total = total;
    }

    /**
     * 构造空分页结果。
     * @param <T> 数据泛型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L);
    }

    /**
     * 获取总量。
     * @return 总量
     */
    public @Nullable Long getTotal() {
        return total;
    }

    /**
     * 设置总量。
     * @param total 总量（可为 null）
     */
    public void setTotal(@Nullable Long total) {
        this.total = total;
    }

    /**
     * 获取数据。
     * @return 数据（永不为 null）
     */
    public List<T> getList() {
        return list;
    }

    /**
     * 设置数据（null 归一为空集合，维持 list 非 null 契约）。
     * @param list 数据
     */
    public void setList(@Nullable List<T> list) {
        this.list = list == null ? Collections.emptyList() : list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageResult)) {
            return false;
        }
        PageResult<?> that = (PageResult<?>) o;
        return Objects.equals(total, that.total) && Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, list);
    }

    @Override
    public String toString() {
        return "PageResult(total=" + total + ", list=" + list + ")";
    }
}
