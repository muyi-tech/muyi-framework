# 版本与发布

## 版本号策略

全仓库版本由根 POM 的 `<revision>` 属性**一处管理**，构建时经
`flatten-maven-plugin`（`resolveCiFriendliesOnly`）展开——发布出的 POM
自包含、可独立解析，下游无需感知 revision 机制。

- 版本号格式：`<major>.<minor>.<patch>`，开发中版本带 `-SNAPSHOT` 后缀
- SNAPSHOT 通过 [Maven Central Snapshots](https://central.sonatype.com/repository/maven-snapshots/) 获取（需显式配置仓库，见[接入指南](getting-started.md)）
- 正式版发布至 Maven Central

## 发布节奏与支持承诺

!!! info "0.x 早期阶段"

    API 仍可能调整，破坏性变更会在 Release 说明中标注 `BREAKING CHANGE`。

| 项 | 承诺 |
|----|------|
| minor 版本 | 约每季度一次 |
| patch 版本 | 按需发布 |
| 最新 minor 版本线 | ✅ 完整支持：Bug 修复 + 新功能 |
| 历史 minor 版本线 | ❌ 不回移植修复，请升级 |

升级建议：依赖版本统一经 BOM（`muyi-boot-dependencies`）锁定，升级时只需调整
BOM 版本号。完整支持策略见仓库 [SUPPORT.md](https://github.com/muyi-tech/muyi-framework/blob/main/SUPPORT.md)。

## 发布流水线

正式版发布由 tag `v*.*.*` 触发，采用**三段式流水线**（对标 Spring Boot 的
stage → verify → promote），每一段失败都会把问题挡在发布之前：

```text
① stage    全量测试 → GPG 签名构建进隔离 staging 仓库 → 断言全部构件
           件套完整（jar/sources/javadoc/pom + 签名）
② verify   从 staging 仓库解析全部构件，证明"发布物完整可消费"
           → 卡 maven-central environment 人工批准门
③ publish  批准后发布至 Maven Central（autoPublish）
           → 自动创建 GitHub Release（generate notes）
           → main 分支 <revision> 自动 bump 到下一个开发版
```

关键设计：

- **先验证后发布**：①② 阶段不触碰 Central，③ 阶段才真正上传
- **消费链路冒烟**：② 用隔离本地仓库仅从 staging 产物解析，证明下游可依赖
- **人工批准门**：③ 前需维护者在 GitHub Environments（`maven-central`）批准
- **发布后自动收尾**：GitHub Release notes 自动生成，下一版 revision 自动推进

## 合规保障

发布构件满足 Maven Central 全部硬性要求：

- ✅ sources jar 与 javadoc jar（真源码模块；无源码门面 jar 自动豁免，pom 构件豁免）
- ✅ GPG 签名（`.asc`）
- ✅ 完整 POM 元数据（name / description / url / license / developers / scm）
- ✅ `io.github.muyi-tech` 命名空间

新增 jar 模块必须挂在 `muyi-boot-parent` 继承链上以继承以上配置，约束已写入
[CONTRIBUTING.md](https://github.com/muyi-tech/muyi-framework/blob/main/CONTRIBUTING.md)。
