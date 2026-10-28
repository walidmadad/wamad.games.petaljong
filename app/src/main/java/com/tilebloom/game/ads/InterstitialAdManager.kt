package com.tilebloom.game.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.tilebloom.game.BuildConfig

interface VictoryAdvertising {
    val ready: Boolean
    fun prepare(activity: Activity)
    fun show(activity: Activity): Boolean
}
object InterstitialService {
    var provider: VictoryAdvertising = InterstitialAdManager
        set(value) { check(BuildConfig.DEBUG); field = value }
}

/** One cached ad, bounded retry, no retained Activity and no queued presentation. */
object InterstitialAdManager : VictoryAdvertising {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var loadedAt = 0L
    private var retryAt = 0L
    override val ready get() = ad != null && RewardedAdManager.requestsPermitted && !AdExposure.busy && SystemClock.elapsedRealtime() - loadedAt < 3_600_000
    override fun prepare(activity: Activity) {
        if (!RewardedAdManager.requestsPermitted || AdExposure.busy || loading || SystemClock.elapsedRealtime() < retryAt) return
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt < 3_600_000) return
        if (BuildConfig.INTERSTITIAL_AD_UNIT.isEmpty()) return
        loading = true; ad = null
        try {
            InterstitialAd.load(activity.applicationContext, BuildConfig.INTERSTITIAL_AD_UNIT, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(value: InterstitialAd) { loading = false; if (RewardedAdManager.requestsPermitted) { ad = value; loadedAt = SystemClock.elapsedRealtime() } }
                override fun onAdFailedToLoad(error: LoadAdError) { loading = false; retryAt = SystemClock.elapsedRealtime() + 30_000; Log.d("TileBloomAds", "Interstitial load failed: ${error.code}") }
            })
        } catch (error: RuntimeException) { loading = false; retryAt = SystemClock.elapsedRealtime() + 30_000; Log.w("TileBloomAds", "Interstitial load failed", error) }
    }
    override fun show(activity: Activity): Boolean {
        if (activity.isDestroyed || activity.isFinishing || !activity.hasWindowFocus() || !RewardedAdManager.requestsPermitted || SystemClock.elapsedRealtime() - loadedAt >= 3_600_000) return false
        val selected = ad ?: return false
        if (!AdExposure.acquire()) return false
        ad = null
        val lifecycle = com.tilebloom.game.domain.FullScreenAdLifecycle()
        fun finish() { if (lifecycle.finish()) AdExposure.release() }
        selected.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { if (lifecycle.shown()) AdExposure.displayed() }
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) { Log.d("TileBloomAds", "Interstitial show failed: ${error.code}"); finish() }
        }
        return try { selected.show(activity); true } catch (error: RuntimeException) { finish(); false }
    }
}
