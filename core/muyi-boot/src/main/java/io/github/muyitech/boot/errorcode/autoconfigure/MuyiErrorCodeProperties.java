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

import io.github.muyitech.boot.errorcode.OverridePolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 错误码注册表配置项（{@code muyi.error-code.*}）。
 *
 * @author keep simple
 * @since 2026/9/14
 */
@ConfigurationProperties("muyi.error-code")
public class MuyiErrorCodeProperties {

    /**
     * 错误码定义文件（YAML），classpath:/file: 混用，逗号分隔。
     *
     * <p>
     * classpath: 随包分发；file: 运维改文案不发版；支持多个，后加载覆盖先加载。
     * 默认空——不建文件也能正常启动，框架零强制。
     */
    private List<String> locations = new ArrayList<>();

    /**
     * 重复码覆盖策略。默认 LAST_WINS（业务文件可覆盖框架文案）。
     */
    private OverridePolicy overridePolicy = OverridePolicy.LAST_WINS;

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public OverridePolicy getOverridePolicy() {
        return overridePolicy;
    }

    public void setOverridePolicy(OverridePolicy overridePolicy) {
        this.overridePolicy = overridePolicy;
    }
}
