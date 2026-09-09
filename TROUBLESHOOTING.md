# Troubleshooting

## Mouse not detected

- Check the OTG adapter/cable — most failures are physical
- Try reconnecting the mouse; watch the app status line update
- Confirm your Android device supports USB Host (`android.hardware.usb.host`);
  the Play Store listing equivalent would filter on it
- Open **USB DIAGNOSTICS** — it lists every USB descriptor it saw. If the
  device appears with a non-Logitech VID, it will not be controlled

## USB permission denied

- Tap **GRANT USB ACCESS** when prompted. Android shows a system dialog per
  device, per connection session
- Re-grant after reconnecting the mouse — Android does not remember USB
  permission across reboots for most devices

## App shows an unsupported Logitech device

- The app gates commands on the LIGHTSYNC family (PIDs 0xC092/0xC09D known).
  Other Logitech PIDs (e.g. G203 Prodigy 0xC084) use a different command
  set — the app lists them for diagnostics but cannot control their lighting

## RGB command returns failure

- Grant USB permission first (see above)
- If status says "Working…" for a long time, a previous transfer may be
  finishing its bounded timeout — wait, then retry. Commands are rejected
  while one is in flight (by design, so rapid taps can't flood USB)
- If a write fails repeatedly, tap COPY DIAGNOSTICS and include it in an
  issue

## Lighting does not visibly change

- The command may have succeeded — reports are honest ("sent successfully"
  means the write count was non-negative, visual confirmation is yours)
- Solid color at brightness 0% = black. Raise brightness
- The mouse re-applies onboard lighting after a power cycle — the app sends
  runtime commands only; re-APPLY after reconnecting

## Mouse disconnected mid-command

- The sequence stops, the connection is released, and status reports the
  failure. Reconnect, grant permission, and retry — no recovery action is
  needed in the app

## Android cursor behaves strangely

- If the mouse is also your Android system pointer, close the app,
  disconnect and reconnect the mouse, and reopen. Early v2 builds sent
  automatic live-preview commands that correlated with pointer ghosting;
  that path was removed in 2.0.2 (see ARCHITECTURE.md) and all later builds
  send USB commands only on explicit button presses

## How to file a useful issue

1. App version (see footnote in-app)
2. Android device + version
3. Mouse exact model + VID/PID from USB diagnostics
4. What happened vs expected
5. Diagnostics output (COPY DIAGNOSTICS → paste)
