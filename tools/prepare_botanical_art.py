#!/usr/bin/env python3
"""Package generated transparent originals as bounded Android assets.

Only crops transparent padding and resamples; does not generate or repaint artwork.
Usage: python3 tools/prepare_botanical_art.py docs/codex44/art-sources.local.json
Original paths are local provenance, not runtime dependencies.
"""
import json
import hashlib
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ('leaf lotus fern flower berries sunflower mushroom butterfly bamboo acorn '
           'maple_leaf pinecone iris ginkgo_leaf cherry_blossom dragonfly cactus lavender '
           'ladybug snail veined_leaf double_leaf yellow_daisy').split()

def main():
    sources = json.loads(Path(sys.argv[1]).read_text())
    assert set(sources) == set(CATALOG), 'Every stable symbol must have one distinct asset'
    assert len(set(sources.values())) == 23, 'Source images must be distinct'
    destination = ROOT / 'app/src/main/res/drawable-nodpi'
    manifest = []
    digests = set()
    sheet = Image.new('RGB', (900, 5 * 248 + 40), '#193d32')
    pen = ImageDraw.Draw(sheet)
    pen.text((8, 5), 'BOTANICAL ASSET REVIEW - desktop composites, not Android screenshots', fill='#f3dfae')
    pen.text((8, 21), 'Large artwork + 16 / 24 / 32 / 48 px samples on neutral ivory swatches', fill='#f3dfae')
    for stable_id, name in enumerate(CATALOG):
        original = Image.open(sources[name]).convert('RGBA')
        alpha = original.getchannel('A')
        assert alpha.getextrema()[0] == 0, f'{name}: missing transparency'
        bounds = alpha.point(lambda value: 255 if value > 16 else 0).getbbox()
        assert bounds, f'{name}: empty image'
        subject = original.crop(bounds)
        subject.thumbnail((224, 224), Image.Resampling.LANCZOS)
        asset = Image.new('RGBA', (256, 256))
        asset.alpha_composite(subject, ((256-subject.width)//2, (256-subject.height)//2))
        path = destination / f'botanical_{name}.webp'
        asset.save(path, 'WEBP', lossless=True, method=6)
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        assert digest not in digests, f'{name}: duplicate normalized artwork'
        digests.add(digest)
        assert Image.open(path).size == (256, 256)
        manifest.append({'stableId': stable_id, 'symbol': name.upper(),
                         'sha256': digest,
                         'resource': path.name, 'width': 256, 'height': 256,
                         'bytes': path.stat().st_size})
        # Asset review only: neutral swatch plus real-size 16/24/32/48px samples.
        x, y = stable_id % 5 * 180, 40 + stable_id // 5 * 248
        pen.rounded_rectangle((x+8,y+4,x+172,y+170),12,fill='#faf2df')
        thumb = asset.resize((148,148),Image.Resampling.LANCZOS)
        sheet.paste(thumb,(x+16,y+8),thumb)
        pen.text((x+8,y+176),f'{stable_id}: {name}',fill='#f3dfae')
        for size, dx in [(16,8),(24,32),(32,64),(48,108)]:
            sample = asset.resize((size,size),Image.Resampling.LANCZOS)
            # Keep small samples on ivory for realistic contrast.
            sheet.paste(Image.new('RGB',(size,size),'#faf2df'),(x+dx,y+192))
            sheet.paste(sample,(x+dx,y+192),sample)
    out = ROOT/'docs/codex44'
    (out/'art-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    sheet.save(out/'catalog-asset-review.png')
    print(f'Packaged {len(manifest)} unique assets: {sum(a["bytes"] for a in manifest):,} bytes')

if __name__ == '__main__':
    main()
