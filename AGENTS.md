# AGENTS.md

Single-module Android app (Kotlin + Jetpack Compose) that turns a phone with an IR blaster into a remote for Friedrich window ACs, using the LG 28-bit IR protocol at 38 kHz. Encoding was reverse-engineered from captures of the OEM remote (see `tools/pibeam_capture.py`).

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests (JVM, JUnit4): `./gradlew test`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.example.friedrichremote.ir.LgAcProtocolTest"`
- AGP requires an Android SDK; set `ANDROID_HOME` or create a gitignored `local.properties` with `sdk.dir`. There is no CI, lint config, or formatter config.
- Gradle daemon runs on JVM toolchain 21 (see `gradle/gradle-daemon-jvm.properties`, resolved via the foojay resolver — needs network on first run); app bytecode targets Java 17.

## Architecture

`MainActivity` -> `ui.RemoteScreen` (Compose) -> `ui.RemoteViewModel` (state) -> `ir.LgAcProtocol` (codec) -> `ir.IrTransmitter` (hardware via `ConsumerIrManager`). No DI; `RemoteViewModel` is constructed with `remember` in the screen and is a plain class (not AndroidX ViewModel).

## IR protocol (non-obvious core)

Everything lives in `ir/LgAcProtocol.kt`. Codes are 28 bits, sent MSB-first, signature `0x88`, LG timing (header 8500/4250 µs, bit mark 550, one-space 1600, zero-space 550) at 38 kHz.

Bit layout: `0x88 << 20 | 1 << 17 | change << 15 | mode << 12 | temp | fan << 4 | checksum`

- `mode` (bits 14-12): COOL=0, DRY=1, FAN_ONLY=2, MONEY_SAVER=6. Mode cycle is COOL → MONEY_SAVER → FAN_ONLY → DRY → COOL (there is no HEAT or AUTO on this unit).
- `fan` (bits 6-4): F1=0, F2=2, F3=4.
- `temp` (bits 11-7, **5 bits**): °F − 59, i.e. 60°F → 1 … 86°F → 27. The high bit (16s place) sits at bit 7 — sharing the fan nibble — while the low nibble is bits 11-8. Temperature steps 1°F per press.
- `change` (bit 15): 0 for the initial power-on command, 1 for setting changes while on. `buildPowerOnPattern` clears it; `buildPattern` sets it.
- `1 << 17` is the always-set "on" flag; power bits 19-18 are 0 when on. checksum (bits 3-0) = `sumNibbles(code >> 4, 4) & 0xF` (sum of nibbles 1-4).

- `OFF_COMMAND = 0x88C0051` (power off) and `SWING_TOGGLE = 0x8813004` (auto-swing; both on/off presses send the same code) are special codes, not built from `AcState`. The UI powers on via `buildPowerOnPattern`; mode/fan/temp presses use `buildPattern`; swing sends `codeToTimingPattern(SWING_TOGGLE)`.
- Each code is transmitted **once** (single data frame, no repeat frame) — the OEM remote does not send a repeat frame on a single press, and sending more than one makes the AC beep multiple times.
- Known-good regression codes in `LgAcProtocolTest` (e.g. `0x882810B`) come from real captures of the OEM remote and should stay stable.
- UI stores temperature in °F (`AcState.tempFahrenheit`, 60–86); there is no °C anywhere anymore.

## Gotchas

- Temperature is **5 bits spanning bits 7-11** (`°F - 59`), not a clean nibble: bit 7 is the temp high bit while bits 6-4 are fan. Do not "fix" it into a single nibble — that breaks the encoding.
- `LgAcProtocol` no longer has mutable `variant`/`onPowerCode` globals; power-on and LG timing are fixed, and the status-bar toggles for those were removed.
- There are no instrumentation (`androidTest`) sources yet, only the JVM tests in `app/src/test`.
