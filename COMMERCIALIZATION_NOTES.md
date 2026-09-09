# Commercialization notes

Preparation only. No outreach, no contact with Logitech, no speculation.

## What belongs to this project

- All application Kotlin code (UI, controller, transport, protocol
  implementation, diagnostics, settings) — written for this project
- All resources (layouts, drawables, original launcher icon)
- All documentation
- Build/CI/verification scripts

## What does NOT belong to this project

- "Logitech", "G102", "G203", "LIGHTSYNC" — trademarks of Logitech
  International S.A. (nominative compatibility use only)
- HID++/LIGHTSYNC protocol facts — interoperability knowledge derived from
  MIT-licensed reference material (see THIRD_PARTY_NOTICES.md); facts are
  not owned by anyone, and the implementation is original

## Third-party obligations

- None that constrain commercial use: all consulted/compiled material is
  MIT or Apache-2.0 (see THIRD_PARTY_NOTICES.md). Apache-2.0 requires
  notice retention (NOTICE/LICENSE text) in redistribution — handled by
  THIRD_PARTY_NOTICES.md.

## License posture

- Project license: MIT — permits commercial use, dual licensing, and
  relicensing by the copyright holder
- A future proprietary/dual-license arrangement would require: copyright
  consolidation (all contributors assign or license under MIT with the
  right to relicense — a CLA would be needed before accepting outside
  contributions if dual licensing is ever planned)
- Trademark: any commercial product name must not imply Logitech
  affiliation. Current display name "G102 Controller" is nominative; a
  distinctive name (e.g. "LIGHTWRENCH" style, unrelated to Logitech marks)
  would be stronger for commercial use. Display name is user-visible only;
  applicationId never changes (breaks upgrades)

## Pre-deal review checklist (if ever needed)

- [ ] Contributor provenance audit (git history — currently single-author,
  trivial)
- [ ] License compatibility re-verification
- [ ] Trademark clearance for any new product name
- [ ] Signing infrastructure (owner-held production key — see
  RELEASE_SIGNING.md)
- [ ] Security review of the USB attack surface (it's small: one claimed
  interface, bounded transfers, no network)

## What would NOT transfer

Any acquisition/license deal could not include Logitech's marks or the
protocol itself (facts). It covers this repository's original code and
documentation only.
