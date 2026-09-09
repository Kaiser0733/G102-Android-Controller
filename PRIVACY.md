# Privacy

**This app operates entirely on your device. It has no network access.**

The Android manifest declares **zero permissions** — not even
`android.permission.INTERNET`. This is verifiable from the source and from
the published APK (`aapt dump badging` shows no `uses-permission` entries).
An app that cannot request network access cannot send data anywhere.

## What the app does with data

| Data | Where it lives | Leaves the device? |
|------|----------------|--------------------|
| Your lighting configuration (color, effect, brightness, speed, direction, zone colors) | Android `SharedPreferences`, private app storage | No |
| USB diagnostics log (device descriptors, command bytes, results — last 500 lines, in memory) | App memory + screen | Only if **you** tap COPY or SHARE |
| Crash log (last 64 KiB, newest kept) | Private app file | Only if **you** tap COPY or SHARE |

## What the app does NOT do

- No analytics
- No telemetry
- No advertising
- No accounts
- No remote server, no backend
- No tracking of any kind
- No access to your files, contacts, camera, microphone, location
- No sale or sharing of data

## USB device details

The app reads USB descriptors of connected Logitech devices (vendor/product
ID, product name) to operate the mouse and to populate diagnostics. This
information is used on-device only and is never transmitted. Diagnostics text
includes device names and command bytes — review the contents before pasting
it into a public bug report.

## No personal information is collected

The app stores no personal data. The crash log contains stack traces only.
