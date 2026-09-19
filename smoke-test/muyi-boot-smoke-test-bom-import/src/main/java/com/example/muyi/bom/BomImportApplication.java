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
package com.example.muyi.bom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例二启动类：仅导入 BOM 接入 Muyi Framework。
 *
 * <p>
 * 与方式一（继承 Parent）代码完全一致，差异仅在 pom 接入方式——证明两种方式对业务代码零侵入。
 *
 * <p>
 * 无需任何 {@code @Import}：引入 {@code muyi-boot-starter-webmvc} 后，
 * 核心异常处理器经 {@code muyi-boot-webmvc} 的 Spring 自动配置（imports 文件）自动注册，
 * 业务异常将被统一包装为 {@code ApiResult} 响应。
 *
 * @author keep simple
 * @since 2026/9/12
 */
@SpringBootApplication
public class BomImportApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BomImportApplication.class, args);
    }
}
