package com.tilebloom.game.domain

data class GeometryMetrics(val layers: Int, val overlapRelationships: Int, val coveredPositions: Int,
    val initiallyBlocked: Int, val initiallyAvailable: Int) {
    val overlapDensity get() = overlapRelationships.toDouble() / (initiallyBlocked + initiallyAvailable).coerceAtLeast(1)
    companion object {
        fun measure(tiles: List<Tile>): GeometryMetrics {
            val live = tiles.filterNot { it.removed }
            val covered = live.count { a -> live.any { it.layer > a.layer && Rules.overlaps(a,it) } }
            val edges = live.sumOf { a -> live.count { it.layer > a.layer && Rules.overlaps(a,it) } }
            val free = live.count { Rules.available(it,live) }
            return GeometryMetrics(live.map { it.layer }.distinct().size,edges,covered,live.size-free,free)
        }
    }
}
