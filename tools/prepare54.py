#!/usr/bin/env python3
"""Package generated originals without repainting; preserve alpha and existing artwork."""
from pathlib import Path
import hashlib,json
from PIL import Image,ImageDraw
root=Path.cwd(); out=root/'docs/codex54'; res=root/'app/src/main/res/drawable-nodpi'
sources=json.loads((out/'art-sources.json').read_text())
manifest=[]
for i,(name,source) in enumerate(sources.items(),23):
 original=Image.open(source).convert('RGBA')
 assert original.getchannel('A').getextrema()[0]==0,name
 bounds=original.getchannel('A').point(lambda a:255 if a>16 else 0).getbbox()
 subject=original.crop(bounds);subject.thumbnail((224,224),Image.Resampling.LANCZOS)
 asset=Image.new('RGBA',(256,256));asset.alpha_composite(subject,((256-subject.width)//2,(256-subject.height)//2))
 target=res/f'botanical_{name}.webp';asset.save(target,'WEBP',lossless=True,method=6)
 manifest.append(dict(stableId=i,symbol=name.upper(),resource=target.name,sha256=hashlib.sha256(target.read_bytes()).hexdigest(),sourceSha256=hashlib.sha256(Path(source).read_bytes()).hexdigest(),width=256,height=256,bytes=target.stat().st_size))
assert len(manifest)==12 and len({m['sha256'] for m in manifest})==12
(out/'art-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
# A labeled asset review, never represented as Android runtime evidence.
sheet=Image.new('RGB',(1000,925),'#193d32');pen=ImageDraw.Draw(sheet)
pen.text((12,12),'ASSET REVIEW - NOT ANDROID SCREENSHOTS. Small ink samples: 24 / 32 / 48 px.',fill='#f3dfae')
for i,name in enumerate(sources):
 x=i%4*250;y=i//4*290+40
 art=Image.open(res/f'botanical_{name}.webp').convert('RGBA')
 pen.rounded_rectangle((x+8,y+4,x+242,y+180),12,fill='#faf2df')
 sample=art.resize((170,170),Image.Resampling.LANCZOS);sheet.paste(sample,(x+40,y+6),sample)
 pen.text((x+8,y+185),name,fill='#f3dfae')
 for size,dx in [(24,15),(32,65),(48,125)]:
  sample=art.resize((size,size),Image.Resampling.LANCZOS)
  pen.rounded_rectangle((x+dx-4,y+210,x+dx+size+4,y+210+size+8),3,fill='#faf2df')
  sheet.paste(sample,(x+dx,y+214),sample)
sheet.save(out/'new-symbols-asset-review.png')
groups=[['flower','pink_peony','pink_camellia'],['sunflower','yellow_marigold','golden_chrysanthemum'],['leaf','monstera_leaf','eucalyptus_leaf'],['iris','purple_crocus','purple_bellflower'],['maple_leaf','red_japanese_maple','orange_sugar_maple'],['bamboo','olive_branch','rosemary_branch']]
sheet=Image.new('RGB',(720,1050),'#193d32');pen=ImageDraw.Draw(sheet)
pen.text((10,10),'EXISTING (left) / NEW VARIANTS - asset composite, not Android.',fill='#f3dfae')
for row,group in enumerate(groups):
 for col,name in enumerate(group):
  x=col*240;y=row*165+40
  pen.rounded_rectangle((x+5,y,x+235,y+135),10,fill='#faf2df')
  art=Image.open(res/f'botanical_{name}.webp').convert('RGBA').resize((128,128),Image.Resampling.LANCZOS)
  sheet.paste(art,(x+55,y+3),art);pen.text((x+8,y+140),name,fill='#f3dfae')
sheet.save(out/'similar-symbols-asset-review.png')
print('Packaged 12 unique transparent assets;',sum(m['bytes'] for m in manifest),'bytes. Existing 23 assets unchanged.')
