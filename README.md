# MatrixClock

[![Build APK on Tag](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml/badge.svg)](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml)
[![Release](https://img.shields.io/github/v/release/sha2kyou/MatrixClock?label=release)](https://github.com/sha2kyou/MatrixClock/releases)
[![Last Commit](https://img.shields.io/github/last-commit/sha2kyou/MatrixClock?label=last%20commit)](https://github.com/sha2kyou/MatrixClock/commits/main)

MatrixClock is an Android app for matrix-style clock and status display.

## Main Features

- The app starts a background web service when launched.
- You can open the admin panel in LAN: `http://<device-ip>:6574`.
- You can control actions with custom key bindings (short/long/double press).
- Supports Clock, Countdown, Status, and Pomodoro modes.

## Quick Start

1. Install and open the app on an Android device.
2. Keep your phone/computer in the same LAN.
3. Open `http://<device-ip>:6574` in a browser.
4. Bind once, then manage display/settings from web admin.

## Build

```bash
./gradlew :app:assembleRelease
```

## Web 管理台（Vite + React）

管理页源码可放在任意目录（例如下载的 `remix_-hardware-management-system`）。修改 UI 后构建并同步到 `assets/web`：

```bash
export MATRIX_WEB_SRC=/path/to/remix_-hardware-management-system
./scripts/sync-web-admin.sh
```

本地开发时在该前端项目下创建 `.env`，设置 `MATRIX_PROXY_TARGET=http://<手机IP>:6574`，再 `npm run dev`，即可把 `/api` 代理到真机。

应用内 `MatrixServer` 会提供 `GET /`（`index.html`）以及 `GET /assets/*`（Vite 打包的 JS/CSS）。

