package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class CompletionEntryTest {
    private fun decode(p: PlayerProgress) = ProgressCodec.decode(ProgressCodec.encode(p))
    private fun won(id: Long): ProgressController {
        val c = ProgressController(PlayerProgress(highestCompleted=id-1, undoCredits=1, hintCredits=0))
        c.session.level.winningSelections.forEach {
            assertTrue(c.session.game.collectRevealed(it))
        }
        assertEquals(id, c.capture().frontier)
        return c
    }
    @Test fun existingVictoriesFinalizeExactlyOnceBeforePlay() {
        for (id in listOf(6L,20L,37L,100L)) {
            val victory = won(id).capture()
            var disk = decode(victory)
            val c = ProgressController(disk)
            assertEquals(Status.WON,c.session.game.status)
            assertTrue(c.finalizeCompletedEntry { disk=decode(it); true })
            assertEquals(id+1,c.session.level.id)
            assertEquals(id,disk.frontier)
            assertEquals(1,disk.undoCredits); assertEquals(0,disk.hintCredits)
            assertTrue(c.session.game.tray.isEmpty()); assertFalse(c.session.game.canUndo)
            assertTrue(c.capture().active!!.game.selections.orEmpty().isEmpty())
            assertEquals(Levels.get(id+1).seed,c.session.level.seed)
            assertEquals(Levels.get(id+1).version,c.session.level.version)
            repeat(4) {
                val entry=ProgressController(disk)
                assertTrue(entry.finalizeCompletedEntry { fail("Unfinished level must not advance"); false })
                val nav=LevelNavigation(entry.session.level.id)
                assertEquals(GardenScreen.START,nav.state.screen)
                nav.play(entry.session.level.id)
                assertEquals(Status.PLAYING,entry.session.game.status)
                assertEquals(id+1,entry.session.level.id)
                disk=decode(entry.capture())
            }
        }
    }
    @Test fun interruptedSaveAndDeathAfterCommitRecoverWithoutSkipping() {
        val c=won(6); val old=c.capture()
        assertFalse(c.finalizeCompletedEntry { false })
        assertEquals(old,c.capture())
        var disk=old
        val pending=ProgressController(decode(disk))
        assertTrue(pending.finalizeCompletedEntry { disk=decode(it); true })
        // Only the durable candidate survives; the old in-memory controller is discarded.
        val recovered=ProgressController(disk)
        assertTrue(recovered.finalizeCompletedEntry { fail("Already committed"); false })
        assertEquals(7L,recovered.session.level.id)
        val retry=ProgressController(decode(old))
        assertTrue(retry.finalizeCompletedEntry { true })
        assertEquals(7L,retry.session.level.id)
    }
    @Test fun unfinishedAndDefeatKeepExactTrayHistoryAndBalances() {
        for (lost in listOf(false,true)) {
            val c=ProgressController(PlayerProgress(undoCredits=0,hintCredits=1))
            val ids=if(lost) listOf(23,21,18,20) else listOf(c.session.level.winningSelections.first())
            ids.forEach { assertTrue(c.session.game.collectRevealed(it)) }
            val before=decode(c.capture()); val restored=ProgressController(before)
            assertTrue(restored.finalizeCompletedEntry { fail("No victory"); false })
            assertEquals(before,restored.capture())
            assertEquals(c.session.game.tray,restored.session.game.tray)
        }
    }
    @Test fun olderSchemaVictoryRetainsCompletionAndUndoOnMigration() {
        val victory=won(6).capture()
        val legacy=ProgressCodec.encode(victory).split('\n').take(5).toMutableList()
        legacy[0]="4"
        var disk=ProgressCodec.decode(legacy.joinToString("\n"))
        val c=ProgressController(disk)
        assertTrue(c.finalizeCompletedEntry { disk=decode(it); true })
        assertEquals(6L,disk.frontier); assertEquals(7L,disk.active!!.id)
        assertEquals(1,disk.undoCredits); assertEquals(3,disk.hintCredits)
    }
}
