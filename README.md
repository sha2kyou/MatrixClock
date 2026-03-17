# MatrixClock

[![Build APK on Tag](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml/badge.svg)](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml)
[![Release](https://img.shields.io/github/v/release/sha2kyou/MatrixClock?label=release)](https://github.com/sha2kyou/MatrixClock/releases)
[![Downloads](https://img.shields.io/github/downloads/sha2kyou/MatrixClock/total?label=downloads)](https://github.com/sha2kyou/MatrixClock/releases)
[![License: MIT](https://img.shields.io/github/license/sha2kyou/MatrixClock?label=license)](https://github.com/sha2kyou/MatrixClock/blob/main/LICENSE)
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

