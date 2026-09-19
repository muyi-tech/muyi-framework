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
package io.github.muyitech.boot.errorcode.autoconfigure;

import io.github.muyitech.boot.errorcode.ConstantsErrorCodeProvider;
import io.github.muyitech.boot.errorcode.DefaultErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.ErrorCodeProvider;
import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.FileErrorCodeProvider;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 错误码注册表自动配置。
 *
 * <p>
 * 装配内容：常量兜底源 + 文件源（locations 非空时）+ 默认注册表。
 * 业务方自定义 {@link ErrorCodeProvider} Bean（DB/Redis/Nacos 动态源）自动接入；
 * 自定义 {@link ErrorCodeRegistry} Bean 时整体 back off。
 *
 * @author keep simple
 * @since 2026/9/14
 */
@AutoConfiguration
@EnableConfigurationProperties(MuyiErrorCodeProperties.class)
public class ErrorCodeAutoConfiguration {

    /**
     * 全局常量兜底源（框架 12 个全局码，最先加载）。
     * @return ConstantsErrorCodeProvider
     */
    @Bean
    @ConditionalOnMissingBean(ConstantsErrorCodeProvider.class)
    public ConstantsErrorCodeProvider constantsErrorCodeProvider() {
        return new ConstantsErrorCodeProvider();
    }

    /**
     * 文件初始化来源（locations 为空时不加载任何文件）。
     * @param properties 错误码配置
     * @return FileErrorCodeProvider
     */
    @Bean
    @ConditionalOnMissingBean(FileErrorCodeProvider.class)
    public FileErrorCodeProvider fileErrorCodeProvider(MuyiErrorCodeProperties properties) {
        return new FileErrorCodeProvider(properties.getLocations());
    }

    /**
     * 默认注册表：按 Provider 顺序构建初始快照。
     * @param providers 错误码来源（有序，含业务自定义动态源）
     * @param properties 错误码配置（覆盖策略）
     * @param applicationContext 应用上下文（事件发布器）
     * @return DefaultErrorCodeRegistry
     */
    @Bean
    @ConditionalOnMissingBean(ErrorCodeRegistry.class)
    public DefaultErrorCodeRegistry errorCodeRegistry(
            ObjectProvider<ErrorCodeProvider> providers,
            MuyiErrorCodeProperties properties,
            ApplicationContext applicationContext) {
        List<ErrorCodeProvider> providerList = providers.orderedStream().toList();
        return new DefaultErrorCodeRegistry(
                providerList, properties.getOverridePolicy(), applicationContext);
    }
}
