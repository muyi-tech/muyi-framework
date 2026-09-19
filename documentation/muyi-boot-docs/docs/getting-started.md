# 接入指南

Muyi Framework 提供两种接入路径，**业务代码完全一致**，差异仅在 `pom.xml`。
两种方式的完整可运行示例见仓库 [smoke-test](https://github.com/muyi-tech/muyi-framework/tree/main/smoke-test)。

## 方式一：继承 Parent（推荐业务应用）

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

一站式获得：

- **依赖版本管理**：import 自 `muyi-boot-dependencies`（BOM），业务应用无需再写任何版本号
- **构建规范**：Java 21 编译、测试三件套（JUnit 5 / Mockito / AssertJ）、Spotless 格式校验、Enforcer 环境约束、JaCoCo 覆盖率
- **可执行 jar**：声明 `spring-boot-maven-plugin` 即零配置 repackage

## 方式二：仅导入 BOM（已有父级时）

适合已有父级（如公司内部 parent）无法更换继承关系的应用：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.muyi-tech.boot</groupId>
            <artifactId>muyi-boot-dependencies</artifactId>
            <version>0.1.0-SNAPSHOT</version>
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

## 门面 Starter 选型

日常接入**只需选择一个门面**，它会同时带入框架增强与 Spring 官方技术栈：

| 门面 | 引入内容 | 适用场景 |
|------|----------|----------|
| `muyi-boot-starter-webmvc` | 核心 starter + WebMVC 异常处理 + `spring-boot-starter-webmvc` + validation | Servlet Web 应用 |
| `muyi-boot-starter-webflux` | 核心 starter + WebFlux 异常处理 + `spring-boot-starter-webflux` + validation | 响应式 Web 应用 |
| `muyi-boot-starter` | 核心 starter（共享异常处理支撑） | 非 Web 应用（定时任务、MQ 消费者等） |

非 Web 应用也可直接引入 `muyi-boot-starter`，复用 `ExceptionHandlerSupport` 与
`muyi-commons-spring` 组件，不引入任何 Web 栈依赖。

测试时可搭配测试门面：

| 测试门面 | 引入内容 |
|----------|----------|
| `muyi-boot-starter-webmvc-test` | `spring-boot-starter-test` + `spring-boot-starter-webmvc-test` |
| `muyi-boot-starter-webflux-test` | 同上（Reactive 版）+ `reactor-test` |

## 完整示例

| 示例 | 接入方式 | 演示能力 |
|------|---------|---------|
| `muyi-boot-smoke-test-parent-inheritance` | 继承 Parent | 统一响应 / 数据字典 / 业务异常 / 模块扩展点（双通道） |
| `muyi-boot-smoke-test-bom-import` | 仅导入 BOM | 同上，验证两种方式零侵入 |
| `muyi-boot-smoke-test-webflux` | 继承 Parent（Reactive 栈） | 同上，验证 `ReactiveModuleExceptionHandler` 对称能力 |

```bash
git clone https://github.com/muyi-tech/muyi-framework.git
cd muyi-framework/smoke-test/muyi-boot-smoke-test-parent-inheritance
mvn spring-boot:run
curl http://localhost:8080/api/demo/hello
# {"code":"0","msg":"","data":"Hello, Muyi Framework!"}
```

## 获取构件

| 版本 | 仓库 | 使用方式 |
|------|------|---------|
| `x.y.z-SNAPSHOT`（开发中） | [Maven Central Snapshots](https://central.sonatype.com/repository/maven-snapshots/) | 需显式配置 snapshot 仓库 |
| `x.y.z`（正式版） | [Maven Central](https://central.sonatype.com/namespace/io.github.muyi-tech) | 发布后零配置，直接使用 |

SNAPSHOT 需在项目显式声明仓库：

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

## 下一步

- 异常与错误码的正确用法：[异常处理指南](exception-handling.md)
- 版本升级与发布节奏：[版本与发布](versioning.md)
