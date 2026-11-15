package com.wamad.petaljong

import android.app.Activity
import android.content.Context
import com.wamad.petaljong.ads.GameAds
import com.wamad.petaljong.domain.*
import com.wamad.petaljong.storage.GameStore

/** Deterministic instrumentation only. Never packaged in the application APK. */
class FakeGameAds(private val context: Context) : GameAds {
    override var changed: (() -> Unit)? = null
    override var failed = false
    override val privacyRequired = false
    override var loading = false
    private var ready = true
    private var ticket: RewardTicket? = null
    private val gate by lazy { RewardPresentationGate(GameStore.get(context).wallet) }
    var starts = 0; private set
    override val available get() = ready && ticket == null
    override fun consent(activity: Activity) {}
    override fun privacy(activity: Activity) {}
    override fun load() { loading = true; changed?.invoke() }
    fun loaded() { loading = false; ready = true; changed?.invoke() }
    fun loadFailure() { loading = false; ready = false; failed = true; changed?.invoke() }
    override fun show(host: Activity): Boolean {
        if (!available) return false
        ticket = gate.begin() ?: return false
        ready = false; starts++; changed?.invoke(); return true
    }
    fun reward() {
        if (ticket?.earned() == true) { GameStore.persist(context); changed?.invoke() }
    }
    fun dismiss() { ticket?.let { gate.finish(it) }; ready = true; ticket = null; changed?.invoke() }
    fun displayFailure() { failed = true; dismiss() }
}
