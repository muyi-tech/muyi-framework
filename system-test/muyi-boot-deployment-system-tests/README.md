# Muyi System Tests

部署系统测试——把示例应用打成**可执行 jar**，以真实 JVM 进程（`java -jar`）启动，
通过 HTTP 验证运行时行为，对标 Spring Boot 的 system / deployment tests。

## 为什么需要这一层

mock 型测试（`@SpringBootTest`）测不到：真实进程、真实端口、真实序列化、真实异常处理链。
本模块覆盖的正是这最后一层——与构建冒烟（`smoke-tests.yml`：构建得出来、能被消费）之上的
**运行时行为正确性**验证。

## 测试对象与用例

| 被测应用（可执行 jar） | 技术栈 | 用例 |
|------------------------|--------|------|
| `muyi-boot-smoke-test-parent-inheritance` | WebMVC | 统一响应 `ApiResult` 包装 / `KeyValue` 数据字典序列化 / 业务异常统一转换 |
| `muyi-boot-smoke-test-webflux` | WebFlux | 同上（双栈行为对齐） |

**业务异常约定**：HTTP 200 + body 中业务码非 0（如 BAD_REQUEST=400）——错误码在 body，
不在 HTTP 状态层。该契约由真实进程测试固化。

## 本地运行

```bash
# 1. 框架安装到本地仓库
mvn clean install -DskipTests

# 2. 构建两个示例应用（产出可执行 jar 并安装到本地仓库）
mvn clean install -DskipTests --file smoke-test/muyi-boot-smoke-test-parent-inheritance/pom.xml
mvn clean install -DskipTests --file smoke-test/muyi-boot-smoke-test-webflux/pom.xml

# 3. 运行部署系统测试（jar 由 maven-dependency-plugin 复制到 target/system-test-jars/）
mvn verify --file system-test/muyi-boot-deployment-system-tests/pom.xml
```

测试日志输出到 `target/system-test-logs/`；可用 `-Dsystem-test.jars.dir=` 覆盖 jar 目录。

## CI

由独立 workflow [`system-tests.yml`](../../.github/workflows/system-tests.yml) 触发
（main push / 手动 / `workflow_call`）——起真实进程重且慢，**不进 PR 门**；
失败时上传 surefire 报告与应用进程日志。

## 模块约束

- 不进根聚合、不进发布链（`packaging=jar` 但仅本地使用）
- 被测示例版本通过 `examples.version` 属性引用，发布后由 release 流程的
  bump-revision 全仓库替换自动同步
