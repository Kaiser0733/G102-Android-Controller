# G102-Android-Controller

Control the RGB lighting of a **Logitech G102 / G203 LIGHTSYNC** mouse
directly from Android over USB/OTG. Built for a Samsung Galaxy Tab A9+
(Android 16), no PC, no G HUB, no root.

**v1 (RGB OFF) is physically verified on real hardware.** v2 adds the full
controller: RGB ON, custom colors, brightness, effects, zones — all riding
the same proven USB/HID++ transport. The v1 baseline is preserved as the git
tag `v1.0-rgb-off-working`.

## 2.0.2 runtime hotfix (versionCode 4)

Live USB preview and auto-apply are disabled. Startup, resume, reconnect,
permission broadcasts and configuration changes send no lighting packets.
Only RGB ON, RGB OFF and APPLY initiate a short USB sequence. No polling.
An in-flight transfer may finish its existing bounded timeout after stop;
remaining packets are cancelled and the connection is released in finally.

The pointer-ghosting mechanism is not physically proven. This release removes
the repeated automatic USB path and awaits a Tab A9+ / G102 retest.
See [HOTFIX-2.0.2.md](HOTFIX-2.0.2.md) for review evidence and test steps.

## What it does

- **RGB OFF** — the physically-verified v1 path: mode switch + solid black
  (BLACK_FALLBACK; no true LED-disable effect is known for this family).
- **RGB ON** — restores your saved lighting (any effect packet re-enables
  lighting per the reference; physical behavior still needs testing).
- **Custom solid color** — presets + hex input (#B76E79), then APPLY.
- **Brightness** — native protocol brightness for Cycle/Wave/Breathe/Blend;
  solid uses honest RGB-scaling (documented, not claimed as hardware).
- **Effects** — Solid, Cycle, Wave (direction), Breathe, Blend, Zones —
  every effect ID byte-verified against the reference implementation.
- **Zones** — 3 independently addressable zones via the 0x12 triple command.
- **Persistence** — your config is remembered app-side (never written to
  mouse onboard memory). Auto-apply is disabled in this hotfix.

## Install & update

Download the latest **G102-Controller-debug** artifact from Actions and
install. From v2 onward every build is signed with one pinned keystore, so
updates install directly over the previous version — no uninstall needed.
(The v1 -> v2 step needs one last uninstall: v1 was signed by GitHub's
throwaway runner key.)

## Build

GitHub Actions is the build environment: push to `main` or run the workflow
manually. CI runs unit tests, lint, and assembleDebug, then uploads the APK
artifact.

## Diagnostics

The **USB DIAGNOSTICS** section logs every descriptor and command with exact
hex bytes; **COPY DIAGNOSTICS** puts it on the clipboard for bug reports.

## Protocol

See [PROTOCOL.md](PROTOCOL.md). Summary — HID++ reports over HID SET_REPORT
control transfers to the vendor interface:

- mode switch (short): `10 FF 0E 5B 01 03 05`
- solid (long): `11 FF 0E 1B 00 01 RR GG BB … 01 @16`
- cycle/wave/breathe/blend: same header, effect IDs 02/03/04/06
- zones: `11 FF 12 1B 01 … 02 … 03 …` + apply `11 FF 12 7B …`

USB identity: VID 046D, PIDs C092 / C09D (G102/G203 LIGHTSYNC family).

## Limitations

- Lighting resets on mouse power-cycle (runtime control only, by design).
- Only RGB OFF is physically verified so far; v2 features await hardware
  testing.
- Auto-apply and live USB preview are unavailable in 2.0.2.

## Safety

Runtime lighting control only. No firmware operations, no onboard memory
modification, no DFU/bootloader commands, nothing irreversible.

## References & licenses

- [smasty/g203-led](https://github.com/smasty/g203-led) — MIT. All command
  vectors derive from its behavior; logic reimplemented in Kotlin.
- [libratbag](https://github.com/libratbag/libratbag) — MIT. HID++ background
  knowledge.
