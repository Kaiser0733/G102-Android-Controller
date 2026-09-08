"""Structural guard for the responsive UI (complements check_hotfix.py)."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
MAIN_KT = ROOT / 'app/src/main/java/com/kaiser0733/g102controller/MainActivity.kt'

# Every ID MainActivity binds via findViewById must exist exactly once per variant.
REQUIRED = {
    'textDevice', 'textResult', 'textDiagnostics', 'textBrightnessLabel',
    'textSpeedLabel', 'textZoneLabel', 'textDirectionLabel', 'textFootnote',
    'btnGrantUsb', 'btnRgbOn', 'btnRgbOff', 'btnApplyColor', 'btnApplyEffect',
    'btnToggleDiagnostics', 'btnCopyDiagnostics', 'btnShareDiagnostics',
    'colorPreview', 'editHex', 'seekBrightness', 'seekSpeed',
    'spinnerEffect', 'radioDirection', 'zoneRow', 'presetRow',
}
# Referenced by comparison in Kotlin (radioDirection children).
EXTRA_ALLOWED = {'radioRight', 'radioLeft'}

failures = []

# 1. All layout XML parses.
for f in RES.rglob('*.xml'):
    try:
        ET.parse(f)
    except ET.ParseError as e:
        failures.append(f'{f.relative_to(ROOT)}: XML parse error: {e}')

# 2. Every activity_main variant exposes each required ID exactly once.
variants = {}
for d in ['layout', 'layout-land', 'layout-sw600dp', 'layout-sw600dp-land']:
    path = RES / d / 'activity_main.xml'
    if not path.exists():
        failures.append(f'missing variant {d}/activity_main.xml')
        continue
    source = path.read_text()
    ids = re.findall(r'@\+id/(\w+)', source)
    # IDs inside included section files count toward the variant's surface.
    includes = re.findall(r'layout="@layout/(\w+)"', source)
    for inc in includes:
        ids += re.findall(r'@\+id/(\w+)', (RES / 'layout' / f'{inc}.xml').read_text())
    variants[d] = ids
    for required in REQUIRED:
        count = ids.count(required)
        if count == 0:
            failures.append(f'{d}/activity_main.xml (+includes): missing ID {required}')
        elif count > 1:
            failures.append(f'{d}/activity_main.xml (+includes): duplicate ID {required} x{count}')

# 3. No ID declared twice within any single section include.
for f in (RES / 'layout').glob('section_*.xml'):
    ids = re.findall(r'@\+id/(\w+)', f.read_text())
    seen = set()
    for i in ids:
        if i in seen:
            failures.append(f'{f.name}: duplicate ID {i}')
        seen.add(i)

# 4. Kotlin findViewById/R.id references all resolve in every variant.
kt_ids = set(re.findall(r'R\.id\.(\w+)', MAIN_KT.read_text()))
kt_ids.discard('btnToggleDiagnostics')  # via property, already covered
for d, ids in variants.items():
    declared = set(ids)
    for k in kt_ids:
        if k not in declared and k not in EXTRA_ALLOWED and k not in {'textTitle', 'textSubtitle'}:
            failures.append(f'{d}: Kotlin references R.id.{k} but no layout declares it')

if failures:
    print('\n'.join(f'FAIL: {f}' for f in failures))
    sys.exit(1)
print(f'layout guard OK: {len(variants)} variants x {len(REQUIRED)} IDs, sections clean')
