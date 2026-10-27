package com.tilebloom.game.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.ump.*
import com.tilebloom.game.BuildConfig
import com.tilebloom.game.storage.GameStore

/** Main-thread, process-owned SDK state. Never retains an Activity or a screen reward closure. */
class RewardedAdManager private constructor(private val context: Context) : GameAds {
    companion object {
        private var instance: RewardedAdManager? = null
        fun get(context: Context) = instance ?: RewardedAdManager(context.applicationContext).also { instance = it }
    }
    private val consent = UserMessagingPlatform.getConsentInformation(context)
    private val handler = Handler(Looper.getMainLooper())
    private var initialized = false
    private var initializing = false
    private var showing = false
    private val gate by lazy { com.tilebloom.game.domain.RewardPresentationGate(GameStore.get(context).wallet) }
    private var ad: RewardedAd? = null
    private var retryAfter = 0L
    private var loadedAt = 0L
    override var failed = false; private set
    override var changed: (() -> Unit)? = null
    override var loading = false; private set
    override val available get() = ad != null && android.os.SystemClock.elapsedRealtime() - loadedAt < 3_300_000 && consent.canRequestAds() && !showing
    override val privacyRequired get() = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    override fun consent(activity: Activity) {
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (!activity.isDestroyed && !activity.isFinishing) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refreshPermission() }
            }
        }, { refreshPermission() })
        refreshPermission()
    }
    override fun privacy(activity: Activity) {
        ad = null
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { refreshPermission() }
    }
    private fun refreshPermission() {
        if (!consent.canRequestAds()) { ad = null; changed?.invoke(); return }
        if (!initialized && !initializing) {
            initializing = true
            try {
                MobileAds.initialize(context) {
                    handler.post { initializing = false; initialized = true; load() }
                }
            } catch (_: Exception) { initializing = false }
        } else load()
        changed?.invoke()
    }
    override fun load() {
        if (ad != null && android.os.SystemClock.elapsedRealtime() - loadedAt >= 3_300_000) ad = null
        if (!initialized || !consent.canRequestAds() || loading || showing || ad != null || android.os.SystemClock.elapsedRealtime() < retryAfter) return
        loading = true; failed = false; changed?.invoke()
        try {
            RewardedAd.load(context, BuildConfig.REWARDED_TEST_UNIT, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(value: RewardedAd) {
                    loading = false; loadedAt = android.os.SystemClock.elapsedRealtime()
                    ad = if (consent.canRequestAds()) value else null
                    changed?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false; failed = true; ad = null
                    retryAfter = android.os.SystemClock.elapsedRealtime() + 30_000
                    changed?.invoke()
                }
            })
        } catch (_: Exception) { loading = false; failed = true; retryAfter = android.os.SystemClock.elapsedRealtime() + 30_000; changed?.invoke() }
    }
    override fun show(host: Activity): Boolean {
        if (!available || host.isDestroyed || host.isFinishing) { load(); return false }
        val current = ad ?: return false
        val ticket = gate.begin() ?: return false
        ad = null; showing = true
        var ended = false
        fun finish() {
            if (ended) return
            ended = true; gate.finish(ticket); showing = false; changed?.invoke(); load()
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) { failed = true; retryAfter = android.os.SystemClock.elapsedRealtime() + 30_000; finish() }
        }
        try {
            current.show(host) {
                if (ticket.earned()) {
                    GameStore.persist(context)
                    // Promptly finish the ordered atomic write even when the activity was replaced.
                    com.tilebloom.game.storage.ProgressRepository.awaitWrites()
                    changed?.invoke()
                }
            }
        } catch (_: Exception) { finish(); return false }
        changed?.invoke()
        return true
    }
}
