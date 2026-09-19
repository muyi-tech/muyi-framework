# Muyi Commons Core

**零 Spring 依赖**的纯工具组件——可在任何 Java 项目中使用。

## 模块内容

- `beans` 包
  - `KeyValue<K, V>`：通用键值对容器（数据字典返回值的载体，序列化为 `key` / `value` 字段）
  - `ArrayValuable`：可枚举值接口（配合枚举体系使用）
- `enums` 包：通用枚举（`SexEnum` / `StatusEnum` / `DeletedEnum`），实现 `ArrayValuable`
  供数据字典接口统一输出

## 设计约束

- **零依赖**：不依赖 Spring，也不依赖 Lombok（访问器手写），任何 Java 项目可直接引入
- 本模块及其上层（[`muyi-commons-spring`](../muyi-commons-spring/README.md)）**不含业务逻辑**

## 使用方式

```xml
<dependency>
    <groupId>io.github.muyi-tech</groupId>
    <artifactId>muyi-commons-core</artifactId>
</dependency>
```

版本由 [`muyi-boot-dependencies`](../../platform/muyi-boot-dependencies/README.md) 统一管理。

## 相关模块

- [`muyi-commons-spring`](../muyi-commons-spring/README.md) — Spring 集成层（异常 / 响应模型 / 校验）
