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
 * 错误码变更广播 SPI：实现方负责把"该刷新了"这个事实传播到集群所有实例。
 *
 * <p>
 * 本期（P1）只立契约，P2 交付 Redis 实现（计划内置
 * {@code RedisErrorCodeReloadNotifier}：pub/sub topic {@code muyi:error-code:reload}，
 * 订阅方收到消息 → 调用本实例 {@link ErrorCodeRegistry#reload()}）。
 * 不内置调度：谁改数据谁 broadcast()，管理端 / DB Provider 实现方自行决定时机。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public interface ErrorCodeReloadNotifier {

    /**
     * 广播刷新通知（如 Redis PUBLISH muyi:error-code:reload）。
     */
    void broadcast();
}
