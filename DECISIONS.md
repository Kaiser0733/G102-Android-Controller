# Design decisions

Short records of the choices that shape the app. Format: decision, reason,
and the trigger that would reopen it.

## Runtime safety

### Explicit commands only

Live USB preview sent lighting packets on every UI event. On a real device
(Tab A9+, Android 16, mouse doubling as the system pointer) this coincided
with duplicated cursor ghosts and escalating lag, so the preview path was
removed entirely. Only ON, OFF, and APPLY initiate USB traffic; startup,
resume, rotation, reconnection, and configuration rendering are USB-silent.
CI enforces this (`scripts/check_hotfix.py`).

### One command at a time

A single application-owned command gate rejects concurrent attempts. An
in-flight transfer finishes within its existing Android timeout; remaining
packets are cancelled and the connection is released in `finally`.

### RGB OFF is regression-tested

The off sequence (mode switch `10 ff 0e 5b 01 03 05` + solid black) is
asserted byte-for-byte in `rgbOffSequence_matchesKnownGoodVector`. Any
change to those bytes fails CI.

### No firmware writes

Commands are runtime lighting control only. No firmware flashing, no
onboard-memory writes, nothing persistent — power-cycling the mouse
restores its saved lighting.

## Protocol

### Off = solid black (no true-off command)

The reference implementation (smasty/g203-led) has no distinct LED-disable
effect for this family; off is solid black, labeled as such in the UI.

### Transport = HID SET_REPORT control transfers

Exactly what the reference does on real hardware (libusb
ctrl_transfer 0x21/0x09/0x0210|0x0211/wIndex=1), mapped to Android's
`UsbDeviceConnection.controlTransfer`. The device replies on the claimed
interface's interrupt IN endpoint.

### Interface selection scans descriptors

The HID++ vendor node is found by descriptor walk (vendor subclass 0 /
protocol 0 first), never hardcoded — except the reference's fallback index 1.

### Solid brightness is RGB scaling

The protocol has a native brightness byte for cycle-class effects only.
Solid color brightness is app-side RGB scaling, labeled honestly in the UI.

### Effects

Solid, Cycle, Wave (direction 01=right / 06=left), Breathe, Blend, Zones
(3 zones via feature 0x12). Rate is milliseconds, inverted to a 0-100 speed
slider. Brightness clamps to 1-100 because 0 is only expressible as solid
black.

## App architecture

### Zero runtime dependencies

One Activity, plain views, no AndroidX — the app is small enough that a
dependency wall buys nothing. JUnit4 is test-only.

### Zero permissions

The manifest requests nothing — not even INTERNET. Nothing can leave the
device (see PRIVACY.md).

### Persistence is app-side only

Configuration lives in SharedPreferences. The mouse's onboard memory is
never written.

### Honest results

"Sent successfully" means a non-negative write count on every packet.
Responses are reported when read, absence stated, visual confirmation left
to the user.

### Device handles are never cached

UsbDevice objects are re-enumerated on every operation; stale handles
become silent failures after reconnect.

## Release engineering

### Versioning

versionCode increments monotonically; versionName is semantic (2.2.0 =
public beta packaging on a byte-identical 2.1.1 app).

### Pinned debug key

CI signs all debug APKs with one committed keystore so installs supersede
cleanly. It is a convenience, not an authenticity guarantee; production
signing comes from GitHub Actions Secrets (RELEASE_SIGNING.md).

### Display name, applicationId

The display name is "G102 Controller" (descriptive, nominative).
applicationId `com.kaiser0733.g102controller` never changes — renaming
would break upgrades.
