package com.tilebloom.game.presentation.board

import com.tilebloom.game.domain.Status

/** One terminal presentation; derived afresh after every move, restore or transition. */
data class ScreenPresentation(val victory: Boolean, val footer: Boolean, val restart: Boolean, val advance: Boolean) {
    companion object {
        fun forStatus(status: Status, hasNext: Boolean) = ScreenPresentation(
            victory = status == Status.WON,
            footer = status != Status.WON,
            restart = status != Status.WON,
            advance = status == Status.WON && hasNext
        )
    }
}
