package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GardenDiversityTest {
    @Test fun footprintsHaveBoundedNoveltyAndValidTerraces() {
        for (id in 6L..120L) {
            val layout = AdvancedLayouts.layout(id)
            val recent = (maxOf(6,id-10) until id).map { AdvancedLayouts.layout(it) }
            assertFalse("duplicate $id", AdvancedLayouts.recentDuplicate(layout,recent))
            assertEquals(layout,AdvancedLayouts.layout(id))
            for (variant in 0..5) {
                val slots = AdvancedLayouts.layout(id,variant).slots
                assertEquals(0,slots.size % 2)
                for (a in slots) {
                    val tile = Tile(a.id,Symbol.LEAF,a.x,a.y,a.layer)
                    assertFalse(slots.any { b -> b.id != a.id && a.layer == b.layer && Rules.overlaps(tile,Tile(b.id,Symbol.LEAF,b.x,b.y,b.layer)) })
                }
            }
        }
        assertEquals(8,(6L..13).map { AdvancedLayouts.layout(it).id.substringBefore('-') }.distinct().size)
        for (id in 6L..13) assertTrue((0..3).map { AdvancedLayouts.fingerprint(AdvancedLayouts.layout(id+it*8)) }.distinct().size >= 3)
    }
    @Test fun matrixReplaysProductionControllerAndRecordsEvidence() {
        val rows = mutableListOf("level,difficulty,family,variation,tiles,layers,fingerprint,theme,witness,durationMs,targetMet,blocked,overlaps,noveltyLimited")
        val ids = (8L..60L).toList() + (100L..120L).toList() + listOf(250L,500L,1000L)
        for (id in ids) {
            val start = System.nanoTime()
            val level = Levels.get(id)
            val durationMs = (System.nanoTime()-start)/1000000
            LevelValidator.validate(level)
            val game = Game(level.tiles)
            for (tile in level.winningSelections) {
                assertTrue(game.isAvailable(tile)); assertTrue(game.collectRevealed(tile)); assertNotEquals(Status.LOST,game.status)
            }
            assertEquals(Status.WON,game.status); assertTrue(game.tray.isEmpty())
            game.restart(); assertEquals(level.tiles,game.tiles)
            val fallback = EndlessGenerator.generate(id,true); LevelValidator.validate(fallback)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(AdvancedLayouts.fingerprint(level.layout).toByteArray()).joinToString("") { "%02x".format(it) }
            val metrics = GeometryMetrics.measure(level.tiles)
            rows.add("$id,${DifficultyPolicy.difficulty(id)},${level.layout.id.substringBefore('-')},${level.layout.id},${level.tiles.size},${GeometryMetrics.measure(level.tiles).layers},${digest},${GardenTheme.forLevel(id).id},PASS,$durationMs,${level.qualityTargetMet},${metrics.initiallyBlocked},${metrics.overlapRelationships},${level.noveltyLimited}")
        }
        File("build/codex30-matrix.csv").apply { parentFile.mkdirs(); writeText(rows.joinToString("\n")) }
    }
    @Test fun themesAreStableAndCeramicContrastIsPreserved() {
        assertEquals(listOf("spring","sunrise","lotus","twilight","autumn"),GardenTheme.all.map { it.id })
        for (id in 1L..120L) {
            val theme = GardenTheme.forLevel(id)
            assertEquals(theme,GardenTheme.forLevel(id))
            assertNotEquals(GardenTheme.ivory,theme.ceramic)
            assertNotEquals(theme.background,theme.ceramic)
        }
    }
    @Test fun versionSevenSaveStillRestores() {
        val level = VersionSevenGenerator.generate(17)
        val game = Game(level.tiles); assertTrue(game.collectRevealed(level.winningSelections.first()))
        val session = GardenSession()
        assertTrue(session.restore(SessionSnapshot(level.id,level.seed,level.version,game.snapshot())))
        assertEquals(level,session.level); assertEquals(game.snapshot(),session.game.snapshot())
    }
}
