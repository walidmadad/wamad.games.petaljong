package com.tilebloom.game

import android.test.InstrumentationTestCase
import com.tilebloom.game.audio.GameAudioManager
import com.tilebloom.game.domain.AudioPreferences

/** Isolated preference namespace; never clears or changes player data. */
@Suppress("DEPRECATION")
class AudioPreferencesDeviceTest : InstrumentationTestCase() {
    fun testPreferencesSurviveManagerRecreation() {
        val app = instrumentation.targetContext
        val name = "audio_test_${System.nanoTime()}"
        try {
            instrumentation.runOnMainSync {
                val first = GameAudioManager(app, name)
                assertEquals(AudioPreferences(), first.policy.preferences)
                first.preferences(AudioPreferences(false, true, true))
                first.close()
                val second = GameAudioManager(app, name)
                assertEquals(AudioPreferences(false, true, true), second.policy.preferences)
                second.close()
            }
        } finally { app.deleteSharedPreferences(name) }
    }
}
