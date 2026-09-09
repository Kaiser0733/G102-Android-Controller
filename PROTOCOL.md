# LIGHTSYNC RGB-OFF — protocol notes (G102 / G203 LIGHTSYNC)

> **Reverse-engineered, not official.** Nothing here is documented by
> Logitech. All command behavior was derived from the MIT-licensed
> g203-led reference implementation and cross-checked against libratbag
> HID++ knowledge (see References). Facts below are labeled by evidence
> level: **[verified]** = physically confirmed on real hardware,
> **[reference]** = byte-verified against the reference implementation,
> **[assumed]** = reasonable inference, not independently confirmed.

## Provenance

Every byte below is derived from the MIT-licensed reference implementation
[smasty/g203-led](https://github.com/smasty/g203-led) (LightSync support by
TheAquaSheep), cross-checked against [libratbag](https://github.com/libratbag/libratbag)
HID++ driver knowledge. The formatted packet vectors were extracted from the
reference with a script (see `research/verified_vectors.json` in the task log) —
no packet in this app was invented from memory.

Device identity: the G102 LIGHTSYNC and G203 LIGHTSYNC ship under the same
USB identity `046D:C092`; a later hardware revision appears as `046D:C09D`.
Both are the LIGHTSYNC family (the older Prodigy `046D:C084` uses a different
command set and is out of v1 scope).

## Transport

The reference talks to the mouse with USB **control transfers** implementing
**HID SET_REPORT**, aimed at the vendor HID++ interface:

| Field      | Value | Meaning |
|------------|-------|---------|
| bmRequestType | 0x21 | host→device, class, interface recipient |
| bRequest   | 0x09 | HID SET_REPORT |
| wValue     | 0x0210 / 0x0211 | (report type OUTPUT=2) << 8 \| report ID |
| wIndex     | 0x01 | the HID++ interface number (interface 1 on this family) |
| wLength    | 7 or 20 | payload = the HID++ report itself |

A 7-byte report goes as report ID **0x10** (short), a 20-byte report as
report ID **0x11** (long). The device answers on the interface's interrupt IN
endpoint (0x82 in the reference's view of the device); the reference reads
20 bytes after every command.

On Android this maps to `UsbDeviceConnection.controlTransfer()` with the same
field values; the IN read maps to `bulkTransfer()` on the claimed interface's
interrupt IN endpoint (Android exposes interrupt endpoints through the bulk API).

## The two v1 commands

### 1. Mode switch (short report, always sent first)

```
10 FF 0E 5B 01 03 05
│  │  │  │  │  └─┴─ fixed payload 03 05
│  │  │  │  └─ 01 = disable onboard-memory application
│  │  │  └─ 5B = device-mode function
│  │  └─ 0E = HID++ feature index (RGB control node on this family)
│  └─ FF = wired device index
└─ 10 = short report ID
```

This makes the runtime color command actually stick — without it the mouse
re-applies its saved onboard lighting. It is a runtime mode switch, reversible
on power-cycle; it does NOT modify onboard memory.

### 2. RGB OFF = solid color 00 00 00 (long report)

```
11 FF 0E 1B 00 01 00 00 00 00 00 00 00 00 00 00 01 00 00 00
│  │  │  │  │  │  │  │  │  └─ bytes 9..15 zero    └─┬─┘└─┬─┘
│  │  │  │  │  │  └─┴─┴─ RGB at bytes 6..8   apply  bytes 17..19 zero
│  │  │  │  │  │                                 flag at byte 16
│  │  │  │  │  └─ 01 = solid-color effect variant
│  │  │  │  └─ 00 = padding
│  │  │  └─ 1B = set-color-effect function
│  │  └─ 0E = feature index
│  └─ FF = wired device index
└─ 11 = long report ID
```

Zero red, zero green, zero blue: the LEDs are commanded to emit nothing.
This is **BLACK_FALLBACK**, not a distinct LED-disable effect — no true-off
effect is known for this hardware family, so the app reports it honestly.

## What is NOT used (and why)

- **HID++ 2.0 feature discovery (0x8071, ROOT_GET_FEATURE)**: the LIGHTSYNC
  G102/G203 family takes fixed HID++ 1.0-style commands (feature index 0x0E
  directly) — exactly what the working reference uses. Feature-ID→feature-index
  translation is a HID++ 2.0 concept that does not apply to this command path.
- **Zone-specific commands**: the reference's color-effect command lights all
  zones with one packet (its `triple` command is a different, optional path).
- **DPI / profiles / macros / onboard memory writes**: out of scope; no such
  command exists in this app.

## v2 extensions (same transport, reference-verified vectors)

Solid/Cycle/Wave/Breathe/Blend reuse the long-report header with effect IDs
01/02/03/04/06 (byte 4), rate bytes (milliseconds, little-endian, bytes
9-10 for cycle-class effects), direction byte for wave (01 = right,
06 = left), and a native brightness byte (byte 11) for cycle-class effects.
Zones use feature 0x12 (triple set 0x1B + apply 0x7B). All v2 vectors are
**[reference]** — byte-verified against the reference's formatted output by
script before implementation, and frozen in the unit-test suite. Physical
verification of individual v2 effect bytes on hardware: partial (G102
LIGHTSYNC, primary test environment); see COMPATIBILITY.md for what is
physically verified per release.

## References & licenses

- [smasty/g203-led](https://github.com/smasty/g203-led) — MIT. Command vectors
  and the LightSync mode-switch sequence are derived from its behavior.
- [libratbag](https://github.com/libratbag/libratbag) — MIT. HID++ background.
- Protocol behavior is reimplemented in Kotlin; no code is copied verbatim.
