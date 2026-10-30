package com.tilebloom.game.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tilebloom.game.domain.ConnectivityStatus
import com.tilebloom.game.domain.ConnectivityState

/** Default-route validation only; neither consent nor ad inventory is consulted. */
interface NetworkObserver {
    fun start()
    fun stop()
    fun retry()
    fun destroy()
}

/** Injectable source for deterministic Android UI tests; release always uses the platform. */
object ConnectivityService {
    var factory: (Context, (ConnectivityStatus) -> Unit) -> NetworkObserver = ::ConnectivityMonitor
        set(value) {
            check(com.tilebloom.game.BuildConfig.DEBUG)
            field = value
        }
}

class ConnectivityMonitor(context: Context, private val changed: (ConnectivityStatus) -> Unit) : NetworkObserver {
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
    private val state = ConnectivityState()
    private var current: Network? = null
    private var timeout: Runnable? = null
    private fun now() = android.os.SystemClock.elapsedRealtime()
    private fun publish() {
        val expected = generation
        val value = state.status
        main.post { if (active && expected == generation) changed(value) }
        timeout?.let(handler::removeCallbacks)
        timeout = null
        state.delay(now())?.let { delay ->
            timeout = Runnable { if (active) { state.tick(now()); publish() } }.also {
                handler.postDelayed(it, delay)
            }
        }
    }
    private fun observe(validated: Boolean) {
        state.observe(validated, now())
        publish()
    }
    // Only startup/retry snapshots query synchronously; callbacks use their supplied capabilities.
    private fun snapshot() {
        val revision = state.revision
        current = manager.activeNetwork
        val caps = current?.let(manager::getNetworkCapabilities)
        state.snapshot(usable(caps), now(), revision)
        publish()
    }
    private fun usable(caps: NetworkCapabilities?) =
        com.tilebloom.game.domain.InternetPolicy.validated(
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    override fun start() {
        if (active) return
        active = true
        val expected = ++generation
        handler.post {
            if (!active || expected != generation) return@post
            state.begin(now())
            callback = object : ConnectivityManager.NetworkCallback() {
                private fun valid() = active && expected == generation
                override fun onAvailable(network: Network) {
                    if (!valid()) return
                    current = network
                    observe(false)
                }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (valid() && network == current) observe(usable(capabilities))
                }
                override fun onLost(network: Network) {
                    if (valid() && network == current) { current = null; observe(false) }
                }
            }.also { manager.registerDefaultNetworkCallback(it, handler) }
            // Registration and snapshot run on the callback handler: queued newer events win.
            snapshot()
        }
    }
    override fun retry() {
        val expected = generation
        handler.post {
            if (active && expected == generation) { state.begin(now()); snapshot() }
        }
    }
    override fun stop() {
        active = false; generation++
        handler.post {
            timeout?.let(handler::removeCallbacks); timeout = null
            callback?.let { manager.unregisterNetworkCallback(it) }; callback = null
            current = null
        }
    }
    override fun destroy() { stop(); handler.post { worker.quitSafely() } }
}
