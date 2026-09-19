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

import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;

/**
 * 错误码注册表默认实现：{@code volatile} 快照 + 原子引用替换。
 *
 * <p>
 * 一致性模型（单实例基础）：
 *
 * <ul>
 *   <li>读路径无锁——{@code find} 只读 {@code volatile} 快照引用，零竞争、无中间态；
 *   <li>写路径（{@link #register} / {@link #reload}）持锁构建新快照后原子替换引用，
 *       读线程要么看到完整旧快照、要么看到完整新快照；
 *   <li>重复码按 {@link OverridePolicy} 处理：LAST_WINS 覆盖（warn）/ FIRST_WINS 保留
 *       （debug）/ REJECT 抛 {@link IllegalStateException}（启动期 fail-fast）。
 * </ul>
 *
 * <p>
 * 键规范化：数字码统一十进制无前导零形态（{@code "500"}），字符串码按命名规则原样使用
 * ——保证同一码值全集群唯一形态。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public class DefaultErrorCodeRegistry implements ErrorCodeRegistry {

    /**
     * 共享日志。
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultErrorCodeRegistry.class);

    /**
     * 错误码来源（构造时按 {@link Ordered#getOrder()} 降序排列：值大者先加载）。
     */
    private final List<ErrorCodeProvider> providers;

    /**
     * 重复码覆盖策略。
     */
    private final OverridePolicy overridePolicy;

    /**
     * 事件发布器（可选，reload 完成后发布 {@link ErrorCodeReloadedEvent}；测试场景可为 null）。
     */
    private final @Nullable ApplicationEventPublisher eventPublisher;

    /**
     * 写路径锁（register / reload 互斥）。
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 静态基线：初始化时一次性加载的静态来源定义（{@code supportsDynamicReload() == false}）。
     * reload 仅重跑动态来源并叠加在本基线上——静态定义永不丢失。
     */
    private final Map<String, ErrorCode> staticBaseline;

    /**
     * 错误码快照（key = 规范化码值）。volatile 保证读线程可见性与原子替换。
     */
    private volatile Map<String, ErrorCode> snapshot;

    /**
     * 构造注册表并完成初始加载。
     * @param providers 错误码来源（内部按 order 降序重排，无需调用方保证顺序）
     * @param overridePolicy 重复码覆盖策略
     * @param eventPublisher 事件发布器（可为 null）
     */
    public DefaultErrorCodeRegistry(
            List<ErrorCodeProvider> providers,
            OverridePolicy overridePolicy,
            @Nullable ApplicationEventPublisher eventPublisher) {
        this.providers = sortProviders(providers);
        this.overridePolicy = overridePolicy == null ? OverridePolicy.LAST_WINS : overridePolicy;
        this.eventPublisher = eventPublisher;
        this.staticBaseline = Map.copyOf(loadStatic());
        Map<String, ErrorCode> initial = new HashMap<>(staticBaseline);
        mergeDynamic(initial);
        this.snapshot = Map.copyOf(initial);
        LOG.info(
                "[DefaultErrorCodeRegistry][初始化完成：{} 个错误码，策略 {}，来源 {}]",
                this.snapshot.size(),
                this.overridePolicy,
                this.providers.size());
    }

    @Override
    public Optional<ErrorCode> find(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.get(code));
    }

    @Override
    public void register(ErrorCode errorCode) {
        if (errorCode == null || errorCode.getCode() == null) {
            return;
        }
        lock.lock();
        try {
            Map<String, ErrorCode> next = new HashMap<>(snapshot);
            applyPolicy(next, errorCode, "register");
            snapshot = Map.copyOf(next);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reload() {
        lock.lock();
        try {
            Map<String, ErrorCode> next = new HashMap<>(staticBaseline);
            mergeDynamic(next);
            int size = next.size();
            snapshot = Map.copyOf(next);
            LOG.info("[reload][错误码快照已刷新：{} 个]", size);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ErrorCodeReloadedEvent(this, size));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 仅加载静态来源（{@code supportsDynamicReload() == false}）构建基线草稿。
     * @return 静态基线草稿（可变 Map，调用方决定是否固化）
     */
    private Map<String, ErrorCode> loadStatic() {
        Map<String, ErrorCode> next = new HashMap<>();
        for (ErrorCodeProvider provider : providers) {
            if (!provider.supportsDynamicReload()) {
                mergeFrom(next, provider);
            }
        }
        return next;
    }

    /**
     * 将动态来源重新加载的结果合并进目标草稿（reload 场景）。
     * @param target 快照草稿（静态基线副本）
     */
    private void mergeDynamic(Map<String, ErrorCode> target) {
        for (ErrorCodeProvider provider : providers) {
            if (!provider.supportsDynamicReload()) {
                continue;
            }
            mergeFrom(target, provider);
        }
    }

    /**
     * 加载单个来源并按覆盖策略合并进目标草稿。
     * @param target 快照草稿
     * @param provider 错误码来源
     */
    private void mergeFrom(Map<String, ErrorCode> target, ErrorCodeProvider provider) {
        Collection<ErrorCode> loaded;
        try {
            loaded = provider.load();
        } catch (RuntimeException ex) {
            // 来源加载失败直接失败——错误码是系统契约，带病快照比启动失败更危险
            throw new IllegalStateException(
                    "[loadSnapshot][来源 " + provider.getClass().getName() + " 加载失败]", ex);
        }
        for (ErrorCode errorCode : loaded) {
            applyPolicy(target, errorCode, provider.getClass().getSimpleName());
        }
    }

    /**
     * 来源排序：遵循 Spring {@link Ordered} 约定——order 值小者先加载
     * （常量源 {@code HIGHEST_PRECEDENCE + 100} 最先、文件源 {@code + 200} 其后、
     * 业务动态源默认 order 最后，配合 LAST_WINS 形成"越靠近业务的来源话语权越大"）。
     * 稳定排序，同值保持传入顺序。
     * @param providers 原始来源列表
     * @return 升序排列后的不可变列表
     */
    private static List<ErrorCodeProvider> sortProviders(List<ErrorCodeProvider> providers) {
        return providers.stream().sorted(Comparator.comparingInt(Ordered::getOrder)).toList();
    }

    /**
     * 按覆盖策略将一条错误码写入快照草稿。
     * @param target 快照草稿
     * @param errorCode 待写入错误码
     * @param sourceName 来源描述（日志用）
     */
    private void applyPolicy(
            Map<String, ErrorCode> target, ErrorCode errorCode, String sourceName) {
        String key = errorCode.getCode();
        ErrorCode existing = target.get(key);
        if (existing == null) {
            target.put(key, errorCode);
            return;
        }
        // if-else 早退链替代 switch：enum switch 会被 javac 补一个 default(NPE) 分支，
        // 字节码级不可达、测试无法触达（所有分支必须可测）；行为与原 switch 完全等价
        if (overridePolicy == OverridePolicy.REJECT) {
            throw new IllegalStateException(
                    "[applyPolicy][错误码 " + key + " 重复注册（REJECT 策略）：来源 " + sourceName + "]");
        }
        if (overridePolicy == OverridePolicy.FIRST_WINS) {
            LOG.debug(
                    "[applyPolicy][错误码 {} 重复，FIRST_WINS：保留 {} 的定义，忽略 {}]",
                    key,
                    existing.getMsg(),
                    sourceName);
            return;
        }
        LOG.warn(
                "[applyPolicy][错误码 {} 重复，LAST_WINS：{} 的定义覆盖 {} 的定义]",
                key,
                sourceName,
                existing.getMsg());
        target.put(key, errorCode);
    }

    /**
     * 获取当前快照（测试与监控用，勿在业务代码持有引用——快照会被原子替换）。
     * @return 当前快照
     */
    Map<String, ErrorCode> currentSnapshot() {
        return snapshot;
    }

    /**
     * 供子类/测试查看来源列表。
     * @return 不可变来源列表
     */
    List<ErrorCodeProvider> providersView() {
        return new ArrayList<>(providers);
    }
}
