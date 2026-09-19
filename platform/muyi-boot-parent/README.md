# Muyi Boot Parent

业务应用专用 parent——**继承即得完整依赖管理与开箱即用的构建配置**，对标 Spring Boot 的
`spring-boot-starter-parent`。

## 模块定位

- 依赖管理：import [`muyi-boot-dependencies`](../muyi-boot-dependencies/README.md)（BOM），
  业务应用**无需再写任何版本号**
- 构建配置：继承聚合根 `muyi-boot-build`（仓库根 pom）的插件管理

## 默认提供的构建配置

| 项 | 说明 |
|----|------|
| `spring-boot-maven-plugin` | 默认绑定 `repackage` 到 package 阶段，业务应用显式声明即零配置产出可执行 jar |
| 测试三件套 | `junit-jupiter` / `mockito-core` + `mockito-junit-jupiter` / `assertj-core` 默认 test scope |
| `maven-surefire-plugin` / `maven-jar-plugin` / `maven-source-plugin` | 默认配置与版本管理 |
| `maven-enforcer-plugin` | 构建环境约束（Maven 版本等） |
| `jacoco-maven-plugin` | 覆盖率统计 |
| `lombok` + 注解处理器 | `spring-boot-autoconfigure-processor` / `spring-boot-configuration-processor` 默认引入 |

## 使用方式

```xml
<parent>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-parent</artifactId>
    <version>${muyi.version}</version>
    <relativePath/>
</parent>
```

搭配一个门面 starter 即完成接入（可选 `muyi-boot-starter` / `muyi-boot-starter-webmvc` / `muyi-boot-starter-webflux`，见根 README 模块结构）。

## 相关模块

- [`muyi-boot-dependencies`](../muyi-boot-dependencies/README.md) — 依赖管理来源
- [完整示例：`muyi-boot-smoke-test-parent-inheritance`](../../smoke-test/muyi-boot-smoke-test-parent-inheritance/) —
  继承 parent 的接入示范
