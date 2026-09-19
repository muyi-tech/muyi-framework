#!/usr/bin/env bash
# check-version-align.sh — 防脱节护栏（版本对齐校验）
#
# 背景：smoke-test/ 与 system-test/ 刻意独立于根聚合构建（外部消费者模拟：
#       不进默认构建、不进发布链），其 pom 中 parent version / BOM import
#       version / 自身 version / <examples.version> 必须是可解析的硬编码
#       字面量——Maven 属性不跨 reactor，无法用 ${revision} 表达。
#       因此对齐只能靠「字面量 + 校验」：本脚本即校验端。
#
# 规则：上述 pom 中所有以 -SNAPSHOT 结尾的 <version> 字面量与
#       <examples.version> 必须等于根 pom 的 <revision>；
#       release 版本字面量（如 spring-boot-starter-parent 4.1.1）无
#       -SNAPSHOT 后缀，天然不参与校验、不会误伤。
#
# 使用方：.githooks/pre-commit（本地提交即时拦截）与
#         _quality-gate.yml 的 version-guard job（CI 兜底）。
#         两处跑同一脚本——单一事实来源。
#         本地可被 git commit --no-verify 绕过，故 CI 必留。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

REVISION="$(sed -n 's/.*<revision>\(.*\)<\/revision>.*/\1/p' "$ROOT/pom.xml" | head -1 | tr -d '[:space:]')"
if [ -z "$REVISION" ]; then
  echo "✗ check-version-align: 无法从根 pom.xml 解析 <revision>" >&2
  exit 1
fi

errors=()
while IFS=: read -r file lineno content; do
  [ -n "$file" ] || continue
  value="$(printf '%s\n' "$content" | sed -E 's/^.*<(version|examples\.version)>([^<]*)<\/(version|examples\.version)>.*/\2/')"
  if [ "$value" != "$REVISION" ]; then
    errors+=("${file#"$ROOT"/}:${lineno}: ${value}（期望 ${REVISION}）")
  fi
done < <(grep -rnE --include='pom.xml' '<version>[^<]*-SNAPSHOT</version>|<examples\.version>[^<]+</examples\.version>' \
           "$ROOT/smoke-test" "$ROOT/system-test" 2>/dev/null || true)

if [ "${#errors[@]}" -gt 0 ]; then
  {
    echo "✗ 版本脱节：smoke-test / system-test 存在与根 pom <revision>${REVISION}</revision> 不一致的硬编码版本："
    printf '  - %s\n' "${errors[@]}"
    echo "  修复：bash scripts/align-versions.sh 一键对齐后重新提交"
  } >&2
  exit 1
fi

echo "✓ 版本对齐：smoke-test / system-test 与 <revision>${REVISION}</revision> 一致"
