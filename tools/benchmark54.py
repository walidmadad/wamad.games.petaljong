#!/usr/bin/env python3
"""Reproducible old/new production-controller portfolio; JVM times, not Android times."""
from pathlib import Path
import tempfile, sys
exec(compile(Path('tools/jvm54.py').read_text(), 'tools/jvm54.py', 'exec'), globals())
out=Path(tempfile.mkdtemp(prefix='petaljong54-'))
source=out/'Benchmark.kt'
source.write_text('''import com.wamad.petaljong.domain.*
fun main(args:Array<String>) {
 val modes=if(args.contains("--comparison")) listOf(true,false) else listOf(args.contains("--old"))
 Levels.get(1); VersionElevenGenerator.generate(8); EndlessGenerator.generate(8)
 println("level,version,tier,tiles,layout,symbols,mixedFamilies,mysteries,legal,pairs,blocked,overlaps,depth,pressure,threeHeld,delayed,greedy,simple,lookahead,greedyProbeWins,simpleProbeWins,target,witness,candidates,generationMs,fingerprint,heldoutGreedyWins,heldoutShortWins,heldoutStrongWins,traps,consequential,pairOnly,initialVisiblePairs")
 for(id in listOf(1L,7,8,12,15,16,23,30,31,40,50,51,75,100,101,150,200,201,501,1001)) for(old in modes) {
 val start=System.nanoTime()
 val l=if(id<6) Levels.get(id) else if(old) VersionElevenGenerator.generate(id) else EndlessGenerator.generate(id)
 val ms=(System.nanoTime()-start)/1e6
 LevelValidator.validate(l)
 val a=StrategicAnalyzer.analyze(l.copy(version=12))
 val greedy=(10..21).count { PlanningPlayer.play(l,SimulatedPlayer.Strategy.GREEDY,tieBreak=it).won }
 val short=(10..13).count { PlanningPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,600,3,it).won }
 val strong=(10..13).count { PlanningPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,1800,6,it).won }
 val fingerprint=l.tiles.joinToString(";"){ "${it.id}:${it.symbol.stableId}:${it.x}:${it.y}:${it.layer}:${it.mystery}" }.hashCode()
 println("$id,${l.version},${DifficultyPolicy.difficulty(id)},${l.tiles.size},${l.layout.id},${l.tiles.map{it.symbol}.distinct().size},${SymbolFamilies.mixedCount(l.tiles)},${l.tiles.count{it.mystery}},${a.initialLegal},${a.immediatePairs},${a.blocked},${a.coverRelationships},${a.blockingDepth},${a.witnessPressure},${a.witnessThreeHeld},${a.delayedMatches},${a.greedy.won},${a.simple.won},${a.lookahead.won},${a.greedyProbeWins},${a.simpleProbeWins},${l.qualityTargetMet},true,${l.candidatesEvaluated},$ms,${fingerprint},$greedy,$short,$strong,${a.provenImmediateTraps},${a.consequentialTrayChoices},${a.pairOnlyOutcome},${Game(l.tiles).matchingPairs().size}")
 }
}
''')
sources=list((ROOT/'app/src/main/java/com/wamad/petaljong/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/wamad/petaljong/presentation/board').glob('*.kt'))
base=Path(sys.argv[sys.argv.index('--classes')+1]) if '--classes' in sys.argv else None
kotlin([source] if base else sources+[source],out,':'.join([stdlib,annotations]+([str(base)] if base else [])))
run('java','-XX:-UsePerfData','-Xmx512m','-cp',':'.join([str(out),stdlib]+([str(base)] if base else [])),'BenchmarkKt',*sys.argv[1:])
