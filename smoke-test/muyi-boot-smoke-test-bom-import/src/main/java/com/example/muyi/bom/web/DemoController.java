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
package com.example.muyi.bom.web;

import com.example.muyi.bom.constant.DemoErrorCode;
import com.example.muyi.bom.exception.DemoModuleException;
import io.github.muyitech.common.core.beans.KeyValue;
import io.github.muyitech.common.core.enums.SexEnum;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import io.github.muyitech.common.spring.pojo.ApiResult;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例接口：演示 Muyi Framework 通用组件用法（与方式一完全一致）。
 *
 * @author keep simple
 * @since 2026/9/12
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 统一响应：ApiResult 包装成功结果。
     *
     * @return 问候语
     */
    @GetMapping("/hello")
    public ApiResult<String> hello() {
        return ApiResult.success("Hello, Muyi Framework!");
    }

    /**
     * 数据字典：KeyValue 容器 + SexEnum 枚举。
     *
     * @return 性别编码/描述列表
     */
    @GetMapping("/sex")
    public ApiResult<List<KeyValue<Integer, String>>> sexEnums() {
        return ApiResult.success(
                Arrays.stream(SexEnum.values())
                        .map(e -> new KeyValue<>(e.getCode(), e.getDesc()))
                        .toList());
    }

    /**
     * 业务异常：抛出 ServiceException，由 CoreExceptionHandler 统一转换为 ApiResult 错误响应。
     *
     * @return 不会正常返回
     */
    @GetMapping("/error")
    public ApiResult<String> error() {
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST);
    }

    /**
     * 模块级自定义拦截：抛出非 {@code ServiceException} 体系的业务专有异常
     * {@link DemoModuleException}，核心处理器不识别该类型，由
     * {@code DemoCustomModuleExceptionHandler} 优先拦截并返回自定义错误响应。
     *
     * @return 不会正常返回
     */
    @GetMapping("/module-error")
    public ApiResult<String> moduleError() {
        throw new DemoModuleException(DemoErrorCode.MODULE_ERROR);
    }
}
