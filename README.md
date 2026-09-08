# G102-Android-Controller

Turn off the RGB lighting of a **Logitech G102 / G203 LIGHTSYNC** mouse
directly from Android over USB/OTG. Built for a Samsung Galaxy Tab A9+
(Android 16), no PC, no G HUB, no root.

## What v1 does

Exactly one thing, deliberately: **TURN RGB OFF** — a mode-switch command
followed by a solid-color command of `00 00 00` (labeled BLACK_FALLBACK; no
true LED-disable effect is known for this hardware family). The app detects
the mouse, walks its USB descriptors, requests permission, sends the
LIGHTSYNC commands over HID SET_REPORT, reads the device's response, and
reports honestly what happened.

If no mouse is found, or permission is missing, or a transfer fails, the UI
says so — no fake success states.

## Build

GitHub Actions is the build environment (the target tablet can't compile):

1. Push to `main` (or run the workflow manually: Actions → Android Build →
   Run workflow).
2. CI runs `testDebugUnitTest`, `lintDebug`, `assembleDebug`.
3. Download the **G102-Controller-debug** artifact from the run page — that's
   the debug-signed APK.

## Install & test (Samsung Galaxy Tab A9+)

1. Download the APK artifact on the tablet, open it, allow install from this source.
2. Connect the G102 via USB/OTG adapter.
3. Open "G102 Controller".
4. If the "GRANT USB ACCESS" button is visible, tap it and allow in the system
   dialog (Android shows one prompt per device per app; permission lasts until
   the mouse is unplugged).
5. Tap **TURN RGB OFF** and watch the mouse.

## Diagnostics

The **USB DIAGNOSTICS** section shows every device/interface/endpoint
descriptor, and every command with its exact hex bytes, transfer result, and
response. **COPY DIAGNOSTICS** puts the whole report on the clipboard for
pasting back to development; **SHARE** sends it through any app.

## Protocol

Two HID++ reports over HID SET_REPORT control transfers to the mouse's
vendor interface — see [PROTOCOL.md](PROTOCOL.md) for the full byte map and
reasoning. Summary:

- mode switch (short): `10 FF 0E 5B 01 03 05`
- RGB off (long): `11 FF 0E 1B 00 01 00 00 00 … 01 00 00`

USB identity: VID 046D, PIDs C092 / C09D (G102/G203 LIGHTSYNC family).
Other Logitech devices are listed as "unknown model" and remain diagnosable.

## Limitations

- RGB settings may reset after disconnect/reconnect or power cycle — runtime
  control only, by design.
- No hardware in CI: cloud builds prove compilation, tests, and lint; the
  physical effect is verified on the real mouse.
- v1 scope is RGB OFF only. No DPI, profiles, macros, or lighting effects yet.

## Safety

Runtime lighting control only. No firmware operations, no onboard memory
modification, no DFU/bootloader commands, nothing irreversible.

## References & licenses

- [smasty/g203-led](https://github.com/smasty/g203-led) — MIT. The LIGHTSYNC
  command vectors and mode-switch sequence derive from its behavior; protocol
  logic reimplemented in Kotlin, not copied.
- [libratbag](https://github.com/libratbag/libratbag) — MIT. HID++ background
  knowledge.
