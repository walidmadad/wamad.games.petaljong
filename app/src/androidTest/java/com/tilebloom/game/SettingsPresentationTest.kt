package com.tilebloom.game

import android.app.AlertDialog
import android.test.ActivityInstrumentationTestCase2
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Switch
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.AppLanguage

/** Rendering assertions complement, but do not replace, device screenshot review. */
@Suppress("DEPRECATION")
class SettingsPresentationTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private fun assertBareGear(screen: MainActivity) {
        val gear = screen.findViewById<View>(R.id.settings)
        assertNull("Settings must have no background in either state", gear.background)
        gear.isPressed = true
        assertNull(gear.background)
        gear.isPressed = false
        val d = screen.resources.displayMetrics.density
        assertTrue(gear.width >= 48 * d && gear.height >= 48 * d)
        assertTrue(gear.contentDescription.isNotBlank())
    }
    private fun dialog(): AlertDialog = MainActivity::class.java.getDeclaredField("settingsDialog")
        .apply { isAccessible = true }.get(activity) as AlertDialog
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    fun testClosingSettingsPreservesGameplaySnapshot() {
        val screen = activity
        val deadline = android.os.SystemClock.uptimeMillis() + 30000
        var ready = false
        while (!ready && android.os.SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync { ready = screen.isPuzzleReady }
            if (!ready) android.os.SystemClock.sleep(20)
        }
        assertTrue("Puzzle must finish loading", ready)
        instrumentation.runOnMainSync {
            screen.findViewById<View>(R.id.continue_level).performClick()
            screen.findViewById<View>(R.id.tutorial_skip)?.performClick()
            val controller = com.tilebloom.game.storage.GameStore.get(screen)
            val before = com.tilebloom.game.domain.ProgressCodec.encode(controller.capture())
            assertBareGear(screen)
            screen.findViewById<View>(R.id.settings).performClick()
            val settings = dialog()
            assertTrue(settings.isShowing)
            settings.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertFalse(settings.isShowing)
            assertEquals(before, com.tilebloom.game.domain.ProgressCodec.encode(controller.capture()))
            assertTrue(screen.findViewById<View>(R.id.board).isShown)
        }
    }

    fun testModalSoundPersistenceAndBundledLanguageRows() {
        val screen = activity
        instrumentation.runOnMainSync { assertBareGear(screen); screen.findViewById<View>(R.id.settings).performClick() }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val settings = dialog()
            val window = settings.window!!
            assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
            assertEquals(.28f, window.attributes.dimAmount)
            assertTrue(screen.findViewById<View>(R.id.level_start).isShown)
            val sound = settings.findViewById<Switch>(R.id.settings_sound)
            assertTrue(sound.isShown); assertFalse(sound.showText)
            assertTrue(sound.height >= (48 * screen.resources.displayMetrics.density).toInt())
            val initial = sound.isChecked
            try {
                for (enabled in listOf(false, true)) {
                    sound.isChecked = enabled
                    assertEquals(!enabled, screen.getSharedPreferences("garden_audio", 0).getBoolean("muted", false))
                }
            } finally { sound.isChecked = initial }
            settings.findViewById<View>(R.id.settings_language).performClick()
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val picker = dialog()
            val rows = descendants(picker.window!!.decorView).filter { it.tag?.toString()?.startsWith("language:") == true }
            assertEquals(9, rows.size)
            assertEquals(1, rows.count { it.isSelected })
            rows.forEach { row ->
                assertTrue(row.isClickable)
                val flag = descendants(row).filterIsInstance<ImageView>().single()
                assertNotNull(flag.drawable)
                assertEquals(View.LAYOUT_DIRECTION_LTR, flag.layoutDirection)
                assertTrue(row.minimumHeight >= (48 * screen.resources.displayMetrics.density).toInt())
            }
            val selected = AppLanguage.selected(screen)
            rows.single { it.tag == "language:$selected" }.performClick()
            assertFalse(picker.isShowing)
            assertEquals(selected, AppLanguage.selected(screen))
        }
    }
}
