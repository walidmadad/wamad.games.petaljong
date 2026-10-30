package com.tilebloom.game.domain

import kotlin.math.abs

/** Geometry-only index for bounded analysis, never a substitute for Game.select validation.
 * Unknown artwork does not participate. Larger arbitrary fixtures use production Rules. */
internal class AvailabilityIndex(private val tiles: List<Tile>) {
    private val indices = tiles.withIndex().associate { it.value.id to it.index }
    private val above = LongArray(tiles.size)
    private val left = LongArray(tiles.size)
    private val right = LongArray(tiles.size)
    init {
        if (tiles.size <= 64) for ((i,t) in tiles.withIndex()) for ((j,o) in tiles.withIndex()) {
            if(i==j) continue
            val bit=1L shl j
            if(o.layer>t.layer && Rules.overlaps(t,o)) above[i]=above[i] or bit
            if(o.layer==t.layer && minOf(t.y+1,o.y+1)-maxOf(t.y,o.y)>Rules.EPS) {
                if(abs(o.x+1-t.x)<=Rules.EPS) left[i]=left[i] or bit
                if(abs(t.x+1-o.x)<=Rules.EPS) right[i]=right[i] or bit
            }
        }
    }
    fun free(remaining: List<Tile>): List<Tile> {
        if(tiles.size>64) return remaining.filter { Rules.available(it,remaining) }
        var mask=0L
        for(t in remaining) if(!t.removed) mask=mask or (1L shl indices.getValue(t.id))
        return remaining.filter { t ->
            val i=indices.getValue(t.id)
            !t.removed && above[i] and mask==0L && (left[i] and mask==0L || right[i] and mask==0L)
        }
    }
}
