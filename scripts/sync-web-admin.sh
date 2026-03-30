#!/usr/bin/env bash
# Build the Vite admin app and sync output into app/src/main/assets/web/
#
# Usage:
#   ./scripts/sync-web-admin.sh
# Optional: MATRIX_WEB_SRC=/other/path ./scripts/sync-web-admin.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${MATRIX_WEB_SRC:-$ROOT/admin}"
DEST="$ROOT/app/src/main/assets/web"

if [[ ! -f "$SRC/package.json" ]]; then
  echo "Admin UI not found: $SRC"
  echo "Set MATRIX_WEB_SRC to the admin project root, or add admin/ under the repo."
  exit 1
fi

echo ">>> npm install & build: $SRC"
if [[ -f "$SRC/package-lock.json" ]]; then
  (cd "$SRC" && npm ci && npm run build)
else
  (cd "$SRC" && npm install && npm run build)
fi

if [[ ! -f "$SRC/dist/index.html" ]] || [[ ! -d "$SRC/dist/assets" ]]; then
  echo "Build output missing: expected $SRC/dist/index.html and $SRC/dist/assets/"
  exit 1
fi

echo ">>> sync -> $DEST"
mkdir -p "$DEST"
rm -rf "$DEST/assets"
cp -f "$SRC/dist/index.html" "$DEST/index.html"
cp -R "$SRC/dist/assets" "$DEST/assets"

echo "Done. Rebuild the APK to pick up assets."
