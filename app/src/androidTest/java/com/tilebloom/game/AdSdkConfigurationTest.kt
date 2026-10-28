package com.tilebloom.game

import android.content.pm.PackageManager
import android.test.InstrumentationTestCase
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.UserMessagingPlatform

/** Linkage/configuration checks only; actual consent-gated initialization needs a live test. */
@Suppress("DEPRECATION")
class AdSdkConfigurationTest : InstrumentationTestCase() {
    fun testOfficialDebugSdkAndManifestConfiguration() {
        val context = instrumentation.targetContext
        val app = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assertEquals("ca-app-pub-3940256099942544~3347511713", app.metaData.getString("com.google.android.gms.ads.APPLICATION_ID"))
        assertEquals("ca-app-pub-3940256099942544/5224354917", BuildConfig.REWARDED_AD_UNIT)
        assertEquals("ca-app-pub-3940256099942544/1033173712", BuildConfig.INTERSTITIAL_AD_UNIT)
        assertEquals(25, MobileAds.getVersion().majorVersion)
        assertNotNull(UserMessagingPlatform.getConsentInformation(context))
    }
}
