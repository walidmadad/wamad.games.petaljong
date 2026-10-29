package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class StrategicDifficultyTest {
    companion object {
        private val expert by lazy { Levels.get(75) }
    }
    @Test fun exactBoundariesAreStable() {
        listOf(1L,7,8,15,16,30,31,1001).zip(listOf(0,0,1,1,2,2,3,3)).forEach { (id,tier) ->
            assertEquals(tier, DifficultyPolicy.difficulty(id).ordinal)
        }
    }
    @Test fun expertWitnessUsesIndividualProductionActionsWithoutOverflow() {
        LevelValidator.validate(expert)
        val g = Game(expert.tiles)
        var peak = 0
        expert.winningSelections.forEach { id ->
            assertTrue(g.isAvailable(id)); assertTrue(g.collectRevealed(id)); assertNotEquals(Status.LOST,g.status)
            peak = maxOf(peak,g.tray.size)
        }
        assertEquals(Status.WON,g.status); assertEquals(3,peak)
        assertEquals(expert.tiles.size,expert.winningSelections.distinct().size)
        assertTrue(expert.tiles.groupingBy { it.symbol }.eachCount().values.all { it % 2 == 0 })
    }
    @Test fun playersAreDeterministicAndRespectBudgets() {
        for (strategy in SimulatedPlayer.Strategy.entries) {
            val a=SimulatedPlayer.play(expert,strategy,200)
            assertEquals(a,SimulatedPlayer.play(expert,strategy,200))
            assertTrue(a.nodes <= 200); assertTrue(a.actions.size <= expert.tiles.size)
        }
        assertEquals(0,SimulatedPlayer.play(expert,SimulatedPlayer.Strategy.LOOKAHEAD,0).nodes)
    }
    @Test fun timeoutIsUnknownAndNeverAnUnsolvableProof() {
        val g=Game(expert.tiles)
        val a=PuzzleAnalyzer.analyzeTray(expert.tiles,g.snapshot(),0)
        assertEquals(Solvability.UNKNOWN_BUDGET_EXCEEDED,a.result.outcome)
        assertEquals(0,a.result.nodes); assertEquals(0,a.losingContinuations)
        assertEquals(a.available,a.unknownContinuations)
        val b=PuzzleAnalyzer.analyze(expert,25)
        assertTrue(b.searchNodes<=25); assertTrue(b.unknownAlternatives>0); assertFalse(b.complete)
    }
    @Test fun scoringRejectsTrivialProfilesAndSelectsMeasuredBest() {
        val old=VersionEightGenerator.generate(75)
        val trivial=PuzzleAnalyzer.strategic(old)
        assertEquals(0,trivial.delayedMatches); assertFalse(trivial.meets(Difficulty.EXPERT))
        val a=expert.strategicAnalysis!!
        assertEquals(expert.qualityTargetMet,a.meets(Difficulty.EXPERT) && ExpertProgression.targets(expert.id).accepts(a))
        assertTrue(a.score(Difficulty.EXPERT)>trivial.score(Difficulty.EXPERT))
        assertEquals(expert.candidateQuality.maxOf { it.strategic!!.let { a -> a.score(Difficulty.EXPERT) + ExpertProgression.targets(expert.id).score(a) } },a.score(Difficulty.EXPERT) + ExpertProgression.targets(expert.id).score(a))
        assertTrue(expert.candidatesEvaluated in 2..EndlessGenerator.CANDIDATES)
        assertTrue(expert.candidateQuality.all { it.searchNodes<=1800 })
    }
    @Test fun lookaheadImprovesOnShallowPlayersOnFixedExpertSample() {
        val a=expert.strategicAnalysis!!
        assertFalse(a.greedy.won); assertFalse(a.simple.won); assertTrue(a.lookahead.won)
        assertTrue(a.delayedMatches>=3); assertEquals(3,a.witnessPeak)
    }
    @Test fun forkAndUndoRestoreExactTrayAndHistory() {
        val g=Game(expert.tiles)
        expert.winningSelections.dropLast(1).forEach { id ->
            if (g.remaining.any { it.id == id && it.concealed }) assertTrue(g.reveal(id))
            val before=g.snapshot(); val tiles=g.tiles; val tray=g.tray
            val branch=g.fork(); assertTrue(branch.collectRevealed(id)); assertEquals(before,g.snapshot())
            assertTrue(g.collectRevealed(id)); assertEquals(branch.snapshot(),g.snapshot())
            assertTrue(g.undo())
            assertEquals(before.moves,g.snapshot().moves); assertEquals(before.selections,g.snapshot().selections)
            assertEquals(tiles.map { if (it.mystery) it.copy(revealed=false) else it },g.tiles)
            assertEquals(tray,g.tray); assertNull(g.activeMysteryId)
            assertTrue(g.collectRevealed(id))
        }
    }
    @Test fun hintsStaySelectableOnDelayedWitnessStates() {
        val g=Game(expert.tiles)
        expert.winningSelections.take(12).forEach { id ->
            val hint=HintPolicy.suggest(expert.tiles,g.snapshot(),40)
            hint.id?.let { assertTrue(g.isAvailable(it)) }
            assertTrue(g.collectRevealed(id))
        }
    }
    @Test fun v8SavePreservesBoardTrayBalancesAndAdState() {
        val old=VersionEightGenerator.generate(75)
        val g=Game(old.tiles); assertTrue(g.collectRevealed(old.winningSelections.first()))
        val saved=PlayerProgress(active=SessionSnapshot(75,old.seed,8,g.snapshot()),highestCompleted=74,undoCredits=2,hintCredits=1)
        val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
        assertFalse(restored.recovered); assertEquals(old,restored.session.level)
        assertEquals(g.snapshot(),restored.session.game.snapshot()); assertEquals(g.tray,restored.session.game.tray)
        assertEquals(2,restored.wallet.balance); assertEquals(1,restored.hintCredits)
        assertEquals(saved.frontier,restored.capture().frontier)
    }
    @Test fun newSavesReplayExactIndividualHistoryAndAdvanceOne() {
        val g=Game(expert.tiles); expert.winningSelections.take(7).forEach { assertTrue(g.collectRevealed(it)) }
        val saved=PlayerProgress(active=SessionSnapshot(75,expert.seed,expert.version,g.snapshot()),highestCompleted=74,undoCredits=2,hintCredits=1)
        val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
        assertFalse(restored.recovered); assertEquals(g.snapshot(),restored.session.game.snapshot())
        expert.winningSelections.drop(7).forEach { assertTrue(restored.session.game.collectRevealed(it)) }
        val next=restored.prepareNext()!!; assertEquals(76L,next.id); assertTrue(restored.commitNext(next))
        assertEquals(76L,restored.session.level.id); assertEquals(2,restored.wallet.balance)
    }
    @Test fun coveredIdentitiesCannotChangeFirstLookaheadDecision() {
        val tiles=listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LOTUS,2.0,0.0,0),
            Tile(2,Symbol.FERN,0.0,0.0,1),Tile(3,Symbol.FLOWER,2.0,0.0,1),
            Tile(4,Symbol.FERN,4.0,0.0,0),Tile(5,Symbol.FLOWER,6.0,0.0,0))
        val l=LevelDefinition(31,0,9,BoardLayout("hidden",tiles.map { TileSlot(it.id,it.x,it.y,it.layer) }),tiles,emptyList(),0)
        val first=SimulatedPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,400).actions.first()
        for (a in Symbol.entries.take(6)) for (b in Symbol.entries.take(6)) {
            val changed=l.copy(tiles=tiles.map { if(it.id==0) it.copy(symbol=a) else if(it.id==1) it.copy(symbol=b) else it })
            assertEquals(first,SimulatedPlayer.play(changed,SimulatedPlayer.Strategy.LOOKAHEAD,400).actions.first())
        }
    }
    @Test fun constructionIsDeterministicAndBounded() {
        val start=System.nanoTime()
        assertEquals(expert,EndlessGenerator.generate(75))
        assertTrue((System.nanoTime()-start)/1_000_000 < 30000)
        val fallback=EndlessGenerator.generate(75,true)
        LevelValidator.validate(fallback); assertFalse(fallback.qualityTargetMet)
    }
}
