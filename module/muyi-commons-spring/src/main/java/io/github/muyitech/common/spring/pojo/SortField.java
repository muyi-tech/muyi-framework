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
import java.util.Objects;

/**
 * 排序字段 DTO。
 *
 * @author keep simple
 * @since 2024/8/2
 */
public class SortField implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 顺序 - 升序。
     */
    public static final String ORDER_ASC = "asc";

    /**
     * 顺序 - 降序。
     */
    public static final String ORDER_DESC = "desc";

    /**
     * 字段。
     */
    private String field;

    /**
     * 顺序。
     */
    private String order;

    /**
     * 空构造方法。
     */
    public SortField() {}

    /**
     * 全参构造方法。
     * @param field 字段
     * @param order 顺序
     */
    public SortField(String field, String order) {
        this.field = field;
        this.order = order;
    }

    /**
     * 获取字段。
     * @return 字段
     */
    public String getField() {
        return field;
    }

    /**
     * 设置字段。
     * @param field 字段
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * 获取顺序。
     * @return 顺序
     */
    public String getOrder() {
        return order;
    }

    /**
     * 设置顺序。
     * @param order 顺序
     */
    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SortField)) {
            return false;
        }
        SortField that = (SortField) o;
        return Objects.equals(field, that.field) && Objects.equals(order, that.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }

    @Override
    public String toString() {
        return "SortField(field=" + field + ", order=" + order + ")";
    }
}
