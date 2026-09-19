# Muyi Boot Smoke Tests

冒烟测试应用（对标 Spring Boot 的 `spring-boot-smoke-tests`）：以真实业务应用视角消费
Maven Central 发布的框架构件，验证门面 starter 端到端可用。**不参与根聚合**，
由独立 workflow（[smoke-tests.yml](../.github/workflows/smoke-tests.yml)）在 CI 构建。

| 冒烟应用 | 接入方式 | 适用场景 |
|------|---------|---------|
| [muyi-boot-smoke-test-parent-inheritance](muyi-boot-smoke-test-parent-inheritance) | 继承 `muyi-boot-parent` | **推荐**。新业务应用，无历史父级约束，一站式获得依赖管理 + 构建规范 + 统一测试依赖 |
| [muyi-boot-smoke-test-bom-import](muyi-boot-smoke-test-bom-import) | 仅 `import` BOM | 已有父级（如 `spring-boot-starter-parent` 或公司内部 parent），无法更换继承关系 |
| [muyi-boot-smoke-test-webflux](muyi-boot-smoke-test-webflux) | 继承 `muyi-boot-parent` | 响应式（WebFlux）栈接入 |

## 演示能力

- **统一响应**：`ApiResult<T>` 包装接口返回（`code` / `msg` / `data`）
- **数据字典**：`KeyValue<K, V>` + `SexEnum` 输出编码/描述列表
- **业务异常**：`ServiceExceptionUtil.exception(...)` 抛出，由全局异常处理器统一转换为错误响应
- **模块扩展**：`DemoModuleExceptionHandler` 演示 `ModuleExceptionHandler` 扩展点（模块级异常优先处理，返回 `null` 交回核心兜底）

## 前置条件

- JDK 21+
- Maven 3.8.6+
- 框架构件已安装到本地仓库：

```bash
# 在 muyi-framework 根目录执行
mvn clean install -DskipTests
```

> 冒烟应用 pom 已配置 Central 快照仓库，若构件已发布到 Maven Central Snapshots 亦可在线解析；正式版发布后移除 `<repositories>` 段即可零配置使用。

## 构建与运行

```bash
# 方式一：继承 Parent
cd muyi-boot-smoke-test-parent-inheritance
mvn spring-boot:run

# 方式二：仅导入 BOM
cd muyi-boot-smoke-test-bom-import
mvn spring-boot:run

# 响应式：WebFlux
cd muyi-boot-smoke-test-webflux
mvn spring-boot:run
```

## 验证接口

```bash
curl http://localhost:8080/api/demo/hello
# {"code":"0","msg":"","data":"Hello, Muyi Framework!"}

curl http://localhost:8080/api/demo/sex
# {"code":"0","msg":"","data":[{"key":1,"value":"男"},{"key":2,"value":"女"}]}

curl http://localhost:8080/api/demo/error
# {"code":"400","msg":"请求参数不正确","data":null}

curl http://localhost:8080/api/demo/i18n-error
# {"code":"1001001000","msg":"用户 muyi 不存在","data":null}      # 默认中文
# Accept-Language: en-US 时 → {"code":"1001001000","msg":"User muyi does not exist",...}

curl http://localhost:8080/api/demo/module-error
# {"code":"1001001001","msg":"模块级异常处理器演示：业务专有异常已被自定义拦截","data":null}
```

## 相关模块

- [`muyi-boot-deployment-system-tests`](../system-test/muyi-boot-deployment-system-tests/README.md) —
  把本目录应用打成可执行 jar，以真实 JVM 进程启动并断言 HTTP 行为（最后一层部署验证）
