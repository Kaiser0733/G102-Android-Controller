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

    def test_android_swatches_are_opaque(self):
        calls = [line.strip() for line in MAIN.read_text().splitlines() if 'setBackgroundColor(' in line]
        self.assertTrue(calls)
        for call in calls:
            self.assertIn('ColorUtils.rgbToAndroidColor(', call)

if __name__ == '__main__':
    unittest.main()
