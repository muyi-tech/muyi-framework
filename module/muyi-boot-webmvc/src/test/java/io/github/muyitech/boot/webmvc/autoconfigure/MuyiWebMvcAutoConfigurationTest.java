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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.muyitech.boot.webmvc.exception.CoreExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link MuyiWebMvcAutoConfiguration} 自动配置单元测试。
 *
 * <p>
 * 使用 {@code ApplicationContextRunner} 验证条件装配：Servlet 环境注册、Reactive 环境不注册、
 * 业务自定义 Bean 时 back off、enabled 开关关闭时不注册。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class MuyiWebMvcAutoConfigurationTest {

    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(MuyiWebMvcAutoConfiguration.class));

    @Test
    void servletWebApplication_registersCoreExceptionHandler() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(CoreExceptionHandler.class);
                    assertThat(context).hasSingleBean(MuyiWebMvcProperties.class);
                });
    }

    @Test
    void userDefinedBean_backsOff() {
        // 业务已注册同类型 Bean：自动配置 back off，容器中仍只有 1 个（若未 back off 会是 2 个）
        runner.withBean(CoreExceptionHandler.class)
                .run(context -> assertThat(context).hasSingleBean(CoreExceptionHandler.class));
    }

    @Test
    void enabledFalse_doesNotRegister() {
        runner.withPropertyValues("muyi.webmvc.exception-handler.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CoreExceptionHandler.class));
    }

    @Test
    void reactiveWebApplication_doesNotRegister() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MuyiWebMvcAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(CoreExceptionHandler.class));
    }
}
