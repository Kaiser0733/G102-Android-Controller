# Architecture

One Activity, zero permissions, zero network, zero runtime dependencies.
The whole app is ~16 Kotlin files by design (DECISIONS.md D6).

## Layers

```
UI (MainActivity — single Activity, XML layouts)
  │  user intent = explicit button press only
  ▼
Configuration/state
  LightingConfig (immutable data) + LightingStore (SharedPreferences)
  CommandGate / CommandSession (one command at a time; busy = rejected)
  ▼
controller/LightingActions — ON / OFF / APPLY semantics
controller/RgbCommandComposer — config → ordered List<ByteArray>
  ▼
protocol/ — pure packet builders (NO Android imports, JVM-testable)
  ProtocolPackets   (mode switch, solid color — the physically verified v1 path)
  LightSyncEffects  (cycle/wave/breathe/blend/zones — v2 layer)
  ▼
usb/LogitechUsbManager — Android UsbManager: enumerate, permission,
  claim HID++ interface, controlTransfer (HID SET_REPORT), read responses
  ▼
Physical device (Logitech G102/G203 LIGHTSYNC over USB OTG)
```

## Design decisions that matter

**Protocol is separated from transport.** `protocol/` builds pure byte
arrays with no Android dependency; `usb/` only moves bytes. Every protocol
builder is unit-tested against reference-extracted vectors on the JVM
without a device or emulator.

**Explicit commands only (2.0.2 hotfix).** Early v2 sent live-preview
packets on every UI event. On a real device (Tab A9+, Android 16, the mouse
simultaneously acting as the system pointer) this correlated with pointer
ghosting and escalating lag. The fix was structural: no UI event can reach
USB anymore. Only button presses enter `LightingActions`, through a
single-command gate that rejects concurrent attempts. Startup, resume,
rotation, reconnect, and config rendering are USB-silent by construction,
and CI re-verifies this on every push (`scripts/check_hotfix.py`).

**RGB OFF is regression-pinned.** The v1 sequence that physically turned a
real G102's lighting off (`10 ff 0e 5b 01 03 05` + solid black) is frozen
byte-for-byte in
`regression_rgbOffSequence_isPhysicallyVerifiedV1`. Any protocol change
that alters it fails CI. See DECISIONS.md D21.

**Honesty in results.** "Sent successfully" means a non-negative write
count on every packet; device responses are reported when read and their
absence stated; visual confirmation is left to the user (D10).

**Local-only.** No INTERNET permission exists, so nothing can phone home.
Settings live in SharedPreferences; diagnostics in a bounded in-memory ring
buffer; crash traces in a private file capped at 64 KiB.

**Runtime-only lighting.** Commands are reversible runtime mode switches
and color/effect reports. No firmware, DFU, or onboard-memory writes exist
in the codebase.

## Diagnostics flow

`DiagnosticBuffer` (bounded, 500 lines × 512 chars) records every USB event
descriptively — enumeration, interface selection, permission, raw packet
hex, write counts, responses. `UsbDiagnostics` formats device descriptors.
COPY/SHARE export is a user action only.
