package com.wamad.petaljong.presentation.board

enum class GardenScreen { START, GAME }
data class NavigationState(val levelId: Long, val screen: GardenScreen)

/** UI state only. Never owns, regenerates or mutates a puzzle or the credit wallet. */
class LevelNavigation(activeLevel: Long, restored: NavigationState? = null) {
    var state: NavigationState = restored?.copy(levelId = activeLevel)
        ?: NavigationState(activeLevel, GardenScreen.START)
        private set

    /** Committed advancement preserves presentation; only fresh entry defaults to START. */
    fun synchronize(activeLevel: Long) {
        if (state.levelId != activeLevel) state = NavigationState(activeLevel, state.screen)
    }
    fun play(activeLevel: Long) {
        synchronize(activeLevel)
        state = state.copy(screen = GardenScreen.GAME)
    }
    /** Victory and in-flight generation stay visible until a verified advance succeeds. */
    fun back(activeLevel: Long, won: Boolean, transitioning: Boolean) {
        if (!won && !transitioning) home(activeLevel)
    }
    fun home(activeLevel: Long) {
        synchronize(activeLevel)
        state = state.copy(screen = GardenScreen.START)
    }
}
