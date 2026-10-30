# SPDX-License-Identifier: GPL-3.0-or-later
"""Prepare launcher bitmap variants from the user's preserved original PNG."""
from pathlib import Path
import hashlib
import io
import json
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parent
DENSITIES = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
NAMES = ('ic_launcher', 'ic_launcher_round', 'ic_launcher_foreground')


def prepare_app_icon(work):
    folder = ROOT / 'app-assets/launcher'
    provenance = json.loads((folder / 'provenance.json').read_text())
    data = (folder / 'app_icon.png').read_bytes()
    assert hashlib.sha256(data).hexdigest() == provenance['sha256'], 'Launcher source changed without provenance update'
    with Image.open(io.BytesIO(data)) as image:
        assert image.format == 'PNG' and image.width == image.height and image.width >= 192
        original = image.convert('RGBA')
    resources = {}
    for density, size in DENSITIES.items():
        target = work / 'res' / ('mipmap-' + density)
        target.mkdir(exist_ok=True)
        for name in NAMES:
            # Foreground is retained for existing resource references (108 dp slot).
            pixels = size * 108 // 48 if name.endswith('_foreground') else size
            output = target / (name + '.png')
            original.resize((pixels, pixels), Image.Resampling.LANCZOS).save(output, format='PNG')
            resources[output.relative_to(work).as_posix()] = {
                'pixels': pixels,
                'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
            }
    return {'source_sha256': provenance['sha256'], 'source_size': list(original.size), 'resources': resources}


def validate_packaged_icon(apk, work, prepared):
    """aapt may crunch PNG bytes; compare decoded pixels in every density slot."""
    checked = set()
    with zipfile.ZipFile(apk) as archive:
        for info in archive.infolist():
            parts = Path(info.filename).parts
            if len(parts) != 3 or parts[0] != 'res':
                continue
            canonical = 'res/' + parts[1].split('-v')[0] + '/' + parts[2]
            if canonical not in prepared['resources']:
                continue
            with Image.open(io.BytesIO(archive.read(info))) as packed, Image.open(work / canonical) as wanted:
                assert packed.size == wanted.size and packed.convert('RGBA').tobytes() == wanted.convert('RGBA').tobytes(), ('Packaged icon mismatch', info.filename)
            checked.add(canonical)
    assert checked == set(prepared['resources']), ('Missing launcher variants', set(prepared['resources']) - checked)
    return {**prepared, 'packaged_variants_verified': len(checked)}
