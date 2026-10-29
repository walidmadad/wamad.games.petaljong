package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.BoardGeometry
import org.junit.Assert.*
import org.junit.Test

class AdvancedMahjongTest {
    private val matrix = listOf(1L,7,8,15,16,30,31,50,100,500,1000)
    @Test fun matrixUsesControllerAndSharedTouchGeometry() {
        matrix.forEach { id ->
            val start = System.nanoTime()
            val level = Levels.get(id)
            assertEquals(level, Levels.get(id)); LevelValidator.validate(level)
            val metric = GeometryMetrics.measure(level.tiles)
            val analysis = PuzzleAnalyzer.analyze(level,2000)
            val tray = TrayPressure.measure(level)
            println("CODEX29 id=$id tiles=${level.tiles.size} layers=${metric.layers} overlaps=${metric.overlapRelationships} density=${metric.overlapDensity} blocked=${metric.initiallyBlocked} free=${metric.initiallyAvailable} choices=${analysis.consequenceChoiceStates} pairRisk=${analysis.riskyChoiceStates} unknown=${analysis.unknownAlternatives} tray=$tray target=${level.qualityTargetMet} totalMs=${(System.nanoTime()-start)/1000000}")
            if(id >= 8) { assertTrue(metric.layers >= 3); assertTrue(metric.coveredPositions >= 18) }
            if(id >= 31) {
                assertEquals(4,metric.layers); assertTrue(metric.overlapRelationships >= 70)
                assertTrue(metric.initiallyBlocked >= 34); assertTrue(PuzzleAnalyzer.strategic(level).delayedMatches >= 3)
                assertTrue(PuzzleAnalyzer.strategic(level).witnessPressure >= 6)
                assertEquals(PuzzleAnalyzer.strategic(level).meets(Difficulty.EXPERT), level.qualityTargetMet)
            }
            for(width in listOf(320f,360f,400f,800f)) {
                val geo = BoardGeometry(width-32f,480f,tiles=level.tiles)
                assertTrue(geo.scale >= 38f)
                val game = Game(level.tiles)
                for(tid in level.winningSelections) {
                    val tile = game.remaining.first { it.id == tid }
                    val center = geo.face(tile).center
                    assertEquals(tid,geo.hitTest(game.tiles,center.x,center.y)?.id)
                    assertTrue(game.select(tid)); assertNotEquals(Status.LOST,game.status)
                }
                assertEquals(Status.WON,game.status); assertTrue(game.tray.isEmpty())
            }
        }
    }
    @Test fun bothFamiliesAndEveryTerraceHaveRealBlocking() {
        for(id in listOf(8L,9,16,17,31,32)) for(variant in 0..5) {
            val layout = AdvancedLayouts.layout(id,variant)
            val tiles = layout.slots.map { Tile(it.id,Symbol.LEAF,it.x,it.y,it.layer) }
            for(a in tiles) {
                assertFalse(tiles.any { it.id != a.id && it.layer == a.layer && Rules.overlaps(a,it) })
                if(a.layer > 0) assertTrue(tiles.any { it.layer == a.layer-1 && Rules.overlaps(a,it) })
            }
            for(z in 1..tiles.maxOf { it.layer }) assertTrue(tiles.any { a -> a.layer == z-1 && tiles.any { it.layer == z && Rules.overlaps(a,it) } })
            val game = Game(tiles)
            tiles.filter { !Rules.available(it,tiles) }.forEach { assertFalse(game.select(it.id)) }
        }
        assertNotEquals(EndlessGenerator.geometryFingerprint(AdvancedLayouts.layout(31)),EndlessGenerator.geometryFingerprint(AdvancedLayouts.layout(32)))
    }
    @Test fun coverageUnlockAndUndoAreExact() {
        val level = Levels.get(31); val game = Game(level.tiles)
        var unlocked = false
        for(id in level.winningSelections) {
            val before = game.snapshot(); val free = game.remaining.filter { game.isAvailable(it.id) }.map { it.id }.toSet()
            assertTrue(game.select(id))
            if(game.remaining.any { it.id !in free && game.isAvailable(it.id) }) unlocked = true
            if(game.canUndo) { val after = game.snapshot(); assertTrue(game.undo()); assertEquals(before,game.snapshot()); assertTrue(game.select(id)); assertEquals(after,game.snapshot()) }
        }
        assertTrue(unlocked)
    }
    @Test fun analyzerCountsIntermediateTerraceUnlocks() {
        val tiles = (0..5).map { Tile(it,Symbol.LEAF,(it%2)*2.0,0.0,it/2) }
        val level = LevelDefinition(31,0,7,BoardLayout("fixture",tiles.map { TileSlot(it.id,it.x,it.y,it.layer) }),
            tiles,listOf(PairMove(4,5),PairMove(2,3),PairMove(0,1)),0)
        assertEquals(2,PuzzleAnalyzer.analyze(level).upperUnlockMoves)
    }
    @Test fun hiddenArtworkRemainsPrivateUntilCoverageIsRemoved() {
        val lower = Tile(0,Symbol.LEAF,0.0,0.0,0)
        val upper = Tile(1,Symbol.LOTUS,0.0,0.0,1)
        val tiles = listOf(lower, upper)
        val geometry = com.tilebloom.game.presentation.board.BoardGeometry(360f, 480f, tiles = tiles)
        val center = geometry.face(lower).center
        fun visible(t: Tile, state: List<Tile>) = com.tilebloom.game.presentation.board.TileVisibility
            .faceVisibleAt(t, state, geometry, center.x, center.y)
        assertFalse(visible(lower, tiles))
        assertTrue(visible(upper, tiles))
        assertTrue(visible(lower, listOf(lower, upper.copy(removed = true))))
        val shifted = upper.copy(x = .5)
        val exposed = geometry.face(lower).left + geometry.scale * .2f
        assertTrue(com.tilebloom.game.presentation.board.TileVisibility.faceVisibleAt(lower,
            listOf(lower, shifted), geometry, exposed, center.y))
    }
    @Test fun metricsHaveHandCheckedValues() {
        val tiles = listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LEAF,.5,0.0,1))
        assertEquals(GeometryMetrics(2,1,1,1,1),GeometryMetrics.measure(tiles))
    }
    @Test fun historicalV5AndV6RestoreExactTrayHistoryAndBalances() {
        for(version in listOf(5,6)) for(id in listOf(7L,15,30,31,1000)) {
            val level = if(version == 5) VersionFiveGenerator.generate(id) else VersionSixGenerator.generate(id)
            val game = Game(level.tiles); assertTrue(game.select(level.winningSelections.first()))
            val progress = PlayerProgress(active=SessionSnapshot(id,level.seed,version,game.snapshot()),highestCompleted=id-1,undoCredits=2,hintCredits=1)
            val restored = ProgressController(ProgressCodec.decode(ProgressCodec.encode(progress)))
            assertFalse(restored.recovered); assertEquals(level,restored.session.level)
            assertEquals(game.snapshot(),restored.session.game.snapshot()); assertEquals(game.tray,restored.session.game.tray)
            assertEquals(2,restored.wallet.balance); assertEquals(1,restored.hintCredits)
        }
    }
}
