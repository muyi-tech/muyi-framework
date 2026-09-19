# Muyi Boot（核心）

跨 Web 栈共享的框架核心——错误码运行时注册表与异常处理共享支撑。
对标 `spring-boot` 核心构件的"能力下沉、Web 无关"定位。

## 模块内容

- `errorcode` 包：错误码注册表体系
  - `ErrorCodeRegistry` / `DefaultErrorCodeRegistry`：volatile 快照 + 原子替换，读路径无锁；
    数字码规范化（十进制无前导零）；`reload()` 全来源重聚合并发布 `ErrorCodeReloadedEvent`
  - `ErrorCodeProvider`（SPI，`Ordered`）：`ConstantsErrorCodeProvider`（框架 12 全局码兜底）、
    `FileErrorCodeProvider`（`muyi.error-code.locations` 文件源）、业务自定义源（DB/Nacos 挂载点）
  - `OverridePolicy`：重复码策略（`LAST_WINS` / `FIRST_WINS` / `REJECT` fail-fast）
  - `ErrorCodeMessages`：i18n 三级回退纯函数（`zh-CN` → `zh` → 默认 msg）+ `{}` 重格式化
  - `autoconfigure`：`ErrorCodeAutoConfiguration` 装配注册表
    （`muyi.error-code.locations` / `muyi.error-code.override-policy`）
- `exception.support` 包
  - `ExceptionHandlerSupport`：双栈核心处理器的共享接口——业务异常降噪、
    cause 链检查与 500 兜底（`handleServiceException` / `handleUnexpectedException`）

## 设计约束

- **Web 无关**：不依赖 Servlet / WebFlux；HTTP 适配层在
  [`muyi-boot-webmvc`](../../module/muyi-boot-webmvc/README.md) /
  [`muyi-boot-webflux`](../../module/muyi-boot-webflux/README.md)
- 异常处理的行为契约与扩展点以文档站为准：
  [异常处理指南](../../documentation/muyi-boot-docs/docs/exception-handling.md) ·
  [错误码与国际化](../../documentation/muyi-boot-docs/docs/i18n.md)

## 使用方式

Web 应用经门面引入即可，无需直接依赖本模块：

```xml
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-starter</artifactId> <!-- 或 -webmvc / -webflux 门面 -->
</dependency>
```

## 相关模块

- [`muyi-boot-starter`](../../starter/muyi-boot-starter/README.md) — 本模块的门面（纯 pom 聚合）
- [`muyi-commons-spring`](../../module/muyi-commons-spring/README.md) — ErrorCode / ServiceException 定义处
