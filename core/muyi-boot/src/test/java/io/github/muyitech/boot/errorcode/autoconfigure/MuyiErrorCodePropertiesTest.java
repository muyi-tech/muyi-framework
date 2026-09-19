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

import io.github.muyitech.boot.errorcode.OverridePolicy;
import org.junit.jupiter.api.Test;

/**
 * {@link MuyiErrorCodeProperties} 单元测试。
 *
 * @author keep simple
 * @since 2026/9/14
 */
class MuyiErrorCodePropertiesTest {

    @Test
    void defaults_locationsEmptyAndLastWins() {
        MuyiErrorCodeProperties properties = new MuyiErrorCodeProperties();

        // 零强制：默认不加载文件也能启动
        assertThat(properties.getLocations()).isEmpty();
        // 默认允许业务文件覆盖框架文案
        assertThat(properties.getOverridePolicy()).isEqualTo(OverridePolicy.LAST_WINS);
    }

    @Test
    void setters_mutateState() {
        MuyiErrorCodeProperties properties = new MuyiErrorCodeProperties();

        properties.setLocations(java.util.List.of("classpath:a.yaml", "file:/b.yaml"));
        properties.setOverridePolicy(OverridePolicy.REJECT);

        assertThat(properties.getLocations()).containsExactly("classpath:a.yaml", "file:/b.yaml");
        assertThat(properties.getOverridePolicy()).isEqualTo(OverridePolicy.REJECT);
    }

    @Test
    void prefix_isErrorCode() throws Exception {
        // 配置前缀契约：muyi.error-code.*
        assertThat(
                        MuyiErrorCodeProperties.class
                                .getAnnotation(
                                        org.springframework.boot.context.properties
                                                .ConfigurationProperties.class)
                                .value())
                .isEqualTo("muyi.error-code");
    }
}
