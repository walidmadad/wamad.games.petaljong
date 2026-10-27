package com.tilebloom.game.domain

import kotlin.math.abs

enum class Symbol { LEAF, LOTUS, FERN, FLOWER, BERRIES, SUNFLOWER }
enum class Status { PLAYING, WON, NO_MOVES }
data class Tile(val id: Int, val symbol: Symbol, val x: Double, val y: Double, val layer: Int, val removed: Boolean = false)
data class PairMove(val first: Int, val second: Int)
data class GameSnapshot(val moves: List<PairMove>, val selectedId: Int?)

object Rules {
    // Board-space tolerance: contacts within one millionth of a tile are edges.
    const val EPS = 1e-6
    private fun span(a: Double, b: Double) = minOf(a + 1, b + 1) - maxOf(a, b) > EPS
    fun overlaps(a: Tile, b: Tile) = span(a.x, b.x) && span(a.y, b.y)
    fun available(tile: Tile, tiles: List<Tile>): Boolean {
        if (tile.removed) return false
        val others = tiles.filter { !it.removed && it.id != tile.id }
        if (others.any { it.layer > tile.layer && overlaps(tile, it) }) return false
        val neighbors = others.filter { it.layer == tile.layer && span(tile.y, it.y) }
        val left = neighbors.any { abs(it.x + 1 - tile.x) <= EPS }
        val right = neighbors.any { abs(tile.x + 1 - it.x) <= EPS }
        return !left || !right
    }
}

class Game(definition: List<Tile>) {
    private val initial = definition.map { it.copy(removed = false) }
    init {
        require(initial.map { it.id }.distinct().size == initial.size)
        require(initial.all { it.x.isFinite() && it.y.isFinite() && it.layer >= 0 })
    }
    private var current = initial.toList()
    private var history = emptyList<PairMove>()
    val tiles: List<Tile> get() = current.toList()
    val remaining: List<Tile> get() = current.filterNot { it.removed }
    var selected: Int? = null
        private set
    val remainingPairs: Int get() = remaining.size / 2
    fun isAvailable(id: Int) = current.find { it.id == id }?.let { Rules.available(it, current) } ?: false
    fun matchingPairs(): List<PairMove> {
        val free = current.filter { Rules.available(it, current) }
        return free.flatMapIndexed { i, a -> free.drop(i + 1).filter { it.symbol == a.symbol }.map { PairMove(a.id, it.id) } }
    }
    val status: Status get() = when { remaining.isEmpty() -> Status.WON; matchingPairs().isEmpty() -> Status.NO_MOVES; else -> Status.PLAYING }
    fun match(a: Int, b: Int): Boolean {
        val first = current.find { it.id == a } ?: return false
        val second = current.find { it.id == b } ?: return false
        if (a == b || first.symbol != second.symbol || !isAvailable(a) || !isAvailable(b)) return false
        current = current.map { if (it.id == a || it.id == b) it.copy(removed = true) else it }
        history = history + PairMove(a, b)
        selected = null
        return true
    }
    fun select(id: Int) {
        if (!isAvailable(id)) return
        val previous = selected
        when { previous == id -> selected = null; previous != null && match(previous, id) -> Unit; else -> selected = id }
    }
    fun snapshot() = GameSnapshot(history.toList(), selected)
    // Replay exact accepted moves; arbitrary re-pairing of removed IDs can deadlock.
    // Validate on a candidate first so corrupt state never partially changes this game.
    fun restore(snapshot: GameSnapshot): Boolean {
        val candidate = Game(initial)
        if (!snapshot.moves.all { candidate.match(it.first, it.second) }) return false
        snapshot.selectedId?.let {
            if (!candidate.isAvailable(it)) return false
            candidate.select(it)
        }
        current = candidate.current.toList()
        history = candidate.history.toList()
        selected = candidate.selected
        return true
    }
    /** Accepted matches only; replay validates the entire shorter history before commit. */
    val canUndo: Boolean get() = history.isNotEmpty() && status != Status.WON
    fun undo(): Boolean {
        if (!canUndo) return false
        return restore(GameSnapshot(history.dropLast(1), null))
    }
    fun restart() { current = initial.toList(); history = emptyList(); selected = null }
}

object LevelOne {
    val witness: List<PairMove> = (0..3).map { PairMove(16 + it, 20 + it) } +
        (0..3).flatMap { listOf(PairMove(it, 4 + it), PairMove(8 + it, 12 + it)) }
    fun tiles(): List<Tile> {
        val symbols = mutableMapOf<Int, Symbol>()
        witness.forEachIndexed { i, p -> symbols[p.first] = Symbol.entries[i % 6]; symbols[p.second] = Symbol.entries[i % 6] }
        return (0..23).map { id ->
            if (id < 16) Tile(id, symbols.getValue(id), (id % 4).toDouble(), (id / 4).toDouble(), 0)
            else Tile(id, symbols.getValue(id), (id % 4) + 0.5, ((id - 16) / 4) + 0.5, 1)
        }
    }
}
