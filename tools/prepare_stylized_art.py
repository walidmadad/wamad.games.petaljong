#!/usr/bin/env python3
"""Normalize CODEX 47 generated originals; no painting or renderer changes.

Usage: python3 tools/prepare_stylized_art.py /tmp/tilebloom47-sources.json
Historical CODEX 44 manifests remain immutable. Requires Pillow.
"""
import hashlib
import io
import json
import re
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/codex47'
RES = ROOT / 'app/src/main/res/drawable-nodpi'
CATALOG = ('leaf lotus fern flower berries sunflower mushroom butterfly bamboo acorn '
           'maple_leaf pinecone iris ginkgo_leaf cherry_blossom dragonfly cactus lavender '
           'ladybug snail veined_leaf double_leaf yellow_daisy').split()

def digest(data):
    return hashlib.sha256(data).hexdigest()

def main():
    sources = json.loads(Path(sys.argv[1]).read_text())
    assert set(sources) == set(CATALOG), 'Require all 23 generated originals'
    assert len(set(sources.values())) == 23
    renderer = (ROOT / 'app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt').read_text()
    mapped = re.findall(r'R.drawable.botanical_(\w+)', renderer)
    assert mapped == CATALOG, 'Stable IDs must still match production resource order'
    enum = (ROOT / 'app/src/main/java/com/tilebloom/game/domain/Game.kt').read_text().split('enum class Status')[0]
    ids = {int(number): name.lower() for name, number in re.findall(r'([A-Z_]+)\((\d+), \d+\)', enum)}
    assert ids == dict(enumerate(CATALOG)), 'Do not change stable symbol IDs'
    resources = set((ROOT/'app/src').rglob('botanical_*'))
    assert resources == {RES/f'botanical_{name}.webp' for name in CATALOG}, 'Unexpected botanical resource override'
    manifest = []
    old_art = {}
    for stable_id, name in enumerate(CATALOG):
        relative = f'app/src/main/res/drawable-nodpi/botanical_{name}.webp'
        before = subprocess.check_output(['git', '--git-dir=.tilebloom-git', '--work-tree=.',
                                          'show', '0c07e2a:' + relative], cwd=ROOT)
        old_art[name] = Image.open(io.BytesIO(before)).convert('RGBA')
        original = Image.open(sources[name]).convert('RGBA')
        # Generated translucent wings may peak at alpha 254 rather than 255.
        # Require clear background and near-opaque ink; never repaint source alpha.
        minimum_alpha, maximum_alpha = original.getchannel('A').getextrema()
        assert minimum_alpha == 0 and maximum_alpha >= 250, name
        # Ignore almost invisible generation fringe when cropping transparent padding,
        # matching the original project's packaging convention. Preserve alpha inside.
        bounds = original.getchannel('A').point(lambda value: 255 if value > 16 else 0).getbbox()
        assert bounds, name
        subject = original.crop(bounds)
        subject.thumbnail((224, 224), Image.Resampling.LANCZOS)
        asset = Image.new('RGBA', (256, 256))
        asset.alpha_composite(subject, ((256-subject.width)//2, (256-subject.height)//2))
        target = RES / f'botanical_{name}.webp'
        asset.save(target, 'WEBP', lossless=True, method=6)
        data = target.read_bytes()
        assert digest(data) != digest(before), f'Unchanged illustration: {name}'
        manifest.append(dict(stableId=stable_id, symbol=name.upper(), resource=target.name,
                             sha256=digest(data), previousSha256=digest(before),
                             generatedSource=str(Path(sources[name])),
                             generatedSourceSha256=digest(Path(sources[name]).read_bytes()),
                             width=256, height=256, bytes=len(data)))
    assert len({e['sha256'] for e in manifest}) == 23
    OUT.mkdir(exist_ok=True)
    (OUT/'art-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    sheet = Image.new('RGB', (1000, 5*245+45), '#193d32')
    pen = ImageDraw.Draw(sheet)
    pen.text((12, 12), 'NEW ART - asset composites, NOT Android screenshots. Small samples: 24 / 32 / 48 px.', fill='#f3dfae')
    for i, name in enumerate(CATALOG):
        x, y = i%5*200, i//5*245+45
        pen.rounded_rectangle((x+8,y+4,x+192,y+166),12,fill='#faf2df')
        art = Image.open(RES/f'botanical_{name}.webp').convert('RGBA')
        sample = art.resize((160,160), Image.Resampling.LANCZOS)
        sheet.paste(sample,(x+20,y+5),sample)
        pen.text((x+8,y+169), f'{i}: {name}',fill='#f3dfae')
        for size, dx in [(24,8),(32,48),(48,96)]:
            sample=art.resize((size,size), Image.Resampling.LANCZOS)
            sheet.paste('#faf2df',(x+dx,y+185,x+dx+size,y+185+size))
            sheet.paste(sample,(x+dx,y+185),sample)
    sheet.save(OUT/'catalog-comparison-new.png')
    trio = Image.new('RGB',(1050,900),'#193d32')
    pen=ImageDraw.Draw(trio)
    for x, title in [(10,'OLD ARTWORK'),(360,'NEW ARTWORK'),(710,'APP ICON REFERENCE')]:
        pen.text((x,12),title,fill='#f3dfae')
    for i,name in enumerate(['leaf','lavender','flower']):
        y=45+i*280
        for x,art in [(10,old_art[name]),(360,Image.open(RES/f'botanical_{name}.webp').convert('RGBA'))]:
            pen.rounded_rectangle((x,y,x+320,y+220),14,fill='#faf2df')
            sample=art.resize((210,210),Image.Resampling.LANCZOS)
            trio.paste(sample,(x+55,y+5),sample)
            for size,dx in [(24,10),(32,55),(48,110)]:
                sample=art.resize((size,size),Image.Resampling.LANCZOS)
                trio.paste('#faf2df',(x+dx,y+225,x+dx+size,y+225+size))
                trio.paste(sample,(x+dx,y+225),sample)
        pen.text((715,y+70),'Reference image unavailable.',fill='#f3dfae')
        pen.text((715,y+90),'User description used;',fill='#f3dfae')
        pen.text((715,y+110),'no invented icon image.',fill='#f3dfae')
    trio.save(OUT/'reference-trio-before-after.png')
    print(f'Replaced all {len(manifest)} production illustrations: {sum(e["bytes"] for e in manifest):,} bytes')

if __name__ == '__main__':
    main()
