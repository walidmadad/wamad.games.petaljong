package com.wamad.petaljong.domain

/** Economy independent of views. A reward ticket belongs to one SDK presentation. */
class UndoCreditWallet(initial: Int = 3) {
    init { require(initial >= 0) }
    var balance = initial; private set
    fun undo(restore: () -> Boolean): Boolean {
        if (balance == 0 || !restore()) return false
        balance--
        return true
    }
    fun rewardTicket(): RewardTicket = RewardTicket {
        check(balance < Int.MAX_VALUE)
        balance++
    }
}
class RewardTicket internal constructor(private val grant: () -> Unit) {
    private var delivered = false
    fun earned(): Boolean {
        if (delivered) return false
        grant(); delivered = true
        return true
    }
}

/** One presentation at a time; fake implementations belong in test source sets. */
interface RewardedAdProvider<Host> {
    val loading: Boolean
    val available: Boolean
    fun load()
    fun show(host: Host): Boolean
}

/** Owns single-use presentation tickets, independent of SDK object lifecycle. */
class RewardPresentationGate(private val wallet: UndoCreditWallet) {
    private var active: RewardTicket? = null
    fun begin(): RewardTicket? {
        if (active != null) return null
        return wallet.rewardTicket().also { active = it }
    }
    fun finish(ticket: RewardTicket) { if (active === ticket) active = null }
}
