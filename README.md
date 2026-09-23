# Muyi Framework

<div align="center">

**基于 Spring Boot 4 / Java 21 的企业级开发框架**

统一的依赖管理（BOM）· 构建规范（Parent）· 通用组件（Commons）· 起步依赖（Starter）

<a href="https://central.sonatype.com/artifact/io.github.muyi-tech.boot/muyi-boot-dependencies">
    <img
        src="https://img.shields.io/maven-central/v/io.github.muyi-tech.boot/muyi-boot-dependencies?color=green&style=flat-square"
        alt="Maven Central"
    />
</a>
<a href="https://codecov.io/gh/muyi-tech/muyi-framework">
    <img
        src="https://codecov.io/gh/muyi-tech/muyi-framework/branch/main/graph/badge.svg"
        alt="codecov"
    />
</a>
<a href="https://adoptium.net/">
    <img
        src="https://img.shields.io/badge/Java-21-orange?style=flat-square"
        alt="Java 21"
    />
</a>
<a href="./LICENSE">
    <img
        src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square"
        alt="License Apache 2.0"
    />
</a>

</div>

---

## 📖 简介

Muyi Framework 是一套面向企业级应用的开发框架，基于 **Spring Boot 4 / Java 21** 构建。它不重复造轮子，而是把团队工程实践中沉淀的规范固化为可复用的构件：

- **一处定义，处处生效**：依赖版本由 BOM 单一权威源管理，构建规范由 Parent 统一固化
- **开箱即用**：通用组件（统一响应、异常体系、参数校验、数据字典）直接引入
- **零侵入接入**：继承 Parent 或仅导入 BOM，两种方式业务代码完全一致

## ✨ 特性

| 特性            | 说明                                                                                                              |
|---------------|-----------------------------------------------------------------------------------------------------------------|
| **统一依赖管理**    | `muyi-boot-dependencies` 作为依赖版本唯一权威源，`import` 方式导入，版本可控、可审计                                                     |
| **统一构建规范**    | `muyi-boot-parent` 固化插件版本、测试依赖、格式校验、环境约束等工程规范                                                                   |
| **通用工具**      | `muyi-commons-core` 纯工具、零 Spring 依赖；<br/>`muyi-commons-spring` 提供统一响应、异常体系、参数校验等 Spring 集成                      |
| **起步依赖**      | `muyi-boot-starter-webmvc` / `-webflux` 一个依赖引入框架增强与对应 Web 栈全局异常处理；<br/> `muyi-boot-starter` 为跨栈共享支撑门面（非 Web 应用） |
| **单版本源**      | 版本由根 pom `<revision>` 一处管理，flatten-maven-plugin 自动展开                                                            |
| **CI/CD 一体化** | GitHub Actions 双 OS 构建 + License 检查 + Codecov 覆盖率 + Maven Central 自动发布                                          |

## 📐 模块结构

```
muyi-framework
├── platform/                        构建平台
│   ├── muyi-boot-dependencies       BOM，依赖版本单一权威源（import 使用）
│   └── muyi-boot-parent             构建规范 Parent（业务应用继承）
├── core/                            核心本体（对标 spring-boot 本体 artifactId）
│   └── muyi-boot                    共享异常处理器支撑（ExceptionHandlerSupport）
├── module/                          实现模块
│   ├── muyi-commons-core            纯工具，零 Spring 依赖
│   ├── muyi-commons-spring          Spring 集成（统一响应 / 异常体系 / 参数校验）
│   ├── muyi-boot-webmvc             WebMVC 全局异常处理自动装配
│   └── muyi-boot-webflux            WebFlux 全局异常处理自动装配
├── starter/                         门面 starter（官方同款空 jar，零代码）
│   ├── muyi-boot-starter            核心门面（聚合 muyi-boot）
│   ├── muyi-boot-starter-webmvc     WebMVC 一站式门面
│   ├── muyi-boot-starter-webflux    WebFlux 一站式门面
│   └── …-test                       测试门面（-webmvc-test / -webflux-test）
├── smoke-test/                      冒烟测试应用（不参与主构建，CI 冒烟）
│   ├── muyi-boot-smoke-test-parent-inheritance   方式一：继承 Parent
│   ├── muyi-boot-smoke-test-bom-import           方式二：仅导入 BOM
│   └── muyi-boot-smoke-test-webflux             响应式栈
└── system-test/
    └── muyi-boot-deployment-system-tests   部署系统测试（真实 JVM 进程 + HTTP）
```

## 🚀 快速开始

### 方式一：继承 Parent（推荐业务应用）

```xml

<parent>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-parent</artifactId>
    <version>1.0.2</version>
    <relativePath/>
</parent>

<dependencies>
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <!-- Web 应用选 -webmvc / -webflux 门面；非 Web 应用用 muyi-boot-starter -->
    <artifactId>muyi-boot-starter-webmvc</artifactId>
</dependency>
</dependencies>
```

一站式获得：依赖版本管理 + 构建规范 + 统一测试依赖 + WebMVC 栈框架增强。

### 方式二：仅导入 BOM（已有父级时）

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.muyi-tech.boot</groupId>
            <artifactId>muyi-boot-dependencies</artifactId>
            <version>1.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
<dependency>
    <groupId>io.github.muyi-tech.boot</groupId>
    <artifactId>muyi-boot-starter-webmvc</artifactId>
</dependency>
</dependencies>
```

适合已有父级（如 `spring-boot-starter-parent` 或公司内部 parent）无法更换继承关系的应用。

### 📚 完整示例

两种接入方式的完整可运行示例见 **[smoke-test](smoke-test)**，业务代码完全一致，差异仅在 `pom.xml`：

| 示例                                                                                            | 接入方式      | 演示能力                       |
|-----------------------------------------------------------------------------------------------|-----------|----------------------------|
| [muyi-boot-smoke-test-parent-inheritance](smoke-test/muyi-boot-smoke-test-parent-inheritance) | 继承 Parent | 统一响应 / 数据字典 / 业务异常 / 模块扩展点 |
| [muyi-boot-smoke-test-bom-import](smoke-test/muyi-boot-smoke-test-bom-import)                 | 仅导入 BOM   | 同上，验证两种方式零侵入               |

```bash
cd smoke-test/muyi-boot-smoke-test-parent-inheritance
mvn spring-boot:run
# curl http://localhost:8080/api/demo/hello
# {"code":"0","msg":"","data":"Hello, Muyi Framework!"}
```

## 📥 获取构件


SNAPSHOT 需在项目显式声明仓库：

```xml

<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## 🛠 本地开发

**Git hook 安装（clone 后执行一次）**：启用提交前版本对齐校验（smoke-test / system-test 的硬编码版本必须与根 pom `<revision>` 一致，防止版本脱节）：

```bash
git config core.hooksPath .githooks
```

- 手动 bump 主版本后运行 `bash scripts/align-versions.sh` 一键对齐（`release.yml` 发布链的 bump-revision 也会自动全仓库替换）。
- CI 侧由 `_quality-gate.yml` 的 `version-guard` job 兜底（hook 可被 `--no-verify` 绕过，bot 直推不经过本地 hook）。

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=muyi-tech/muyi-framework&type=Date)](https://star-history.com/#muyi-tech/muyi-framework&Date)
