# Runtime hotfix 2.0.2 / versionCode 4

## Baselines and scope

- Physically verified OFF baseline: bc7b6084b16eb57c15f6a2da1de4ab539e89977f,
  immutable tag v1.0-rgb-off-working.
- Starting HEAD: 29367cf60f735011de0042628b4f71252c828fc0 (2.0.1 / 3).
- Work branch: hotfix/v2.0.2-runtime-crash.
- No new effects, services, firmware operations, dependencies or USB protocol changes.

## Investigation

The v1-to-v2 diff leaves LogitechUsbManager and ProtocolPackets unchanged.
V2 adds schedulePreview -> delayed Runnable -> sendCommandList through brightness,
speed, effect selection, wave direction, preset color and zone callbacks. Hex
text changes themselves were local-only. Spinner init/restoration can reach the
same event path; swallowing one callback is not an invariant. Auto-apply sends
from resume and attach, with an Activity-local flag that resets on recreation.

Confirmed other defects: 24-bit RGB passed to Android swatches (zero alpha),
unbounded diagnostic entries and append-only crash.txt, Activity-capturing global
crash-handler chains, and command admission scoped to each Activity.

Repeated force-claims disturbing the active Android pointer is a plausible
mechanism, NOT a proven physical root cause. No tablet crash stack or device-side
trace established it. Previous claims that a spinner fix solved it were premature.

## Hotfix and review

- Delete all preview scheduling and packet-composition helpers. Delete auto-apply
  callbacks/UI; remove its saved preference. UI changes and lifecycle events cannot
  invoke lighting hardware. Only explicit ON/OFF/APPLY call LightingActions.execute.
- Preserve all command bytes and force-claim transport. A global atomic gate admits
  one sequence; one Application-owned executor runs it. Busy taps are rejected.
- Stop/detach mark the Activity session cancelled. Resume does not resurrect it.
  Check cancellation before opening/claiming and between packets. Existing bounded
  synchronous USB calls already entered can return after stop; they are not forcibly
  interrupted. Finally releases interface and closes connection on every exit.
- Worker failures and rejected starts release the command gate. UI updates are on
  main. Detached/no-permission states disable command buttons; recreation cannot
  strand a new Activity waiting for an old Activity's completion callback.
- Convert RGB to opaque ARGB only at each swatch display boundary.
- DiagnosticBuffer retains 500 physical lines with bounded entry lengths; packet
  logging does not post a UI redraw for each line. CrashLog retains newest 64 KiB,
  uses bounded tail reads and trims legacy oversized files. Application installs
  one handler, delegates to the previous handler, and guarantees fatal termination.

## Verification

The source-wiring test was run on the broken code and failed for live-preview/
auto-apply machinery and transparent swatches. It now guards the button-only
entry points, missing schedulers, lifecycle cancellation, and guaranteed cleanup.
JVM tests exercise the actual action model, restored config and repeated local edits,
OFF vectors, ARGB cases, log caps, rapid and concurrent admissions, executor/worker
failure, queued-stop cancellation, detach and resume behavior.

GitHub Actions runs source-wiring checks, testDebugUnitTest, lintDebug,
assembleDebug and packaged APK identity/signature verification. XML results,
lint report and APK certificate/version receipts are uploaded separately from APK.
Physical cursor rendering and 10-minute stability remain unverified until retest.

## Physical retest

1. Install 2.0.2 directly over 2.0.1; no uninstall. Open with G102 connected,
   grant USB permission if requested, then leave controls untouched for 2 minutes.
   Expect stable pointer, no lag/ghost copies, no automatic Working or SET_REPORT.
2. Adjust brightness/effect/color/speed/direction/zones without APPLY. Expect local
   UI changes only, unchanged mouse lighting, no SET_REPORT or lag.
3. Press RGB OFF once. Expect the known-good OFF behavior and stable pointer.
4. Press RGB ON once. Expect lighting returns and pointer remains stable.
5. Select Solid, enter #B76E79, press APPLY once. Expect color changes, stable pointer.
6. Use for at least 10 minutes. Expect no progressive lag or ghost pointers.
   Also background/resume and unplug/replug while idle; neither should send commands.

If anything fails, stop issuing commands. Copy diagnostics immediately if usable;
if it crashes, reopen and copy diagnostics (includes newest crash tail). Report test
number, last button pressed, whether app closed/froze, and whether trails persist
outside the app. Missing crash logs do NOT rule out input/compositor stalls or ANRs.
