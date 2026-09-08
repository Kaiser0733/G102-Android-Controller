"""Source-wiring guard; complements JVM behavior tests, not a device test."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main/java/com/kaiser0733/g102controller/MainActivity.kt'

class HotfixWiring(unittest.TestCase):
    def test_no_automatic_command_machinery(self):
        source = MAIN.read_text()
        for token in ('schedulePreview', 'previewRunnable', 'composePreviewPackets', 'postDelayed', 'maybeAutoApply', 'firstSelection'):
            with self.subTest(token=token):
                self.assertNotIn(token, source)

    def test_hardware_entry_points_are_explicit_buttons_only(self):
        source = MAIN.read_text()
        # A small source-wiring guard backs the real JVM state/action tests.
        self.assertEqual(2, source.count('sendCommandList('))  # declaration + injected sender
        self.assertEqual(3, source.count('actions.execute('))
        self.assertIn('btnRgbOff.setOnClickListener { actions.execute(LightingActions.Action.OFF) }', source)
        self.assertIn('btnRgbOn.setOnClickListener {\n            actions.execute(LightingActions.Action.ON)', source)
        self.assertIn('btnApplyColor.setOnClickListener { applyCurrentConfig() }', source)
        self.assertIn('btnApplyEffect.setOnClickListener { applyCurrentConfig() }', source)
        self.assertEqual(3, source.count('applyCurrentConfig()'))
        self.assertIn('private fun applyCurrentConfig() {\n        store.save(config)\n        actions.execute(LightingActions.Action.APPLY)', source)
        self.assertNotRegex(source, r'\bThread\s*\{')
        self.assertNotIn('setDefaultUncaughtExceptionHandler', source)

    def test_lifecycle_cancels_and_usb_is_always_closed(self):
        source = MAIN.read_text()
        self.assertIn('override fun onStop() {\n        session.stop()', source)
        self.assertIn('session.detach()', source)
        self.assertIn('if (!session.canContinue)', source)
        self.assertIn('} finally {\n            usb.close(connection, claimedInterface)', source)
        self.assertNotIn('loadAutoApply', source)
        self.assertIn('app.commandListener = commandListener', source)
        self.assertIn('if (app.commandListener === commandListener) app.commandListener = null', source)
        self.assertIn('app.notifyCommandFinished()', source)
        self.assertIn('setControlsEnabled(!busy)', source)
        application = (MAIN.parent / 'ControllerApplication.kt').read_text()
        self.assertIn('mainHandler.post { commandListener?.invoke() }', application)
        self.assertNotIn('postDelayed', application)

    def test_android_swatches_are_opaque(self):
        calls = [line.strip() for line in MAIN.read_text().splitlines() if 'setBackgroundColor(' in line]
        self.assertTrue(calls)
        for call in calls:
            self.assertIn('ColorUtils.rgbToAndroidColor(', call)

if __name__ == '__main__':
    unittest.main()
