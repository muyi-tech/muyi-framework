# Muyi Boot Dependencies

依赖版本 BOM（Bill of Materials）——**全仓库依赖版本的单一事实源**，对标 Spring Boot 官方
`spring-boot-dependencies`。

## 模块定位

- 持有所有第三方依赖版本属性（Spring Boot 4.x、Spring AI、Spring AI Alibaba、AgentScope 等），
  并通过 `dependencyManagement` 全量管理
- 继承聚合根 [`muyi-boot-build`](../../pom.xml)（仓库根 pom）的插件配置；
  发布时经聚合根的 `flatten-maven-plugin`（`resolveCiFriendliesOnly`）展开 `${revision}`，
  发布后 BOM **自包含、可独立 import**
- 本模块是 `pom` 打包，不含代码

## 使用方式

**方式一：不继承 parent，直接 import BOM**（适合已有 parent 链的项目）：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.muyi-tech.boot</groupId>
            <artifactId>muyi-boot-dependencies</artifactId>
            <version>${muyi.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**方式二：继承 [`muyi-boot-parent`](../muyi-boot-parent/README.md)**（推荐，同时获得依赖管理
与构建配置，无需手动 import）。

## 相关模块

- [`muyi-boot-parent`](../muyi-boot-parent/README.md) — 业务应用 parent，依赖管理源自本 BOM
- [完整示例：`muyi-boot-smoke-test-bom-import`](../../smoke-test/muyi-boot-smoke-test-bom-import/) — import BOM 的接入示范
