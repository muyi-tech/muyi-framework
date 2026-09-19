#!/usr/bin/env bash
# align-versions.sh — 一键对齐：把 smoke-test/system-test 的版本字面量改写为
# 根 pom <revision> 的当前值（release.yml bump-revision 的本地等价物）。
#
# 背景：这两个目录独立于根聚合（外部消费者模拟），Maven 属性不跨 reactor，
#       版本只能是硬编码字面量；手动 bump 主版本时容易漏改，故提供本脚本。
# 语义：与 release.yml 的 bump-revision 一致——按「旧值全文替换」处理，
#       覆盖 <version>/<examples.version> 元素与注释中的版本文字。
# 用法：手动改完根 pom <revision> 后执行 bash scripts/align-versions.sh；
#       完成后自动运行 check-version-align.sh 护栏校验。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

REVISION="$(sed -n 's/.*<revision>\(.*\)<\/revision>.*/\1/p' "$ROOT/pom.xml" | head -1 | tr -d '[:space:]')"
if [ -z "$REVISION" ]; then
  echo "✗ align-versions: 无法从根 pom.xml 解析 <revision>" >&2
  exit 1
fi

# 与 bump-revision 相同的目标范围：smoke-test / system-test 下全部 pom.xml
files="$(grep -rlE --include='pom.xml' '[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT' \
           "$ROOT/smoke-test" "$ROOT/system-test" 2>/dev/null || true)"

if [ -z "$files" ]; then
  echo "无需对齐：未发现 SNAPSHOT 版本字面量"
  bash "$ROOT/scripts/check-version-align.sh"
  exit 0
fi

changed=0
while IFS= read -r f; do
  [ -n "$f" ] || continue
  # 发现该文件中所有与目标 revision 不同的旧 SNAPSHOT 值，逐一全文替换
  # （含注释里的版本文字——与 release.yml grep -F 全文替换语义一致）
  while IFS= read -r old; do
    [ -n "$old" ] || continue
    old_pat="$(printf '%s' "$old" | sed 's/\./\\./g')"
    sed -i "s/${old_pat}/${REVISION}/g" "$f"
    echo "对齐: ${f#"$ROOT"/} : ${old} -> ${REVISION}"
    changed=$((changed + 1))
  done < <(grep -oE '[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT' "$f" | sort -u | grep -vx "$REVISION" || true)
done <<< "$files"

if [ "$changed" -eq 0 ]; then
  echo "无需对齐：版本字面量已一致"
fi

bash "$ROOT/scripts/check-version-align.sh"
