# Security policy

## Supported versions

Public beta builds from GitHub Releases. Older engineering builds are
unsupported.

## How to report a security issue

**Preferred: GitHub's private vulnerability reporting.**

1. Go to the repository's **Security** tab → "Report a vulnerability".
   (If the maintainer has not enabled it yet: open a regular issue labeled
   `security` and the maintainer will handle it privately from there.)

Do not open public issues for exploitable vulnerabilities before the
maintainer has responded.

## What counts as a security issue here

- A malicious or tampered APK circulating under this project's name —
  report where you got it, we will publish the legitimate signer
  fingerprint to compare against
- Signing/secrets exposure (e.g. a leaked production key would be published
  here and revoked)
- USB/device-control safety concerns (unexpected device commands, behavior
  outside the documented runtime-lighting scope)
- Accidentally committed secrets or personal information

## What this app can and cannot do

This app sends HID++ runtime-lighting commands to a directly USB-connected
Logitech mouse. It does not:

- flash firmware or modify onboard memory
- send commands to devices other than the one the user explicitly grants
  USB permission for
- access the network (the APK declares zero permissions — no INTERNET)

## Signing status of published APKs

- Debug/beta APKs: pinned debug key (public, convenience only — authenticity
  cannot be proven from it). See [RELEASE_SIGNING.md](RELEASE_SIGNING.md).
- Production APKs: owner-held key once configured; fingerprint published in
  each release.
