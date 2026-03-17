# MatrixClock

[![Build](https://img.shields.io/badge/build-on%20tag-blue)](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml)
[![Release](https://img.shields.io/badge/release-v1.0.0-blueviolet)](https://github.com/sha2kyou/MatrixClock/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](https://github.com/sha2kyou/MatrixClock/blob/main/LICENSE)
[![Last Commit](https://img.shields.io/badge/last%20commit-main-informational)](https://github.com/sha2kyou/MatrixClock/commits/main)

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

