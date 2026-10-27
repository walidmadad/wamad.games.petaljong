package com.tilebloom.game
import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test
class PuzzleAnalyzerTest {
    @Test fun oldSnapshotRejected() { val s=GardenSession(); assertFalse(s.restore(s.snapshot().copy(version=0))); assertTrue(s.game.snapshot().moves.isEmpty()) }
    @Test fun forcedAndMultiple() { val a=PuzzleAnalyzer.analyze(Levels.get(1)); assertTrue(a.forced>0); assertTrue(a.multiple>0); assertTrue(a.consequenceChoiceStates>0); assertEquals(a.branching.size,a.forced+a.multiple) }
    @Test fun bounded() { val a=PuzzleAnalyzer.analyze(Levels.get(3),3); assertTrue(a.searchNodes<=3); assertTrue(a.unknownAlternatives>0) }
    @Test fun audit() {
        fun randomWins(l: LevelDefinition): Int = (0L..31L).count { seed ->
            val r=java.util.Random(seed); val g=Game(l.tiles)
            while(g.status==Status.PLAYING) { val moves=g.matchingPairs(); val p=moves[r.nextInt(moves.size)]; check(g.match(p.first,p.second)) }
            g.status==Status.WON
        }
        (1..5).forEach { id ->
            val current=Levels.get(id)
            val old=if(id==1) LevelDefinition(1,0,0,Layouts.original,LevelOne.tiles(),LevelOne.witness,0)
                else LevelGenerator.generate(id,id*101L,current.layout,1)
            println("ORIGINAL $id ${PuzzleAnalyzer.analyze(old)} randomWins=${randomWins(old)}/32")
            println("UPDATED $id ${PuzzleAnalyzer.analyze(current)} randomWins=${randomWins(current)}/32 candidates=${current.candidatesEvaluated} target=${current.qualityTargetMet}")
        }
        println("DEADLOCK ${deadlockPath(Game(Levels.get(1).tiles))}")
    }
    @Test fun qualityMatrix() {
        var generation=0L; var analysis=0L; var attempts=0; var accepted=0; var exhausted=0; var candidateExhausted=0
        for(layout in listOf(Layouts.original,Layouts.terrace,Layouts.woodland,Layouts.courtyard)) for(seed in 0L..3L) {
            val start=System.nanoTime(); val l=LevelGenerator.generate(3,seed,layout)
            generation+=System.nanoTime()-start; val t=System.nanoTime(); val a=PuzzleAnalyzer.analyze(l,1000)
            analysis+=System.nanoTime()-t; LevelValidator.validate(l)
            assertTrue(l.candidatesEvaluated in 1..6); assertTrue(a.searchNodes<=1000)
            candidateExhausted+=l.candidateQuality.count { it.unknownAlternatives>0 }; attempts+=l.candidatesEvaluated; if(l.qualityTargetMet) accepted++; if(!a.complete) exhausted++
            println("MATRIX ${layout.id}/$seed choices=${a.consequenceChoiceStates} risk=${a.riskyChoiceStates} target=${l.qualityTargetMet} candidates=${l.candidatesEvaluated} unknown=${a.unknownAlternatives}")
        }
        println("MATRIX SUMMARY boards=16 candidates=$attempts acceptedProfiles=$accepted fallback=${16-accepted} discardedCandidates=${attempts-16} searchExhausted=$exhausted candidateSearchExhausted=$candidateExhausted generationMs=${generation/1000000} analysisMs=${analysis/1000000}")
    }
    @Test fun levelOneFixture() {
        val l=Levels.get(1); val a=PuzzleAnalyzer.analyze(l)
        assertEquals(2,a.initialPairs); assertEquals(1,a.riskyChoiceStates)
        assertEquals(List(6){4},l.tiles.groupingBy { it.symbol }.eachCount().values.toList())
        val game=Game(l.tiles); listOf(16 to 20,0 to 4,8 to 12,17 to 21,1 to 5,9 to 13,15 to 18,19 to 23).forEach { assertTrue(game.match(it.first,it.second)) }
        assertEquals(Status.NO_MOVES,game.status)
    }
    @Test fun emptyAnalysis() { val a=PuzzleAnalyzer.analyze(LevelDefinition(0,0,2,BoardLayout("empty",emptyList()),emptyList(),emptyList(),0)); assertEquals(0,a.tileCount); assertTrue(a.complete) }
    @Test fun fallbackBounded() { val l=LevelGenerator.generateWithProfile(2,42,Layouts.terrace,qualityAttempts=2,minimumChoices=100); assertFalse(l.qualityTargetMet); assertEquals(2,l.candidatesEvaluated); LevelValidator.validate(l) }
    @Test fun empty() { assertEquals(Solvability.SOLVABLE, PuzzleSolver.solve(emptyList(),0).outcome) }
    @Test fun budget() { assertEquals(Solvability.UNKNOWN_BUDGET_EXCEEDED, PuzzleSolver.solve(Levels.get(1).tiles,0).outcome) }
    @Test fun deadlock() { assertEquals(Solvability.UNSOLVABLE, PuzzleSolver.solve(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.FERN,2.0,0.0,0))).outcome) }
    @Test fun deterministicAndImmutable() { val l=Levels.get(1); val before=l.copy(); assertEquals(PuzzleAnalyzer.analyze(l),PuzzleAnalyzer.analyze(l)); assertEquals(before,l); assertEquals(Solvability.SOLVABLE,PuzzleSolver.solve(l.tiles).outcome) }
    @Test fun invalid() { val l=Levels.get(1); try { PuzzleAnalyzer.analyze(l.copy(witness=emptyList())); fail() } catch (_: IllegalArgumentException) {} }
}

fun deadlockPath(game: Game): List<PairMove> {
    val seen=mutableSetOf<Set<Int>>()
    var nodes=0
    fun find(tiles: List<Tile>): List<PairMove>? {
        if(nodes++ >= 20000 || !seen.add(tiles.map { it.id }.toSet())) return null
        val g=Game(tiles)
        if(g.status==Status.NO_MOVES) return emptyList()
        for(p in g.matchingPairs()) {
            val tail=find(tiles.filter { it.id!=p.first && it.id!=p.second })
            if(tail!=null) return listOf(p)+tail
        }
        return null
    }
    return checkNotNull(find(game.remaining))
}
