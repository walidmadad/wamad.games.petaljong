package com.wamad.petaljong.domain

enum class RewardPurpose { UNDO, HINT }

/** An immutable purpose and a one-shot SDK callback. Never re-created on rotation. */
class AdReward(val purpose: RewardPurpose, private val grant: (RewardPurpose) -> Boolean) {
    private var called = false
    fun cancel() { called = true }
    fun earned(): Boolean {
        if (called) return false
        called = true
        return grant(purpose)
    }
}

/** Serializes presentations independently of SDK callbacks and Activity instances. */
class AdPresentation {
    private var active: AdReward? = null
    fun begin(purpose: RewardPurpose, grant: (RewardPurpose) -> Boolean): AdReward? {
        if (active != null) return null
        return AdReward(purpose, grant).also { active = it }
    }
    fun finish(ticket: AdReward): Boolean {
        if (active !== ticket) return false
        active = null
        return true
    }
}
