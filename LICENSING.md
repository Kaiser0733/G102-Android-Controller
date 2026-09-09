# Licensing

## Summary

This project is licensed under the **MIT License** (see [LICENSE](LICENSE)).
All incorporated third-party material is MIT- or Apache-2.0-licensed, which
MIT is compatible with. No GPL/LGPL code is incorporated.

## What this project's license covers

Original work in this repository:

- All Kotlin application, USB, protocol, controller, diagnostics, and
  settings code (written from scratch for this project)
- All layouts, drawables, launcher icon, and resources (original vector art)
- All documentation written for this project
- Build scripts, CI workflow, and verification scripts

## Relationship to upstream reference material

This project is a clean-room-style consumer of protocol knowledge, not a
derivative work of upstream code:

1. **Protocol facts are not code.** The HID++/LIGHTSYNC byte layouts
   (report IDs, feature indices, color positions, effect IDs, zone tags)
   describe how Logitech hardware interoperates. These are functional
   interoperability facts — uncopyrightable in themselves — used to talk to
   the mouse. They were learned from the MIT-licensed
   [smasty/g203-led](https://github.com/smasty/g203-led) reference and
   cross-checked against [libratbag](https://github.com/libratbag/libratbag)
   knowledge.
2. **No upstream code is incorporated.** Nothing here is a translation of
   g203-led's Python; the controller is an independent Kotlin implementation
   using Android's USB Host API (g203-led uses libusb). No files are copied.
3. **Test vectors are derived, and disclosed.** The byte vectors asserted in
   the unit tests were mechanically extracted from the reference
   implementation's formatted output by a script, and every test file states
   this in its header. This is factual (the reference's own output), from an
   MIT-licensed work, and disclosed rather than hidden.

## Why MIT

- Every incorporated or consulted upstream is MIT (g203-led, libratbag) or
  Apache-2.0 (Kotlin stdlib, Android SDK) — permissive. Nothing forces
  copyleft.
- MIT matches the licenses of the ecosystem this project builds on and keeps
  community contribution friction low.
- It permits commercial use, dual licensing, and relicensing by the
  copyright holder (see [COMMERCIALIZATION_NOTES.md](COMMERCIALIZATION_NOTES.md)).

## Required notices when redistributing

If you distribute this project (source or binary), retain the LICENSE file
and the notices in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Binary redistributors of the APK must note that the Kotlin Standard Library
(Apache 2.0, JetBrains) is compiled inside.

## Trademark note

"Logitech", "G102", "G203", "LIGHTSYNC" and related marks belong to
Logitech International S.A. This project is unofficial and not affiliated
with, endorsed by, sponsored by, or supported by Logitech. Product names are
used solely to identify hardware compatibility (nominative use).
