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

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebFlux 异常处理配置项。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@ConfigurationProperties("muyi.webflux.exception-handler")
public class MuyiWebFluxProperties {

    /**
     * 是否启用框架全局异常处理器（ReactiveCoreExceptionHandler）。设为 false 时不注册，业务方可完全自定义。
     */
    private boolean enabled = true;

    /**
     * 是否启用未匹配路由 404 的统一包装（MuyiNotFoundWebExceptionHandler）。设为 false 时 404 交还框架默认处理。
     */
    private boolean handleNotFound = true;

    /**
     * 业务异常日志降噪配置（框架零内置名单，全部由业务方配置）。
     */
    private final ServiceExceptionLog serviceExceptionLog = new ServiceExceptionLog();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHandleNotFound() {
        return handleNotFound;
    }

    public void setHandleNotFound(boolean handleNotFound) {
        this.handleNotFound = handleNotFound;
    }

    /**
     * 获取降噪配置。
     * @return 降噪配置
     */
    public ServiceExceptionLog getServiceExceptionLog() {
        return serviceExceptionLog;
    }

    /**
     * 业务异常日志降噪配置项。
     */
    public static class ServiceExceptionLog {

        /**
         * 不打印日志的消息名单（message 完全匹配时跳过）。默认空——框架零内置。
         */
        private List<String> ignoreMessages = new ArrayList<>();

        /**
         * 打印的非工具类栈帧数。0 = 静默；负数同 0。
         */
        private int stackTraceFrames = 1;

        public List<String> getIgnoreMessages() {
            return ignoreMessages;
        }

        public void setIgnoreMessages(List<String> ignoreMessages) {
            this.ignoreMessages = ignoreMessages;
        }

        public int getStackTraceFrames() {
            return stackTraceFrames;
        }

        public void setStackTraceFrames(int stackTraceFrames) {
            this.stackTraceFrames = stackTraceFrames;
        }
    }
}
