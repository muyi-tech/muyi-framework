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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * {@link MuyiWebMvcProperties} 单元测试。
 *
 * <p>
 * 覆盖默认值与 Binder 绑定（键 {@code muyi.webmvc.exception-handler.enabled}）。
 *
 * @author keep simple
 * @since 2026/9/13
 */
class MuyiWebMvcPropertiesTest {

    @Test
    void defaults_enabledTrue() {
        MuyiWebMvcProperties properties = new MuyiWebMvcProperties();

        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void binder_bindsEnabledFalse() {
        MapConfigurationPropertySource source =
                new MapConfigurationPropertySource(
                        Map.of("muyi.webmvc.exception-handler.enabled", "false"));

        MuyiWebMvcProperties properties =
                new Binder(source)
                        .bind(
                                "muyi.webmvc.exception-handler",
                                Bindable.of(MuyiWebMvcProperties.class))
                        .get();

        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void setEnabled_roundTrip() {
        MuyiWebMvcProperties properties = new MuyiWebMvcProperties();

        properties.setEnabled(false);

        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void serviceExceptionLog_defaultsAndSetters() {
        MuyiWebMvcProperties properties = new MuyiWebMvcProperties();
        MuyiWebMvcProperties.ServiceExceptionLog log = properties.getServiceExceptionLog();

        // 嵌套对象默认值：降噪名单为空、打印 1 帧
        assertThat(log.getIgnoreMessages()).isEmpty();
        assertThat(log.getStackTraceFrames()).isEqualTo(1);

        log.setIgnoreMessages(List.of("无效的刷新令牌"));
        log.setStackTraceFrames(3);
        assertThat(log.getIgnoreMessages()).containsExactly("无效的刷新令牌");
        assertThat(log.getStackTraceFrames()).isEqualTo(3);
    }

    @Test
    void binder_bindsServiceExceptionLog() {
        MapConfigurationPropertySource source =
                new MapConfigurationPropertySource(
                        Map.of(
                                "muyi.webmvc.exception-handler.service-exception-log.ignore-messages[0]",
                                "无效的刷新令牌",
                                "muyi.webmvc.exception-handler.service-exception-log.stack-trace-frames",
                                "2"));

        MuyiWebMvcProperties properties =
                new Binder(source)
                        .bind(
                                "muyi.webmvc.exception-handler",
                                Bindable.of(MuyiWebMvcProperties.class))
                        .get();

        assertThat(properties.getServiceExceptionLog().getIgnoreMessages())
                .containsExactly("无效的刷新令牌");
        assertThat(properties.getServiceExceptionLog().getStackTraceFrames()).isEqualTo(2);
    }
}
