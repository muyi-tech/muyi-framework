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
package io.github.muyitech.boot.errorcode;

/**
 * 错误码重复注册时的覆盖策略。
 *
 * <p>
 * 注意：{@link #REJECT} 与"文件覆盖框架码"能力互斥——强管控环境选了 REJECT，
 * 文件里就不许再出现与框架码同值的条目（500 覆盖即启动失败）；
 * 要覆盖能力就选 LAST_WINS / FIRST_WINS。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public enum OverridePolicy {

    /**
     * 后注册者覆盖先注册者（默认），warn 日志。业务文件覆盖框架文案。
     */
    LAST_WINS,

    /**
     * 先注册者保留，重复注册 debug 日志。严格保护框架默认码。
     */
    FIRST_WINS,

    /**
     * 遇重复码抛 {@link IllegalStateException}，启动失败。强管控环境，防配置打架。
     */
    REJECT
}
