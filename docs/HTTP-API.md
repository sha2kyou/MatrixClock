# LAN HTTP API reference

HTTP API exposed by **`MatrixServer`** (NanoHTTPD) on **port 6574** while the MatrixClock app is running—the same port as the bundled web admin.

---

## Contents

- [Overview](#overview)
- [Authentication & request format](#authentication--request-format)
- [Endpoints](#endpoints)
  - [Static content](#static-content)
  - [Auth](#auth)
  - [Display & clock](#display--clock)
  - [Pomodoro](#pomodoro)
  - [Device & keys](#device--keys)
  - [Operation log](#operation-log)
- [Data types](#data-types)
- [HTTP status codes](#http-status-codes)
- [Source code](#source-code)

---

## Overview

| Item | Value |
|------|--------|
| **Base URL** | `http://<device-ip>:6574` |
| **Typical use** | Same Wi‑Fi / LAN as the phone; see project [README](../README.md) for firewall and lifecycle notes. |

**Quick route map**

| Group | Methods | Paths |
|-------|---------|--------|
| Static | `GET` | `/`, `/assets/*` |
| Auth | `POST`, `GET` | `/api/auth/bind`, `/api/auth/reset`, `/api/auth/list`, `/api/auth/device-info`, `/api/auth/revoke` |
| Display & clock | `POST`, `GET` | `/api/mode`, `/api/clock/template`, `/api/display` |
| Pomodoro | `GET`, `POST` | `/api/pomodoro/configs`, `/api/pomodoro/sessions` |
| Device & keys | `GET`, `POST` | `/api/device/info`, `/api/keys/settings` |
| Log | `GET` | `/api/oplog` |

---

## Authentication & request format

### Session token (`token`)

After **`POST /api/auth/bind`** succeeds, the response body is a secret **token**. Send it as a **query parameter** named `token` on every protected request. Invalid or missing tokens usually receive **`403 Forbidden`** with body `Forbidden`.

### Where parameters go

The bundled admin UI sends **`token` and most other fields in the URL query string**, including for `POST`. Other clients may use `application/x-www-form-urlencoded` if NanoHTTPD maps those fields into the same `parameters` map.

### JSON bodies

Use **`Content-Type: application/json`** when required. Only these routes read a JSON body:

- **`POST /api/auth/device-info`**
- **`POST /api/pomodoro/configs`**

---

## Endpoints

### Static content

#### `GET /`

Serves the web admin **`index.html`**.

| Query | Description |
|-------|-------------|
| — | No parameters. |

| Outcome | Description |
|---------|-------------|
| **`200`** | HTML |
| **`403`** | Plain text if the web UI is disabled in app settings: `Web interface is disabled in settings.` |

#### `GET /assets/*`

Serves hashed Vite build assets (e.g. `/assets/index-….js`).

| Query | Description |
|-------|-------------|
| — | No parameters. |

| Outcome | Description |
|---------|-------------|
| **`200`** | Asset bytes |
| **`404`** | Path not present under `assets/` in the shipped bundle |

---

### Auth

#### `POST /api/auth/bind`

Starts on-device approval; the request may **block** until the user acts.

| Query / body | Description |
|----------------|-------------|
| — | No query parameters or body. |

| Response | Meaning |
|----------|---------|
| **`200`** + plain text token | Approved; use as **`token`** on later calls. |
| **`200`** + `DENIED` | Rejected or timed out. |
| **`200`** + empty body | Treat as failure (no token). |

#### `POST /api/auth/reset`

Removes the session identified by the given token from the authorized list.

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | Session to remove (normally the caller’s own). |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Token was valid and removed. |
| **`403`** | Token missing or unknown. |

#### `GET /api/auth/list`

Lists authorized clients (for an already-authorized caller).

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | Must be a valid session. |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON array of **`AuthRecord`** — see [Data types](#data-types). |

#### `POST /api/auth/device-info`

Updates metadata stored for the caller’s **`AuthRecord`**.

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | Session updating its own row. |

**JSON body**

| Field | Required | Description |
|-------|----------|-------------|
| **`deviceName`** | No | Friendly label (e.g. browser name). |
| **`deviceModel`** | No | Client-reported model string. |
| **`systemVersion`** | No | Client-reported OS / browser version. |
| **`batteryLevel`** | No | **0–100**, or omit / `null` if unknown. |

Server updates the **`AuthRecord`** for this **`token`**: each field uses the JSON value when non-**`null`**; **`null`** / omitted keys keep the previous stored value on the device. An **empty string** still overwrites with empty.

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |
| **`400`** | Missing body, invalid JSON, or decode error (message in body) |

#### `POST /api/auth/revoke`

Revokes **another** device’s session.

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | Authorized caller. |
| **`revokeToken`** | Yes | **`AuthRecord.token`** to remove. |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |
| **`403`** | Caller invalid or **`revokeToken`** blank |

---

### Display & clock

#### `POST /api/mode`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |
| **`mode`** | No | `CLOCK` = clock; `TEXT` = text / countdown / status. Omitted → **`CLOCK`**. |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |
| **`400`** | **`mode`** present but not a valid enum (only **`CLOCK`**, **`TEXT`**). |

#### `GET /api/clock/template`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON `{"template": <int>}` — stored **1–3**. |

#### `POST /api/clock/template`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |
| **`template`** | No | Integer; **`toIntOrNull()`** then clamped **1–3**; missing / invalid → **1**. |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |

#### `POST /api/display`

Switches to **`TEXT`** mode and updates the main line.

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |
| **`text`** | No | Main message; default empty. App **uppercases** for display. |
| **`color`** | No | Android **`Color.parseColor`** (e.g. `#RRGGBB`). Invalid → red. |
| **`duration`** | No | Countdown **seconds**. Omitted, non-integer, or **≤ 0** → **persistent** status: app sets **`remainingSeconds = -1`** (no tick; stays until mode change or pomodoro). **> 0** → countdown; **`icon`** cleared. |
| **`style`** | No | **1–3** (clamped). **1** ≈ two-line; **2** ≈ full-width bar; **3** ≈ text + side matrix. Omitted / invalid → UI default **1**. |
| **`icon`** | No | Only for **persistent** mode. Case-insensitive: **`star`**, **`moon`**, **`phone`**, **`alarm`**, **`clock`**, **`stopwatch`**; empty / unknown → none. |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |

---

### Pomodoro

#### `GET /api/pomodoro/configs`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON array of **`PomodoroConfig`**. |

#### `POST /api/pomodoro/configs`

Replaces the **entire** preset list on the device.

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

**Body**

| Content | Required | Description |
|---------|----------|-------------|
| JSON array of **`PomodoroConfig`** | Yes | Full replacement list. |

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |
| **`400`** + `Missing body` | No body |
| **`500`** | Plain text if JSON decode fails |

#### `GET /api/pomodoro/sessions`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON array of **`PomodoroSession`**. |

---

### Device & keys

#### `GET /api/device/info`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON **`DeviceInfo`** for the **phone** (not the browser). |
| **`500`** | Building the snapshot failed |

#### `GET /api/keys/settings`

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |

| Outcome | Description |
|---------|-------------|
| **`200`** | JSON with **`volUpShort`**, **`volUpLong`**, **`volUpDouble`**, **`volDownShort`**, **`volDownLong`**, **`volDownDouble`**. |

#### `POST /api/keys/settings`

All six fields are **query** parameters. Values outside the allowed set are **replaced** with defaults.

| Field | Allowed | Default |
|-------|---------|---------|
| **`volUpShort`**, **`volDownShort`** | `menu`, `pomodoro` | `pomodoro` |
| **`volUpLong`**, **`volDownLong`** | `menu`, `pomodoro` | `menu` |
| **`volUpDouble`**, **`volDownDouble`** | `menu`, `pomodoro`, `none`, `switch_pomodoro` | `switch_pomodoro` |

**Semantics:** **`menu`** → settings; **`pomodoro`** → start primary preset or exit text to clock; **`switch_pomodoro`** → next preset during pomodoro; **`none`** → double-press does nothing.

| Outcome | Description |
|---------|-------------|
| **`200`** + `OK` | Success |

---

### Operation log

#### `GET /api/oplog`

Paged operation log (newest-first slice per request).

| Query | Required | Description |
|-------|----------|-------------|
| **`token`** | Yes | — |
| **`page`** | No | Zero-based page; negatives → **0**; default **0**. |
| **`pageSize`** | No | **10–100**; default **50**. |

**`200`** JSON object (internal `OpLogPageResponse`; field names stable):

| Field | Description |
|-------|-------------|
| **`items`** | **`OpLogEntry`** array. Log is **newest-first**; **`page` = 0** is the latest slice. |
| **`hasMore`** | `true` if more rows exist (server reads **`pageSize + 1`** to detect). |

---

## Data types

Field names match Kotlin **`kotlinx.serialization`** (camelCase).

### `AuthRecord`

| Field | Type | Description |
|-------|------|-------------|
| **`token`** | string | Client secret. |
| **`ip`** | string | Observed IP (may be empty on migrated data). |
| **`deviceName`** | string | Label from admin or client. |
| **`deviceModel`** | string | Client-reported model. |
| **`systemVersion`** | string | Client-reported OS / browser version. |
| **`batteryLevel`** | int | **0–100** or **-1** if unknown. |
| **`createdAt`** | long | Epoch millis when created. |

### `DeviceInfo` (phone)

| Field | Type | Description |
|-------|------|-------------|
| **`model`** | string | `Build.MODEL` |
| **`manufacturer`** | string | `Build.MANUFACTURER` |
| **`device`** | string | `Build.DEVICE` |
| **`androidVersion`** | string | `Build.VERSION.RELEASE` |
| **`sdkInt`** | int | `Build.VERSION.SDK_INT` |
| **`batteryLevel`** | int | **0–100** or **-1** |
| **`batteryStatus`** | string | `charging`, `full`, `discharging`, `not_charging`, `unknown` |
| **`screenWidthPx`**, **`screenHeightPx`** | int | Display size in px |
| **`screenDensity`** | float | `DisplayMetrics.density` |
| **`isCharging`** | bool | Charging or full |

### `DeviceInfoUpdate` (→ `POST /api/auth/device-info`)

| Field | Type | Description |
|-------|------|-------------|
| **`deviceName`** | string? | Optional; `null` / omitted may keep previous value. |
| **`deviceModel`** | string? | Same |
| **`systemVersion`** | string? | Same |
| **`batteryLevel`** | int? | Optional **0–100** |

### `PomodoroConfig`

| Field | Type | Description |
|-------|------|-------------|
| **`id`** | string | Stable preset id. |
| **`text`** | string | Label during countdown. |
| **`durationSec`** | int | Focus length in seconds. |
| **`colorHex`** | string | `#RRGGBB`-style color, or the literal **`random`** (case-insensitive) for a random preset color in the app. |
| **`isPrimary`** | bool | Preset for hardware **`pomodoro`** short-press when multiple exist. |
| **`countdownStyle`** | int | **1** two-line; **2** full-width bar; **3** text + side matrix (UI clamps **1–3**). |
| **`cycleTotal`** | int | Daily cycle target for progress / star. |

### `PomodoroSession`

| Field | Type | Description |
|-------|------|-------------|
| **`configId`** | string | Preset id for this run. |
| **`configText`** | string | Label snapshot at start. |
| **`startTimeMillis`** | long | Start time (epoch ms). |
| **`plannedDurationSec`** | int | Planned seconds. |
| **`completed`** | bool | Finished without cancel. |
| **`actualDurationSec`** | int | Recorded seconds. |

### `OpLogEntry`

| Field | Type | Description |
|-------|------|-------------|
| **`timeMillis`** | long | When the action occurred. |
| **`action`** | string | Short code (`display_text`, `mode`, `auth_bind`, …). |
| **`detail`** | string | Detail (may be truncated). |
| **`ip`** | string | Client IP if known. |

---

## HTTP status codes

| Code | Typical use in this API |
|------|-------------------------|
| **`200`** | Success (body may be plain text, JSON, HTML, or binary). |
| **`400`** | Bad request (bad JSON, missing body, invalid **`mode`**, …). |
| **`403`** | Missing / invalid **`token`**, or web UI disabled for **`GET /`**. |
| **`404`** | Unknown path or missing static asset. |
| **`500`** | Server / internal error (`INTERNAL_ERROR`): e.g. `GET /api/device/info` throws, or `POST /api/pomodoro/configs` JSON decode throws (body often the exception message). |

---

## Source code

| Part | Location |
|------|----------|
| Server routing | [`MatrixServer.kt`](../app/src/main/java/cn/tr1ck/matrixclock/data/api/MatrixServer.kt) |
| Admin client helpers | [`api.ts`](../admin/src/matrix/api.ts) |
