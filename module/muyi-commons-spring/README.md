# Muyi Commons Spring

Spring 集成的通用组件——**统一响应模型、异常体系与校验工具**，是框架异常处理能力的基石。

## 模块内容

### 统一响应（`pojo` 包）

| 类 | 说明 |
|----|------|
| `ApiResult<T>` | 统一响应体（`code` / `msg` / `data`），业务异常时 HTTP 200 + body 携带错误码 |
| `PageParam` / `PageResult<T>` | 分页请求与分页结果 |
| `SortField` / `SortablePageParam` | 可排序分页 |

### 异常体系（`exception` 包）

| 类 | 说明 |
|----|------|
| `ServiceException` | 业务异常（预期内，日志降噪）——构造后终态不可变，支持 cause 异常链；系统级错误用错误码段区分（4xx/5xx/999） |
| `ServiceExceptionUtil` | 异常快捷构造（配合错误码） |
| `ErrorCode` | 错误码对象（String 码值 + 默认文案 + 多语言文案）；`GlobalErrorCodeConstants` 提供全局码（如 `BAD_REQUEST`="400"） |

> 业务错误码的分段与登记规范见框架设计文档第 4 章及文档站「错误码分配表」，
> 原 `ServiceErrorCodeRange` 注释约定已废弃删除。

### 校验（`validation` 包）

- `@Mobile` / `@Telephone`：手机号 / 座机号校验（`MobileValidator` / `TelephoneValidator`）
- `@InEnum`：值必须在指定枚举范围内（`InEnumValidator` / `InEnumCollectionValidator`）
- `ValidationUtils`：编程式校验工具

## 设计约束

- 仅依赖 Spring 基础包（`spring-core` / `spring-context`）与校验 API，**不含 Web 栈**——
  异常到 HTTP 响应的转换由 [`muyi-boot-webmvc`](../muyi-boot-webmvc/README.md)
  / [`muyi-boot-webflux`](../muyi-boot-webflux/README.md) 完成

## 使用方式

业务异常抛出示例：

```java
throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST);
```

版本由 [`muyi-boot-dependencies`](../../platform/muyi-boot-dependencies/README.md) 统一管理；
通常经由 `muyi-boot-starter` 传递依赖获得，无需单独引入。

## 相关模块

- [`muyi-commons-core`](../muyi-commons-core/README.md) — 零 Spring 依赖的底层工具
- [`muyi-boot-starter`](../../starter/muyi-boot-starter/README.md) — 异常处理共享逻辑
