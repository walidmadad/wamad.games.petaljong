package com.wamad.petaljong.ads

import android.app.Activity
import com.wamad.petaljong.domain.RewardPurpose

interface Advertising {
    val state: RewardedAdManager.State
    val privacyRequired: Boolean
    fun attach(activity: Activity)
    fun detach(activity: Activity)
    fun privacyOptions(activity: Activity)
    fun prepare(purpose: RewardPurpose) {}
    fun retry() {}
    fun show(activity: Activity, purpose: RewardPurpose): Boolean
}

/** Test source sets may supply a deterministic provider; shipping default is the Google SDK. */
object AdvertisingService {
    var provider: Advertising = RewardedAdManager
        set(value) {
            check(com.wamad.petaljong.BuildConfig.DEBUG) { "Advertising provider is fixed in release builds" }
            field = value
        }
}
