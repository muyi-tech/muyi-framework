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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * {@link MuyiWebFluxProperties} 单元测试。
 *
 * <p>
 * 覆盖默认值与 Binder 绑定（键 {@code muyi.webflux.exception-handler.enabled} /
 * {@code handle-not-found}）。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class MuyiWebFluxPropertiesTest {

    @Test
    void defaults_bothEnabled() {
        MuyiWebFluxProperties properties = new MuyiWebFluxProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isHandleNotFound()).isTrue();
    }

    @Test
    void binder_bindsBothFlags() {
        MapConfigurationPropertySource source =
                new MapConfigurationPropertySource(
                        Map.of(
                                "muyi.webflux.exception-handler.enabled", "false",
                                "muyi.webflux.exception-handler.handle-not-found", "false"));

        MuyiWebFluxProperties properties =
                new Binder(source)
                        .bind(
                                "muyi.webflux.exception-handler",
                                Bindable.of(MuyiWebFluxProperties.class))
                        .get();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isHandleNotFound()).isFalse();
    }

    @Test
    void binder_bindsServiceExceptionLogNoise() {
        // 降噪配置：ignore-messages（逗号分隔列表）与 stack-trace-frames 绑定到嵌套配置项
        MapConfigurationPropertySource source =
                new MapConfigurationPropertySource(
                        Map.of(
                                "muyi.webflux.exception-handler.service-exception-log.ignore-messages",
                                "会话已过期,令牌失效",
                                "muyi.webflux.exception-handler.service-exception-log.stack-trace-frames",
                                "3"));

        MuyiWebFluxProperties properties =
                new Binder(source)
                        .bind(
                                "muyi.webflux.exception-handler",
                                Bindable.of(MuyiWebFluxProperties.class))
                        .get();

        assertThat(properties.getServiceExceptionLog().getIgnoreMessages())
                .containsExactly("会话已过期", "令牌失效");
        assertThat(properties.getServiceExceptionLog().getStackTraceFrames()).isEqualTo(3);
    }
}
