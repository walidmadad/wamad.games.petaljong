#!/usr/bin/env python3
"""Original pinned v11 engine vs final v12 engine, three-repeat interleaved JVM timing.
Original sources are compiled under a diagnostic-only package so both engines coexist.
No baseline implementation is changed; only package/import prefixes are relocated.
"""
from pathlib import Path
import tempfile,sys,subprocess
exec(compile(Path('tools/jvm54.py').read_text(),'tools/jvm54.py','exec'),globals())
out=Path(tempfile.mkdtemp(prefix='petaljong54-performance-'))
revision=Path('docs/codex54/baseline-revision.txt').read_text().strip()
base=Path(sys.argv[sys.argv.index('--classes')+1]) if '--classes' in sys.argv else None
paths=subprocess.check_output(['git','--git-dir=.tilebloom-git','ls-tree','-r','--name-only',revision],text=True).splitlines()
original=[]
old=out/'original';old.mkdir()
for name in paths:
 if name.endswith('.kt') and '/main/' in name and ('/domain/' in name or '/presentation/board/' in name):
  target=old/Path(name).name
  text=subprocess.check_output(['git','--git-dir=.tilebloom-git','show',f'{revision}:{name}'],text=True)
  target.write_text(text.replace('com.tilebloom.game','com.tilebloom.baseline'))
  original.append(target)
source=out/'Performance.kt'
source.write_text('''import com.tilebloom.game.domain.EndlessGenerator as NewGenerator
import com.tilebloom.game.domain.LevelValidator as NewValidator
import com.tilebloom.baseline.domain.EndlessGenerator as OldGenerator
import com.tilebloom.baseline.domain.LevelValidator as OldValidator
fun main(){
 OldGenerator.generate(8);NewGenerator.generate(8)
 println("level,version,run,candidates,generationMs,fingerprint")
 for(id in listOf(8L,16,31,75,150,501,1001)) for(run in 0..2) for(old in if(run%2==0) listOf(true,false) else listOf(false,true)) {
 val start=System.nanoTime()
 if(old){
 val l=OldGenerator.generate(id);val ms=(System.nanoTime()-start)/1e6;OldValidator.validate(l)
 val fingerprint=l.tiles.joinToString(";"){ "${it.id}:${it.symbol.stableId}:${it.x}:${it.y}:${it.layer}:${it.mystery}" }.hashCode()
 println("$id,${l.version},$run,${l.candidatesEvaluated},$ms,$fingerprint")
 }else{
 val l=NewGenerator.generate(id);val ms=(System.nanoTime()-start)/1e6;NewValidator.validate(l)
 val fingerprint=l.tiles.joinToString(";"){ "${it.id}:${it.symbol.stableId}:${it.x}:${it.y}:${it.layer}:${it.mystery}" }.hashCode()
 println("$id,${l.version},$run,${l.candidatesEvaluated},$ms,$fingerprint")
 }
 }
}''')
sources=original+[source]
if not base:sources+=list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))
kotlin(sources,out,':'.join([stdlib,annotations]+([str(base)] if base else [])))
run('java','-cp',':'.join([str(out),stdlib]+([str(base)] if base else [])),'PerformanceKt')
