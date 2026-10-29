package com.tilebloom.game.domain

/** Pure, overflow-safe progression. Targets are proxies, never claims of human difficulty. */
object VersionSevenDifficultyPolicy {
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
            listOf(24..28,32..36,40..44,44..48)[stage],
            minOf(symbols(level).size, listOf(6,8,10,16)[stage]),
            listOf("classic", "compact"), 0..(stage + 1).coerceAtMost(3),
            listOf(12,22,30,34)[stage], listOf(2,4,6,8)[stage],
            6, 6, 2000 + stage * 500, ((level - 1) % 8).toInt(), .5, stage)
    }
    fun symbols(level: Long) = Symbol.entries.filter { it.stableId < 20 && it.introducedAt <= level }
    fun introduced(level: Long) = Symbol.entries.filter { it.stableId < 20 && it.introducedAt == level && level > 1 }
    fun transitionMessage(level: Long): String? = when(level) {
        8L -> "Hard Garden Challenge"
        16L -> "Very Hard Garden Challenge"
        31L -> "Expert Garden Challenge"
        else -> if (introduced(level).isNotEmpty()) "New tiles discovered." else null
    }
}
