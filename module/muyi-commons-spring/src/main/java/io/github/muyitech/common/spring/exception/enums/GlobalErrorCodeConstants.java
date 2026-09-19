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
package io.github.muyitech.common.spring.exception.enums;

/**
 * 全局错误码常量（final class，非接口——避免"常量接口"反模式）。
 *
 * <p>
 * 0-999 系统异常编码保留。一般情况下，使用 HTTP 响应状态码
 * （https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Status），虽然 HTTP 响应状态码作为业务使用
 * 表达能力偏弱，但是使用在系统层面还是非常不错的。比较特殊的是，因为之前一直使用 0 作为成功，就不使用 200 啦。
 *
 * <p>
 * 码值为字符串形态；本类常量经框架注册表（ConstantsErrorCodeProvider）登记为兜底源，
 * 引用写法不变（{@code GlobalErrorCodeConstants.BAD_REQUEST}），但禁止 implements。
 *
 * @author keep simple
 * @since 2025/5/21
 */
public final class GlobalErrorCodeConstants {

    private GlobalErrorCodeConstants() {}

    /** 成功（唯一成功码）。 */
    public static final ErrorCode SUCCESS = new ErrorCode("0", "成功");

    // ========== 客户端错误段 ==========

    /** 请求参数不正确。 */
    public static final ErrorCode BAD_REQUEST = new ErrorCode("400", "请求参数不正确");

    /** 账号未登录。 */
    public static final ErrorCode UNAUTHORIZED = new ErrorCode("401", "账号未登录");

    /** 没有该操作权限。 */
    public static final ErrorCode FORBIDDEN = new ErrorCode("403", "没有该操作权限");

    /** 请求未找到。 */
    public static final ErrorCode NOT_FOUND = new ErrorCode("404", "请求未找到");

    /** 请求方法不正确。 */
    public static final ErrorCode METHOD_NOT_ALLOWED = new ErrorCode("405", "请求方法不正确");

    /** 请求失败，请稍后重试。 */
    public static final ErrorCode LOCKED = new ErrorCode("423", "请求失败，请稍后重试");

    /** 请求过于频繁，请稍后重试。 */
    public static final ErrorCode TOO_MANY_REQUESTS = new ErrorCode("429", "请求过于频繁，请稍后重试");

    // ========== 服务端错误段 ==========

    /** 系统异常。 */
    public static final ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode("500", "系统异常");

    /** 功能未实现/未开启。 */
    public static final ErrorCode NOT_IMPLEMENTED = new ErrorCode("501", "功能未实现/未开启");

    /** 错误的配置项。 */
    public static final ErrorCode ERROR_CONFIGURATION = new ErrorCode("502", "错误的配置项");

    /** 未知错误。 */
    public static final ErrorCode UNKNOWN = new ErrorCode("999", "未知错误");
}
