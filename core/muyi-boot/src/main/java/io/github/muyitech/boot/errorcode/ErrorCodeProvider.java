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
package io.github.muyitech.boot.errorcode;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Collection;
import org.springframework.core.Ordered;

/**
 * 错误码来源 SPI（有序、可插拔）。
 *
 * <p>
 * 实现 {@link Ordered} 控制加载顺序（遵循 Spring 约定：order 值越小越先加载，
 * 配合覆盖策略形成"越靠近业务的来源话语权越大"）：框架内置常量源
 * {@code HIGHEST_PRECEDENCE + 100} 最先、文件源 {@code HIGHEST_PRECEDENCE + 200}
 * 其后；业务方实现 DB/Redis/Nacos 等动态源时注册为 Bean 即自动接入（默认 order
 * 排在最后，话语权最大），无侵入。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public interface ErrorCodeProvider extends Ordered {

    /**
     * 全量加载本来源的错误码。
     * @return 错误码定义集合（可为空集合）
     */
    Collection<ErrorCode> load();

    /**
     * 是否支持运行时重载。文件源 = false（启动期一次性加载）；
     * DB/Redis 等动态源返回 true，{@link ErrorCodeRegistry#reload()} 时才会重新执行。
     * @return 是否支持动态重载
     */
    default boolean supportsDynamicReload() {
        return false;
    }
}
