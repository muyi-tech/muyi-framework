# 贡献指南（CONTRIBUTING）

感谢您对 Muyi Framework 的关注！本文档说明开发环境、提交规范与新增模块的完整清单。

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21+（CI 使用 Eclipse Temurin 21） |
| Maven | 3.8.6+（CI 使用 3.9.x） |
| Git | 任意近期版本 |

## 快速开始

```bash
git clone https://github.com/muyi-tech/muyi-framework.git
cd muyi-framework

mvn clean verify        # 全量构建 + 测试 + Spotless 格式校验
mvn spotless:apply      # 格式不符合时自动修复
```

构建冒烟测试应用（验证框架构件能被业务应用消费，CI 冒烟同样执行）：

```bash
mvn clean install -DskipTests
mvn clean verify --file smoke-test/muyi-boot-smoke-test-parent-inheritance/pom.xml
```

## 提交规范（Conventional Commits）

提交信息格式：`<type>(<scope>): <subject>`

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `refactor` | 重构（不改变行为） |
| `test` | 测试相关 |
| `build` | 构建系统 / 依赖变更 |
| `ci` | CI 配置变更 |
| `chore` | 杂项维护 |

- `scope` 建议使用模块名：`feat(commons): 新增 PageResult 分页模型`
- 破坏性变更：标题加 `!`（如 `feat!:`），并在 footer 写明 `BREAKING CHANGE: <说明>`
- 关联 Issue：footer 写 `Closes #123`

## 分支与 PR 流程

1. Fork 本仓库，从 `main` 拉出特性分支
2. 提交变更（遵守上述提交规范）
3. 发起 Pull Request，模板见 `.github/PULL_REQUEST_TEMPLATE.md`
4. 等待 CI 检查通过（PR Build：license + 双 OS 构建测试 + Codecov；Smoke Tests：examples 冒烟）
5. PR 合并后由维护者按语义化版本节奏发布

## 新增模块指南（三件套公式）

新增一个能力模块时，按以下结构落位与命名（对标 Spring Boot 官方仓库的模块化策略）：

```
muyi-framework/
├── module/
│   ├── muyi-commons-<name>              通用组件实现（Spring 可选）
│   └── muyi-boot-<name>                 能力实现模块（自动装配等）
├── starter/
│   ├── muyi-boot-starter-<name>         门面 starter（纯 pom，零代码）
│   └── muyi-boot-starter-<name>-test    测试门面 starter（纯 pom，零代码）
```

### 必做清单

- [ ] **继承链**：所有 jar 模块必须继承 `muyi-boot-parent`，由其统一提供 sources/javadoc/GPG 插件 —— 这是 Maven Central 发布合规的硬约束，独立游离于继承链之外的 jar 模块无法完成发布
- [ ] **BOM 登记**：全部新构件（含 starter、test starter）登记进 `muyi-boot-dependencies`（BOM 是版本唯一权威源）
- [ ] **门面规则**：门面 starter 为纯 pom 零代码；实现依赖全部 `optional`，未匹配的技术栈（如无 Web 环境）运行期自动 back-off（条件装配降级），不阻断启动
- [ ] **依赖边界**：实现模块只依赖 commons 模块与第三方库；`muyi-commons-core` 保持零 Spring 依赖
- [ ] **代码规范**：新增文件包含 Apache-2.0 版权头与完整 Javadoc，格式经 `mvn spotless:apply`
- [ ] **文档**：模块 README 四段式（功能特性 / 使用方式 / 自动装配 / 扩展点），并更新根 README 的模块结构图
- [ ] **示例**：在 `smoke-test/` 下新增或更新可运行示例 —— CI 冒烟（Smoke Tests workflow）会自动构建全部冒烟应用，保证门面真实可消费

### 版本与发布

- 版本由根 pom `<revision>` 单一管理（flatten 展开），**禁止** `mvn versions:set`（会撕裂子模块继承链）
- bump `<revision>` 时需同步 `smoke-test/` 与 `system-test/` 内硬编码的版本引用
- 发布流程：push `v*.*.*` tag 自动触发 `release.yml` 发布到 Maven Central

## 行为准则

本项目采用 [Contributor Covenant 1.4 行为准则](CODE_OF_CONDUCT.md)，所有参与者
（issue / PR / 评审 / 社区渠道）均受其约束，违规处理亦按该文件执行。

## 许可证

向本仓库提交贡献，即表示您同意以 [Apache License 2.0](LICENSE) 许可您的贡献。
