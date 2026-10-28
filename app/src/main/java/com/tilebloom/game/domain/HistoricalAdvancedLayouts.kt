package com.tilebloom.game.domain

/** Compact, bounded footprints. Every terrace contains an even number of positions. */
object HistoricalAdvancedLayouts {
    fun layout(level: Long, variant: Int = 0): BoardLayout {
        val stage = VersionSevenDifficultyPolicy.difficulty(level).ordinal
        val classic = level % 2L == 0L
        val slots = mutableListOf<TileSlot>()
        fun add(x: Double, y: Double, z: Int) { slots.add(TileSlot(slots.size, x, y, z)) }
        if (classic) {
            for (y in 0..4) for (x in 0..5) {
                if (y == 0 || y == 4 || x !in 2..3) add(x.toDouble(), y.toDouble(), 0)
            }
            val rows = if (stage == 0) listOf(1.5) else if (stage == 1) listOf(.5, 3.5) else listOf(.5, 2.0, 3.5)
            for (y in rows) for (x in listOf(.5,1.5,3.5,4.5)) add(x, y, 1)
            if (stage >= 1) {
                val ys = if (stage == 3) listOf(.75,1.75,2.75,3.75) else listOf(.75,3.75)
                for (y in ys) for (x in listOf(1.0,4.0)) add(x,y,2)
            }
            if (stage >= 2) for (x in listOf(1.0,4.0)) {
                if (stage == 3) { add(x,1.25,3); add(x,2.75,3) } else add(x,3.75,3)
            }
        } else {
            for (y in 0..4) for (x in 0..3) add(x.toDouble(), y.toDouble(),0)
            val rows = if (stage == 0) listOf(1.0,2.0) else listOf(.5,1.5,2.5,3.5)
            val xs = if (stage == 0) listOf(.5,2.5) else listOf(.5,1.5,2.5)
            for (y in rows) for (x in xs) add(x,y,1)
            if (stage >= 1) for (y in (if(stage == 1) listOf(1.0,3.0) else listOf(.75,1.75,2.75,3.75)))
                for (x in listOf(1.0,2.0)) add(x,y,2)
            if (stage >= 2) for (x in listOf(1.0,2.0)) {
                add(x,1.25,3); if (stage == 3) add(x,2.75,3)
            }
        }
        val mirror = ((level / 2 + variant) % 2L == 1L)
        val stagger = ((level / 4 + variant) % 3L).toInt() * .125
        val rise = ((level / 2 + variant) % 3L).toInt() * .1
        val maxX = slots.maxOf { it.x }
        return BoardLayout("${if(classic) "classic" else "compact"}-$stage-${level % 24}-$variant",
            slots.map { it.copy(x = (if(mirror) maxX - it.x else it.x) + if(it.layer > 0) stagger else 0.0, y = it.y + if(it.layer > 0) rise else 0.0) })
    }
}
