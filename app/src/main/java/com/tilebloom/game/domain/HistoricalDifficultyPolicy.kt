package com.tilebloom.game.domain

/** Pure, overflow-safe progression. Targets are proxies, never claims of human difficulty. */
object HistoricalDifficultyPolicy {
    fun tier(level: Long): Long { require(level > 0); return (level - 1) / 20 }
    fun profile(level: Long): DifficultyProfile {
        val tier = tier(level)
        val stage = minOf(tier, 6).toInt()
        val first = tier * 20 + 1 // <= level, even at Long.MAX_VALUE
        val last = first + minOf(19L, Long.MAX_VALUE - first)
        val ranges = listOf(24..28, 28..32, 32..36, 36..40, 40..44, 40..48, 40..48)
        return DifficultyProfile(tier, first, last, ranges[stage], minOf(16, 6 + stage * 2),
            listOf("river", "steps", "cross", "wings", "split", "pyramid", "offset", "staggered"),
            0..1, 12 + stage * 2, 2 + stage + minOf(6L, maxOf(0L, tier - 6)).toInt(),
            2 + stage / 2, 4, 1500 + stage * 250, ((level - 1) % 20).toInt(),
            if (tier >= 2 && tier % 3 != 0L) .5 else 0.0, (tier % 3).toInt())
    }
    fun symbols(level: Long) = Symbol.entries.filter { it.stableId < 20 && it.introducedAt <= level }
    fun introduced(level: Long) = Symbol.entries.filter { it.stableId < 20 && it.introducedAt == level && level > 1 }
    fun transitionMessage(level: Long): String? = if (level > 1 && (level - 1) % 20 == 0L)
        "New Garden Challenge!" + if (introduced(level).isNotEmpty()) " New tiles discovered." else "" else null
}
