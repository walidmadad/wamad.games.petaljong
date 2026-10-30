package com.tilebloom.game.domain

/** Explicit recognition families; equality/matching always uses the full stable symbol ID. */
enum class SymbolFamily { LEAVES, FLOWERS, YELLOW_FLOWERS, GREENERY, PURPLE_FLOWERS, BRANCHES, OTHER }
object SymbolFamilies {
    fun family(symbol: Symbol): SymbolFamily = when (symbol) {
        Symbol.LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF, Symbol.MAPLE_LEAF, Symbol.GINKGO_LEAF, Symbol.MONSTERA_LEAF, Symbol.EUCALYPTUS_LEAF, Symbol.RED_JAPANESE_MAPLE, Symbol.ORANGE_SUGAR_MAPLE -> SymbolFamily.LEAVES
        Symbol.FLOWER, Symbol.CHERRY_BLOSSOM, Symbol.PINK_PEONY, Symbol.PINK_CAMELLIA -> SymbolFamily.FLOWERS
        Symbol.SUNFLOWER, Symbol.YELLOW_DAISY, Symbol.YELLOW_MARIGOLD, Symbol.GOLDEN_CHRYSANTHEMUM -> SymbolFamily.YELLOW_FLOWERS
        Symbol.FERN, Symbol.BAMBOO, Symbol.CACTUS -> SymbolFamily.GREENERY
        Symbol.LOTUS, Symbol.LAVENDER, Symbol.IRIS, Symbol.PURPLE_CROCUS, Symbol.PURPLE_BELLFLOWER -> SymbolFamily.PURPLE_FLOWERS
        Symbol.OLIVE_BRANCH, Symbol.ROSEMARY_BRANCH -> SymbolFamily.BRANCHES
        else -> SymbolFamily.OTHER
    }
    fun required(level: Long): List<Symbol> = when (DifficultyPolicy.difficulty(level)) {
        Difficulty.NORMAL -> emptyList()
        Difficulty.HARD -> listOf(Symbol.LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF)
        Difficulty.VERY_HARD -> listOf(Symbol.LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF, Symbol.SUNFLOWER, Symbol.YELLOW_DAISY)
        Difficulty.EXPERT -> listOf(Symbol.LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF, Symbol.SUNFLOWER, Symbol.YELLOW_DAISY, Symbol.LOTUS, Symbol.LAVENDER)
    }
    fun mixedCount(tiles: List<Tile>) = tiles.map { it.symbol }.distinct().groupBy(::family)
        .count { it.key != SymbolFamily.OTHER && it.value.size >= 2 }
}

/** Saturating targets keep tiles readable and work bounded even at Long.MAX_VALUE.
 * Later 25-level windows approach practical limits; local levels retain variation. */
data class ExpertTargets(val band: Long, val delayed: Int, val pressure: Int, val depth: Int, val maxShallowWins: Int) {
    fun accepts(a: StrategicAnalysis) = a.delayedMatches >= delayed && a.witnessPressure >= pressure &&
        a.blockingDepth >= depth && a.greedyProbeWins <= maxShallowWins && a.simpleProbeWins <= maxShallowWins
    fun score(a: StrategicAnalysis) = (if (accepts(a)) 50000 else 0) +
        minOf(delayed, a.delayedMatches) * 80 + minOf(pressure, a.witnessPressure) * 30 + minOf(depth, a.blockingDepth) * 100
}
object ExpertProgression {
    fun targets(level: Long): ExpertTargets {
        if (level < 31) return ExpertTargets(0, 0, 0, 0, 3)
        val band = when { level <= 50 -> 0L; level <= 75 -> 1L; level <= 100 -> 2L; else -> 3L + (level - 101) / 25 }
        val step = minOf(12L, band).toInt()
        return ExpertTargets(band, 6 + step, 12 + step, 4 + minOf(2, step / 3), if (step >= 4) 0 else 1)
    }
}
object MysteryPlacement {
    fun apply(level: LevelDefinition): LevelDefinition {
        val id = level.id
        val count = when {
            id <= 7 -> 0
            id <= 15 -> 2 + (id % 2).toInt()
            id <= 30 -> 4 + (id % 3).toInt()
            id <= 50 -> 6 + (id % 3).toInt()
            else -> 7 + (id % 4).toInt()
        }
        // Spread along the verified route: some immediately free, others behind dependencies.
        // Free revelation costs no tray space; it is information discovery, not added proven difficulty.
        val required = SymbolFamilies.required(id)
        val symbols = level.tiles.map { it.symbol }.distinct()
        val replacement = (required + symbols).distinct()
        val remap = if (symbols.containsAll(required)) symbols.associateWith { it } else symbols.zip(replacement).toMap()
        val order = level.winningSelections
        val offset = (id % 3).toInt()
        val ids = (0 until minOf(count, order.size)).map { order[(it * order.size / count + offset) % order.size] }.toSet()
        return level.copy(tiles = level.tiles.map { it.copy(symbol = remap.getValue(it.symbol), mystery = it.id in ids, revealed = false) }).also(LevelValidator::validate)
    }
}
