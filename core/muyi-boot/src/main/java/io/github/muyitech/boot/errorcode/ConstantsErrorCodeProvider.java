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
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.Ordered;

/**
 * 全局常量兜底源：把 {@link GlobalErrorCodeConstants} 的 12 个全局码注册进注册表。
 *
 * <p>
 * 能力边界：只注册框架全局码，业务方自己的 {@code XxxErrorCodeConstants} 代码常量
 * <b>不会自动登记</b>——异常照常抛出（message 自带），但若希望该码参与 i18n 翻译 /
 * RPC 本地翻译，必须把定义写入 {@code muyi.error-code.locations} 文件或自实现
 * {@link ErrorCodeProvider}；框架不扫业务类路径，避免启动期反射成本。
 *
 * <p>
 * order = {@code HIGHEST_PRECEDENCE + 100}：最先加载（兜底源），
 * 文件源与业务动态源在其后，可按覆盖策略覆盖框架默认文案。
 *
 * @author keep simple
 * @since 2026/9/14
 */
public class ConstantsErrorCodeProvider implements ErrorCodeProvider {

    /**
     * 全局错误码常量集合（框架 12 个码）。
     */
    private static final List<ErrorCode> CONSTANTS =
            List.of(
                    GlobalErrorCodeConstants.SUCCESS,
                    GlobalErrorCodeConstants.BAD_REQUEST,
                    GlobalErrorCodeConstants.UNAUTHORIZED,
                    GlobalErrorCodeConstants.FORBIDDEN,
                    GlobalErrorCodeConstants.NOT_FOUND,
                    GlobalErrorCodeConstants.METHOD_NOT_ALLOWED,
                    GlobalErrorCodeConstants.LOCKED,
                    GlobalErrorCodeConstants.TOO_MANY_REQUESTS,
                    GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR,
                    GlobalErrorCodeConstants.NOT_IMPLEMENTED,
                    GlobalErrorCodeConstants.ERROR_CONFIGURATION,
                    GlobalErrorCodeConstants.UNKNOWN);

    @Override
    public Collection<ErrorCode> load() {
        return new ArrayList<>(CONSTANTS);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
