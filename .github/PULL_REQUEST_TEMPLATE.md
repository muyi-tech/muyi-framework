## 变更说明

<!-- 简述本 PR 做了什么、为什么。关联 Issue 用 Closes/Fixes #123 -->

## 变更类型

- [ ] ✨ 新功能（feat）
- [ ] 🐛 Bug 修复（fix）
- [ ] 📝 文档（docs）
- [ ] ♻️ 重构（refactor，不改变行为）
- [ ] ✅ 测试（test）
- [ ] 🔧 构建/CI（build/ci）
- [ ] 🧹 杂项（chore/style/perf）

## 验证方式

<!-- 如何验证：单元测试、集成测试、运行示例等 -->

- [ ] 本地 `mvn clean verify` 全量通过（含 Spotless 格式校验，可用 `mvn spotless:apply` 修复）
- [ ] 新增/修复逻辑已补充单元测试

## 自检清单

- [ ] 提交信息符合 Conventional Commits（如 `feat(commons): ...`）
- [ ] 新增文件包含 Apache-2.0 版权头与完整 Javadoc
- [ ] 新增构件已在 BOM（`muyi-boot-dependencies`）登记
- [ ] 新增 jar 模块继承 `muyi-boot-parent`（发布合规要求）
- [ ] 相关 README / 文档已更新
