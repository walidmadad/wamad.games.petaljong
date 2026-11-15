package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

class LevelPreparationTest {
    @Test fun sharedResultThresholdAndRetry() {
        val tasks = mutableListOf<() -> Unit>(); var time = 0L; var calls = 0; var fail = true
        val prep = LevelPreparation({ tasks.add(it) }, { id -> calls++; if (fail) error("fixture"); Levels.get(id) }, { time })
        prep.request(2); prep.request(2)
        assertEquals(1, tasks.size)
        assertFalse(prep.state!!.showLoading(249)); assertTrue(prep.state!!.showLoading(250))
        tasks.removeAt(0)(); assertTrue(prep.state!!.result!!.isFailure)
        prep.request(2, true); prep.request(2, true); assertEquals(1, tasks.size)
        fail = false; tasks.removeAt(0)()
        assertEquals(2L, prep.state!!.result!!.getOrThrow().id)
        prep.request(2); assertTrue(tasks.isEmpty()); assertEquals(2, calls)
        assertFalse(prep.state!!.showLoading(1000))
    }
    @Test fun obsoleteResultsRejected() {
        val tasks = mutableListOf<() -> Unit>()
        val prep = LevelPreparation({ tasks.add(it) }, { Levels.get(1).copy(id = it) })
        prep.request(2); prep.request(3)
        tasks[0](); assertEquals(3L, prep.state!!.target); assertNull(prep.state!!.result)
        tasks[1](); assertEquals(3L, prep.state!!.result!!.getOrThrow().id)
    }
    @Test fun failedSaveAndExactAdvancementPreserveWalletsAndCompletedVictory() {
        val level = Levels.get(1)
        val saved = PlayerProgress(highestCompleted = 1, undoCredits = 2, hintCredits = 1,
            active = SessionSnapshot(1, level.seed, level.version, GameSnapshot(level.witness, null)))
        val progress = ProgressController(saved)
        val before = progress.capture()
        assertFalse(progress.finalizeCompletedEntry { false })
        assertEquals(before, progress.capture())
        val next = progress.prepareNext()!!
        assertEquals(2L, next.id)
        assertNull(progress.nextProgress(next.copy(id = 3)))
        assertTrue(progress.commitNext(next))
        assertFalse(progress.commitNext(next))
        assertEquals(2L, progress.session.level.id)
        assertEquals(1L, progress.capture().frontier)
        assertEquals(2, progress.wallet.balance); assertEquals(1, progress.hintCredits)
        assertEquals(2L, ProgressController(ProgressCodec.decode(ProgressCodec.encode(before))).prepareNext()!!.id)
    }
    @Test fun availabilityMatchesAllocationBasedReference() {
        val tiles = LevelOne.tiles()
        for (mask in 0..255) {
            val remaining = tiles.filterIndexed { i, _ -> i >= 8 || mask and (1 shl i) != 0 }
            for (tile in remaining) {
                val others = remaining.filter { it.id != tile.id }
                fun span(a: Double,b: Double) = minOf(a+1,b+1)-maxOf(a,b)>Rules.EPS
                val left = others.any { it.layer == tile.layer && span(tile.y,it.y) && kotlin.math.abs(it.x+1-tile.x)<=Rules.EPS }
                val right = others.any { it.layer == tile.layer && span(tile.y,it.y) && kotlin.math.abs(tile.x+1-it.x)<=Rules.EPS }
                val expected = others.none { it.layer>tile.layer && Rules.overlaps(tile,it) } && (!left || !right)
                assertEquals(expected, Rules.available(tile,remaining))
            }
        }
    }
}
