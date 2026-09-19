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
package com.example.muyi.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例三启动类：继承 muyi-boot-parent 接入 Muyi Framework 的响应式（WebFlux）应用。
 *
 * <p>
 * 无需任何 {@code @Import}：引入 {@code muyi-boot-starter-webflux} 后，
 * 响应式核心异常处理器经 {@code muyi-boot-webflux} 的 Spring 自动配置（imports 文件）自动注册，
 * 业务异常将被统一包装为 {@code ApiResult} 响应。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@SpringBootApplication
public class WebFluxApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(WebFluxApplication.class, args);
    }
}
