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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultErrorCodeRegistry} 单元测试。
 *
 * <p>
 * 覆盖：find 命中/未命中/null；register 写入与覆盖策略三态；reload 幂等与动态源刷新；
 * 来源加载失败 fail-fast；事件发布。
 *
 * @author keep simple
 * @since 2026/9/14
 */
class DefaultErrorCodeRegistryTest {

    /** 测试用静态来源工厂：固定返回给定错误码集合（Ordered 顺序 0）。 */
    private static ErrorCodeProvider staticProvider(Collection<ErrorCode> codes) {
        return new ErrorCodeProvider() {
            @Override
            public Collection<ErrorCode> load() {
                return codes;
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

    /** 测试用失败来源工厂：load 即抛异常，验证 fail-fast。 */
    private static ErrorCodeProvider failingProvider() {
        return new ErrorCodeProvider() {
            @Override
            public Collection<ErrorCode> load() {
                throw new IllegalStateException("DB 连不上");
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

    @Test
    void find_hitsRegisteredCode() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "用户不存在")))),
                        OverridePolicy.LAST_WINS,
                        null);

        assertThat(registry.find("1001")).isPresent();
        assertThat(registry.find("1001").orElseThrow().getMsg()).isEqualTo("用户不存在");
    }

    @Test
    void find_missingOrNull_returnsEmpty() {
        DefaultErrorCodeRegistry registry = new DefaultErrorCodeRegistry(List.of(), null, null);

        assertThat(registry.find("9999")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }

    @Test
    @DisplayName("LAST_WINS：后注册者覆盖，快照原子替换")
    void register_lastWins_overridesExisting() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "旧文案")))),
                        OverridePolicy.LAST_WINS,
                        null);

        registry.register(new ErrorCode("1001", "新文案"));

        assertThat(registry.find("1001").orElseThrow().getMsg()).isEqualTo("新文案");
    }

    @Test
    @DisplayName("FIRST_WINS：先注册者保留")
    void register_firstWins_keepsExisting() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "先到的文案")))),
                        OverridePolicy.FIRST_WINS,
                        null);

        registry.register(new ErrorCode("1001", "后来的文案"));

        assertThat(registry.find("1001").orElseThrow().getMsg()).isEqualTo("先到的文案");
    }

    @Test
    @DisplayName("REJECT：初始化与单条注册遇重复码均 fail-fast")
    void register_rejectPolicy_throws() {
        assertThatThrownBy(
                        () ->
                                new DefaultErrorCodeRegistry(
                                        List.of(
                                                staticProvider(
                                                        List.of(
                                                                new ErrorCode("1001", "第一次"),
                                                                new ErrorCode("1001", "重复")))),
                                        OverridePolicy.REJECT,
                                        null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复注册");

        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "初始")))),
                        OverridePolicy.REJECT,
                        null);
        assertThatThrownBy(() -> registry.register(new ErrorCode("1001", "再注册")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reload 仅重跑支持动态重载的来源，静态源定义保留")
    void reload_rerunsOnlyDynamicProviders() {
        ErrorCode staticCode = new ErrorCode("500", "静态来源");
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(
                                staticProvider(List.of(staticCode)),
                                new ErrorCodeProvider() {
                                    private int version = 1;

                                    @Override
                                    public java.util.Collection<ErrorCode> load() {
                                        return List.of(new ErrorCode("2001", "动态文案 v" + version++));
                                    }

                                    @Override
                                    public boolean supportsDynamicReload() {
                                        return true;
                                    }

                                    @Override
                                    public int getOrder() {
                                        return 0;
                                    }
                                }),
                        OverridePolicy.LAST_WINS,
                        null);

        assertThat(registry.find("2001").orElseThrow().getMsg()).isEqualTo("动态文案 v1");

        registry.reload();

        // 动态源重跑拿到新版本
        assertThat(registry.find("2001").orElseThrow().getMsg()).isEqualTo("动态文案 v2");
        // 静态源定义保留
        assertThat(registry.find("500").orElseThrow().getMsg()).isEqualTo("静态来源");
    }

    @Test
    @DisplayName("reload 幂等：重复刷新无害")
    void reload_isIdempotent() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "文案")))), null, null);

        registry.reload();
        registry.reload();

        assertThat(registry.find("1001")).isPresent();
    }

    @Test
    @DisplayName("来源加载失败时启动失败，不带病运行")
    void providerFailure_failsFast() {
        assertThatThrownBy(
                        () -> new DefaultErrorCodeRegistry(List.of(failingProvider()), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("加载失败");
    }

    @Test
    @DisplayName("reload 完成后发布 ErrorCodeReloadedEvent")
    void reload_publishesEvent() {
        List<ErrorCodeReloadedEvent> events = new java.util.ArrayList<>();
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(
                                staticProvider(
                                        List.of(
                                                new ErrorCode("1001", "文案"),
                                                new ErrorCode("1002", "文案二")))),
                        OverridePolicy.LAST_WINS,
                        event -> {
                            if (event instanceof ErrorCodeReloadedEvent reloaded) {
                                events.add(reloaded);
                            }
                        });

        registry.reload();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("快照不可变：外部无法修改注册表内容")
    void snapshot_isImmutable() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "文案")))), null, null);

        Map<String, ErrorCode> snapshot = registry.currentSnapshot();
        assertThatThrownBy(() -> snapshot.put("1002", new ErrorCode("1002", "注入")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("常量兜底源注册 12 个全局码")
    void constantsProvider_registersAllGlobalCodes() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(List.of(new ConstantsErrorCodeProvider()), null, null);

        assertThat(registry.find("0")).isPresent();
        assertThat(registry.find("400")).isPresent();
        assertThat(registry.find("500")).isPresent();
        assertThat(registry.find("999")).isPresent();
    }

    @Test
    @DisplayName("register(null)/register(无码)：null 守卫跳过，不影响已有快照")
    void register_nullOrCodeless_noopDoesNotThrow() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(
                        List.of(staticProvider(List.of(new ErrorCode("1001", "文案")))),
                        OverridePolicy.LAST_WINS,
                        null);

        // null 守卫：不抛异常、快照不受影响
        registry.register(null);
        registry.register(new ErrorCode(null, "无码定义"));

        assertThat(registry.find("1001")).isPresent();
        assertThat(registry.find("9999")).isEmpty();
    }

    @Test
    @DisplayName("providersView：防御性拷贝，外部修改不影响注册表")
    void providersView_isDefensiveCopy() {
        DefaultErrorCodeRegistry registry =
                new DefaultErrorCodeRegistry(List.of(staticProvider(List.of())), null, null);

        List<ErrorCodeProvider> view = registry.providersView();
        assertThat(view).hasSize(1);

        view.clear();
        assertThat(registry.providersView()).hasSize(1);
    }
}
