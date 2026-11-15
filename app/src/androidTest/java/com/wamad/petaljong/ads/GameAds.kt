package com.wamad.petaljong.ads

import android.app.Activity
import com.wamad.petaljong.domain.RewardedAdProvider

interface GameAds : RewardedAdProvider<Activity> {
    var changed: (() -> Unit)?
    val failed: Boolean
    val privacyRequired: Boolean
    fun consent(activity: Activity)
    fun privacy(activity: Activity)
}
