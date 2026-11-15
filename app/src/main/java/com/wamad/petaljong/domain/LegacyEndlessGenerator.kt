package com.wamad.petaljong.domain

/** Version 4: signed 64-bit identity, SplitMix64 seed, eight silhouette cycle, four reflections.
 * No dependency on wall time, process randomness, or player history. At most 6 × 64
 * construction attempts and 6 × 2000 analyzer nodes; verified fallback relaxes quality only.
 */
object LegacyEndlessGenerator {
    const val VERSION = 4
    const val RECENT_WINDOW = 4
    private val rows = listOf(
        listOf(0 to 3, 0 to 4, 1 to 5, 2 to 4, 2 to 4), // winding terraces
        listOf(0 to 4, -1 to 5, -1 to 5, 0 to 4, 1 to 2), // stepped garden
        listOf(1 to 2, 0 to 4, -1 to 6, 0 to 4, 1 to 4), // layered cross
        listOf(-1 to 5, 0 to 5, 1 to 4, 0 to 4, 2 to 2), // asymmetric wings
        listOf(0 to 4, 0 to 4, -1 to 6, 1 to 2, 0 to 4), // split garden
        listOf(1 to 2, 0 to 4, -1 to 6, -1 to 6, 1 to 2), // compact pyramid
        listOf(0 to 5, -1 to 5, 0 to 5, 1 to 5), // offset rectangles
        listOf(0 to 4, -1 to 6, 0 to 6, 1 to 4) // staggered garden
    )
    private val names = listOf("river", "steps", "cross", "wings", "split", "pyramid", "offset", "staggered")
    fun seed(id: Long): Long {
        require(id >= 6)
        var z = id + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31) xor VERSION.toLong()
    }
    fun layout(id: Long): BoardLayout {
        require(id >= 6)
        val index = ((id - 6) % rows.size).toInt()
        val cycle = (id - 6) / rows.size
        val slots = mutableListOf<TileSlot>()
        rows[index].forEachIndexed { y, row -> repeat(row.second) { x ->
            slots.add(TileSlot(slots.size, (row.first + x).toDouble(), y.toDouble(), 0))
        } }
        // Upper terraces are supported by central rows. Counts stabilize at 28 tiles.
        val topCount = if (id < 25) 4 else 8
        repeat(topCount) { n ->
            val y = n / 2 + .5
            val x = rows[index][n / 2].first + n % 2 + if (cycle % 2 == 0L) .5 else 0.0
            slots.add(TileSlot(slots.size, x, y, 1))
        }
        val reflected = slots.map { it.copy(x = if (cycle % 4 >= 2) -it.x else it.x,
            y = if (cycle % 2 == 1L) -it.y else it.y) }
        return BoardLayout("${names[index]}-${cycle % 4}", reflected)
    }
    fun geometryFingerprint(layout: BoardLayout) = layout.slots.map { Triple(it.x, it.y, it.layer) }.toSet()
    fun generate(id: Long, forceFallback: Boolean = false): LevelDefinition {
        val seed = seed(id)
        val layout = layout(id)
        // Distinct silhouettes within the last four identities preclude exact board duplicates,
        // regardless of symbols. Bounded, stateless comparison; no historical boards stored.
        for (distance in 1..RECENT_WINDOW) {
            if (id - distance >= 6) check(geometryFingerprint(layout) != geometryFingerprint(layout(id - distance)))
        }
        val preferred = if (forceFallback) null else runCatching {
            LevelGenerator.generateWithProfile(id, seed, layout, minimumChoices = if (id < 25) 4 else if (id < 100) 6 else 8)
        }.getOrNull()
        val accepted = preferred ?: LevelGenerator.generate(id, seed, layout, version = 1)
        return accepted.copy(seed = seed, version = VERSION).also(LevelValidator::validate)
    }
}
