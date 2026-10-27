package com.tilebloom.game

import android.test.ActivityInstrumentationTestCase2
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.MahjongBoardView
import android.widget.Button

@Suppress("DEPRECATION")
class LaunchSmokeTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    fun testLaunchAndRestart() {
        val screen = activity
        assertNotNull(screen.findViewById<MahjongBoardView>(R.id.board))
        val restart = screen.findViewById<Button>(R.id.restart)
        assertNotNull(restart)
        instrumentation.runOnMainSync { restart.performClick() }
        instrumentation.waitForIdleSync()
        assertFalse(screen.isFinishing)
    }
}
