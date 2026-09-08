# DECISIONS.md

Every non-obvious choice, with What / Why / Change-trigger.

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
  Selection order: (a) HID-class interface that is NOT subclass 1/protocol 2
  (plain mouse), (b) the only HID interface if just one exists, (c) reference
  fallback index 1. The choice and full descriptors go to diagnostics.
  Why: gaming mice expose several interfaces; the HID++ node on this family is
  interface 1, but the scan keeps unknown Logitech models workable and honest.
  Change-trigger: diagnostics from a real device showing the HID++ node
  elsewhere — adjust the scan, never hardcode blindly.

- **D5 — Device discovery: VID 0x046D gate, PID preference list, no PID lock.**
  C092 and C09D are known-good LIGHTSYNC PIDs (sorted first); every other
  Logitech device is still listed and diagnosable but marked "unknown model".
  Why: spec explicitly forbids a single hardcoded PID; revisions vary.
  Change-trigger: a new LIGHTSYNC PID confirmed by hardware evidence.

- **D6 — Zero runtime dependencies, no AndroidX, plain Activity + XML.**
  Why: the whole app is one Activity, one USB manager class, one protocol
  object, one diagnostics formatter. Compose/AndroidX would add a dependency
  wall for zero benefit. JUnit4 is test-only.
  Change-trigger: a second screen or navigation need — then AndroidX, not before.

- **D7 — Toolchain: AGP 8.9.2, Kotlin 2.1.10, Gradle wrapper 8.13, JDK 17,
  compileSdk/targetSdk 35, minSdk 24.**
  Why: the mutually-compatible verified set from prior successful CI builds;
  AGP 8.9.x officially pairs with Gradle 8.x and JDK 17; compileSdk 35 is the
  newest stable AGP 8.9 supports without warnings. minSdk 24 covers the target
  tablet (Android 16) with margin for older OTG devices.
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
  Why: development happens in cloud CI without hardware; the first physical
  test may fail, and the diagnostics blob is the only evidence channel.
  Change-trigger: none.

- **D12 — Unit tests assert reference-extracted vectors.**
  Why: tests that merely restate the implementation prove nothing. The
  known-good hex vectors come from the reference implementation's formatted
  output, mechanically extracted.
  Change-trigger: none.
