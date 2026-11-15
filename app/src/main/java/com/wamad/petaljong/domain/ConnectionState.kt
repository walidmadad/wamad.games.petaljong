package com.wamad.petaljong.domain

/** Input closes at once; repeated network callbacks cannot postpone the panel indefinitely. */
class ConnectionState {
    var online = false; private set
    private var offlineSince: Long? = null
    fun update(validated: Boolean, now: Long) {
        online = validated
        if (validated) offlineSince = null else if (offlineSince == null) offlineSince = now
    }
    fun panelVisible(now: Long) = !online && offlineSince?.let { now - it >= HANDOVER_MS } == true
    fun panelDelay(now: Long) = offlineSince?.let { (HANDOVER_MS - (now - it)).coerceAtLeast(0) } ?: HANDOVER_MS
    companion object { const val HANDOVER_MS = 600L }
}
