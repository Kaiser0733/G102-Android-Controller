"""Validate the built artifact, not just Gradle source declarations (runs on CI)."""
from pathlib import Path
import hashlib
import os
import subprocess


def run(*args):
    return subprocess.check_output(args, text=True).strip()


sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
tools = sorted((sdk / 'build-tools').glob('*/apksigner'))[-1].parent
apk = Path('app/build/outputs/apk/debug/app-debug.apk')
badging = run(str(tools / 'aapt'), 'dump', 'badging', str(apk)).splitlines()[0]
for required in ("name='com.kaiser0733.g102controller'", "versionCode='7'", "versionName='2.2.0'"):
    assert required in badging, badging
signing = run(str(tools / 'apksigner'), 'verify', '--print-certs', str(apk))
certificate = subprocess.check_output(['keytool', '-exportcert', '-keystore', 'debug.keystore',
    '-storepass', 'android', '-alias', 'androiddebugkey'])
expected = hashlib.sha256(certificate).hexdigest()
digests = [line.split('certificate SHA-256 digest: ', 1)[1]
    for line in signing.splitlines() if 'certificate SHA-256 digest: ' in line]
assert digests and set(digests) == {expected}, signing
receipt = '\n'.join([badging, signing, 'APK SHA-256: ' + hashlib.sha256(apk.read_bytes()).hexdigest(),
    'Pinned certificate matches debug.keystore: PASS']) + '\n'
print(receipt)
output = Path('app/build/reports/hotfix-apk.txt')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(receipt)
