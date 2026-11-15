#!/usr/bin/env python3
"""Bounded deterministic v10 portfolio; actual JVM timings, never device timings."""
from pathlib import Path
import tempfile
prefix=Path('tools/benchmark32.py').read_text().split("out=Path(")[0]
exec(prefix)
out=Path(tempfile.mkdtemp(prefix='tilebloom35-benchmark-'))
source=out/'Benchmark.kt'
source.write_text('''import com.wamad.petaljong.domain.*
fun main() {
 Levels.get(1); EndlessGenerator.generate(8)
 println("level,tier,layout,tiles,layers,legal,immediatePairs,blocked,overlaps,depth,pressure,delayed,mixedFamilies,mysteries,greedy,simple,lookahead,nodes,target,witness,candidates,generationMs,targetDelayed,targetPressure,targetDepth")
 for(id in listOf(1L,7,8,12,15,16,23,30,31,40,50,51,63,75,76,88,100,101,125,150,151,501,1001)) {
  val start=System.nanoTime(); val l=if(id<6) Levels.get(id) else EndlessGenerator.generate(id)
  val ms=(System.nanoTime()-start)/1e6
  LevelValidator.validate(l)
  val a=PuzzleAnalyzer.strategic(l); val t=ExpertProgression.targets(id)
  println("$id,${DifficultyPolicy.difficulty(id)},${l.layout.id},${l.tiles.size},${l.tiles.map{it.layer}.distinct().size},${a.initialLegal},${a.immediatePairs},${a.blocked},${a.coverRelationships},${a.blockingDepth},${a.witnessPressure},${a.delayedMatches},${SymbolFamilies.mixedCount(l.tiles)},${l.tiles.count{it.mystery}},${a.greedy.won},${a.simple.won},${a.lookahead.won},${a.lookahead.nodes},${l.qualityTargetMet},true,${l.candidatesEvaluated},$ms,${t.delayed},${t.pressure},${t.depth}")
 }
}
''')
sources=list((ROOT/'app/src/main/java/com/wamad/petaljong/domain').glob('*.kt'))
sources+=list((ROOT/'app/src/main/java/com/wamad/petaljong/presentation/board').glob('*.kt'))
kotlin(sources+[source],out,':'.join([stdlib,annotations]))
run('java','-XX:-UsePerfData','-Xmx512m','-cp',f'{out}:{stdlib}','BenchmarkKt')
