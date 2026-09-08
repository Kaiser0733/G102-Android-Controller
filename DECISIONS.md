# DECISIONS.md

Every non-obvious choice, with What / Why / Change-trigger.

## 2.0.2 hotfix decisions (supersede D17 and D19)

- Remove live USB preview entirely. Every configuration callback changes local
  state only. ON/OFF/APPLY are the only lighting-command entry points. Physical
  pointer instability followed the preview release, but its mechanism remains
  unproven; do not equate removal of a suspect path with hardware confirmation.
- Disable auto-apply and remove the old preference. The old guard was scoped
  to an Activity, not a physical USB connection, so recreation could reapply.
- Keep the baseline USB manager and protocol builders byte-identical. Add only
  admission/cancellation checks around the existing finite command sequence.
- One Application-owned executor and atomic command gate reject busy taps;
  no queue of user commands. Stop/detach cancel subsequent work. An already
  entered synchronous USB call finishes within its existing Android timeout;
  the finally block releases/closes the connection, without blocking the UI.
- Diagnostics retain 500 lines, with each line bounded. Crash files retain the
  newest 64 KiB, including truncation of oversized logs left by 2.0.1. Install
  crash handling once in Application, never once per Activity.
- Convert 24-bit RGB to opaque ARGB only at View boundaries. Protocol RGB and
  all OFF bytes/order stay unchanged. Version 2.0.2 / code 4 retains signing.

## v2 additions (historical; hotfix overrides above)

- **D13 — RGB ON = mode switch + saved effect, with a visible floor.**
  Why: the reference has no separate "on" command — any effect packet
  re-enables lighting. A solid config at brightness 0 would re-send black,
  so ON clamps brightness to >=1 and falls back to white if the scaled
  color is pure black. ON always illuminates.
  Change-trigger: evidence of a hardware sleep/wake toggle command.

- **D14 — Brightness: native where the protocol has it, RGB-scaling for solid.**
  Cycle/wave/breathe/blend carry a real brightness byte (verified positions
  in the reference vectors). Solid does NOT — its brightness is app-side
  RGB scaling (255*p/100, integer floor), labeled honestly in the UI.
  Change-trigger: discovery of a solid-brightness byte in protocol traffic.

- **D15 — Effects implemented: Solid, Cycle, Wave, Breathe, Blend, Zones.**
  All six exist in the reference with verified templates. Wave direction
  right=0x01 left=0x06; rate is milliseconds (1000-65535), inverted to a
  0-100 speed slider (high = fast). Brightness clamps to 1..100 because the
  reference clamps 0 -> 1 (0 is not expressible except via solid black).
  Change-trigger: physical test showing an effect ID rejected.

- **D16 — Zones via the 0x12 triple command (set + 0x7B apply).**
  The reference's `triple` command addresses three zones (tags 01/02/03,
  feature 0x12) and is followed by an apply packet — hard evidence the
  G102/G203 LIGHTSYNC exposes 3 independently addressable zones. UI cycles
  each zone through a palette; custom per-zone hex can come later.
  Change-trigger: physical test showing zones not independently colored.

- **D17 — Live preview debounced at 120ms, solid/breathe paths only.**
  Why: picker/seekbar churn would flood USB otherwise; 120ms after the last
  change is responsive and gentle. Effects with rate params apply via the
  APPLY button only (each change is a full restart of the effect).
  Change-trigger: physical instability during preview — then APPLY-only mode.

- **D18 — Persistence is app-side only (SharedPreferences).**
  Saved: color, brightness, effect, rate, direction, zones, auto-apply flag.
  Never written to mouse EEPROM/onboard memory — power-cycle resets the mouse
  to its onboard lighting, the app re-applies on demand (or via auto-apply).
  Change-trigger: never for EEPROM; UI may grow.

- **D19 — Auto-apply is once per connection, never a background service.**
  When enabled and the mouse (re)attaches with permission while the app is
  foreground, the saved config is applied once; the guard re-arms on detach.
  Modern Android makes background USB services unreliable — documented
  limitation, not silently pretended.
  Change-trigger: a reliable foreground-service pattern emerges.

- **D20 — Pinned debug keystore, versionCode 2.**
  Every CI run now signs with the same committed debug keystore, so future
  APKs install directly over v2. v1 was runner-throwaway-signed, so the
  one-time v1 -> v2 update requires a single uninstall (last one ever).
  Change-trigger: production signing (later, with LO's keystore).

- **D21 — Regression pin: the v1 RGB OFF sequence is test-frozen.**
  `regression_rgbOffSequence_isPhysicallyVerifiedV1` asserts the exact
  physically-verified bytes; any change to that path fails CI by design.

## v1 decisions (unchanged, preserved)

- **D1 — RGB OFF = solid color (0,0,0), labeled BLACK_FALLBACK.**
  Why: the working reference implementation (smasty/g203-led, MIT) has no
  distinct LED-disable effect for this family; its "off" is solid black, and it
  demonstrably drives real LIGHTSYNC hardware. The UI never claims TRUE_OFF.
  Change-trigger: evidence of a genuine disable effect for the G102 (from
  device responses or protocol docs) — then implement and label TRUE_OFF.

- **D2 — Transport = HID SET_REPORT control transfers, wIndex = interface 1.**
  Why: exactly what the reference does on real hardware (libusb ctrl_transfer
  0x21/0x09/0x0210|0x0211/wIndex=1). Android's equivalent is
  UsbDeviceConnection.controlTransfer with iface.id as index.
  Change-trigger: physical-device diagnostics showing control transfers
  rejected (negative/short writes) — then try interrupt OUT if an endpoint exists.

- **D3 — Mode switch sent before the color command, every time.**
  Why: the reference always sends `10 ff 0e 5b 01 03 05` first; without it the
  onboard lighting overrides the runtime color. Reversible, runtime-only.
  Change-trigger: none foreseen; it is part of the known-good sequence.

- **D4 — Interface selection: scan, don't assume index 0.**
  Preference order: (a) HID vendor node (subclass 0/protocol 0), (b) any HID
  interface that is not boot mouse (1/2) or keyboard (1/1), (c) the only HID
  interface, (d) reference fallback index 1. Full descriptors go to diagnostics.
  Change-trigger: diagnostics from a real device showing the HID++ node
  elsewhere — adjust the scan, never hardcode blindly.

- **D5 — Device discovery: VID 0x046D gate, PID preference list, no PID lock.**
  C092 and C09D are known-good LIGHTSYNC PIDs (sorted first); every other
  Logitech device is still listed and diagnosable but marked "unknown model".
  Change-trigger: a new LIGHTSYNC PID confirmed by hardware evidence.

- **D6 — Zero runtime dependencies, no AndroidX, plain Activity + XML.**
  Why: the whole app is one Activity, one USB manager class, protocol objects,
  one diagnostics formatter. Compose/AndroidX would add a dependency wall for
  zero benefit. JUnit4 is test-only.
  Change-trigger: a second screen or navigation need — then AndroidX, not before.

- **D7 — Toolchain: AGP 8.9.2, Kotlin 2.1.10, Gradle wrapper 8.13, JDK 17,
  compileSdk/targetSdk 35, minSdk 24.**
  Why: the mutually-compatible verified set; AGP 8.9.x officially pairs with
  Gradle 8.x and JDK 17; compileSdk 35 is the newest stable AGP 8.9 supports.
  Change-trigger: a newer AGP needing compileSdk 36+ — move the whole matrix
  together, never mix.

- **D8 — USB permission: explicit + FLAG_MUTABLE PendingIntent (S+),
  RECEIVER_NOT_EXPORTED (33+).**
  Why: UsbManager mutates the permission broadcast intent to attach
  EXTRA_DEVICE/EXTRA_PERMISSION_GRANTED; an immutable PendingIntent silently
  breaks the flow on Android 12+, and runtime receivers must declare
  exported-ness on 13+. Confirmed by a real merged fix (hradio/omri-usb PR 1).
  Change-trigger: none; this is required platform behavior.

- **D9 — Device never cached across commands.**
  Why: Android re-creates UsbDevice objects on reconnect; a stale handle turns
  into a silent failure. Every operation re-enumerates and matches by
  deviceName. Change-trigger: none.

- **D10 — Results are honest by construction.**
  "Sent successfully" requires a non-negative write count for both packets;
  responses are reported when read, their absence is stated, and visual
  confirmation is explicitly left to the user.
  Change-trigger: never.

- **D11 — Diagnostics from day one, COPY + SHARE.**
  Change-trigger: none.

- **D12 — Unit tests assert reference-extracted vectors.**
  Change-trigger: none.
