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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.muyitech.common.spring.exception.ServiceException;
import io.github.muyitech.common.spring.exception.enums.ErrorCode;
import io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

/**
 * {@link ApiResult} 单元测试。
 */
class ApiResultTest {

    @Test
    void success_setsSuccessCodeAndData() {
        ApiResult<String> result = ApiResult.success("data");
        assertEquals(GlobalErrorCodeConstants.SUCCESS.getCode(), result.getCode());
        assertEquals("data", result.getData());
        assertEquals("", result.getMsg());
    }

    @Test
    void error_setsCodeAndMessage() {
        ApiResult<Void> result = ApiResult.error("400", "参数错误");
        assertEquals("400", result.getCode());
        assertEquals("参数错误", result.getMsg());
    }

    @Test
    void error_rejectsSuccessCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ApiResult.error(GlobalErrorCodeConstants.SUCCESS.getCode(), "不应该成功"));
    }

    @Test
    void error_formatsParams() {
        ApiResult<Void> result = ApiResult.error(new ErrorCode("400", "参数 {} 不正确"), "pageNo");
        assertEquals("400", result.getCode());
        assertEquals("参数 pageNo 不正确", result.getMsg());
    }

    @Test
    void error_fromErrorCode_setsCodeAndMsg() {
        ApiResult<Void> result = ApiResult.error(new ErrorCode("403", "没有该操作权限"));
        assertEquals("403", result.getCode());
        assertEquals("没有该操作权限", result.getMsg());
    }

    @Test
    void error_errorCodeRejectsSuccessCode() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ApiResult.error(
                                new ErrorCode(GlobalErrorCodeConstants.SUCCESS.getCode(), "不该成功")));
    }

    @Test
    void error_varargsRejectsSuccessCode() {
        // varargs 重载同样拒绝 SUCCESS 码 + 参数（防误用：成功码不允许携带错误参数）
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ApiResult.error(
                                new ErrorCode(GlobalErrorCodeConstants.SUCCESS.getCode(), "不该成功"),
                                "参数"));
    }

    @Test
    void isSuccess_detectsSuccessCode() {
        assertTrue(ApiResult.isSuccess(GlobalErrorCodeConstants.SUCCESS.getCode()));
        assertFalse(ApiResult.isSuccess("400"));
    }

    @Test
    void isSuccess_isError_onResult() {
        ApiResult<String> ok = ApiResult.success("x");
        assertTrue(ok.isSuccess());
        assertFalse(ok.isError());
        ApiResult<Void> bad = ApiResult.error("500", "系统异常");
        assertFalse(bad.isSuccess());
        assertTrue(bad.isError());
    }

    @Test
    void checkError_success_noThrow() {
        ApiResult<String> ok = ApiResult.success("data");
        ok.checkError();
        assertEquals("data", ok.getCheckedData());
    }

    @Test
    void checkError_failure_throwsServiceException() {
        ApiResult<Void> bad = ApiResult.error("1001", "用户不存在");
        ServiceException ex = assertThrows(ServiceException.class, bad::checkError);
        assertEquals("1001", ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void getCheckedData_failure_throwsServiceException() {
        ApiResult<Void> bad = ApiResult.error("1001", "用户不存在");
        assertThrows(ServiceException.class, bad::getCheckedData);
    }

    @Test
    void error_fromServiceException() {
        ApiResult<Void> result = ApiResult.error(new ServiceException("1002", "手机号已存在"));
        assertEquals("1002", result.getCode());
        assertEquals("手机号已存在", result.getMsg());
    }

    @Test
    void error_fromApiResult() {
        ApiResult<Void> source = ApiResult.error("400", "参数错误");
        ApiResult<String> result = ApiResult.error(source);
        assertEquals("400", result.getCode());
        assertEquals("参数错误", result.getMsg());
        // 防御性断言：确保 Assert 工具正常可用
        Assert.notNull(result, "result 不能为空");
    }

    @Test
    void code_fieldIsString() throws NoSuchFieldException {
        // 契约：code 字段为 String 形态，JSON 序列化天然输出字符串（system-test 断言 "0"/"400" asText）
        assertEquals(String.class, ApiResult.class.getDeclaredField("code").getType());
    }

    @Test
    void setters_roundTrip() {
        ApiResult<String> result = new ApiResult<>();
        result.setCode("500");
        result.setMsg("系统异常");
        result.setData("payload");
        assertEquals("500", result.getCode());
        assertEquals("系统异常", result.getMsg());
        assertEquals("payload", result.getData());
    }

    @Test
    void success_nullData_allowed() {
        ApiResult<String> result = ApiResult.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
        assertEquals("", result.getMsg());
    }

    @Test
    void equals_sameValues_true() {
        ApiResult<String> a = ApiResult.success("data");
        ApiResult<String> b = ApiResult.success("data");
        assertEquals(a, b);
        // 对称性
        assertEquals(b, a);
        // hashCode 一致性
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstance_true() {
        ApiResult<String> result = ApiResult.success("data");
        assertEquals(result, result);
    }

    @Test
    void equals_nullAndDifferentType_false() {
        ApiResult<String> result = ApiResult.success("data");
        assertFalse(result.equals(null));
        assertFalse(result.equals(new Object()));
    }

    @Test
    void equals_differentCodeMsgOrData_false() {
        ApiResult<String> base = ApiResult.success("data");
        // code 不同（success vs error）
        assertNotEquals(base, ApiResult.error("400", ""));
        // msg 不同
        assertNotEquals(ApiResult.error("400", "msg1"), ApiResult.error("400", "msg2"));
        // data 不同
        assertNotEquals(base, ApiResult.success("other"));
    }

    @Test
    void equals_nullFields_symmetric() {
        ApiResult<String> a = new ApiResult<>();
        ApiResult<String> b = new ApiResult<>();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsFields() {
        ApiResult<String> result = ApiResult.success("data");
        String text = result.toString();
        assertTrue(text.contains("ApiResult"));
        assertTrue(text.contains("data"));
    }
}
