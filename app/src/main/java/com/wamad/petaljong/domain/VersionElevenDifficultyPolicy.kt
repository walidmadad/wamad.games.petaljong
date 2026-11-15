package com.wamad.petaljong.domain

object VersionElevenDifficultyPolicy {
    fun difficulty(level: Long): Difficulty { require(level > 0); return when(level) {
        in 1..7 -> Difficulty.NORMAL
        in 8..15 -> Difficulty.HARD
        in 16..30 -> Difficulty.VERY_HARD
        else -> Difficulty.EXPERT
    } }
    fun tier(level: Long) = difficulty(level).ordinal.toLong()
    fun profile(level: Long): DifficultyProfile {
        val stage = difficulty(level).ordinal
        val first = listOf(1L,8L,16L,31L)[stage]
        val last = listOf(7L,15L,30L,Long.MAX_VALUE)[stage]
        return DifficultyProfile(stage.toLong(), first, last,
            listOf(20..34,32..46,38..52,46..60)[stage],
            minOf(symbols(level).size, listOf(6,8,10,16)[stage]),
            AdvancedLayouts.families, 0..(stage + 1).coerceAtMost(3),
            listOf(12,22,30,34)[stage], listOf(2,4,6,8)[stage],
            6, 6, 4000 + stage * 1000, ((level - 1) % 8).toInt(), .5, stage)
    }
    fun symbols(level: Long) = Symbol.entries.filter { it.stableId < 23 && it.introducedAt <= level }
    fun introduced(level: Long) = Symbol.entries.filter { it.stableId < 23 && it.introducedAt == level && level > 1 }
    fun transitionMessage(level: Long): String? = when(level) {
        8L -> "Hard Garden Challenge"
        16L -> "Very Hard Garden Challenge"
        31L -> "Expert Garden Challenge"
        else -> if (introduced(level).isNotEmpty()) "New tiles discovered." else null
    }
}
