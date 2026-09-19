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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * 分页查询参数。
 *
 * @author keep simple
 * @since 2024/8/2
 */
public class PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Integer PAGE_NO = 1;

    private static final Integer PAGE_SIZE = 10;

    /**
     * 每页条数 - 不分页（服务端内部旁路专用）。
     *
     * <p>
     * 例如说，导出接口在服务端代码里把已构造的 {@link #pageSize} 设为 -1 表示查询全量。
     *
     * <p>
     * ⚠️ 不可用于请求入参：{@link #pageSize} 上的 {@code @Min(1)} 会在 Bean Validation
     * （@Valid 绑定）下拒绝 -1——防止外部调用方借"不分页"拉取全表。需要全量请走显式的
     * 导出/批处理接口，由服务端自行设置。
     */
    public static final Integer PAGE_SIZE_NONE = -1;

    /**
     * 当前页码，从 1 开始。
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    /**
     * 每页条数，最大值为 100。
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 100, message = "每页条数最大值为 100")
    private Integer pageSize = PAGE_SIZE;

    /**
     * 获取当前页码。
     * @return 当前页码，从 1 开始
     */
    public Integer getPageNo() {
        return pageNo;
    }

    /**
     * 设置当前页码。
     * @param pageNo 当前页码，从 1 开始
     */
    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    /**
     * 获取每页条数。
     * @return 每页条数
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页条数。
     * @param pageSize 每页条数
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageParam)) {
            return false;
        }
        PageParam that = (PageParam) o;
        return Objects.equals(pageNo, that.pageNo) && Objects.equals(pageSize, that.pageSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNo, pageSize);
    }

    @Override
    public String toString() {
        return "PageParam(pageNo=" + pageNo + ", pageSize=" + pageSize + ")";
    }
}
