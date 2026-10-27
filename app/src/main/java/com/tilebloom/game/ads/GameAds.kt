package com.tilebloom.game.ads

import android.app.Activity
import com.tilebloom.game.domain.RewardedAdProvider

interface GameAds : RewardedAdProvider<Activity> {
    var changed: (() -> Unit)?
    val failed: Boolean
    val privacyRequired: Boolean
    fun consent(activity: Activity)
    fun privacy(activity: Activity)
}
