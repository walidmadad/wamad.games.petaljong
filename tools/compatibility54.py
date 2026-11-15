#!/usr/bin/env python3
"""Compare frozen v11 with the pinned repository revision in separate JVMs, using stable IDs only."""
from pathlib import Path
import tempfile, subprocess, sys
exec(compile(Path('tools/jvm54.py').read_text(),'tools/jvm54.py','exec'),globals())
out=Path(tempfile.mkdtemp(prefix='petaljong54-compat-'))
old=out/'old'; old.mkdir()
revision=Path('docs/codex54/baseline-revision.txt').read_text().strip()
paths=subprocess.check_output(['git','--git-dir=.tilebloom-git','ls-tree','-r','--name-only',revision],text=True).splitlines()
original=[]
for name in paths:
 if name.endswith('.kt') and ('/domain/' in name or '/presentation/board/' in name) and '/main/' in name:
  target=old/Path(name).name
  target.write_bytes(subprocess.check_output(['git','--git-dir=.tilebloom-git','show',f'{revision}:{name}']))
  original.append(target)
body='''import com.wamad.petaljong.domain.*
fun main(){for(id in listOf(7L,8,12,16,31,75,101,501)){
 val l=GEN.generate(id)
 val text=l.tiles.joinToString(";"){ "${it.id}:${it.symbol.stableId}:${it.x}:${it.y}:${it.layer}:${it.mystery}" }+":"+l.winningSelections.joinToString(",")
 println("$id,${l.seed},${java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString(""){ "%02x".format(it) }}")
}}'''
base=Path(sys.argv[sys.argv.index('--classes')+1]) if '--classes' in sys.argv else None
results=[]
for label,gen,sources in [('head','EndlessGenerator',original),('retained','VersionElevenGenerator',list((ROOT/'app/src/main/java/com/wamad/petaljong/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/wamad/petaljong/presentation/board').glob('*.kt')))]:
 # The pinned historical engine still uses its original namespace.
 source=out/f'{label}.kt'; source.write_text(body.replace('GEN',gen).replace('com.wamad.petaljong', 'com.tilebloom.game') if label == 'head' else body.replace('GEN',gen))
 dest=out/label;dest.mkdir(exist_ok=True)
 use=base if label=='retained' else None
 kotlin([source] if use else sources+[source],dest,':'.join([stdlib,annotations]+([str(use)] if use else [])))
 result=subprocess.check_output(['java','-XX:-UsePerfData','-Xmx512m','-cp',':'.join([str(dest),stdlib]+([str(use)] if use else [])),label.capitalize()+'Kt'],text=True)
 Path(f'docs/codex54/compatibility-{label}.csv').write_text(result)
 results.append(result)
assert results[0]==results[1],'Historical generation changed!'
print('PASS: eight original v11 seeds, stable symbol identities, positions, mysteries and complete witnesses match the pinned baseline exactly.')
