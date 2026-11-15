package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.createPreviewSession
import org.junit.Assert.*
import org.junit.Test

class DeveloperPreviewTest {
    @Test fun advancedPreviewsUseProductionBoardsAndNeverMutateNormalSession() {
        val normal = ProgressController(PlayerProgress(highestCompleted = 14, tutorial = TutorialStep.SKIPPED))
        val saved = ProgressCodec.encode(normal.capture())
        for (id in listOf(50L, 100L, 110L, 150L, 200L)) {
            val preview = createPreviewSession(id)
            val production = Levels.get(id)
            assertEquals(id, preview.session.level.id)
            assertEquals(production, preview.session.level)
            LevelValidator.validate(preview.session.level)
            val snapshot = preview.session.game.snapshot()
            val hint = preview.session.game.availableTiles().first()
            assertTrue(preview.acceptHint(hint.id, snapshot))
            assertEquals(2, preview.hintCredits)
            val first = production.winningActions.first { it.kind != ActionKind.REVEAL }
            if (production.tiles.first { it.id == first.tileId }.concealed) preview.session.game.reveal(first.tileId)
            assertTrue(preview.session.game.select(first.tileId))
            assertTrue(preview.undo())
            assertEquals(2, preview.wallet.balance)
            preview.session.restart()
            // Real witness plays through mystery reveals and four-slot tray rules.
            production.winningActions.forEach {
                assertTrue(if (it.kind == ActionKind.REVEAL) preview.session.game.reveal(it.tileId)
                    else preview.session.game.select(it.tileId))
            }
            assertEquals(Status.WON, preview.session.game.status)
            preview.capture()
            assertEquals(saved, ProgressCodec.encode(normal.capture()))
            assertEquals(15L, normal.session.level.id)
        }
    }
    @Test fun rejectsNonPositiveLevels() {
        for (id in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertTrue(runCatching { createPreviewSession(id) }.isFailure)
        }
    }
}
