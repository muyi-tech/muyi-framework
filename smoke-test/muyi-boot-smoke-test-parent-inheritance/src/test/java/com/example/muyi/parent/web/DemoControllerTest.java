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
package com.example.muyi.parent.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link DemoController} 集成测试。
 *
 * @author keep simple
 * @since 2026/9/12
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void hello_returnsUnifiedSuccessResult() throws Exception {
        mockMvc.perform(get("/api/demo/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value("Hello, Muyi Framework!"));
    }

    @Test
    void sexEnums_returnsKeyValueList() throws Exception {
        mockMvc.perform(get("/api/demo/sex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").isNumber())
                .andExpect(jsonPath("$.data[0].value").isString());
    }

    @Test
    void error_returnsBusinessErrorResult() throws Exception {
        mockMvc.perform(get("/api/demo/error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }

    @Test
    void i18nError_zhCN_returnsChineseMessage() throws Exception {
        mockMvc.perform(get("/api/demo/i18n-error").header("Accept-Language", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001001000"))
                .andExpect(jsonPath("$.msg").value("用户 muyi 不存在"));
    }

    @Test
    void i18nError_enUS_returnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/api/demo/i18n-error").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001001000"))
                .andExpect(jsonPath("$.msg").value("User muyi does not exist"));
    }

    @Test
    void i18nError_unmatchedLocale_fallsBackToDefaultMessage() throws Exception {
        mockMvc.perform(get("/api/demo/i18n-error").header("Accept-Language", "fr-FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001001000"))
                .andExpect(jsonPath("$.msg").value("用户 muyi 不存在"));
    }

    @Test
    void moduleError_interceptedByCustomModuleHandler() throws Exception {
        mockMvc.perform(get("/api/demo/module-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001001001"))
                .andExpect(jsonPath("$.msg").value("模块级异常处理器演示：业务专有异常已被自定义拦截"));
    }
}
