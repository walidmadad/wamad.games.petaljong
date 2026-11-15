#!/usr/bin/env python3
"""Audit packaged ink bounds and proportional Canvas fit; no Android visual claims.
Requires Pillow. Writes numerical evidence only; never alters art assets.
An optional manifest path selects an older catalog. "old" dimensions below compare
the former fitting formula on the current image, not the old illustration itself.
"""
import json
import hashlib
import re
import sys
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/com/wamad/petaljong/presentation/MahjongBoardView.kt').read_text()
section = source.split('private fun opticalSize')[1].split('private val artworkPaint')[0]
limits = {}
for line in section.splitlines():
    match = re.search(r'-> ([\d.]+)f to ([\d.]+)f', line)
    if match:
        pair = tuple(map(float, match.groups()))
        for symbol in re.findall(r'Symbol\.([A-Z_]+)', line):
            limits[symbol] = pair
        if 'else' in line:
            fallback = pair
manifest_path = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'docs/codex47/art-manifest.json'
manifest = json.loads(manifest_path.read_text())
assert sorted(entry['stableId'] for entry in manifest) == list(range(23))
assert len({entry['resource'] for entry in manifest}) == 23
results = []
for entry in manifest:
    path = root / 'app/src/main/res/drawable-nodpi' / entry['resource']
    assert hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256'], entry['symbol']
    image = Image.open(path).convert('RGBA')
    assert image.size == (256, 256), entry['symbol']
    bounds = image.getchannel('A').getbbox()
    assert bounds, entry['symbol']
    w, h = bounds[2] - bounds[0], bounds[3] - bounds[1]
    mw, mh = limits.get(entry['symbol'], fallback)
    scale = min(mw / w, mh / h)
    new = (w * scale * .34, h * scale * .34)
    old = (w / image.width * 2.24 * .34, h / image.height * 2.24 * .34)
    assert new[0] <= .82 and new[1] <= 1.01
    assert abs(new[0] / new[1] - w / h) < 1e-9
    assert scale > 2.24 / image.width, entry['symbol']
    results.append(dict(symbol=entry['symbol'], stableId=entry['stableId'], alphaBounds=bounds,
                        oldInkTileUnits=old, newInkTileUnits=new,
                        linearIncreasePercent=(scale / (2.24 / image.width) - 1) * 100))
print(json.dumps(results, indent=2))
