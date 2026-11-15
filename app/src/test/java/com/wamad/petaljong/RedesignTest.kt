package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.BoardGeometry
import org.junit.Assert.*
import org.junit.Test

class RedesignTest {
    private val level get() = Levels.get(1)
    private fun layout(id: String, rows: List<Pair<Int,Int>>, caps: List<Pair<Double,Double>>): BoardLayout {
        val slots = mutableListOf<TileSlot>()
        rows.forEachIndexed { y, row -> repeat(row.second) { x -> slots.add(TileSlot(slots.size,(row.first+x).toDouble(),y.toDouble(),0)) } }
        caps.forEach { slots.add(TileSlot(slots.size,it.first,it.second,1)) }
        return BoardLayout(id,slots)
    }
    @Test fun boundedCandidatePortfolio() {
        val layouts = listOf(
            layout("sprout",listOf(1 to 2,0 to 4,0 to 4,1 to 4,1 to 4),listOf(.5 to .5,1.5 to .5,1.5 to 1.5,2.5 to 1.5,2.0 to 3.0,3.0 to 3.0)),
            layout("steps",listOf(0 to 3,0 to 4,1 to 4,1 to 4,2 to 3),listOf(.5 to .5,1.5 to .5,1.5 to 2.5,2.5 to 2.5,2.0 to 3.5,3.0 to 3.5)),
            layout("crescent",listOf(1 to 3,0 to 3,0 to 3,0 to 4,1 to 5),listOf(.5 to .5,1.5 to .5,.5 to 2.5,1.5 to 2.5,2.5 to 3.5,3.5 to 3.5)),
            Layouts.winding)
        val start = System.nanoTime()
        for (l in layouts) for (seed in 100L..if(l.id=="winding") 123L else 111L) {
            val candidate = LevelGenerator.generate(1,seed,l,1)
            LevelValidator.validate(candidate) // Production replay, including final WON.
            val a = PuzzleAnalyzer.analyze(candidate,10000)
            assertTrue(a.searchNodes<=10000)
            val geometry = BoardGeometry(284f,360f,tiles=candidate.tiles)
            println("PORTFOLIO ${l.id},$seed,${a.initialPairs},${a.consequenceChoiceStates},${a.riskyChoiceStates},${a.forced},${a.upperUnlockMoves},${a.solvableAlternatives},${a.unsolvableAlternatives},${a.unknownAlternatives},${a.searchNodes},${geometry.scale}")
        }
        println("PORTFOLIO_MS ${(System.nanoTime()-start)/1000000}")
    }
    @Test fun silhouetteIsSteppedAndAsymmetric() {
        LevelValidator.validate(level)
        assertNotEquals(Layouts.original.slots,level.layout.slots)
        assertEquals(listOf(3,4,4,4,3),level.tiles.filter { it.layer==0 }.groupBy { it.y }.values.map { it.size })
        assertEquals(6,level.tiles.count { it.layer==1 })
        assertEquals(5,level.tiles.map { it.y.toInt() }.distinct().size)
    }
    @Test fun frozenConstructionReproducesExactly() {
        val regenerated=LevelGenerator.generate(1,123L,Layouts.winding,1)
        assertEquals(level.tiles,regenerated.tiles); assertEquals(level.witness,regenerated.witness)
        assertEquals(3,level.version); assertEquals(2,LevelGenerator.VERSION)
    }
    @Test fun documentedDesignTargets() {
        val a=PuzzleAnalyzer.analyze(level)
        assertTrue(a.complete); assertEquals(2,a.initialPairs)
        assertEquals(8,a.consequenceChoiceStates); assertEquals(1,a.riskyChoiceStates)
        assertEquals(3,a.forced); assertEquals(5,a.upperUnlockMoves)
        assertTrue(a.branching.max()<=5)
        assertEquals(List(6){4},level.tiles.groupingBy { it.symbol }.eachCount().values.toList())
    }
    @Test fun capCentersAreLaterallyBlockedAndLowerTilesCovered() {
        val game=Game(level.tiles)
        assertFalse(game.isAvailable(19)); assertFalse(game.isAvailable(22))
        assertFalse(game.isAvailable(1)); assertFalse(game.isAvailable(11))
        assertTrue(game.isAvailable(18)); assertTrue(game.isAvailable(23))
    }
    @Test fun impactsAgreeWithProductionAndRespectSharedBudget() {
        val impacts=PuzzleAnalyzer.alternatives(level.tiles,10000)
        val before=Game(level.tiles).remaining.filter { Rules.available(it,level.tiles) }.map { it.id }.toSet()
        impacts.forEach { impact ->
            val game=Game(level.tiles); assertTrue(game.match(impact.move.first,impact.move.second))
            assertEquals(game.remaining.filter { game.isAvailable(it.id) && it.id !in before }.map { it.id }.toSet(),impact.newlyAvailable)
            assertEquals(PuzzleSolver.solve(game.remaining).outcome,impact.result.outcome)
        }
        assertTrue(impacts.map { it.newlyAvailable }.distinct().size>1)
        assertTrue(PuzzleAnalyzer.alternatives(level.tiles,1).sumOf { it.result.nodes }<=1)
        assertTrue(PuzzleAnalyzer.alternatives(level.tiles,0).all { it.result.outcome==Solvability.UNKNOWN_BUDGET_EXCEEDED })
    }
    @Test fun witnessChoicesIncludeSafeAlternativesAndConfirmedRisk() {
        val game=Game(level.tiles)
        var risks=0; var equivalent=0
        level.witness.forEachIndexed { index,move ->
            val impacts=PuzzleAnalyzer.alternatives(game.remaining)
            if(impacts.any { it.result.outcome==Solvability.UNSOLVABLE }) {
                risks++
                assertTrue(impacts.any { it.result.outcome==Solvability.SOLVABLE })
            }
            equivalent+=impacts.size-impacts.map { it.newlyAvailable }.distinct().size
            println("IMPACTS step=$index $impacts")
            assertTrue(game.match(move.first,move.second))
        }
        println("EQUIVALENT_EXTRA_ASSOCIATIONS $equivalent")
        assertEquals(1,risks); assertTrue(equivalent>0); assertEquals(Status.WON,game.status)
    }
    @Test fun touchTargetsFitPortraitAndTrackEveryRemoval() {
        for ((w,h) in listOf(284f to 360f,360f to 520f,412f to 650f)) {
            val geometry=BoardGeometry(w,h,tiles=level.tiles)
            assertTrue(geometry.scale*.93f>=48f)
            val game=Game(level.tiles)
            level.tiles.forEach { val r=geometry.shadow(it); assertTrue(r.left>=0 && r.top>=0 && r.right<=w && r.bottom<=h) }
            level.witness.forEach { move ->
                for(id in listOf(move.first,move.second)) {
                    val point=geometry.face(game.remaining.first { it.id==id }).center
                    assertEquals(id,geometry.hitTest(game.tiles,point.x,point.y)?.id)
                    game.select(id)
                }
            }
            assertEquals(Status.WON,game.status)
            game.restart(); assertEquals(level.tiles,game.tiles)
        }
    }
    @Test fun codexNineUnfinishedBoardMigratesWithoutErasingCompletions() {
        // Exact schema 1 envelope and version 2 Level 1 moves from CODEX 09.
        val old=ProgressCodec.decode("1\n1,2\n1,0,2\n16,20;0,4")
        val controller=ProgressController(old)
        assertTrue(controller.recovered); assertEquals(setOf(1,2),controller.capture().completed)
        assertEquals(3L,controller.highestUnlocked); assertEquals(Levels.get(3).tiles,controller.session.game.tiles)
        assertTrue(controller.session.game.snapshot().moves.isEmpty())
        val reload=ProgressController(ProgressCodec.decode(ProgressCodec.encode(controller.capture())))
        assertFalse(reload.recovered); assertEquals(controller.capture(),reload.capture())
    }
    @Test fun codexNineOtherLevelsRetainIdentityAndMovesDuringSchemaMigration() {
        val l=Levels.get(2)
        val move=l.witness.first()
        val raw="1\n1\n2,202,2\n${move.first},${move.second}"
        val controller=ProgressController(ProgressCodec.decode(raw))
        assertFalse(controller.recovered); assertEquals(listOf(move),controller.session.game.snapshot().moves)
    }
    @Test fun legacyHistoryIsNeverAppliedToNewGeometry() {
        val session=GardenSession(); val before=session.snapshot()
        assertFalse(session.restore(SessionSnapshot(1,0,2,GameSnapshot(Levels.legacyIntroductory.witness,null))))
        assertEquals(before,session.snapshot())
    }
    @Test fun auditSelectedBoard() {
        for(l in listOf(Levels.legacyIntroductory)+(1..5).map { Levels.get(it) }) {
            val wins=(0L..31L).count { seed ->
                val r=java.util.Random(seed); val g=Game(l.tiles)
                while(g.status==Status.PLAYING && g.matchingPairs().isNotEmpty()) { val moves=g.matchingPairs(); val p=moves[r.nextInt(moves.size)]; check(g.match(p.first,p.second)) }
                g.status==Status.WON
            }
            println("DESIGN_AUDIT version=${l.version} ${PuzzleAnalyzer.analyze(l)} randomWins=$wins/32")
        }
        println("SELECTED_TILES ${level.tiles}")
        println("SELECTED_WITNESS ${level.witness}")
        println("SELECTED_DEADLOCK ${deadlockPath(Game(level.tiles))}")
    }
}
