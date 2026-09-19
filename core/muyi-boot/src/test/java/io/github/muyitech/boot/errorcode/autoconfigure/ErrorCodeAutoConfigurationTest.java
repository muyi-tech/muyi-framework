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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.muyitech.boot.errorcode.ConstantsErrorCodeProvider;
import io.github.muyitech.boot.errorcode.DefaultErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.ErrorCodeProvider;
import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.errorcode.FileErrorCodeProvider;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ErrorCodeAutoConfiguration} 装配测试。
 *
 * <p>
 * 覆盖：默认装配（常量兜底源 + 文件源 + 默认注册表）、locations 绑定文件加载、
 * 业务自定义 {@link ErrorCodeProvider} 自动接入、自定义 {@link ErrorCodeRegistry}
 * 时注册表 Bean back off（provider 仍装配）、@ConditionalOnMissingBean 各分支。
 *
 * @author keep simple
 * @since 2026/9/17
 */
class ErrorCodeAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ErrorCodeAutoConfiguration.class));

    /** 测试用静态来源工厂：固定返回给定错误码集合。 */
    private static ErrorCodeProvider staticProvider(Collection<ErrorCode> codes) {
        return new ErrorCodeProvider() {
            @Override
            public int getOrder() {
                return 0;
            }

            @Override
            public Collection<ErrorCode> load() {
                return codes;
            }
        };
    }

    @Test
    @DisplayName("默认装配：常量兜底源 + 文件源 + 默认注册表全部就位，全局码可查")
    void defaultAssembly_registersAllBeans() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(ConstantsErrorCodeProvider.class);
                    assertThat(context).hasSingleBean(FileErrorCodeProvider.class);
                    assertThat(context).hasSingleBean(ErrorCodeRegistry.class);
                    assertThat(context.getBean(ErrorCodeRegistry.class))
                            .isInstanceOf(DefaultErrorCodeRegistry.class);
                    // 全局常量码（BAD_REQUEST=400）经常量兜底源进入注册表
                    assertThat(context.getBean(ErrorCodeRegistry.class).find("400")).isPresent();
                });
    }

    @Test
    @DisplayName("locations 绑定：muyi.error-code.locations 指向 YAML 时文件码进入注册表")
    void locationsProperty_loadsFileCodes() {
        runner.withPropertyValues(
                        "muyi.error-code.locations=classpath:errorcodes/valid-error-codes.yaml")
                .run(
                        context -> {
                            ErrorCodeRegistry registry = context.getBean(ErrorCodeRegistry.class);
                            // 文件中的业务码（valid-error-codes.yaml）
                            assertThat(registry.find("1001001000")).isPresent();
                            // 文件对全局 500 的文案覆盖生效（LAST_WINS）
                            assertThat(registry.find("500"))
                                    .hasValueSatisfying(
                                            code ->
                                                    assertThat(code.getMsg())
                                                            .isEqualTo("系统开小差了,请稍后重试"));
                        });
    }

    @Test
    @DisplayName("业务自定义 ErrorCodeProvider Bean：自动接入默认注册表")
    void customProviderBean_mergedIntoRegistry() {
        runner.withUserConfiguration(CustomProviderConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ErrorCodeRegistry.class);
                            assertThat(context.getBean(ErrorCodeRegistry.class).find("9009009001"))
                                    .isPresent();
                        });
    }

    @Test
    @DisplayName("业务自定义 ErrorCodeRegistry Bean：注册表 back off，provider 仍装配")
    void customRegistryBean_backsOffDefaultRegistry() {
        runner.withUserConfiguration(CustomRegistryConfig.class)
                .run(
                        context -> {
                            // 注册表由业务方提供，自动装配的 DefaultErrorCodeRegistry 退出
                            assertThat(context.getBean(ErrorCodeRegistry.class))
                                    .isSameAs(
                                            context.getBean("myRegistry", ErrorCodeRegistry.class));
                            assertThat(context).doesNotHaveBean(DefaultErrorCodeRegistry.class);
                            // 常量兜底源不受影响（仅注册表整体 back off）
                            assertThat(context).hasSingleBean(ConstantsErrorCodeProvider.class);
                        });
    }

    @Test
    @DisplayName("业务自定义 ConstantsErrorCodeProvider：兜底源 back off（@ConditionalOnMissingBean）")
    void customConstantsProvider_backsOffDefault() {
        runner.withUserConfiguration(CustomConstantsProviderConfig.class)
                .run(
                        context -> {
                            // 自动装配的 ConstantsErrorCodeProvider 退出，注册表仍装配
                            assertThat(context)
                                    .getBeans(ConstantsErrorCodeProvider.class)
                                    .hasSize(1);
                            assertThat(context)
                                    .getBean(ConstantsErrorCodeProvider.class)
                                    .extracting(ErrorCodeProvider::load)
                                    .isEqualTo(List.of());
                        });
    }

    /** 业务自定义 ErrorCodeProvider 配置（模拟 DB/Redis 动态源）。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomProviderConfig {
        @Bean
        ErrorCodeProvider dynamicProvider() {
            return staticProvider(List.of(new ErrorCode("9009009001", "自定义动态码")));
        }
    }

    /** 业务自定义 ErrorCodeRegistry 配置（整体 back off 场景）。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomRegistryConfig {
        @Bean
        ErrorCodeRegistry myRegistry() {
            return new ErrorCodeRegistry() {
                @Override
                public Optional<ErrorCode> find(String code) {
                    return Optional.empty();
                }

                @Override
                public void register(ErrorCode errorCode) {
                    // 测试用：不接收注册
                }

                @Override
                public void reload() {
                    // 测试用：无动态源
                }
            };
        }
    }

    /** 业务自定义 ConstantsErrorCodeProvider 配置（兜底源 back off 场景）。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomConstantsProviderConfig {
        @Bean
        ConstantsErrorCodeProvider constantsErrorCodeProvider() {
            return new ConstantsErrorCodeProvider() {
                @Override
                public Collection<ErrorCode> load() {
                    return List.of();
                }
            };
        }
    }
}
