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

import java.util.List;
import java.util.Objects;

/**
 * 分页查询参数 + 排序。
 *
 * @author keep simple
 * @since 2024/8/2
 */
public class SortablePageParam extends PageParam {

    /**
     * 排序字段。
     */
    private List<SortField> sortingFields;

    /**
     * 获取排序字段。
     * @return 排序字段
     */
    public List<SortField> getSortingFields() {
        return sortingFields;
    }

    /**
     * 设置排序字段。
     * @param sortingFields 排序字段
     */
    public void setSortingFields(List<SortField> sortingFields) {
        this.sortingFields = sortingFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SortablePageParam)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SortablePageParam that = (SortablePageParam) o;
        return Objects.equals(sortingFields, that.sortingFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sortingFields);
    }

    @Override
    public String toString() {
        return "SortablePageParam(super="
                + super.toString()
                + ", sortingFields="
                + sortingFields
                + ")";
    }
}
