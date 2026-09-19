# Muyi Boot Starter

门面 starter —— **聚合 `muyi-boot` 核心**（WebMVC / WebFlux 全局异常处理器的共享支撑
`ExceptionHandlerSupport`），本身为 **纯 pom、零代码**：类的实现位于
[`core/muyi-boot`](../../core/muyi-boot/pom.xml)（对标 Spring Boot 的本体命名惯例：
`spring-boot` 本体即无后缀）。

## 引入内容

- `muyi-boot`：`ExceptionHandlerSupport` 共享异常处理逻辑，包括
  - **业务异常日志降噪**：预期内业务异常（如参数错误）降级为 WARN/DEBUG，不污染告警
  - **系统异常 cause 链兜底**：非预期异常记录完整 cause 链，便于排障
- 依赖 [`muyi-commons-spring`](../../module/muyi-commons-spring/README.md)：
  `ServiceException` 体系与 `ApiResult` 统一响应

## 使用方式

- **Web 应用**：无需直接引入——由
  [`muyi-boot-starter-webmvc`](../muyi-boot-starter-webmvc/pom.xml) /
  [`muyi-boot-starter-webflux`](../muyi-boot-starter-webflux/pom.xml) 传递依赖
- **非 Web 应用**（定时任务、MQ 消费者等需要统一异常语义的场景）：直接引入本门面复用
  `ExceptionHandlerSupport` 与 `muyi-commons-spring` 组件

```xml
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-starter</artifactId>
</dependency>
```

## 相关模块

- [`muyi-boot`](../../core/muyi-boot/pom.xml) — 核心本体（类与依赖的真实所在地）
- [`muyi-boot-webmvc`](../../module/muyi-boot-webmvc/README.md) — WebMVC 异常处理自动装配
- [`muyi-boot-webflux`](../../module/muyi-boot-webflux/README.md) — WebFlux 异常处理自动装配
