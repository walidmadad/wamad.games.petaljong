package com.wamad.petaljong.presentation.board

import com.wamad.petaljong.domain.GardenTheme

/** Permanent presentation identity. Never persisted or selected from puzzle metadata. */
object HomeTheme {
    val palette = GardenTheme("home", 0xff173f35.toInt(), 0xff38745b.toInt(),
        0xffb9d5b9.toInt(), 0xffe6b6c7.toInt()).palette
}
