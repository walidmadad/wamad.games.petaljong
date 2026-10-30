package com.tilebloom.game

import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.View
import android.widget.TextView
import com.tilebloom.game.presentation.*
import com.tilebloom.game.storage.*
import com.tilebloom.game.domain.*
import com.tilebloom.game.ads.*
import java.io.File

/** Uses isolated progress and fake ads. Restores the original app language after the test. */
@Suppress("DEPRECATION")
class LanguageSwitchInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-languages40"
    private val network = ConnectivityService.factory
    private val ads = AdvertisingService.provider
    private var original = ""
    private var shown = 0
    private var privacyOpened = 0
    private var latest: MainActivity? = null
    private fun ui(work: () -> Unit) = instrumentation.runOnMainSync(work)
    override fun setUp() {
        super.setUp()
        original = AppLanguage.selected(instrumentation.targetContext)
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        val context = instrumentation.targetContext
        File(context.filesDir, storage).writeText(ProgressCodec.encode(PlayerProgress(tutorial = com.tilebloom.game.domain.TutorialStep.SKIPPED, undoCredits = 2, hintCredits = 1)))
        File(context.filesDir, "$storage.bak").delete()
        ConnectivityService.factory = { _, changed -> object : NetworkObserver {
            override fun start() { changed(com.tilebloom.game.domain.ConnectivityStatus.ONLINE) }
            override fun stop() {}
            override fun destroy() {}
            override fun retry() { changed(com.tilebloom.game.domain.ConnectivityStatus.ONLINE) }
        } }
        AdvertisingService.provider = object : Advertising {
            override val state = RewardedAdManager.State.IDLE
            override val privacyRequired = true
            override fun attach(activity: android.app.Activity) {}
            override fun detach(activity: android.app.Activity) {}
            override fun privacyOptions(activity: android.app.Activity) { privacyOpened++ }
            override fun show(activity: android.app.Activity, purpose: RewardPurpose): Boolean { shown++; return false }
        }
        seedReturningPlayer(instrumentation.targetContext, storage)
    }
    override fun tearDown() {
        ui { latest?.finish(); AppLanguage.select(instrumentation.targetContext, original) }
        super.tearDown()
        ConnectivityService.factory = network; AdvertisingService.provider = ads
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun ready(screen: MainActivity) {
        latest = screen
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            ui { ready = screen.isPuzzleReady }
            if (ready) { instrumentation.waitForIdleSync(); return }
            SystemClock.sleep(20)
        }
        fail("Locale recreation did not restore puzzle")
    }
    fun testSwitchAndRelaunchKeepBoardTrayCreditsSoundAndAds() {
        val first = activity; ready(first)
        ui {
            assertTrue(first.findViewById<View>(R.id.continue_level).isClickable)
            first.findViewById<View>(R.id.continue_level).performClick()
        }
        val controller = GameStore.get(instrumentation.targetContext)
        ui {
            check(controller.session.game.select(controller.session.level.winningSelections.first()))
            GameStore.persist(first)
        }
        val snapshot = ProgressCodec.encode(controller.capture())
        val audio = instrumentation.targetContext.getSharedPreferences("garden_audio", 0).all
        val target = if (AppLanguage.selected(first) == "fr") "de" else "fr"
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            ui {
                AppLanguage.select(first, target)
                if (Build.VERSION.SDK_INT < 33) first.recreate()
            }
            val second = instrumentation.waitForMonitorWithTimeout(monitor, 15000) as? MainActivity
                ?: throw AssertionError("Language selection must refresh Activity")
            ready(second)
            assertEquals(target, AppLanguage.selected(second))
            assertEquals(snapshot, ProgressCodec.encode(GameStore.get(second).capture()))
            assertEquals(audio, instrumentation.targetContext.getSharedPreferences("garden_audio", 0).all)
            ui {
                assertTrue(second.findViewById<View>(R.id.board).isShown)
                assertNull(second.findViewById<View>(R.id.counter))
                val level = MainActivity::class.java.getDeclaredField("levelLabel").apply { isAccessible = true }.get(second) as TextView
                assertEquals(second.getString(R.string.level_number, controller.session.level.id), level.text.toString())
                second.findViewById<View>(R.id.settings).performClick()
            }
            val dialogField = MainActivity::class.java.getDeclaredField("settingsDialog").apply { isAccessible = true }
            ui {
                val dialog = dialogField.get(second) as android.app.AlertDialog
                assertNotNull(dialog.findViewById<View>(R.id.settings_language))
                dialog.findViewById<View>(R.id.settings_privacy).performClick()
                dialog.dismiss()
                second.finish()
            }
            ProgressRepository.awaitWrites()
            ProgressRepository.useTestStorage(storage) // Simulate loss of the process-owned controller.
            val third = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            ready(third)
            assertEquals(target, AppLanguage.selected(third))
            assertEquals(snapshot, ProgressCodec.encode(GameStore.get(third).capture()))
            assertEquals(0, shown)
            assertEquals(1, privacyOpened)
            // Clear the override: the empty list is the system-following mode on every API.
            ui { third.finish(); AppLanguage.select(instrumentation.targetContext, "") }
            val automatic = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            ready(automatic)
            assertEquals("", AppLanguage.selected(automatic))
            val systemLocales = if (Build.VERSION.SDK_INT >= 33)
                automatic.getSystemService(android.app.LocaleManager::class.java).systemLocales
                else android.content.res.Resources.getSystem().configuration.locales
            val expected = automatic.createConfigurationContext(android.content.res.Configuration(automatic.resources.configuration).apply { setLocales(systemLocales) })
            assertEquals(expected.getString(R.string.settings_label), automatic.getString(R.string.settings_label))
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
