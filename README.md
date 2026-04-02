# MatrixClock

[![Build APK on Tag](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml/badge.svg)](https://github.com/sha2kyou/MatrixClock/actions/workflows/build-apk-on-tag.yml)
[![Release](https://img.shields.io/github/v/release/sha2kyou/MatrixClock?label=release)](https://github.com/sha2kyou/MatrixClock/releases)
[![Last Commit](https://img.shields.io/github/last-commit/sha2kyou/MatrixClock?label=last%20commit)](https://github.com/sha2kyou/MatrixClock/commits/main)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat&logo=kotlin&logoColor=white)
![compileSdk](https://img.shields.io/badge/compileSdk-36-3DDC84?style=flat&logo=android&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26%2B-3DDC84?style=flat&logo=android&logoColor=white)

MatrixClock is an Android app for matrix-style clock and status display.

> [!CAUTION]
> **Disclaimer**
>
> **Vibe coding** side project, built for fun. **OLED / AMOLED** panels may **burn in** if a clock-style UI stays on for very long stretches—**use at your own risk**; you’re responsible for how you run it and for your device.

**Layout:** Android code in `app/`; the LAN admin panel (Vite + React) lives in **`admin/`** at the repository root. Built assets are synced into `app/src/main/assets/web/` before packaging (see **Web admin** below).

## Download

Get the APK from [**Releases**](https://github.com/sha2kyou/MatrixClock/releases/latest) (file name `MatrixClock-v*.apk`).

## Previews

Screenshots live under [`preview/`](preview/).

**Clock**

| | | |
|:---:|:---:|:---:|
| ![Clock 1](preview/clock1.png) | ![Clock 2](preview/clock2.png) | ![Clock 3](preview/clock3.png) |

**Pomodoro**

| | | |
|:---:|:---:|:---:|
| ![Pomodoro 1](preview/pomodoro1.png) | ![Pomodoro 2](preview/pomodoro2.png) | ![Pomodoro 3](preview/pomodoro3.png) |

**Web admin (LAN)**

![Web admin](preview/admin.png)

## Requirements

- **Android**: 8.0 or newer (API 26+).
- **LAN**: Phone and browser PC must be on the **same Wi‑Fi / LAN** (guest networks often isolate clients).
- **Firewall / router**: Unblock inbound **TCP port 6574** to the phone if your network blocks device-to-device access.
- **App running**: The HTTP server runs in the app process; keep the app running (foreground or in recents). It stops when the app is fully closed or the process is killed.

## Permissions & privacy

Declared permissions: `INTERNET`, `ACCESS_WIFI_STATE`, `VIBRATE`. No location, contacts, or microphone. No bundled third-party analytics SDK. Anyone on the same LAN who can reach the phone’s admin URL can attempt the in-app bind/auth flow.

## Main Features

- LAN admin UI over **HTTP on port 6574** (NanoHTTPD) while the app is running.
- Custom key bindings (short/long/double press).
- Clock, Countdown, Status, and Pomodoro modes.

## Default hardware keys

Uses **Volume +** and **Volume −** (same defaults on both keys). The app consumes these keys so the system volume may not change while the clock is open.

| Gesture | Default action |
|--------|----------------|
| **Short press** | **Pomodoro**: start the primary preset (or leave the pomodoro/text screen and return to the clock). |
| **Long press** (~500 ms) | **Menu**: open settings. |
| **Double press** (~350 ms between taps) | **Switch pomodoro preset**: next preset while a pomodoro countdown is showing (needs at least two presets; otherwise does nothing). |

Remap actions in the web admin **Keys** section after binding.

## Quick Start

1. Install, open the app, stay on the **same LAN** as your computer (see Requirements).
2. In a browser, open `http://<device-ip>:6574` (shown in-app when Wi‑Fi is up).
3. Bind once, then manage display/settings from the web admin.

## FAQ

- **Can’t open `http://<device-ip>:6574`?** Check same Wi‑Fi, correct IP, app is running, and that nothing blocks port 6574 (firewall / “AP isolation” on the router).
- **APK won’t install?** Use the APK from **Download** above; allow **Install unknown apps** for your browser or file manager if prompted.
- **Still fails?** Try another browser, confirm the phone’s IP didn’t change (DHCP), and temporarily disable VPN on the phone/PC.

## Versioning

Tags look like `v1.0.6`. CI maps them to `versionName` (without `v`) and `versionCode = major×10000 + minor×100 + patch`. Artifacts are published via [**Releases**](https://github.com/sha2kyou/MatrixClock/releases) (see **Download** for the APK name pattern).

## Build

```bash
./gradlew :app:assembleRelease
```

`assembleRelease` needs a release keystore (default path `app/release.keystore`, or configure `SIGNING_*` via environment variables or `-P` Gradle properties). For a quick dev install without signing setup, use `./gradlew :app:assembleDebug`.

## Web admin (Vite + React)

The web admin is a Vite + React app whose **source** lives in [`admin/`](admin/) at the repo root (not bundled raw in the APK: `scripts/sync-web-admin.sh` runs `npm install` / `npm run build` there and copies `dist/` into `app/src/main/assets/web/` for the Android app).

After UI changes:

```bash
./scripts/sync-web-admin.sh
```

Override the default path with `MATRIX_WEB_SRC` if the admin project is elsewhere.

For local frontend development, add a `.env` in `admin/` with `MATRIX_PROXY_TARGET=http://<device-ip>:6574`, then run `npm run dev` so `/api` proxies to the phone.

`MatrixServer` serves `GET /` (`index.html`) and `GET /assets/*` for the Vite-built JS/CSS.

## HTTP API (LAN, port 6574)

Full reference — routes, query/body parameters, and JSON models: **[`docs/HTTP-API.md`](docs/HTTP-API.md)**.

## License

This project is licensed under the [MIT License](LICENSE).

