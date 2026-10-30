package com.tilebloom.game.domain

enum class ConnectivityStatus { CHECKING, ONLINE, OFFLINE }

/** Worker-confined reducer. Deadlines never move on repeated negative observations. */
class ConnectivityState(
    private val verificationMs: Long = 1500,
    private val handoverMs: Long = 600
) {
    var status = ConnectivityStatus.CHECKING; private set
    var revision = 0L; private set
    private var deadline: Long? = null
    fun begin(now: Long) {
        revision++
        status = ConnectivityStatus.CHECKING
        deadline = now + verificationMs
    }
    fun observe(validated: Boolean, now: Long) {
        revision++
        if (validated) {
            status = ConnectivityStatus.ONLINE
            deadline = null
        } else if (status == ConnectivityStatus.ONLINE) {
            status = ConnectivityStatus.CHECKING
            deadline = now + handoverMs
        }
    }
    fun snapshot(validated: Boolean, now: Long, expectedRevision: Long) {
        if (revision == expectedRevision) observe(validated, now)
    }
    fun tick(now: Long) {
        if (status == ConnectivityStatus.CHECKING && deadline?.let { now >= it } == true) {
            status = ConnectivityStatus.OFFLINE
            deadline = null
        }
    }
    fun delay(now: Long): Long? = deadline?.let { (it - now).coerceAtLeast(0) }
}
