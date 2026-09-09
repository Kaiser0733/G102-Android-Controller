# Compatibility

## Physically tested hardware

| Device | VID | PID | Android host | Status |
|--------|-----|-----|--------------|--------|
| Logitech G102 LIGHTSYNC | 0x046D | 0xC092 | Samsung Galaxy Tab A9+, Android 16 | ✅ Physically verified (primary test environment) |

The G102 LIGHTSYNC used for verification identifies as USB `046D:C092`.

## Expected but untested

| Device | VID | PID | Reasoning |
|--------|-----|-----|-----------|
| Logitech G203 LIGHTSYNC | 0x046D | 0xC092 | Same USB identity as G102 LIGHTSYNC (the family shares `046D:C092`; both are LIGHTSYNC-generation hardware with the same command set) |
| Logitech G102/G203 LIGHTSYNC (later revision) | 0x046D | 0xC09D | Second known LIGHTSYNC-family PID seen in reference material; app accepts it (preference list) but it has not been physically tested |
| Phones/tablets with USB Host + OTG, Android 7.0+ | — | — | App requires only `android.hardware.usb.host`; any Android device supporting OTG mouse connections should work, untested on others |

## Known incompatible

| Device | VID | PID | Reason |
|--------|-----|-----|--------|
| G203 Prodigy (pre-LIGHTSYNC) | 0x046D | 0xC084 | Different (non-LIGHTSYNC) command set; app lists it as unknown but will not send LIGHTSYNC vectors — do not expect control |

The app accepts **any** Logitech VID device for diagnostics (the USB device
filter matches vendor 0x046D), but lighting commands are built for the
LIGHTSYNC command family. Unknown PIDs are labeled honestly in the UI.

## Reporting

Tested another mouse or Android host? Please open a **Hardware
Compatibility Report** issue — VID/PID (from the app's USB diagnostics),
device model, and which features worked. Rows are added here from verified
reports only.
