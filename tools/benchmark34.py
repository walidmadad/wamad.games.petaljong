#!/usr/bin/env python3
"""Same JVM, levels and players for frozen v8 and current v9; CSV on stdout."""
from pathlib import Path
import sys, tempfile
prefix=Path('tools/benchmark32.py').read_text().split("out=Path(")[0]
exec(prefix)
out=Path(tempfile.mkdtemp(prefix='tilebloom34-benchmark-'))
source=out/'Benchmark.kt'
source.write_text('''import com.tilebloom.game.domain.*
fun variant(l: LevelDefinition, salt: Int): LevelDefinition {
 val ids=l.tiles.map { it.id }.toMutableList()
 java.util.Collections.shuffle(ids,java.util.Random(34000L+salt))
 val map=l.tiles.map { it.id }.zip(ids).toMap()
 return l.copy(tiles=l.tiles.map { it.copy(id=map.getValue(it.id)) },
 layout=l.layout.copy(slots=l.layout.slots.map { it.copy(id=map.getValue(it.id)) }),
 witness=l.witness.map { PairMove(map.getValue(it.first),map.getValue(it.second)) },
 selectionWitness=l.selectionWitness?.map { map.getValue(it) })
}
fun main() {
 // Warm both paths before timing. IDs 1–5 share unchanged cached introductory definitions.
 for(id in 1L..5L) Levels.get(id)
 VersionEightGenerator.generate(8); EndlessGenerator.generate(8)
 println("version,level,tier,tiles,family,layers,legal,pairs,blocked,overlaps,blockingDepth,pressure,peak,delayed,maxDelay,greedy,simple,lookahead,nodes,target,witness,ms,holdoutRuns,holdoutGreedy,holdoutSimple,holdoutLookahead,selectionProbes,selectionGreedyWins,selectionSimpleWins,oneHeld,twoHeld,threeHeld,candidates,noveltyLimited,analysisMs,safe,losing,unknown,searchNodes,depth1Wins")
 for (id in listOf(1L,2,3,4,5,6,7,8,12,15,16,23,30,31,40,50,51,75,100,101,501,1001)) for(v in listOf(8,9)) {
 val start=System.nanoTime()
 val l=if(id<6) Levels.get(id) else if(v==8) VersionEightGenerator.generate(id) else EndlessGenerator.generate(id)
 val ms=(System.nanoTime()-start)/1000000.0
 val analysisStart=System.nanoTime()
 val a=PuzzleAnalyzer.strategic(l)
 val analysisMs=(System.nanoTime()-analysisStart)/1000000.0
 LevelValidator.validate(l)
 val holdouts=if(id>=16) (1..4).map { variant(l,it).also(LevelValidator::validate) } else emptyList()
 val gw=holdouts.count { SimulatedPlayer.play(it,SimulatedPlayer.Strategy.GREEDY).won }
 val sw=holdouts.count { SimulatedPlayer.play(it,SimulatedPlayer.Strategy.SIMPLE).won }
 val lw=holdouts.count { SimulatedPlayer.play(it,SimulatedPlayer.Strategy.LOOKAHEAD).won }
 var safe=0; var losing=0; var unknown=0; var nodes=0
 val replay=Game(l.tiles)
 l.winningSelections.forEachIndexed { index,id ->
  if(index%6==0) {
   val r=PuzzleAnalyzer.analyzeTray(l.tiles,replay.snapshot(),1000-nodes)
   nodes+=r.result.nodes; safe+=r.safeContinuations; losing+=r.losingContinuations; unknown+=r.unknownContinuations
  }
  check(replay.select(id))
 }
 val depth1=SimulatedPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,depth=1).won
 println("$v,$id,${DifficultyPolicy.difficulty(id)},${l.tiles.size},${l.layout.id},${l.tiles.map{it.layer}.distinct().size},${a.initialLegal},${a.immediatePairs},${a.blocked},${a.coverRelationships},${a.blockingDepth},${a.witnessPressure},${a.witnessPeak},${a.delayedMatches},${a.longestPartnerDelay},${a.greedy.won},${a.simple.won},${a.lookahead.won},${a.lookahead.nodes},${l.qualityTargetMet},true,$ms,${holdouts.size},$gw,$sw,$lw,${a.probes},${a.greedyProbeWins},${a.simpleProbeWins},${a.witnessOneHeld},${a.witnessTwoHeld},${a.witnessThreeHeld},${l.candidatesEvaluated},${l.noveltyLimited},$analysisMs,$safe,$losing,$unknown,$nodes,$depth1")
 }
}
''')
sources=list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))
sources += list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))
kotlin(sources+[source],out,':'.join([stdlib,annotations]))
run('java','-XX:-UsePerfData','-Xmx512m','-cp',f'{out}:{stdlib}','BenchmarkKt')
