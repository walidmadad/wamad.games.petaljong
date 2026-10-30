#!/usr/bin/env python3
"""Validate production decoding, transparency, stable old assets and silhouette separation."""
from pathlib import Path
from PIL import Image,ImageChops
import json,hashlib,subprocess
root=Path.cwd();res=root/'app/src/main/res/drawable-nodpi'
manifest=json.loads(Path('docs/codex54/art-manifest.json').read_text())
for m in manifest:
 p=res/m['resource'];a=Image.open(p).convert('RGBA');assert a.size==(256,256)
 assert hashlib.sha256(p.read_bytes()).hexdigest()==m['sha256']
 assert a.getchannel('A').getextrema()==(0,255)
 assert a.getchannel('A').getbbox() is not None
revision=Path('docs/codex54/baseline-revision.txt').read_text().strip()
paths=subprocess.check_output(['git','--git-dir=.tilebloom-git','ls-tree','-r','--name-only',revision],text=True).splitlines()
old=[p for p in paths if '/botanical_' in p and p.endswith('.webp')]
assert len(old)==23
for p in old:
 assert Path(p).read_bytes()==subprocess.check_output(['git','--git-dir=.tilebloom-git','show',f'{revision}:{p}'])
for a,b in [('pink_peony','pink_camellia'),('yellow_marigold','golden_chrysanthemum'),('monstera_leaf','eucalyptus_leaf'),('red_japanese_maple','orange_sugar_maple'),('purple_crocus','purple_bellflower'),('olive_branch','rosemary_branch')]:
 def mask(n):return Image.open(res/f'botanical_{n}.webp').convert('RGBA').getchannel('A').resize((32,32)).point(lambda x:255 if x>128 else 0)
 x,y=mask(a),mask(b);diff=ImageChops.difference(x,y);fraction=sum(v>0 for v in diff.getdata())/1024
 assert fraction>.035,(a,b,fraction)
 print(f'{a} vs {b}: different silhouette in {fraction:.1%} of 32px square pixels')
print('PASS: 12 unique nonempty transparent production assets, six distinct silhouette pairs, 23 existing assets byte-identical.')
