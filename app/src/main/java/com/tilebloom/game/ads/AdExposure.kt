package com.tilebloom.game.ads

import android.content.Context
import android.os.SystemClock
import com.tilebloom.game.domain.InterstitialFrequencyPolicy

/** Separate bounded preferences; no board or wallet mutations. Main-thread only. */
object AdExposure {
    private var prefs: android.content.SharedPreferences? = null
    lateinit var policy: InterstitialFrequencyPolicy; private set
    private var gate = com.tilebloom.game.domain.FullScreenAdGate()
    val busy get() = gate.busy
    var changed: (() -> Unit)? = null
    fun useTestStorage(context: Context, name: String?) {
        check(com.tilebloom.game.BuildConfig.DEBUG)
        prefs = null; gate = com.tilebloom.game.domain.FullScreenAdGate()
        storageName = name ?: "ad_frequency"
        initialize(context)
    }
    private var storageName = "ad_frequency"
    fun initialize(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(storageName, Context.MODE_PRIVATE)
        val duration = runCatching { prefs!!.getLong("active", 0) }.getOrDefault(0)
        val victory = runCatching { prefs!!.getLong("victory", 0) }.getOrDefault(0)
        val interrupted = runCatching { prefs!!.getBoolean("presenting", false) }.getOrDefault(true)
        policy = InterstitialFrequencyPolicy(if (interrupted) 0 else duration, victory)
    }
    fun save(): Boolean = prefs?.edit()?.putLong("active", policy.duration(SystemClock.elapsedRealtime()))?.putLong("victory", policy.lastVictory)?.commit() == true
    fun acquire(): Boolean {
        if (busy || prefs?.edit()?.putBoolean("presenting", true)?.commit() != true) return false
        gate.acquire(); changed?.invoke(); return true
    }
    fun displayed() { policy.displayed(); save(); changed?.invoke() }
    fun release() { save(); prefs?.edit()?.putBoolean("presenting", false)?.commit(); gate.release(); changed?.invoke() }
}
