package com.tilebloom.game.ads

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.*
import com.tilebloom.game.BuildConfig
import com.tilebloom.game.R
import com.tilebloom.game.domain.RewardPurpose
import com.tilebloom.game.storage.GameStore
import java.lang.ref.WeakReference

/** Main-thread, process-owned presentation state; never retains an Activity in our callbacks. */
object RewardedAdManager : Advertising {
    enum class State { IDLE, LOADING, READY, SHOWING, FAILED }
    override var state = State.IDLE; private set
    private val presentations = com.tilebloom.game.domain.AdPresentation()
    private var initialized = false
    private var initializing = false
    private var consentBusy = false
    private var ad: RewardedAd? = null
    private var loadedAt = 0L
    private var retryAt = 0L
    private var generation = 0
    private var host = WeakReference<Activity>(null)
    private var consent: ConsentInformation? = null
    val requestsPermitted get() = initialized && !consentBusy && consent?.canRequestAds() == true && host.get()?.let { com.tilebloom.game.presentation.ConnectivityMonitor.validated(it) } == true
    val consentInProgress get() = consentBusy
    override val privacyRequired get() = consent?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun attach(activity: Activity) {
        host = WeakReference(activity)
        val info = UserMessagingPlatform.getConsentInformation(activity.applicationContext)
        consent = info
        if (consentBusy) return
        consentBusy = true
        AdExposure.changed?.invoke()
        val weak = WeakReference(activity)
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            val current = weak.get()
            if (current == null || current.isDestroyed || current.isFinishing) {
                consentBusy = false
                host.get()?.takeUnless { it.isDestroyed || it.isFinishing }?.let { attach(it) }
                return@requestConsentInfoUpdate
            }
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(current) {
                consentBusy = false
                eligibilityChanged()
            }
        }, {
            consentBusy = false
            eligibilityChanged()
        })
        eligibilityChanged()
    }

    private fun eligibilityChanged() {
        AdExposure.changed?.invoke()
        (host.get() as? com.tilebloom.game.presentation.MainActivity)?.refreshPrivacyOptions()
        if (consent?.canRequestAds() != true) {
            generation++
            ad = null
            if (state != State.SHOWING) state = State.IDLE
            return
        }
        val context = host.get()?.applicationContext ?: return
        if (!initialized && !initializing) {
            initializing = true
            try { MobileAds.initialize(context) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    initializing = false; initialized = true; load(); AdExposure.changed?.invoke()
                }
            } } catch (error: RuntimeException) {
                initializing = false; state = State.FAILED
                android.util.Log.w("TileBloomAds", "Initialization failed", error)
            }
        } else if (initialized) load()
    }

    override fun privacyOptions(activity: Activity) {
        if (!privacyRequired || AdExposure.busy || state == State.SHOWING || consentBusy) return
        consentBusy = true
        AdExposure.changed?.invoke()
        generation++; ad = null; state = State.IDLE
        val weak = WeakReference(activity)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            consentBusy = false
            if (error != null) weak.get()?.takeUnless { it.isDestroyed }?.let {
                Toast.makeText(it, R.string.privacy_unavailable, Toast.LENGTH_LONG).show()
            }
            eligibilityChanged()
        }
    }

    override fun retry() { load() }

    fun load() {
        if (!requestsPermitted || consentBusy || consent?.canRequestAds() != true || state == State.SHOWING || state == State.LOADING) return
        if (state == State.READY && SystemClock.elapsedRealtime() - loadedAt < 3_600_000) return
        if (SystemClock.elapsedRealtime() < retryAt) return
        val context = host.get()?.applicationContext ?: return
        if (BuildConfig.REWARDED_AD_UNIT.isEmpty()) return
        ad = null; state = State.LOADING
        val request = ++generation
        try { RewardedAd.load(context, BuildConfig.REWARDED_AD_UNIT, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(value: RewardedAd) {
                if (request != generation || consent?.canRequestAds() != true) return
                ad = value; loadedAt = SystemClock.elapsedRealtime(); state = State.READY
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (request != generation) return
                ad = null; state = State.FAILED; retryAt = SystemClock.elapsedRealtime() + 30_000
                android.util.Log.w("TileBloomAds", "Load failed: ${error.code} ${error.domain}")
            }
        }) } catch (error: RuntimeException) {
            ad = null; state = State.FAILED; retryAt = SystemClock.elapsedRealtime() + 30_000
            android.util.Log.w("TileBloomAds", "Load failed", error)
        }
    }

    /** Only called after the player confirms Watch Ad; loading never queues a presentation. */
    override fun show(activity: Activity, purpose: RewardPurpose): Boolean {
        if (AdExposure.busy || !requestsPermitted || activity.isDestroyed || activity.isFinishing || consentBusy || consent?.canRequestAds() != true || state != State.READY) { load(); return false }
        if (SystemClock.elapsedRealtime() - loadedAt >= 3_600_000) { ad = null; state = State.IDLE; load(); return false }
        val selected = ad ?: return false
        val context = activity.applicationContext
        val transaction = presentations.begin(purpose) {
            val saved = GameStore.reward(context, it)
            Toast.makeText(context, if (saved) R.string.reward_saved else R.string.reward_save_failed, Toast.LENGTH_LONG).show()
            saved
        } ?: return false
        if (!AdExposure.acquire()) { transaction.cancel(); presentations.finish(transaction); return false }
        ad = null; state = State.SHOWING
        val lifecycle = com.tilebloom.game.domain.FullScreenAdLifecycle()
        selected.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { if (lifecycle.shown()) AdExposure.displayed() }
            override fun onAdDismissedFullScreenContent() {
                if (lifecycle.finish() && presentations.finish(transaction)) { state = State.IDLE; AdExposure.release(); load() }
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                transaction.cancel()
                if (!lifecycle.finish() || !presentations.finish(transaction)) return
                state = State.FAILED; retryAt = SystemClock.elapsedRealtime() + 30_000
                AdExposure.release()
                Toast.makeText(context, R.string.ad_unavailable, Toast.LENGTH_LONG).show()
            }
        }
        return try {
            selected.show(activity, OnUserEarnedRewardListener { transaction.earned() })
            true
        } catch (error: RuntimeException) {
            transaction.cancel(); presentations.finish(transaction)
            state = State.FAILED; retryAt = SystemClock.elapsedRealtime() + 30_000
            if (lifecycle.finish()) AdExposure.release()
            android.util.Log.w("TileBloomAds", "Presentation failed", error)
            false
        }
    }

    override fun detach(activity: Activity) { if (host.get() === activity) host.clear() }
}
