package com.tilebloom.game.domain

import java.util.Random

data class TileSlot(val id: Int, val x: Double, val y: Double, val layer: Int)
data class BoardLayout(val id: String, val slots: List<TileSlot>)
data class LevelDefinition(val id: Int, val seed: Long, val version: Int, val layout: BoardLayout,
    val tiles: List<Tile>, val witness: List<PairMove>, val attempts: Int)

object Layouts {
    private fun layout(id: String, rows: List<Pair<Int, Int>>, top: List<Pair<Double, Double>>): BoardLayout {
        val slots = mutableListOf<TileSlot>()
        rows.forEachIndexed { y, (start, count) -> repeat(count) { x ->
            slots.add(TileSlot(slots.size, (start + x).toDouble(), y.toDouble(), 0))
        } }
        top.forEach { (x, y) -> slots.add(TileSlot(slots.size, x, y, 1)) }
        return BoardLayout(id, slots)
    }
    val original = BoardLayout("original", LevelOne.tiles().map { TileSlot(it.id, it.x, it.y, it.layer) })
    val terrace = layout("terrace", listOf(0 to 4, -1 to 6, -1 to 6, 0 to 4),
        listOf(.5 to .5, 1.5 to .5, .5 to 1.5, 1.5 to 1.5))
    val woodland = layout("woodland", listOf(-1 to 4, -1 to 5, 0 to 5, 0 to 4, 1 to 2),
        listOf(.5 to .5, 1.5 to .5, 1.5 to 1.5, 2.5 to 1.5, .5 to 2.5, 1.5 to 2.5, 2.5 to 2.5, 1.5 to 3.5))
    val courtyard = layout("courtyard", listOf(0 to 4, -1 to 6, -1 to 6, 0 to 4),
        listOf(-.5 to .5, .5 to .5, 1.5 to .5, 2.5 to .5, -.5 to 1.5, .5 to 1.5, 1.5 to 1.5, 2.5 to 1.5, .5 to 2.5, 1.5 to 2.5))
}

object LevelValidator {
    fun validate(level: LevelDefinition) {
        val tiles = level.tiles
        require(tiles.isNotEmpty() && tiles.size % 2 == 0)
        require(tiles.map { it.id }.distinct().size == tiles.size)
        require(tiles.all { !it.removed && it.x.isFinite() && it.y.isFinite() && it.layer in 0..1 })
        require(tiles.map { TileSlot(it.id, it.x, it.y, it.layer) } == level.layout.slots)
        require(tiles.groupingBy { it.symbol }.eachCount().values.all { it % 2 == 0 })
        require(tiles.all { a -> tiles.none { b -> a.id != b.id && a.layer == b.layer && Rules.overlaps(a, b) } })
        require(tiles.any { a -> tiles.any { b -> b.layer > a.layer && Rules.overlaps(a, b) } })
        require(tiles.filter { it.layer > 0 }.all { a -> tiles.any { b -> b.layer < a.layer && Rules.overlaps(a, b) } })
        require(level.witness.size * 2 == tiles.size)
        val game = Game(tiles)
        require(level.witness.all { game.match(it.first, it.second) })
        require(game.status == Status.WON && game.remaining.isEmpty())
    }
}

/** Version 1 uses java.util.Random and ordered slot lists; outputs are stable across Android/JVM.
 * At most 64 attempts, each removing at most 16 pairs. No recursive search or unverified fallback.
 */
object LevelGenerator {
    const val VERSION = 1
    const val MAX_ATTEMPTS = 64
    fun generate(id: Int, seed: Long, layout: BoardLayout, version: Int = VERSION): LevelDefinition {
        require(version == VERSION)
        require(layout.slots.size in 2..32 && layout.slots.size % 2 == 0)
        val random = Random(seed xor id.toLong())
        repeat(MAX_ATTEMPTS) { attempt ->
            var remaining = layout.slots.map { Tile(it.id, Symbol.LEAF, it.x, it.y, it.layer) }
            val witness = mutableListOf<PairMove>()
            while (remaining.isNotEmpty()) {
                val free = remaining.filter { Rules.available(it, remaining) }.toMutableList()
                if (free.size < 2) break
                val a = free.removeAt(random.nextInt(free.size))
                val b = free[random.nextInt(free.size)]
                witness.add(PairMove(a.id, b.id))
                remaining = remaining.filter { it.id != a.id && it.id != b.id }
            }
            if (remaining.isNotEmpty()) return@repeat
            val symbols = MutableList(witness.size) { Symbol.entries[it % Symbol.entries.size] }
            java.util.Collections.shuffle(symbols, random)
            val assignments = mutableMapOf<Int, Symbol>()
            witness.forEachIndexed { i, p -> assignments[p.first] = symbols[i]; assignments[p.second] = symbols[i] }
            val result = LevelDefinition(id, seed, version, layout, layout.slots.map {
                Tile(it.id, assignments.getValue(it.id), it.x, it.y, it.layer)
            }, witness, attempt + 1)
            try { LevelValidator.validate(result); return result } catch (_: IllegalArgumentException) { }
        }
        error("No verified level for id=$id seed=$seed layout=${layout.id} within $MAX_ATTEMPTS attempts")
    }
}

object Levels {
    const val COUNT = 5
    fun get(id: Int): LevelDefinition = when (id) {
        1 -> LevelDefinition(1, 0, 0, Layouts.original, LevelOne.tiles(), LevelOne.witness, 0).also(LevelValidator::validate)
        2 -> LevelGenerator.generate(2, 202L, Layouts.terrace)
        3 -> LevelGenerator.generate(3, 303L, Layouts.woodland)
        4 -> LevelGenerator.generate(4, 404L, Layouts.courtyard)
        5 -> LevelGenerator.generate(5, 505L, Layouts.woodland)
        else -> error("Unknown introductory level $id")
    }
}

data class SessionSnapshot(val id: Int, val seed: Long, val version: Int, val game: GameSnapshot)
class GardenSession {
    var level = Levels.get(1); private set
    var game = Game(level.tiles); private set
    val canAdvance get() = game.status == Status.WON && level.id < Levels.COUNT
    fun restart() = game.restart()
    fun advance(): Boolean {
        if (!canAdvance) return false
        val next = Levels.get(level.id + 1)
        level = next; game = Game(next.tiles)
        return true
    }
    fun snapshot() = SessionSnapshot(level.id, level.seed, level.version, game.snapshot())
    fun restore(saved: SessionSnapshot): Boolean {
        val candidate = try { Levels.get(saved.id) } catch (_: IllegalStateException) { return false }
        if (candidate.seed != saved.seed || candidate.version != saved.version) return false
        val restored = Game(candidate.tiles)
        if (!restored.restore(saved.game)) return false
        level = candidate; game = restored
        return true
    }
}
