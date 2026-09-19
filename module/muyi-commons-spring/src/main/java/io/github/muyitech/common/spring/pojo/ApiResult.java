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
package io.github.muyitech.common.spring.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * 通用返回。
 *
 * @param <T> 数据泛型
 * @author keep simple
 * @since 2024/8/2
 */
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码（字符串形态），参见 {@link ErrorCode}。
     */
    private String code;

    /**
     * 错误提示，用户可阅读，参见 {@link ErrorCode}。
     */
    private String msg;

    /**
     * 返回数据。
     *
     * <p>
     * 可空：无数据响应（如无返回值的操作）时为 null。
     */
    private @Nullable T data;

    /**
     * 将传入的 result 对象，转换成另外一个泛型结果的对象。
     *
     * <p>
     * 因为 A 方法返回的 ApiResult 对象，不满足调用其的 B 方法的返回，所以需要进行转换。
     * @param result 传入的 result 对象
     * @param <T> 返回的泛型
     * @return 新的 ApiResult 对象
     */
    public static <T> ApiResult<T> error(ApiResult<?> result) {
        return error(result.getCode(), result.getMsg());
    }

    /**
     * 根据错误码与错误提示构造错误结果。
     * @param code 错误码（字符串形态）
     * @param message 错误提示
     * @param <T> 数据泛型
     * @return 错误结果
     */
    public static <T> ApiResult<T> error(String code, String message) {
        Assert.isTrue(
                !Objects.equals(GlobalErrorCodeConstants.SUCCESS.getCode(), code), "code 必须是错误的！");
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.msg = message;
        return result;
    }

    /**
     * 根据错误码与格式化参数构造错误结果。
     * @param errorCode 错误码
     * @param params 格式化参数，替换消息模版中的 {} 占位符
     * @param <T> 数据泛型
     * @return 错误结果
     */
    public static <T> ApiResult<T> error(ErrorCode errorCode, Object... params) {
        Assert.isTrue(
                !Objects.equals(GlobalErrorCodeConstants.SUCCESS.getCode(), errorCode.getCode()),
                "code 必须是错误的！");
        ApiResult<T> result = new ApiResult<>();
        result.code = errorCode.getCode();
        result.msg = ServiceExceptionUtil.doFormat(errorCode.getCode(), errorCode.getMsg(), params);
        return result;
    }

    /**
     * 根据错误码构造错误结果。
     * @param errorCode 错误码
     * @param <T> 数据泛型
     * @return 错误结果
     */
    public static <T> ApiResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    /**
     * 构造成功结果。
     * @param data 返回数据
     * @param <T> 数据泛型
     * @return 成功结果
     */
    public static <T> ApiResult<T> success(@Nullable T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.data = data;
        result.msg = "";
        return result;
    }

    /**
     * 判断错误码是否表示成功。
     * @param code 错误码（字符串形态）
     * @return 是否成功
     */
    public static boolean isSuccess(String code) {
        return Objects.equals(code, GlobalErrorCodeConstants.SUCCESS.getCode());
    }

    /**
     * 当前结果是否成功。
     * @return 是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return isSuccess(code);
    }

    /**
     * 当前结果是否失败。
     * @return 是否失败
     */
    @JsonIgnore
    public boolean isError() {
        return !isSuccess();
    }

    // ========= 和 Exception 异常体系集成 =========

    /**
     * 判断是否有异常。如果有，则抛出 {@link ServiceException} 异常。
     *
     * <p>
     * 已知限制：ApiResult 不承载格式化参数（args），跨进程重建的 ServiceException
     * args 为 null——远端响应若需 i18n 重格式化模版，请携带完整 ErrorCode 定义文件，
     * 或等业务侧透传字段（P2 议题，见全项目 review 报告）。
     */
    public void checkError() throws ServiceException {
        if (isSuccess()) {
            return;
        }
        // 业务异常
        throw new ServiceException(code, msg);
    }

    /**
     * 判断是否有异常。如果有，则抛出 {@link ServiceException} 异常；如果没有，则返回 {@link #data} 数据。
     * @return 返回数据
     */
    @JsonIgnore
    public T getCheckedData() {
        checkError();
        return data;
    }

    /**
     * 根据业务异常构造错误结果。
     * @param serviceException 业务异常
     * @param <T> 数据泛型
     * @return 错误结果
     */
    public static <T> ApiResult<T> error(ServiceException serviceException) {
        return error(serviceException.getCode(), serviceException.getMessage());
    }

    /**
     * 获取错误码。
     * @return 错误码（字符串形态）
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置错误码。
     * @param code 错误码（字符串形态）
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取错误提示。
     * @return 错误提示，用户可阅读
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置错误提示。
     * @param msg 错误提示
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取返回数据。
     * @return 返回数据
     */
    public @Nullable T getData() {
        return data;
    }

    /**
     * 设置返回数据。
     * @param data 返回数据（可为 null）
     */
    public void setData(@Nullable T data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiResult)) {
            return false;
        }
        ApiResult<?> that = (ApiResult<?>) o;
        return Objects.equals(code, that.code)
                && Objects.equals(msg, that.msg)
                && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, msg, data);
    }

    @Override
    public String toString() {
        return "ApiResult(code=" + code + ", msg=" + msg + ", data=" + data + ")";
    }
}
