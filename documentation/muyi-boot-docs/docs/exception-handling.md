# 异常处理指南

Muyi Framework 的异常处理能力由三部分组成：**统一响应模型**（`muyi-commons-spring`）、
**双栈自动装配**（`muyi-boot-webmvc` / `muyi-boot-webflux`）、**模块扩展点**
（`ModuleExceptionHandler` 标记接口）。

## 统一响应：ApiResult

所有 HTTP 响应统一包装为 `ApiResult<T>`（`code` / `msg` / `data`）：

```json
{
  "code": "0",
  "msg": "",
  "data": { "key": 1, "value": "example" }
}
```

分页场景使用 `PageParam` / `PageResult<T>`，可排序分页使用
`SortablePageParam` / `SortField`。

## 异常体系

| 异常 | 定位 | 日志行为 |
|------|------|----------|
| `ServiceException` | 业务异常（预期内），携带 `ErrorCode` | **降级为 WARN/DEBUG**，不污染告警 |
| `ServerException` | 系统异常（非预期） | 记录完整 cause 链，便于排障 |

业务代码中通过工具类抛出（配合全局错误码常量）：

```java
import static io.github.muyitech.common.spring.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import io.github.muyitech.common.spring.exception.ServiceExceptionUtil;

throw ServiceExceptionUtil.exception(BAD_REQUEST);
```

各业务模块使用专属错误码段时，遵循框架设计文档第 4 章的分配与登记规范
（见仓库 `docs/design/exception-handling-redesign.md`），实现 `ErrorCode`
定义错误码对象，并通过 `muyi.error-code.locations` 注册进运行时注册表。

## 行为约定（双栈一致）

WebMVC 与 WebFlux 两个技术栈**行为完全对齐**，业务代码无需感知差异：

- **业务异常**（`ServiceException`）：HTTP 200 + body 中业务码非 `"0"`
  （框架约定：错误码在 body 且为字符串形态，不在 HTTP 状态层）
- **系统异常**：由核心处理器兜底，完整 cause 链记录日志
- **404**：路由未匹配统一转换为错误 `ApiResult`——WebMVC 处理
  `NoHandlerFoundException`/`NoResourceFoundException`（随核心处理器常开）；WebFlux
  同样转换，可用 `muyi.webflux.exception-handler.handle-not-found=false` 交回框架默认

!!! note "该契约由真实进程测试固化"

    双栈行为由部署系统测试验证：示例应用打成可执行 jar、以 `java -jar` 启动真实 JVM 进程、
    通过 HTTP 断言响应格式，见仓库 `system-test/muyi-boot-deployment-system-tests` 模块。

## 模块扩展点

各业务模块可注册自己的异常处理器，实现 `ModuleExceptionHandler`（WebFlux 为对应的
Reactive 标记接口）：

```java
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1) // 通用模块处理器置核心兜底前一格
public class ModuleSampleWebExceptionHandler implements ModuleExceptionHandler {

    @Override
    public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        // SPI 通道：只处理本模块异常，其余返回 null 放行
        if (ex instanceof ModuleSampleWebException webEx) {
            return ApiResult.error(webEx.getErrorCode(), webEx.getArgs());
        }
        return null;
    }

    @ExceptionHandler(ModuleSampleWebException.class)
    public ApiResult<?> handleModuleSampleWebException(ModuleSampleWebException ex) {
        // advice 通道：Spring 精确匹配优先于核心兜底
        return ApiResult.error(ex.getErrorCode(), ex.getArgs());
    }
}
```

处理链路：

1. 核心处理器优先调用各模块处理器（按 `@Order` 优先级）
2. 模块处理器返回**非空**结果则直接返回
3. 返回 `null` 表示不处理，交由后续处理器
4. 最终由核心处理器兜底

!!! note "SPI 分发的接入点"

    模块处理器由核心处理器的 `@ExceptionHandler(Exception.class)` 兜底入口在落 500
    之前依序分发——未被更精确 advice 方法匹配的异常都会先经过模块扩展点，扩展点全部
    放行（返回 `null`）后才进入系统异常兜底。启动日志会输出本次装载的模块处理器清单
    及其优先级，便于集成方核对。

    两条纪律：处理器应保持**无副作用状态**——advice 兜底链内部首个命中即短路，但业务方若
    同时使用编程式入口 `allExceptionHandler` 与 advice 通道，同一异常仍可能被询问两次；
    请求 Locale 双栈语义一致——均从请求作用域解析（WebMVC 取 `HttpServletRequest.getLocale()`、
    WebFlux 取 exchange 的 `LocaleContext`），请求未携带时回退 `LocaleContextHolder`，最终默认值。

### 自定义实现（完整案例）

仓库三个冒烟工程提供了可直接运行的完整案例：业务模块抛出**非 `ServiceException`
体系的专有异常**（`ServiceException` 为 final 类，不可继承），核心处理器不识别该
类型——模块处理器在核心兜底前拦截并翻译为自定义响应：

```java
// 业务专有异常：携带 ErrorCode，核心处理器不识别
public class DemoModuleException extends RuntimeException {

    private final ErrorCode errorCode;

    public DemoModuleException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

// 模块级自定义处理器（双通道，与仓库冒烟工程实现一致）：
// 业务专有异常优先经 advice 通道精确拦截；SPI 通道兜住"非精确匹配"路径
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 专拦本模块业务异常，最先执行
public class DemoCustomModuleExceptionHandler implements ModuleExceptionHandler {

    @ExceptionHandler(DemoModuleException.class)
    public ApiResult<?> handleDemoModuleException(DemoModuleException ex) {
        return ApiResult.error(ex.getErrorCode(), ex.getArgs());
    }

    @Override
    public ApiResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        // SPI 通道：识别本模块专有异常则翻译为自定义响应，其余放行交回核心兜底
        if (ex instanceof DemoModuleException moduleEx) {
            return ApiResult.error(moduleEx.getErrorCode(), moduleEx.getArgs());
        }
        return null;
    }
}
```

完整可运行示例见仓库 `smoke-test` 下的三个冒烟工程
（`muyi-boot-smoke-test-parent-inheritance` / `muyi-boot-smoke-test-bom-import` /
`muyi-boot-smoke-test-webflux`；响应式栈实现 `ReactiveModuleExceptionHandler`，
方法签名为 `allExceptionHandler(ServerWebExchange, Throwable)`），并由部署系统
测试以真实 JVM 进程验证该链路。

## 配置开关

框架全局异常处理器可通过配置关闭或让位：

| 配置项 | 默认 | 说明 |
|--------|------|------|
| `muyi.webmvc.exception-handler.enabled` | `true` | 设为 `false` 不注册 WebMVC 核心处理器，业务方可完全自定义 |
| `muyi.webmvc.exception-handler.service-exception-log.ignore-messages` | 空 | 业务异常命中这些 message 时降为 debug 且不打栈（高频预期异常降噪） |
| `muyi.webmvc.exception-handler.service-exception-log.stack-trace-frames` | `1` | 业务异常日志保留的栈帧数 |
| `muyi.webflux.exception-handler.enabled` | `true` | 同上（Reactive 栈） |
| `muyi.webflux.exception-handler.handle-not-found` | `true` | 设为 `false` 后 404 不再转 `ApiResult`，交回 WebFlux 默认行为 |
| `muyi.webflux.exception-handler.service-exception-log.*` | 同 WebMVC | 降噪两键，Reactive 栈同名 |
| `muyi.error-code.locations` | 空 | 错误码定义文件（`classpath:`/`file:` 前缀），见[错误码与国际化](i18n.md) |
| `muyi.error-code.override-policy` | `LAST_WINS` | 重复码策略（`LAST_WINS`/`FIRST_WINS`/`REJECT`） |

业务方已定义同类型 Bean 时，自动装配**自动 back off**（`@ConditionalOnMissingBean`）。

## 下一步

- 错误码分配与区间规则：[错误码分配表](error-codes.md)
- 运行时注册表、i18n 三级回退与热更新：[错误码与国际化](i18n.md)
- 设计决策母本：仓库 `docs/design/exception-handling-redesign.md`
- 完整接入示例：[接入指南](getting-started.md)
