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
package io.github.muyitech.systemtest;

/**
 * WebFlux 栈部署系统测试：被测对象为 {@code muyi-boot-smoke-test-webflux} 的可执行 jar
 * （继承 muyi-boot-parent 接入框架的响应式应用）。
 *
 * @author keep simple
 * @since 2026/9/14
 */
class WebFluxDeploymentTest extends AbstractJarDeploymentTest {

    WebFluxDeploymentTest() {
        super("muyi-boot-smoke-test-webflux-", "webflux");
    }
}
