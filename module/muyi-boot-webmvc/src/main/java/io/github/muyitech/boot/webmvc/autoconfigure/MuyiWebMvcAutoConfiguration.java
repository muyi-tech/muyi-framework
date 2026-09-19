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
package io.github.muyitech.boot.webmvc.autoconfigure;

import io.github.muyitech.boot.webmvc.exception.CoreExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * WebMVC 全局异常处理自动配置。
 *
 * <p>
 * 在 Servlet Web 应用中注册 {@link CoreExceptionHandler}；业务方已定义同类型 Bean 时
 * 自动 back off，可通过 {@code muyi.webmvc.exception-handler.enabled} 开关关闭。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(
        prefix = "muyi.webmvc.exception-handler",
        name = "enabled",
        matchIfMissing = true)
@EnableConfigurationProperties(MuyiWebMvcProperties.class)
public class MuyiWebMvcAutoConfiguration {

    /**
     * 全局异常处理器。
     * @return CoreExceptionHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public CoreExceptionHandler coreExceptionHandler() {
        return new CoreExceptionHandler();
    }
}
