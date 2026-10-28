package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.BoardGeometry
import org.junit.Assert.*
import org.junit.Test

class DifficultyTest {
    private val milestones = listOf(1L,20,21,40,41,60,61,80,81,100,101,120,121,200,500,1000,10000,Long.MAX_VALUE)
    @Test fun boundariesAndSafeRanges() {
        val expected = mapOf(1L to Difficulty.NORMAL,7L to Difficulty.NORMAL,8L to Difficulty.HARD,
            15L to Difficulty.HARD,16L to Difficulty.VERY_HARD,30L to Difficulty.VERY_HARD,
            31L to Difficulty.EXPERT,100L to Difficulty.EXPERT,1000L to Difficulty.EXPERT)
        expected.forEach { (id, tier) -> assertEquals(tier,DifficultyPolicy.difficulty(id)) }
        milestones.forEach { id -> val p = DifficultyPolicy.profile(id)
            assertEquals(DifficultyPolicy.difficulty(id).ordinal.toLong(),p.tierIndex)
            assertTrue(id in p.firstLevel..p.lastLevel); assertTrue(p.activeSymbols <= 16)
        }
    }
    @Test fun stableCatalogAndMilestones() {
        assertEquals((0..19).toList(),Symbol.entries.map { it.stableId })
        assertEquals(listOf(Symbol.LEAF,Symbol.LOTUS,Symbol.FERN,Symbol.FLOWER,Symbol.BERRIES,Symbol.SUNFLOWER),Symbol.entries.take(6))
        (1L..5).forEach { assertEquals(Symbol.entries.take(6),DifficultyPolicy.symbols(it)) }
        listOf(21L,41,61,81,101).forEachIndexed { i,id ->
            assertTrue(DifficultyPolicy.introduced(id).containsAll(Symbol.entries.slice(6+i*2..7+i*2)))
            val level=Levels.get(id)
            DifficultyPolicy.introduced(id).forEach { symbol -> assertTrue(level.tiles.count { it.symbol==symbol } >= 2) }
        }
        assertNull(DifficultyPolicy.transitionMessage(121))
        assertNull(DifficultyPolicy.transitionMessage(122))
    }
    @Test fun profileMatrixVerifiedAndTouchable() {
        (milestones + listOf(22L,39,42,59,62,79,82,99,102,119,122,140,141)).distinct().forEach { id ->
            val start=System.nanoTime(); val level=Levels.get(id); val generationMs=(System.nanoTime()-start)/1000000
            val analysisStart=System.nanoTime(); val a=PuzzleAnalyzer.analyze(level,3000)
            println("CODEX14 HOST level=$id ms=$generationMs analysisMs=${(System.nanoTime()-analysisStart)/1000000} tiles=${a.tileCount} symbols=${a.symbols} free=${a.initialFree} pairs=${a.initialPairs} blocked=${a.initialBlocked} forced=${a.forced} choices=${a.consequenceChoiceStates} risk=${a.riskyChoiceStates} unknown=${a.unknownAlternatives} target=${level.qualityTargetMet}")
            LevelValidator.validate(level); assertEquals(level,Levels.get(id))
            assertTrue(level.tiles.groupingBy { it.symbol }.eachCount().values.all { it>=2 && it%2==0 })
            if(id>=6) { val p=DifficultyPolicy.profile(id)
                assertTrue(level.tiles.size in p.targetTiles); assertEquals(p.activeSymbols,level.tiles.map { it.symbol }.distinct().size)
                assertTrue(level.candidatesEvaluated<=p.candidateBudget)
                assertTrue(level.candidateQuality.all { it.searchNodes<=p.analysisBudget })
            }
            for((w,h) in listOf(284f to 300f,360f to 520f,600f to 260f)) {
                val geo=BoardGeometry(w,h,tiles=level.tiles); val game=Game(level.tiles)
                assertTrue(geo.scale >= 30f)
                level.witness.forEach { pair ->
                    for(id2 in listOf(pair.first,pair.second)) { val tile=game.remaining.first { it.id==id2 }; val point=geo.face(tile).center
                        assertEquals(id2,geo.hitTest(game.tiles,point.x,point.y)?.id)
                    }; assertTrue(game.match(pair.first,pair.second))
                }
            }
        }
    }
    @Test fun legacyRestoresExactMovesAndWallet() {
        for(id in listOf(6L,20,21,40,101,10000)) {
            val old=LegacyEndlessGenerator.generate(id); val moves=old.witness.take(2)
            val state=PlayerProgress(active=SessionSnapshot(id,old.seed,4,GameSnapshot(moves,null)),undoCredits=1,highestCompleted=id-1)
            val c=ProgressController(ProgressCodec.decode(ProgressCodec.encode(state)))
            assertEquals(old,c.session.level); assertEquals(moves,c.session.game.snapshot().moves)
            assertEquals(1,c.wallet.balance); assertFalse(c.recovered)
            assertTrue(c.session.restart()); assertEquals(old,c.session.level); assertEquals(1,c.wallet.balance)
        }
    }
    @Test fun transitionsAreExactlyOneAndPersistCredits() {
        for(id in listOf(7L,15,30,31,40,100)) {
            val c=ProgressController(PlayerProgress(highestCompleted=id-1,undoCredits=1))
            c.session.level.witness.forEach { assertTrue(c.session.game.match(it.first,it.second)) }
            val next=c.prepareNext()!!; assertTrue(c.commitNext(next)); assertFalse(c.commitNext(next))
            assertEquals(id+1,c.session.level.id); assertEquals(1,c.wallet.balance); assertFalse(c.select(id.toInt()))
            assertTrue(c.session.restart()); assertEquals(id+1,c.session.level.id)
            val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
            assertEquals(c.session.snapshot(),restored.session.snapshot()); assertEquals(1,restored.wallet.balance)
        }
    }
    @Test fun exhaustedAnalysisStillHasVerifiedFallback() {
        val level=EndlessGenerator.generate(101,true); LevelValidator.validate(level)
        assertFalse(level.qualityTargetMet); assertEquals(1,level.candidatesEvaluated)
        assertTrue(level.candidateQuality.all { it.searchNodes==0 })
        assertEquals(level,EndlessGenerator.generate(101,true))
    }
}
