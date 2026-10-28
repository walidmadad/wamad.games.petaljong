package com.tilebloom.game.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

/** Default-route validation only; neither consent nor ad inventory is consulted. */
interface NetworkObserver {
    fun start()
    fun stop()
    fun retry()
    fun destroy()
}

/** Injectable source for deterministic Android UI tests; release always uses the platform. */
object ConnectivityService {
    var factory: (Context, (Boolean, Boolean) -> Unit) -> NetworkObserver = ::ConnectivityMonitor
        set(value) {
            check(com.tilebloom.game.BuildConfig.DEBUG)
            field = value
        }
}

class ConnectivityMonitor(context: Context, private val changed: (Boolean, Boolean) -> Unit) : NetworkObserver {
    companion object {
        /** Call on a worker thread, including cold save entry. */
        fun validated(context: Context): Boolean {
            val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            val caps = manager.activeNetwork?.let(manager::getNetworkCapabilities)
            return com.tilebloom.game.domain.InternetPolicy.validated(
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
        }
    }
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val worker = HandlerThread("garden-connectivity").apply { start() }
    private val handler = Handler(worker.looper)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var active = false
    @Volatile private var generation = 0
    private var callback: ConnectivityManager.NetworkCallback? = null
    private val state = com.tilebloom.game.domain.ConnectionState()
    private var offline: Runnable? = null
    private fun publish(online: Boolean, panel: Boolean) {
        val expected = generation
        main.post { if (active && expected == generation) changed(online, panel) }
    }
    private fun read() {
        if (!active) return
        offline?.let(handler::removeCallbacks)
        val network = manager.activeNetwork
        val caps = network?.let(manager::getNetworkCapabilities)
        val usable = com.tilebloom.game.domain.InternetPolicy.validated(
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
        val now = android.os.SystemClock.elapsedRealtime()
        state.update(usable, now)
        if (usable) { publish(true, false) } else {
            // Input stops immediately. Only the panel is delayed during a possible handover.
            publish(false, state.panelVisible(now))
            offline = Runnable { if (active) publish(false, state.panelVisible(android.os.SystemClock.elapsedRealtime())) }.also {
                handler.postDelayed(it, state.panelDelay(now))
            }
        }
    }
    override fun start() {
        if (active) return
        active = true; generation++
        handler.post {
            if (!active) return@post
            if (callback == null) {
                callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = read()
                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = read()
                    override fun onLost(network: Network) = read()
                    override fun onUnavailable() = read()
                }.also { manager.registerDefaultNetworkCallback(it, handler) }
            }
            read()
        }
    }
    override fun retry() { handler.post { read() } }
    override fun stop() {
        active = false; generation++
        handler.post {
            offline?.let(handler::removeCallbacks); offline = null
            callback?.let { manager.unregisterNetworkCallback(it) }; callback = null
        }
    }
    override fun destroy() { stop(); handler.post { worker.quitSafely() } }
}
