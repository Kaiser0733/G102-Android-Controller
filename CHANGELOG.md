# Changelog

All notable changes to this project. Dates are commit dates from Git
history; versions follow Android `versionName`.

## [2.2.0-beta] — 2026-09-09 (code 7) — public-release preparation

App behavior is byte-identical to 2.1.1-stable-physical; this version adds
project packaging only.

- MIT license, third-party notices, licensing rationale
- Public documentation set: README rewrite, PRIVACY, SECURITY, CONTRIBUTING,
  CODE_OF_CONDUCT, ARCHITECTURE, COMPATIBILITY, TROUBLESHOOTING,
  RELEASE_SIGNING
- Release workflow with optional production signing from GitHub Actions
  secrets (unsigned-artifact fallback; never mislabeled)
- Issue templates (bug / compatibility / feature) and PR template
- CI: concurrency cancellation, assembleRelease compile check, release/**
  branch coverage
- No hardware, USB, protocol, or controller behavior changes

## [2.1.1] — 2026-09-09 (code 6) — stable physical build

Tag: `v2.1.1-stable-physical` (commit `c1c4cc1`)

- Fix: after rotating the device, freshly inflated views reverted to XML
  defaults (buttons disabled, status text reset). Rotation now re-applies
  live device/session state after re-inflation (read-only refresh; no USB
  command is sent).

## [2.1.0] — 2026-09-08 (code 5)

- Responsive UI: four layout variants (phone/tablet × portrait/landscape),
  shared section includes, dimen scale, 4×2 preset grid
- Rotation re-inflates the matching layout without recreating the Activity
  (configChanges retained; no USB command can fire from rotation)

## [2.0.2] — 2026-09-08 (code 4) — runtime hotfix

Tag: `v2.0.2-stable`

- Removed automatic live USB preview and auto-apply entirely. Startup,
  resume, reconnect, permission broadcasts, and configuration rendering send
  no lighting packets. Only RGB ON / RGB OFF / APPLY initiate USB command
  sequences.
- Bounded runtime state: single command gate, busy rejection, stop/detach
  cancellation, connection always released in `finally`
- Crash capture + diagnostics hardening (bounded lines/bytes)
- Pinned debug keystore: CI builds install over previous versions

## [2.0.1] — 2026-09-08 (code 3)

- versionCode supersede fix (install-over without uninstall)

## [2.0.0] — 2026-09-08 (code 2)

- Full LIGHTSYNC controller: RGB ON, custom colors (presets + hex),
  brightness (native where supported, honest RGB-scaling for solid),
  effects (Solid, Cycle, Wave ± direction, Breathe, Blend), 3-zone colors
- App-side persistence (SharedPreferences; never writes mouse onboard
  memory)
- Effect vectors byte-verified against the reference implementation

## [1.0.0] — 2026-09-08 (code 1) — RGB OFF

Tag: `v1.0-rgb-off-working` (commit `bc7b608`)

- Proof of concept: Android USB Host control of G102/G203 LIGHTSYNC
  lighting. RGB OFF = mode switch + solid black, physically verified
  turning off a real Logitech G102 LIGHTSYNC.
- HID++ SET_REPORT control-transfer transport, interface scanning,
  diagnostics, CI
