# G102 Controller

An unofficial Android app that controls the RGB lighting of **Logitech
G102 / G203 LIGHTSYNC** mice directly over USB — no PC, no G HUB, no root.

> **Unofficial:** this project is not affiliated with, endorsed by,
> sponsored by, or supported by Logitech. Product names are used only to
> identify hardware compatibility.

## What it does

Turn your mouse lighting off, on, or into any color or LIGHTSYNC effect —
straight from an Android phone or tablet with a USB OTG connection. The
app talks HID++ directly to the mouse over Android's USB Host API.

## Why this exists

Logitech's official lighting software (G HUB) is desktop-only. If your
computer is a phone or tablet — or you just want lighting control without
installing anything on your PC — this app does it with nothing but a cable
and an OTG adapter.

## Features

- **RGB OFF** — the physically-verified baseline (mode switch + solid
  black; this hardware family has no separate LED-disable command)
- **RGB ON** — restore lighting with your saved configuration
- **Custom solid colors** — presets or any hex value (`#B76E79`)
- **Brightness** — native protocol brightness for Cycle/Wave/Breathe/
  Blend; solid color uses RGB scaling (labeled honestly — the protocol has
  no solid-brightness byte)
- **LIGHTSYNC effects** — Solid, Cycle, Wave (left/right), Breathe, Blend
- **Zone colors** — three independently addressable LED zones
- **Responsive UI** — phone/tablet, portrait/landscape
- **Local-only** — zero permissions (not even INTERNET), no accounts, no
  telemetry; your settings stay on your device
- **USB diagnostics** — every descriptor and command byte, copyable for bug
  reports

Commands are sent only when **you** press a button. The app never sends USB
traffic on startup, rotation, reconnect, or while you're adjusting sliders.

## Tested hardware

| Status | Device | Android host |
|--------|--------|--------------|
| ✅ Physically verified | Logitech G102 LIGHTSYNC (`046D:C092`) | Samsung Galaxy Tab A9+, Android 16, USB OTG |
| 🧪 Expected, untested | Logitech G203 LIGHTSYNC (same `046D:C092` family) | — |

G203 LIGHTSYNC shares the G102's USB identity and LIGHTSYNC command set,
but only the G102 row is physically verified. See
[COMPATIBILITY.md](COMPATIBILITY.md) for the full matrix and how to add
your device.

## Installation

1. Download the APK from [Releases](https://github.com/Kaiser0733/G102-Android-Controller/releases)
2. Open it; Android will ask to allow installs from your browser/files app —
   allow it once
3. Connect your mouse via USB OTG adapter/cable
4. Open the app, tap **GRANT USB ACCESS** when Android asks
5. Use the controls

No root. No PC. No Termux. Android 7.0+ (API 24) with USB Host.

## Usage

1. **RGB OFF** — lighting turns off
2. **RGB ON** — lighting returns (your saved config)
3. Pick a color (preset or hex) → **APPLY COLOR**
4. Pick an effect (Cycle/Wave/Breathe/Blend/Zones) → adjust speed or
   direction → **APPLY**
5. Rotate freely — the layout adapts; no command is sent by rotation

Lighting is runtime control: the mouse reverts to its onboard configuration
after power-cycling. The app never writes the mouse's onboard memory.

## Safety

This app sends **runtime lighting commands only**. There is no firmware
flashing, no EEPROM/onboard-memory writes, no DFU/bootloader commands —
nothing irreversible exists in the codebase. Disconnect the mouse and it
returns to normal.

## Screenshots

Coming soon — real device screenshots will be added here.

## Demo video

Coming soon.

## Known limitations

- Lighting is runtime-only; the mouse reverts to onboard lighting after
  power-cycle
- Solid-color brightness is RGB scaling, not a native protocol brightness
  byte (labeled in-app)
- Landscape layouts exist for phone/tablet, but one real-device report
  (Tab A9+) observed the effect selector not appearing in landscape while
  portrait worked; unresolved — if you see it, file a bug report with your
  device and diagnostics

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — covers mouse detection,
USB permission, unsupported devices, cursor behavior, and how to file a
useful issue.

## Diagnostics

The in-app **USB DIAGNOSTICS** panel shows every USB descriptor, the
selected interface, permission state, and each command with exact hex
bytes. **COPY DIAGNOSTICS** puts it on the clipboard for bug reports
(it includes device names and command bytes — review before posting
publicly).

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # JVM tests (protocol vectors + logic)
./gradlew lintDebug               # Android lint
```

Requirements: JDK 17, Android SDK 35. GitHub Actions builds every push —
see [CONTRIBUTING.md](CONTRIBUTING.md).

## Documentation

- [PROTOCOL.md](PROTOCOL.md) — the HID++/LIGHTSYNC byte maps and provenance
- [ARCHITECTURE.md](ARCHITECTURE.md) — layer design and why
- [COMPATIBILITY.md](COMPATIBILITY.md) — verified/tested hardware
- [PRIVACY.md](PRIVACY.md) — what data (doesn't) leave your device
- [CHANGELOG.md](CHANGELOG.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Hardware reports welcome —
VID/PID + Android version + what worked.

## License

MIT — see [LICENSE](LICENSE). Third-party notices:
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); licensing rationale:
[LICENSING.md](LICENSING.md).
