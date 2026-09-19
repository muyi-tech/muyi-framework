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
package io.github.muyitech.boot.webflux.autoconfigure;

import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.webflux.exception.MuyiNotFoundWebExceptionHandler;
import io.github.muyitech.boot.webflux.exception.ReactiveCoreExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * WebFlux 全局异常处理自动配置。
 *
 * <p>
 * 在 Reactive Web 应用中注册 {@link ReactiveCoreExceptionHandler} 与
 * {@link MuyiNotFoundWebExceptionHandler}；业务方已定义同类型 Bean 时自动 back off，
 * 可通过 {@code muyi.webflux.exception-handler.enabled} / {@code handle-not-found} 开关控制。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(DispatcherHandler.class)
@ConditionalOnProperty(
        prefix = "muyi.webflux.exception-handler",
        name = "enabled",
        matchIfMissing = true)
@EnableConfigurationProperties(MuyiWebFluxProperties.class)
public class MuyiWebFluxAutoConfiguration {

    /**
     * 响应式全局异常处理器。
     * @return ReactiveCoreExceptionHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveCoreExceptionHandler reactiveCoreExceptionHandler() {
        return new ReactiveCoreExceptionHandler();
    }

    /**
     * 未匹配路由 404 处理器，受 handle-not-found 开关控制。
     * @return MuyiNotFoundWebExceptionHandler
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "muyi.webflux.exception-handler",
            name = "handle-not-found",
            matchIfMissing = true)
    public MuyiNotFoundWebExceptionHandler muyiNotFoundWebExceptionHandler(
            ObjectProvider<ErrorCodeRegistry> errorCodeRegistry) {
        return new MuyiNotFoundWebExceptionHandler(
                JsonMapper.builder().build(), errorCodeRegistry.getIfAvailable());
    }
}
