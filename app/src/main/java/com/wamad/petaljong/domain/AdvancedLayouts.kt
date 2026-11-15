package com.wamad.petaljong.domain

/** Eight compact footprints; scheduling is independent of candidate quality rejection. */
object AdvancedLayouts {
    val families = listOf("turtle", "butterfly", "flower", "pyramid", "bridge", "spiral", "crescent", "islands")
    private val masks = listOf(
        listOf(".####.", "######", "######", "######", ".####."),
        listOf("##..##", "######", ".####.", "######", "##..##"),
        listOf("..##..", ".####.", "######", ".####.", "..##.."),
        listOf("..##..", "..##..", ".####.", ".####.", "######"),
        listOf("##..##", "##..##", "######", "##..##", "##..##"),
        listOf("######", "....##", "####..", "##....", "######"),
        listOf(".#####", "###...", "##....", "###...", ".#####"),
        listOf("##..##", "##..##", "......", ".####.", ".####.")
    )
    fun layout(level: Long, variant: Int = 0): BoardLayout {
        require(level >= 6)
        val family = ((level - 6) % 8).toInt()
        val cycle = ((level - 6) / 8 % 12).toInt()
        val stage = DifficultyPolicy.difficulty(level).ordinal
        val slots = mutableListOf<TileSlot>()
        fun add(x: Double, y: Double, z: Int) { slots.add(TileSlot(slots.size, x, y, z)) }
        masks[family].forEachIndexed { y, row -> row.forEachIndexed { x, c ->
            if (c == '#') add(x.toDouble(), y.toDouble(), 0)
        } }
        // Relocate two base positions into existing negative space, keeping the footprint
        // bounded on short screens. This changes holes and branches, not symbol order.
        if (cycle % 3 != 0) {
            val holes = (0..4).flatMap { y -> (0..5).map { x -> x.toDouble() to y.toDouble() } }
                .filter { (x,y) -> slots.none { it.x == x && it.y == y } }
            val destinations = if (cycle % 3 == 1) holes.take(2) else holes.takeLast(2)
            val source = slots.sortedBy { kotlin.math.abs(it.y-2.0) + kotlin.math.abs(it.x-2.5) }.take(2)
            slots.removeAll(source.toSet())
            destinations.forEach { (x,y) -> add(x,y,0) }
            val normalized = slots.mapIndexed { i,t -> t.copy(id=i) }
            slots.clear(); slots.addAll(normalized)
        }
        // Supported horizontal pairs retain even terraces and a bounded peel witness.
        var support = slots.toList()
        for (z in 1..minOf(3, stage + 1)) {
            val pairs = support.groupBy { it.y }.toSortedMap().values.flatMap { row ->
                val sorted = row.sortedBy { it.x }
                sorted.chunked(2).filter { it.size == 2 && it[1].x - it[0].x == 1.0 }.map { it[0] to it[1] }
            }
            val budget = if (z == 1) listOf(2,4,5,7)[stage] else if (z == 2) (if (stage == 3) 5 else 4) else (if (stage == 3) 3 else 2)
            val rotated = if (pairs.isEmpty()) pairs else pairs.drop((cycle + variant) % pairs.size) + pairs.take((cycle + variant) % pairs.size)
            val chosen = rotated.take(budget)
            val next = mutableListOf<TileSlot>()
            for ((a,b) in chosen) {
                // Deduplicate overlapping pairs before emitting same-layer positions.
                for (t in listOf(a,b)) if (next.none { it.x == t.x && it.y == t.y })
                    next.add(TileSlot(0,t.x,t.y,z))
            }
            if (next.size % 2 != 0) next.removeAt(next.lastIndex)
            val offset = if (z % 2 == 1) .5 else -.5
            val shifted = next.map { it.copy(x = it.x + offset, y = it.y + offset) }
            shifted.forEach { add(it.x,it.y,z) }
            support = shifted
        }
        return BoardLayout("${families[family]}-$cycle-$variant", slots)
    }
    /** Translation independent, includes all terraces; IDs and botanical artwork are excluded. */
    fun fingerprint(layout: BoardLayout): String {
        val x = layout.slots.minOf { it.x }; val y = layout.slots.minOf { it.y }
        return layout.slots.sortedWith(compareBy<TileSlot> { it.layer }.thenBy { it.y }.thenBy { it.x })
            .joinToString(";") { "${it.x-x},${it.y-y},${it.layer}" }
    }
    fun recentDuplicate(layout: BoardLayout, recent: List<BoardLayout>) =
        recent.takeLast(10).any { fingerprint(it) == fingerprint(layout) }
}
