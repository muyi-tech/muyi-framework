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
package io.github.muyitech.boot.webflux.exception;

import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;

import io.github.muyitech.boot.errorcode.ErrorCodeRegistry;
import io.github.muyitech.boot.exception.support.ExceptionHandlerSupport;
import io.github.muyitech.common.spring.pojo.ApiResult;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * 未匹配路由 404 处理器（WebFlux）。
 *
 * <p>
 * WebFlux 中未匹配任何路由的 404 不经过 {@code @RestControllerAdvice}（advice 作用于
 * HandlerAdapter 层，而未匹配路由由 {@code WebExceptionHandler} 链处理），因此需要独立的
 * {@link WebExceptionHandler} 将其包装为 {@link ApiResult} JSON 响应。
 *
 * <p>
 * 仅处理 404 的 {@link ResponseStatusException}；其他异常原样放行（返回 {@code Mono.error}）
 * 交由后续处理器。
 *
 * <p>
 * 响应文案与 advice 侧 404 分支共用 {@link ExceptionHandlerSupport} 统一出口：恒为注册表解析的
 * NOT_FOUND 文案（默认"请求未找到"或业务翻译），不携带请求路径；路径只降级写入日志（warn 级）。
 * 注册表可选注入（缺省时回退错误码默认消息）。
 *
 * @author keep simple
 * @since 2026/9/13
 */
@Order(-2) // 先于框架默认 ResponseStatusExceptionHandler（order=0）执行
public class MuyiNotFoundWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(MuyiNotFoundWebExceptionHandler.class);

    private final JsonMapper jsonMapper;

    private final ErrorCodeRegistry registry;

    /**
     * 默认构造，使用内置 Jackson 3 {@link JsonMapper}，无错误码注册表（回退默认消息）。
     */
    public MuyiNotFoundWebExceptionHandler() {
        this(JsonMapper.builder().build(), null);
    }

    /**
     * 指定 ObjectMapper 构造，无错误码注册表（回退默认消息）。
     * @param jsonMapper Jackson 3 mapper
     */
    public MuyiNotFoundWebExceptionHandler(JsonMapper jsonMapper) {
        this(jsonMapper, null);
    }

    /**
     * 完整构造。
     * @param jsonMapper Jackson 3 mapper
     * @param registry 错误码注册表，可为 null（回退错误码默认消息）
     */
    public MuyiNotFoundWebExceptionHandler(JsonMapper jsonMapper, ErrorCodeRegistry registry) {
        this.jsonMapper = jsonMapper;
        this.registry = registry;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            log.warn("[notFound] {}", exchange.getRequest().getPath().value());
            // WebFlux 契约：exchange.getLocaleContext() 恒非 null，无需判空
            Locale locale = exchange.getLocaleContext().getLocale();
            ApiResult<?> result =
                    ApiResult.error(
                            NOT_FOUND.getCode(),
                            ExceptionHandlerSupport.resolveErrorCodeMessage(
                                    NOT_FOUND, locale, registry));
            return writeJson(exchange, result);
        }
        return Mono.error(ex);
    }

    /**
     * 将错误结果以 JSON 写入响应（HTTP 404 + application/json）。
     * @param exchange 当前请求交换
     * @param result 错误结果
     * @return 写入完成的 Mono
     */
    private Mono<Void> writeJson(ServerWebExchange exchange, ApiResult<?> result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(result));
        return response.writeWith(Mono.just(buffer));
    }
}
