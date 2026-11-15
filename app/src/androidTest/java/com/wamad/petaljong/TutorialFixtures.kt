package com.wamad.petaljong

import android.content.Context
import com.wamad.petaljong.domain.*
import java.io.File

/** Existing gameplay regressions exercise the returning-player path; tutorial tests use fresh saves. */
internal fun seedReturningPlayer(context: Context, storage: String) {
    val file = File(context.filesDir, storage)
    val saved = if (file.exists()) ProgressCodec.decode(file.readText()) else PlayerProgress()
    file.writeText(ProgressCodec.encode(saved.copy(tutorial = TutorialStep.SKIPPED)))
}
