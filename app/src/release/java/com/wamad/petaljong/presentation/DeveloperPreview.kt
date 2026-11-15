package com.wamad.petaljong.presentation

import android.view.View
import com.wamad.petaljong.domain.ProgressController

/** Release has no intent parser, controls, preview Activity or session factory. */
internal object DeveloperPreview {
    fun level(activity: MainActivity) = 1L
    fun active(activity: MainActivity) = false
    fun install(activity: MainActivity, card: View, opening: () -> Unit) = Unit
    fun load(activity: MainActivity): ProgressController = error("Unavailable")
}
