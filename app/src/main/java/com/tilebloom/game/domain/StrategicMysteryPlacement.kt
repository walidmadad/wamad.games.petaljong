package com.tilebloom.game.domain

/** Place mystery information at dependency boundaries, never hide both copies of a pair.
 * Prefer a free-to-inspect symbol whose normal partner lies behind board dependencies.
 * Mysteries remain free to inspect: generation never scores their opacity as difficulty. */
object StrategicMysteryPlacement {
    fun apply(level: LevelDefinition): LevelDefinition {
        val count = when { level.id <= 7 -> 0; level.id <= 15 -> 2; level.id <= 30 -> 4; level.id <= 50 -> 6; else -> 8 }
        val position = level.winningSelections.withIndex().associate { it.value to it.index }
        val initiallyFree = level.tiles.filter { Rules.available(it, level.tiles) }.map { it.id }.toSet()
        val chosen = level.tiles.groupBy { it.symbol }.values.map { copies ->
            copies.maxBy { tile ->
                val coveredPartner = copies.any { it.id != tile.id && it.id !in initiallyFree }
                val gap = copies.maxOf { kotlin.math.abs(position.getValue(it.id) - position.getValue(tile.id)) }
                (if (tile.id in initiallyFree && coveredPartner) 1000 else 0) +
                    level.tiles.count { it.layer < tile.layer && Rules.overlaps(tile, it) } * 20 + gap
            }
        }.sortedWith(compareByDescending<Tile> { tile ->
            (if (tile.id in initiallyFree) 100 else 0) + level.tiles.count { it.layer < tile.layer && Rules.overlaps(tile, it) } * 10
        }.thenBy { (it.id.toLong() + level.id) % level.tiles.size }).take(count).map { it.id }.toSet()
        return level.copy(tiles = level.tiles.map { it.copy(mystery = it.id in chosen, revealed = false) })
    }
}
