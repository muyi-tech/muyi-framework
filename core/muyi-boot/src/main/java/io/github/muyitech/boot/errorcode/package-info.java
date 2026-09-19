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
/**
 * 错误码注册表：业务错误码的统一注册、查找与多语言解析。
 *
 * <p>
 * 分层结构（SPI 在前，默认实现在后）：
 *
 * <ul>
 *   <li><b>扩展点</b>——{@link io.github.muyitech.boot.errorcode.ErrorCodeProvider}
 *       （错误码来源，业务方可实现 DB/Redis/Nacos 动态源）、
 *       {@link io.github.muyitech.boot.errorcode.ErrorCodeRegistry}
 *       （注册表门面，业务方自定义时自动配置整体 back off）、
 *       {@link io.github.muyitech.boot.errorcode.OverridePolicy}（重复码覆盖策略）。
 *   <li><b>默认实现</b>——{@link io.github.muyitech.boot.errorcode.ConstantsErrorCodeProvider}
 *       （常量兜底源，最先加载）、
 *       {@link io.github.muyitech.boot.errorcode.FileErrorCodeProvider}
 *       （YAML 文件源，{@code muyi.error-code.locations} 配置，支持多语言与免发版覆盖）、
 *       {@link io.github.muyitech.boot.errorcode.DefaultErrorCodeRegistry}
 *       （按 Provider order 聚合构建快照，支持热更新事件）。
 *   <li><b>支撑类型</b>——{@link io.github.muyitech.boot.errorcode.ErrorCodeMessages}
 *       （三级回退多语言解析，响应构造时按请求 Locale 调用）、
 *       {@link io.github.muyitech.boot.errorcode.ErrorCodeReloadedEvent} /
 *       {@link io.github.muyitech.boot.errorcode.ErrorCodeReloadNotifier}
 *       （热更新事件与通知器）。
 * </ul>
 *
 * <p>
 * 自动装配在子包 {@code io.github.muyitech.boot.errorcode.autoconfigure}
 * （{@link io.github.muyitech.boot.errorcode.autoconfigure.ErrorCodeAutoConfiguration}
 * 与 {@link io.github.muyitech.boot.errorcode.autoconfigure.MuyiErrorCodeProperties}）。
 *
 * <p>
 * 典型链路：业务抛出 {@code ServiceException} → 异常处理模块按 code 查注册表 →
 * 命中则用 {@link io.github.muyitech.boot.errorcode.ErrorCodeMessages} 按请求 Locale
 * 解析文案（i18n 与热更新即时生效）→ 未命中原样输出异常自带 message。
 */
package io.github.muyitech.boot.errorcode;
