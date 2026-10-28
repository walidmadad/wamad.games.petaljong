package com.tilebloom.game.presentation.board

enum class GardenScreen { START, GAME }
data class NavigationState(val levelId: Long, val screen: GardenScreen)

/** UI state only. Never owns, regenerates or mutates a puzzle or the credit wallet. */
class LevelNavigation(activeLevel: Long, restored: NavigationState? = null) {
    var state: NavigationState = restored?.takeIf { it.levelId == activeLevel }
        ?: NavigationState(activeLevel, GardenScreen.START)
        private set

    /** A newly committed level always waits for Play, including across activity replacement. */
    fun synchronize(activeLevel: Long) {
        if (state.levelId != activeLevel) state = NavigationState(activeLevel, GardenScreen.START)
    }
    fun play(activeLevel: Long) {
        synchronize(activeLevel)
        state = state.copy(screen = GardenScreen.GAME)
    }
    fun home(activeLevel: Long) {
        synchronize(activeLevel)
        state = state.copy(screen = GardenScreen.START)
    }
}
