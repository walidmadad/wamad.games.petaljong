package com.wamad.petaljong.domain

/** v12 catalog only. Historical policies deliberately retain IDs 0..22. */
object BotanicalPalette {
    val additions = Symbol.entries.filter { it.stableId >= 23 }
    private val groups = listOf(
        listOf(Symbol.FLOWER, Symbol.PINK_PEONY, Symbol.PINK_CAMELLIA),
        listOf(Symbol.SUNFLOWER, Symbol.YELLOW_MARIGOLD, Symbol.GOLDEN_CHRYSANTHEMUM, Symbol.YELLOW_DAISY),
        listOf(Symbol.LEAF, Symbol.MONSTERA_LEAF, Symbol.EUCALYPTUS_LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF),
        listOf(Symbol.PURPLE_CROCUS, Symbol.PURPLE_BELLFLOWER, Symbol.LAVENDER, Symbol.IRIS),
        listOf(Symbol.RED_JAPANESE_MAPLE, Symbol.ORANGE_SUGAR_MAPLE, Symbol.MAPLE_LEAF),
        listOf(Symbol.OLIVE_BRANCH, Symbol.ROSEMARY_BRANCH))
    fun relatedGroups(symbols: Collection<Symbol>) = groups.map { g -> g.filter { it in symbols } }.filter { it.size >= 2 }
    fun required(level: Long): List<Symbol> {
        if (level < 8) return emptyList()
        val eligible = groups.map { g -> g.filter { it.introducedAt <= level } }.filter { it.size >= 2 }
        val count = when { level <= 15 -> 1; level <= 30 -> 2; level <= 50 -> 3; else -> 4 }
        val offset = (level % eligible.size).toInt()
        return (eligible.drop(offset) + eligible.take(offset)).take(count).flatMap { it.take(2) }
    }
    fun active(level: Long, pairs: Int): List<Symbol> {
        if (level < 8) return DifficultyPolicy.symbols(level)
        val catalog = Symbol.entries.filter { it.introducedAt <= level }
        val offset = (level % catalog.size).toInt()
        val priority = (SymbolFamilies.required(level) + required(level) + catalog.filter { it.stableId in 16..19 } +
            catalog.filter { it.introducedAt == level } + catalog.drop(offset) + catalog.take(offset)).distinct()
        // Reduce accidental interchangeable partners as Expert advances, without adding tiles.
        val limit = when { level <= 15 -> 10; level <= 30 -> 16; level <= 50 -> 20; level <= 100 -> 24; else -> 30 }
        // Keep at least two extra pairs so choosing which copy to remove can matter.
        return priority.take(minOf(maxOf(1, pairs - 2), limit))
    }
}
