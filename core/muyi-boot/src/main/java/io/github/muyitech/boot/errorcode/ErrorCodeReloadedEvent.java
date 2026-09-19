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

import org.springframework.context.ApplicationEvent;

/**
 * 错误码重载完成事件（{@link ErrorCodeRegistry#reload()} 后发布）。
 *
 * <p>
 * 业务方可监听做本地缓存联动清理；集群广播由 {@link ErrorCodeReloadNotifier}（P2）负责。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public class ErrorCodeReloadedEvent extends ApplicationEvent {

    /**
     * 重载后快照中的错误码总数。
     */
    private final transient int size;

    /**
     * 构造事件。
     * @param source 事件源（发布者）
     * @param size 重载后快照中的错误码总数
     */
    public ErrorCodeReloadedEvent(Object source, int size) {
        super(source);
        this.size = size;
    }

    /**
     * 获取重载后快照中的错误码总数。
     * @return 错误码总数
     */
    public int getSize() {
        return size;
    }
}
