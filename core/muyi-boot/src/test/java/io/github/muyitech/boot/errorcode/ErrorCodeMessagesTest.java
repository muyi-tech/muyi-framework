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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ErrorCodeMessages} 单元测试。
 *
 * <p>
 * 覆盖三级回退链：精确 tag → 语言级 → 默认 msg；无 args 原样输出模板；带 args 复用
 * doFormat 格式化；null 输入防御。
 *
 * @author keep simple
 * @since 2026/9/14
 */
class ErrorCodeMessagesTest {

    /** 样例错误码：中英文案（en 文案含占位符）。 */
    private final ErrorCode sample =
            new ErrorCode(
                    "1001",
                    "用户 {} 不存在",
                    Map.of("zh-CN", "用户 {} 不存在", "en-US", "User {} does not exist"));

    @Test
    void resolve_exactTagMatch_wins() {
        assertThat(ErrorCodeMessages.resolve(sample, Locale.US, new Object[] {"zhang"}))
                .isEqualTo("User zhang does not exist");
    }

    @Test
    void resolve_languageLevelFallback_matchesSimplifiedChinese() {
        // zh-CN_GB / zh_SG 等无精确定义时回退语言级 zh（zh-CN key 以语言级 zh 命中）
        assertThat(
                        ErrorCodeMessages.resolve(
                                sample, Locale.SIMPLIFIED_CHINESE, new Object[] {"zhang"}))
                .isEqualTo("用户 zhang 不存在");
        assertThat(ErrorCodeMessages.resolve(sample, Locale.of("zh", "SG"), new Object[] {"zhang"}))
                .isEqualTo("用户 zhang 不存在");
    }

    @Test
    void resolve_noLocaleMatch_fallsBackToDefaultMsg() {
        assertThat(ErrorCodeMessages.resolve(sample, Locale.FRANCE, null)).isEqualTo("用户 {} 不存在");
    }

    @Test
    void resolve_nullLocale_fallsBackToDefaultMsg() {
        assertThat(ErrorCodeMessages.resolve(sample, null, null)).isEqualTo("用户 {} 不存在");
    }

    @Test
    void resolve_emptyMessages_fallsBackToDefaultMsg() {
        ErrorCode plain = new ErrorCode("1002", "库存不足");
        assertThat(ErrorCodeMessages.resolve(plain, Locale.US, null)).isEqualTo("库存不足");
    }

    @Test
    void resolve_withoutArgs_returnsTemplateAsIs() {
        assertThat(ErrorCodeMessages.resolve(sample, Locale.US, null))
                .isEqualTo("User {} does not exist");
    }

    @Test
    void format_withoutArgs_returnsTemplate() {
        assertThat(ErrorCodeMessages.format("1001", "模板 {} 不变", null)).isEqualTo("模板 {} 不变");
        assertThat(ErrorCodeMessages.format("1001", "模板 {} 不变", new Object[0]))
                .isEqualTo("模板 {} 不变");
    }

    @Test
    void format_withArgs_replacesPlaceholders() {
        assertThat(ErrorCodeMessages.format("1001", "模板 {} 已替换", new Object[] {"占位符"}))
                .isEqualTo("模板 占位符 已替换");
    }

    @Test
    void resolve_nullErrorCode_returnsNull() {
        assertThat(ErrorCodeMessages.resolve(null, Locale.US)).isNull();
        assertThat(ErrorCodeMessages.resolve(null, Locale.US, new Object[] {"x"})).isNull();
    }

    @Test
    @DisplayName("工具类：私有构造器不可外部实例化（纯标记覆盖）")
    @SuppressWarnings("unchecked")
    void privateConstructor_instantiableOnlyViaReflection() throws Exception {
        var constructor =
                (java.lang.reflect.Constructor<ErrorCodeMessages>)
                        ErrorCodeMessages.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        // 工具类无状态，反射实例化只为覆盖私有构造器
        assertThat(constructor.newInstance()).isNotNull();
    }
}
