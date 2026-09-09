# G102 Controller

Unofficial Android app that controls the RGB lighting of Logitech G102/G203
LIGHTSYNC mice directly over USB — no PC, no G HUB, no root.

> Not affiliated with, endorsed by, sponsored by, or supported by Logitech.

## What it does

- RGB on/off
- Custom solid colors (presets or any hex value)
- Brightness (native for cycle-class effects; RGB scaling for solid)
- Effects: Solid, Cycle, Wave (direction), Breathe, Blend, Zones
- Three-zone colors
- Phone and tablet layouts, portrait and landscape
- USB diagnostics with copy/share
- Fully local: zero permissions, no network, no analytics

Lighting commands are sent only when you press a button — never on
startup, rotation, or reconnect.

## Compatibility

| Status | Device | Notes |
|--------|--------|-------|
| ✅ Tested | Logitech G102 LIGHTSYNC (`046D:C092`) | Samsung Galaxy Tab A9+, Android 16, USB OTG |
| 🧪 Expected | Logitech G203 LIGHTSYNC | Same USB identity and command set; untested |

See [COMPATIBILITY.md](COMPATIBILITY.md) for the full matrix and how to
report your hardware.

## Installation

1. Download the APK from [Releases](https://github.com/Kaiser0733/G102-Android-Controller/releases)
2. Allow installs from your browser when Android asks
3. Connect the mouse via a USB OTG adapter
4. Open the app and tap **GRANT USB ACCESS**

Requires Android 7.0+ with USB Host. No root, no PC.

## Usage

1. **RGB OFF** turns the lighting off; **RGB ON** restores your saved setup
2. Pick a color and **APPLY COLOR**
3. Pick an effect, adjust speed or direction, **APPLY**

Lighting is runtime control: power-cycling the mouse restores its onboard
configuration. The app never writes the mouse's onboard memory.

## How it works

The app talks HID++ to the mouse over Android's USB Host API using HID
SET_REPORT control transfers. Packet builders are pure Kotlin with no
Android imports, unit-tested against reference vectors. See
[ARCHITECTURE.md](ARCHITECTURE.md) and [PROTOCOL.md](PROTOCOL.md).

## Building

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # lint
```

JDK 17, Android SDK 35. GitHub Actions builds every push.

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Hardware compatibility reports are
especially welcome — include VID/PID from the app's diagnostics.

## License

MIT — [LICENSE](LICENSE). Third-party notices:
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md);
licensing rationale: [LICENSING.md](LICENSING.md).
