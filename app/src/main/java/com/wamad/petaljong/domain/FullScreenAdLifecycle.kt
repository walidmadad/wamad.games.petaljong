package com.wamad.petaljong.domain

/** Ignores duplicate and late SDK callbacks, including callbacks after a failed show. */
class FullScreenAdLifecycle {
    private var ended = false
    private var displayed = false
    fun shown(): Boolean {
        if (ended || displayed) return false
        displayed = true
        return true
    }
    fun finish(): Boolean {
        if (ended) return false
        ended = true
        return true
    }
}

class FullScreenAdGate {
    var busy = false; private set
    fun acquire(): Boolean { if (busy) return false; busy = true; return true }
    fun release() { busy = false }
}
