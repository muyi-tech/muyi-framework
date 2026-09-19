# 支持指南（SUPPORT）

在使用 Muyi Framework 时遇到问题？请按以下方式寻求帮助。

## 问题渠道

| 问题类型 | 渠道 |
|----------|------|
| 🐛 Bug 报告 | [GitHub Issues](https://github.com/muyi-tech/muyi-framework/issues) + **Bug Report** 模板 |
| ✨ 功能建议 | [GitHub Issues](https://github.com/muyi-tech/muyi-framework/issues) + **Feature Request** 模板 |
| ❓ 使用问题 | [GitHub Issues](https://github.com/muyi-tech/muyi-framework/issues)，标题注明「使用问题」 |
| 🔐 安全漏洞 | **不要公开提 Issue**，见 [SECURITY.md](.github/SECURITY.md)（私有披露渠道） |

提问前请先搜索 [已有 Issues](https://github.com/muyi-tech/muyi-framework/issues?q=)，避免重复；报告 Bug 时请附带最小复现（代码 / 步骤 / 日志），并确认使用的是最新版本。

## 版本支持策略

Muyi Framework 处于 0.x 早期阶段，API 仍可能调整。支持承诺如下：

| 版本线 | 状态 |
|--------|------|
| 最新 minor（`main` 上的 `<revision>`） | ✅ 完整支持：Bug 修复 + 新功能 |
| 历史 minor 版本线 | ❌ 不回移植修复，请升级 |

- 发布节奏：minor 版本约每季度一次，patch 版本按需（早期阶段）
- 升级建议：依赖版本统一经 BOM（`muyi-boot-dependencies`）锁定，升级时只需调整 BOM 版本；破坏性变更会在 Release 说明中标注 `BREAKING CHANGE`

## 响应预期

本项目由社区维护，Issue 按尽力而为（best-effort）响应。提交前请确保本地 `mvn clean verify` 通过——这能极大缩短问题定位时间。
