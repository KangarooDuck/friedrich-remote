# Friedrich Remote

Android app that turns your phone into a remote control for **Friedrich Window Air Conditioners** (models CP06, CP08, and compatible).

Uses the LG 28-bit IR protocol at 38kHz — the same protocol as the OEM Friedrich/AKB73 remote.

## Features

- **Power ON/OFF** — turns the AC on and off
- **Mode control** — COOL, Money Saver, Fan Only, Dry
- **Temperature** — 60–86°F
- **Fan speed** — F1, F2, F3
- **Swing** — toggles auto-swing on/off

## Requirements

- **Android device with an IR blaster** (infrared emitter)
- Android 8.0 (API 26) or later

### Devices with IR blasters

| Brand | Models |
|-------|--------|
| **OnePlus** | 13, 13R, 12 |
| **Huawei** | Mate 10 Pro, P30 Pro, Mate 20 series |
| **Xiaomi** | Mi 9, Mi 10, Redmi Note 12 series |
| **Samsung** | Galaxy S4–S6, Note 3–4 (older models) |

> **Tested on:** OnePlus 13 (confirmed working)

## How It Works

The app generates LG 28-bit infrared codes and sends them via Android's `ConsumerIrManager` API. The IR protocol was reverse-engineered by capturing the OEM Friedrich remote with an IR receiver (see `tools/pibeam_capture.py`).

### Confirmed codes

| Command | Hex Code | Description |
|---------|----------|-------------|
| Power OFF | `0x88C0051` | Turns AC off |
| Power ON | `0x8820103` | COOL, 60°F, F1 fan |
| Swing | `0x8813004` | Auto-swing toggle |

## Build

Open the project in **Android Studio**, sync Gradle, and run on your device.

```
./gradlew assembleDebug
```

## Tests

30 unit tests covering the IR protocol engine and ViewModel state management:

```
./gradlew test
```

---
*Built with Kotlin + Jetpack Compose*
