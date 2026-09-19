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
package io.github.muyitech.common.spring.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 验证工具类。
 *
 * @author keep simple
 * @since 2025/4/7
 */
public class ValidationUtils {

    /**
     * 中国大陆手机号正则：可选 +86/0086 前缀，1 开头，第二位 3-9，共 11 位。
     *
     * <p>
     * 号段字符类内不得混入逗号字面量（历史 bug：{@code 4[0,1,4-9]} 会放行
     * {@code 14,xxxxxxxx}），合法写法为紧邻枚举 {@code 4[014-9]}。
     */
    private static final Pattern PATTERN_MOBILE =
            Pattern.compile(
                    "^(?:(?:\\+|00)86)?1(?:(?:3[\\d])|(?:4[014-9])|(?:5[0-35-9])|(?:6[2567])|(?:7[0-8])|(?:8[\\d])|(?:9[0-35-9]))\\d{8}$");

    /**
     * URL 正则。
     */
    private static final Pattern PATTERN_URL =
            Pattern.compile(
                    "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    /**
     * XML NCName 正则。
     */
    private static final Pattern PATTERN_XML_NCNAME =
            Pattern.compile("[a-zA-Z_][\\-_.0-9_a-zA-Z$]*");

    /**
     * 中国大陆固定电话正则：0 开头的 3-4 位区号，可选连字符，后跟 7-8 位号码。
     */
    private static final Pattern PATTERN_TEL = Pattern.compile("^0\\d{2,3}-?\\d{7,8}$");

    private ValidationUtils() {}

    /**
     * 验证手机号是否合法。
     * @param mobile 待验证的手机号码
     * @return 返回验证结果，如果手机号合法则返回 true，否则返回 false
     */
    public static boolean isMobile(String mobile) {
        // 首先检查手机号码字符串是否为空，然后使用预定义的正则表达式模式进行匹配
        return StringUtils.hasText(mobile) && PATTERN_MOBILE.matcher(mobile).matches();
    }

    /**
     * 验证 URL 是否合法。
     * @param url 待验证的 URL
     * @return 返回验证结果，如果 URL 合法则返回 true，否则返回 false
     */
    public static boolean isURL(String url) {
        return StringUtils.hasText(url) && PATTERN_URL.matcher(url).matches();
    }

    /**
     * 验证字符串是否为合法的 XML NCName。
     * @param str 待验证的字符串
     * @return 返回验证结果，如果是合法的 XML NCName 则返回 true，否则返回 false
     */
    public static boolean isXmlNCName(String str) {
        return StringUtils.hasText(str) && PATTERN_XML_NCNAME.matcher(str).matches();
    }

    /**
     * 验证中国大陆固定电话是否合法。
     * @param tel 待验证的电话号码
     * @return 返回验证结果，如果电话号码合法则返回 true，否则返回 false
     */
    public static boolean isTel(String tel) {
        return StringUtils.hasText(tel) && PATTERN_TEL.matcher(tel).matches();
    }

    /**
     * 对给定的对象进行验证，检查其是否符合指定的验证组。
     *
     * <p>
     * 运行时前提：classpath 需存在 Jakarta Validation provider（如 hibernate-validator）；
     * 本模块将其声明为 test scope，业务应用经 {@code spring-boot-starter-validation}
     * 或 {@code muyi-boot-starter-webmvc/-webflux} 传递获得。无 provider 环境调用本方法
     * 将抛 {@link ExceptionInInitializerError}。
     *
     * @param object 需要进行验证的对象
     * @param groups 一个或多个验证组，用于指定对象需要满足的验证规则集合
     */
    public static void validate(Object object, Class<?>... groups) {
        // 使用缓存的验证器实例对对象进行验证（懒加载，避免无验证实现时影响其他静态方法）
        validate(getValidator(), object, groups);
    }

    /**
     * 验证给定对象是否符合指定的一组验证规则。
     * @param validator 用于执行验证的 Validator 实例
     * @param object 需要进行验证的对象
     * @param groups 验证规则组，可以指定多个验证规则组；如果未指定任何组，则使用默认验证规则
     * @throws ConstraintViolationException 如果对象未通过验证，即存在验证规则违规情况
     */
    public static void validate(Validator validator, Object object, Class<?>... groups) {
        Assert.notNull(validator, "validator 不能为空");
        // 执行对象验证，并收集所有违反验证规则的异常信息
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        // 如果存在违反验证规则的情况，则抛出异常
        if (CollectionUtils.isNotEmpty(constraintViolations)) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    /**
     * 懒加载并缓存默认验证器（线程安全的类加载机制保证单例）。
     * @return 默认验证器实例
     */
    private static Validator getValidator() {
        return ValidatorHolder.VALIDATOR;
    }

    /**
     * 验证器单例持有者。
     */
    private static final class ValidatorHolder {

        private static final Validator VALIDATOR =
                Validation.buildDefaultValidatorFactory().getValidator();
    }
}
