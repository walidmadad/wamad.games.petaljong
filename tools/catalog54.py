#!/usr/bin/env python3
"""Append-only production catalog integration. Existing translations/artwork untouched."""
from pathlib import Path
names='pink_peony pink_camellia purple_crocus purple_bellflower yellow_marigold golden_chrysanthemum monstera_leaf eucalyptus_leaf red_japanese_maple orange_sugar_maple olive_branch rosemary_branch'.split()
labels={
'values':['Pink Peony','Pink Camellia','Purple Crocus','Purple Bellflower','Yellow Marigold','Golden Chrysanthemum','Monstera Leaf','Eucalyptus Leaf','Red Japanese Maple','Orange Sugar Maple','Olive Branch','Rosemary Branch'],
'values-fr':['Pivoine rose','Camélia rose','Crocus violet','Campanule violette','Œillet d’Inde jaune','Chrysanthème doré','Feuille de monstera','Feuille d’eucalyptus','Érable japonais rouge','Érable à sucre orange','Branche d’olivier','Branche de romarin'],
'values-de':['Rosa Pfingstrose','Rosa Kamelie','Violetter Krokus','Violette Glockenblume','Gelbe Tagetes','Goldene Chrysantheme','Monsterablatt','Eukalyptusblatt','Roter Fächerahorn','Oranger Zuckerahorn','Olivenzweig','Rosmarinzweig'],
'values-es':['Peonía rosa','Camelia rosa','Crocus morado','Campanilla morada','Clavel de Indias amarillo','Crisantemo dorado','Hoja de monstera','Hoja de eucalipto','Arce japonés rojo','Arce azucarero naranja','Rama de olivo','Rama de romero'],
'values-pt-rBR':['Peônia rosa','Camélia rosa','Açafrão roxo','Campânula roxa','Cravo-de-defunto amarelo','Crisântemo dourado','Folha de costela-de-adão','Folha de eucalipto','Bordo japonês vermelho','Bordo-açucareiro laranja','Ramo de oliveira','Ramo de alecrim'],
'values-ar':['فاوانيا وردية','كاميليا وردية','زعفران بنفسجي','زهرة الجرس البنفسجية','قطيفة صفراء','أقحوان ذهبي','ورقة مونستيرا','ورقة كينا','قيقب ياباني أحمر','قيقب سكري برتقالي','غصن زيتون','غصن إكليل الجبل'],
'values-hi':['गुलाबी पियोनी','गुलाबी कैमेलिया','बैंगनी क्रोकस','बैंगनी बेलफ्लावर','पीला गेंदा','सुनहरा गुलदाउदी','मॉन्स्टेरा पत्ता','नीलगिरी पत्ता','लाल जापानी मेपल','नारंगी शुगर मेपल','जैतून की शाखा','रोज़मेरी की शाखा'],
'values-ja':['ピンクのシャクヤク','ピンクのツバキ','紫のクロッカス','紫のツリガネソウ','黄色のマリーゴールド','金色のキク','モンステラの葉','ユーカリの葉','赤いイロハモミジ','オレンジのサトウカエデ','オリーブの枝','ローズマリーの枝']}
for folder,words in labels.items():
 p=Path('app/src/main/res')/folder/'strings.xml'
 s=p.read_text()
 if 'name="symbol_pink_peony"' not in s:
  s=s.replace('</resources>',''.join(f'    <string name="symbol_{name}">{word}</string>\n' for name,word in zip(names,words))+'</resources>')
  p.write_text(s)
p=Path('app/src/main/java/com/wamad/petaljong/presentation/MahjongBoardView.kt');s=p.read_text()
if 'Symbol.PINK_PEONY -> R.string' not in s:
 s=s.replace('        Symbol.YELLOW_DAISY -> R.string.symbol_yellow_daisy','        Symbol.YELLOW_DAISY -> R.string.symbol_yellow_daisy\n'+ '\n'.join(f'        Symbol.{n.upper()} -> R.string.symbol_{n}' for n in names))
 s=s.replace('// Bounded process cache: 23 x 256 x 256 ARGB pixels = at most 5.75 MiB.','// Bounded process cache: 35 x 256 x 256 ARGB pixels = at most 8.75 MiB.')
 s=s.replace('arrayOfNulls<Bitmap>(23)','arrayOfNulls<Bitmap>(Symbol.entries.size)').replace('arrayOfNulls<Rect>(23)','arrayOfNulls<Rect>(Symbol.entries.size)').replace('arrayOfNulls<RectF>(23)','arrayOfNulls<RectF>(Symbol.entries.size)')
 s=s.replace('R.drawable.botanical_double_leaf, R.drawable.botanical_yellow_daisy','R.drawable.botanical_double_leaf, R.drawable.botanical_yellow_daisy,\n        '+',\n        '.join(f'R.drawable.botanical_{n}' for n in names))
 s=s.replace('Symbol.LAVENDER, Symbol.FERN, Symbol.BAMBOO ->','Symbol.LAVENDER, Symbol.FERN, Symbol.BAMBOO, Symbol.OLIVE_BRANCH, Symbol.ROSEMARY_BRANCH, Symbol.PURPLE_BELLFLOWER ->')
 p.write_text(s)
