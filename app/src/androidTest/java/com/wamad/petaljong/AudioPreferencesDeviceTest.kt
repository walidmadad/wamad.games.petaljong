package com.wamad.petaljong

import android.test.InstrumentationTestCase
import com.wamad.petaljong.audio.GameAudioManager
import com.wamad.petaljong.domain.AudioPreferences

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
                first.preferences(AudioPreferences(muted = true))
                first.close()
                val second = GameAudioManager(app, name)
                assertEquals(AudioPreferences(muted = true), second.policy.preferences)
                second.close()
            }
        } finally { app.deleteSharedPreferences(name) }
    }
    fun testPreviousDisabledEffectsBecomeMuted() {
        val app = instrumentation.targetContext
        val name = "audio_migration_test_${System.nanoTime()}"
        try {
            app.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit()
                .putBoolean("effects", false).putBoolean("music", true).commit()
            instrumentation.runOnMainSync {
                val manager = GameAudioManager(app, name)
                assertTrue(manager.policy.preferences.muted)
                manager.preferences(manager.policy.preferences.copy(muted = false))
                manager.close()
                val recreated = GameAudioManager(app, name)
                assertFalse(recreated.policy.preferences.muted)
                assertFalse(app.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).contains("music"))
                recreated.close()
            }
        } finally { app.deleteSharedPreferences(name) }
    }

}
