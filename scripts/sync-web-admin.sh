#!/usr/bin/env bash
# 将 Vite 构建后的管理台同步到 app/src/main/assets/web/
# 用法：
#   export MATRIX_WEB_SRC=/path/to/remix_-hardware-management-system
#   ./scripts/sync-web-admin.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${MATRIX_WEB_SRC:-$HOME/Downloads/remix_-hardware-management-system}"
DEST="$ROOT/app/src/main/assets/web"

if [[ ! -f "$SRC/package.json" ]]; then
  echo "未找到前端项目: $SRC"
  echo "请设置 MATRIX_WEB_SRC 为 remix 项目根目录"
  exit 1
fi

echo ">>> npm install & build: $SRC"
(cd "$SRC" && npm install && npm run build)

echo ">>> 同步到 $DEST"
mkdir -p "$DEST"
rm -rf "$DEST/assets"
cp -f "$SRC/dist/index.html" "$DEST/index.html"
cp -R "$SRC/dist/assets" "$DEST/assets"

echo "完成。请重新编译 APK。"
