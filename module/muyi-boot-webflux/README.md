# Muyi Boot WebFlux

WebFlux 响应式异常处理模块——与 [`muyi-boot-webmvc`](../muyi-boot-webmvc/README.md) 行为对齐的
Reactive 实现，让响应式应用获得开箱即用的全局异常处理。

## 模块内容

| 组件 | 职责 |
|------|------|
| `ReactiveCoreExceptionHandler` | 全局兜底异常处理器（响应式 `@RestControllerAdvice`） |
| `ReactiveModuleExceptionHandler` | 模块业务异常（`ServiceException`）处理器 |
| `MuyiNotFoundWebExceptionHandler` | 404（路由未匹配）兜底异常处理器 |
| `MuyiWebFluxAutoConfiguration` | 自动装配（注册于
  `META-INF/spring/...AutoConfiguration.imports`） |
| `MuyiWebFluxProperties` | 配置项（前缀 `muyi.webflux.exception-handler`） |
| 共享逻辑 | 继承 [`muyi-boot-starter`](../../starter/muyi-boot-starter/README.md) 的 `ExceptionHandlerSupport`（日志降噪 / cause 链兜底） |

## 行为约定

与 WebMVC 栈**完全一致**（双栈行为对齐，业务代码无需感知技术栈差异）：

- 所有异常统一转换为 `ApiResult`（见 [`muyi-commons-spring`](../muyi-commons-spring/README.md)）
- **业务异常**（`ServiceException`，如 BAD_REQUEST）：HTTP 200 + body 中业务码非 0
  （框架约定：错误码在 body，不在 HTTP 状态层）
- **系统异常**：由 `ReactiveCoreExceptionHandler` 兜底，完整 cause 链记录日志
- **404**：由 `MuyiNotFoundWebExceptionHandler` 统一转换为错误 `ApiResult`
- 以上行为由部署系统测试在真实进程（`java -jar` → HTTP）下验证：
  [`muyi-system-tests`](../../system-test/muyi-boot-deployment-system-tests/README.md)

## 使用方式

通常通过门面引入（同时获得 Spring 官方栈）：

```xml
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-starter-webflux</artifactId>
</dependency>
```

单独引入（已自带 `spring-boot-starter-webflux` 的项目）：

```xml
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-webflux</artifactId>
</dependency>
```

## 相关模块

- [`muyi-boot-webmvc`](../muyi-boot-webmvc/README.md) — Servlet 栈的等价实现
- [`muyi-commons-spring`](../muyi-commons-spring/README.md) — 异常与响应模型定义
