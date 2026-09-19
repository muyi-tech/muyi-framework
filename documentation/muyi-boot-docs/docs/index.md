# Muyi Framework

**基于 Spring Boot 4 / Java 21 的企业级开发框架。**

它不重复造轮子，而是把团队工程实践中沉淀的规范固化为可复用的构件：

- **一处定义，处处生效**：依赖版本由 BOM 单一权威源管理，构建规范由 Parent 统一固化
- **开箱即用**：统一响应、异常体系、参数校验等通用组件直接引入
- **零侵入接入**：继承 Parent 或仅导入 BOM，两种方式业务代码完全一致

## 构件全景

| 分层 | 构件 | 定位 |
|------|------|------|
| BOM | `muyi-boot-dependencies` | 依赖版本**唯一权威源**，`import` 方式导入 |
| Parent | `muyi-boot-parent` | 业务应用父 POM，固化构建规范 |
| 核心 | `muyi-boot-starter` | 跨 Web 栈共享的异常处理支撑（无 Web 依赖） |
| Web 模块 | `muyi-boot-webmvc` / `muyi-boot-webflux` | 双栈全局异常处理自动装配 |
| 门面 Starter | `muyi-boot-starter-webmvc` / `-webflux` | 一个依赖引入框架增强 + Spring 官方栈 |
| 测试门面 | `muyi-boot-starter-webmvc-test` / `-webflux-test` | Web 应用测试套件 |
| 通用组件 | `muyi-commons-core` / `muyi-commons-spring` | 零 Spring 工具 / Spring 集成层 |

## 文档导航

<div class="grid cards" markdown>

- **[接入指南](getting-started.md)**

  ***

  两种接入路径（继承 Parent / 导入 BOM）、门面 Starter 选型、完整可运行示例。

- **[异常处理指南](exception-handling.md)**

  ***

  统一响应 `ApiResult`、业务异常体系、模块扩展点与配置开关。

- **[错误码分配表](error-codes.md)**

  ***

  框架内置 12 码、区间规则与业务码段规划。

- **[错误码与国际化](i18n.md)**

  ***

  注册表分层与覆盖策略、i18n 三级回退、文件登记与热更新。

- **[版本与发布](versioning.md)**

  ***

  版本号策略、发布节奏与支持承诺、三段式发布流水线。

</div>

## 快速开始

```xml
<parent>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath/>
</parent>

<dependencies>
    <dependency>
        <groupId>io.github.muyi-tech.boot</groupId>
        <artifactId>muyi-boot-starter-webmvc</artifactId>
    </dependency>
</dependencies>
```

完整说明见 [接入指南](getting-started.md)。

## 相关入口

- 仓库与贡献：[GitHub](https://github.com/muyi-tech/muyi-framework) · [CONTRIBUTING](https://github.com/muyi-tech/muyi-framework/blob/main/CONTRIBUTING.md)
- 支持渠道与版本支持策略：[SUPPORT](https://github.com/muyi-tech/muyi-framework/blob/main/SUPPORT.md)
- 构件获取：[Maven Central](https://central.sonatype.com/namespace/io.github.muyi-tech)
