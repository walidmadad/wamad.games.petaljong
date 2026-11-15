package com.wamad.petaljong.presentation

import com.wamad.petaljong.domain.*

/** Debug-only factory. Must run on a worker; owns no repository or normal controller. */
internal fun createPreviewSession(target: Long): ProgressController {
    require(target > 0)
    if (target > 1) {
        val preparation = LevelPreparation({ it() })
        preparation.request(target)
        preparation.state!!.result!!.getOrThrow()
    }
    return ProgressController(PlayerProgress(highestCompleted = target - 1, tutorial = TutorialStep.SKIPPED))
}
