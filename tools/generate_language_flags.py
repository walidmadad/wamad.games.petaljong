# Original compact UI flag artwork. Requires Pillow with RAQM and Noto Naskh Arabic.
# Representative decorations only; the locale mapping lives in AppLanguage.
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import math
out=Path('app/src/main/res/drawable-nodpi');out.mkdir(exist_ok=True)
W,H=504,336
for tag in ['en','fr','es','pt_br','ar','hi','ja','de','automatic']:
 im=Image.new('RGB',(W,H),'white');d=ImageDraw.Draw(im)
 def rect(box,c): d.rectangle(box,fill=c)
 if tag=='en':
  rect((0,0,W,H),'#012169')
  for points in [[(0,0),(W,H)],[(W,0),(0,H)]]: d.line(points,fill='white',width=68)
  for points in [[(0,0),(W,H)],[(W,0),(0,H)]]: d.line(points,fill='#c8102e',width=23)
  rect((205,0,299,H),'white');rect((0,120,W,216),'white');rect((224,0,280,H),'#c8102e');rect((0,140,W,196),'#c8102e')
 elif tag=='fr': rect((0,0,167,H),'#002654');rect((336,0,W,H),'#ed2939')
 elif tag=='de':
  for y,c in [(0,'#000000'),(112,'#dd0000'),(224,'#ffce00')]: rect((0,y,W,y+112),c)
 elif tag=='es':
  rect((0,0,W,84),'#aa151b');rect((0,84,W,252),'#f1bf00');rect((0,252,W,H),'#aa151b')
  # Compact heraldic shield, crown and pillars retain the flag's identity at UI scale.
  rect((128,139,179,199),'#aa151b');rect((153,143,175,165),'white');rect((131,169,151,192),'white')
  d.polygon([(128,199),(179,199),(154,215)],fill='#aa151b');rect((126,130,181,139),'#aa151b')
  for x in [111,192]: rect((x,145,x+6,201),'white');rect((x-4,200,x+10,205),'#aa151b')
 elif tag=='pt_br':
  rect((0,0,W,H),'#009739');d.polygon([(252,35),(470,168),(252,301),(34,168)],fill='#ffdf00');d.ellipse((159,75,345,261),fill='#012169')
  d.arc((120,147,382,330),190,340,fill='white',width=13)
  for x,y in [(222,119),(274,147),(203,185),(248,213),(290,193),(267,235),(312,181)]: d.ellipse((x-2,y-2,x+2,y+2),fill='white')
 elif tag=='ar':
  rect((0,0,W,H),'#006c35')
  font=ImageFont.truetype('/usr/share/fonts/truetype/noto/NotoNaskhArabic-Bold.ttf',45)
  d.text((252,115),'لا إله إلا الله محمد رسول الله',font=font,fill='white',anchor='mm',direction='rtl')
  d.polygon([(106,215),(386,215),(386,224),(133,224),(106,220)],fill='white');rect((374,207,382,237),'white');rect((382,226,411,234),'white')
 elif tag=='hi':
  rect((0,0,W,112),'#ff9933');rect((0,224,W,H),'#138808');d.ellipse((203,119,301,217),outline='#000080',width=5)
  for n in range(24):
   a=n*math.pi/12;d.line((252,168,252+47*math.cos(a),168+47*math.sin(a)),fill='#000080',width=2)
  d.ellipse((246,162,258,174),fill='#000080')
 elif tag=='ja': d.ellipse((151,67,353,269),fill='#bc002d')
 else:
  rect((0,0,W,H),'#173f33');d.ellipse((110,26,394,310),outline='#e8cf91',width=13);d.ellipse((185,26,319,310),outline='#e8cf91',width=9);d.line((110,168,394,168),fill='#e8cf91',width=10);d.arc((110,91,394,245),0,360,fill='#e8cf91',width=8)
 im.save(out/f'flag_{tag}.png',optimize=True)
