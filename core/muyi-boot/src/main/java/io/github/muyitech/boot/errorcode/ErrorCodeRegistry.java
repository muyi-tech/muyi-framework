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
import java.util.Optional;

/**
 * 错误码注册表（core，无 Web 栈依赖）。
 *
 * <p>
 * 注册表只做增强、不做强制：查不到返回 empty，不注册错误码一切照常工作。
 *
 * <p>
 * 线程语义：读路径无锁（快照原子替换，{@code volatile} 引用），写路径（register / reload）
 * 由实现保证原子性与可见性。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public interface ErrorCodeRegistry {

    /**
     * 查询错误码定义（String 形态，key 为规范化码值）；查不到返回 empty（不强制注册）。
     * @param code 错误码（规范化字符串形态）
     * @return 错误码定义；未注册返回 empty
     */
    Optional<ErrorCode> find(String code);

    /**
     * 注册错误码；重复码按 override-policy 处理并记录日志。
     * @param errorCode 错误码定义
     */
    void register(ErrorCode errorCode);

    /**
     * 重新执行所有 {@link ErrorCodeProvider#supportsDynamicReload()} 的 Provider，
     * 原子替换快照。★ 分布式热更新入口（幂等，重复刷新无害）。
     */
    void reload();
}
