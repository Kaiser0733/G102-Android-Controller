# Contributing

Thanks for considering a contribution!

## Project ground rules

1. **The hardware-control code is production-critical.** `usb/`,
   `protocol/`, and `controller/` byte behavior is physically verified.
   Changes there require: protocol evidence, tests, and physical
   verification. Don't refactor for taste.
2. **The physically-verified RGB OFF sequence is regression-pinned** — the
   exact byte sequence is asserted in unit tests and fails CI if altered.
   This is intentional. See `ProtocolPacketsTest` /
   `regression_rgbOffSequence_isPhysicallyVerifiedV1`.
3. **Explicit USB commands only.** The app must never send lighting packets
   as a side effect of startup, resume, rotation, config rendering, or UI
   events. Only explicit button presses (RGB ON / RGB OFF / APPLY) may send.
   CI guards this (`scripts/check_hotfix.py`).
4. **Zero runtime dependencies, zero permissions** (see DECISIONS.md D6).
   Do not add libraries or manifest permissions without discussion.

## Getting set up

```bash
git clone https://github.com/Kaiser0733/G102-Android-Controller.git
cd G102-Android-Controller
```

Requirements: JDK 17, Android SDK 35. Or just push a branch — GitHub Actions
builds, tests, and lints on every push to `main`, `hotfix/**`, `ui/**`, and
`release/**` branches.

## Running tests

```bash
./gradlew testDebugUnitTest   # JVM unit tests (protocol vectors, logic)
./gradlew lintDebug           # Android lint
./gradlew assembleDebug       # debug APK
```

CI additionally runs `scripts/check_hotfix.py` (no-auto-USB wiring guard)
and `scripts/check_layouts.py` (layout ID completeness guard) before tests.

## Project structure

```
app/src/main/java/com/kaiser0733/g102controller/
├── MainActivity.kt        # single-Activity UI
├── ControllerApplication.kt
├── controller/            # command gate/session, actions, RgbCommandComposer
├── protocol/              # HID++ packets, LIGHTSYNC effects, color math (pure JVM)
├── usb/                   # Android USB host transport, diagnostics
├── settings/              # LightingConfig, SharedPreferences store
└── diagnostics/           # bounded ring buffer, crash log
```

The `protocol/` package has **no Android imports** — it's fully unit-testable
on the JVM. Keep it that way.

## Reporting hardware compatibility

Open a **Hardware Compatibility Report** issue (template provided). Include:

- exact mouse model
- VID/PID (shown in the app's USB diagnostics)
- Android device + version
- which features worked (RGB OFF / ON / color / brightness / effects / zones)

VID/PID + Android version is the minimum useful report.

## Code expectations

- Match existing style (small functions, honest results, comments explain
  *why*, DECISIONS.md records non-obvious choices)
- New protocol behavior needs test vectors from the reference implementation
  or captured device traffic — never invent bytes
- No secrets, no personal info in commits; CI scans history for them

## Pull requests

Use the PR template (`.github/pull_request_template.md`). State clearly
whether a change was hardware-tested, and on what device.
