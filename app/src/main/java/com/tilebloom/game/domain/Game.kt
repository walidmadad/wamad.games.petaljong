package com.tilebloom.game.domain

import kotlin.math.abs

/** Append only. Explicit IDs are persistence contracts, independent of catalog ordering. */
enum class Symbol(val stableId: Int, val introducedAt: Long) {
    LEAF(0, 1), LOTUS(1, 1), FERN(2, 1), FLOWER(3, 1), BERRIES(4, 1), SUNFLOWER(5, 1),
    MUSHROOM(6, 21), BUTTERFLY(7, 21), BAMBOO(8, 41), ACORN(9, 41),
    MAPLE_LEAF(10, 61), PINECONE(11, 61), IRIS(12, 81), GINKGO_LEAF(13, 81),
    CHERRY_BLOSSOM(14, 101), DRAGONFLY(15, 101),
    CACTUS(16, 6), LAVENDER(17, 11), LADYBUG(18, 21), SNAIL(19, 41)
}
enum class Status { PLAYING, WON, LOST, NO_MOVES }
data class Tile(val id: Int, val symbol: Symbol, val x: Double, val y: Double, val layer: Int, val removed: Boolean = false)
data class PairMove(val first: Int, val second: Int)
data class GameSnapshot(val moves: List<PairMove>, val selectedId: Int?, val selections: List<Int>? = null, val rulesVersion: Int = 2)

object Rules {
    // Board-space tolerance: contacts within one millionth of a tile are edges.
    const val EPS = 1e-6
    private fun span(a: Double, b: Double) = minOf(a + 1, b + 1) - maxOf(a, b) > EPS
    fun overlaps(a: Tile, b: Tile) = span(a.x, b.x) && span(a.y, b.y)
    fun available(tile: Tile, tiles: List<Tile>): Boolean {
        if (tile.removed) return false
        var left = false
        var right = false
        for (other in tiles) {
            if (other.removed || other.id == tile.id) continue
            if (other.layer > tile.layer && overlaps(tile, other)) return false
            if (other.layer == tile.layer && span(tile.y, other.y)) {
                if (abs(other.x + 1 - tile.x) <= EPS) left = true
                if (abs(tile.x + 1 - other.x) <= EPS) right = true
            }
        }
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
    private var actions = emptyList<Int>()
    private var held = emptyList<Tile>()
    val tray: List<Tile> get() = held.toList()
    val clearedTiles: List<Tile> get() {
        val ids = history.flatMap { listOf(it.first, it.second) }.toSet()
        return initial.filter { it.id in ids }
    }
    val traySlots: List<Tile?> get() = List(4) { held.getOrNull(it) }
    val tiles: List<Tile> get() = current.toList()
    val remaining: List<Tile> get() = current.filterNot { it.removed }
    var selected: Int? = null
        private set
    val remainingPairs: Int get() = (remaining.size + held.size) / 2
    fun isAvailable(id: Int) = current.find { it.id == id }?.let { Rules.available(it, current) } ?: false
    fun matchingPairs(): List<PairMove> {
        val free = current.filter { Rules.available(it, current) }
        return free.flatMapIndexed { i, a -> free.drop(i + 1).filter { it.symbol == a.symbol }.map { PairMove(a.id, it.id) } }
    }
    val status: Status get() = when {
        held.size == 4 -> Status.LOST
        remaining.isEmpty() -> if (held.isEmpty()) Status.WON else Status.LOST
        remaining.none { Rules.available(it, current) } -> Status.LOST
        else -> Status.PLAYING
    }
    fun match(a: Int, b: Int): Boolean {
        val first = current.find { it.id == a } ?: return false
        val second = current.find { it.id == b } ?: return false
        if (a == b || first.symbol != second.symbol || !isAvailable(a) || !isAvailable(b)) return false
        if (held.isNotEmpty() || status != Status.PLAYING) return false
        return select(a) && select(b)
    }
    /** One atomic selection. Matching resolves before terminal-state evaluation. */
    fun select(id: Int): Boolean {
        if (status != Status.PLAYING || !isAvailable(id)) return false
        val tile = current.first { it.id == id }
        current = current.map { if (it.id == id) it.copy(removed = true) else it }
        val partner = held.firstOrNull { it.symbol == tile.symbol }
        if (partner == null) held = held + tile else {
            held = held.filterNot { it.id == partner.id }
            history = history + PairMove(partner.id, id)
        }
        actions = actions + id
        selected = null
        return true
    }
    fun snapshot() = GameSnapshot(history.toList(), null, actions.toList())
    // Replay exact accepted moves; arbitrary re-pairing of removed IDs can deadlock.
    // Validate on a candidate first so corrupt state never partially changes this game.
    fun restore(snapshot: GameSnapshot): Boolean {
        val candidate = Game(initial)
        if (snapshot.rulesVersion != 2) return false
        val replay = snapshot.selections ?: snapshot.moves.flatMap { listOf(it.first, it.second) }
        if (!replay.all { candidate.select(it) }) return false
        current = candidate.current.toList()
        history = candidate.history.toList()
        selected = null
        actions = candidate.actions.toList()
        held = candidate.held.toList()
        return true
    }
    /** Accepted individual selections only; replay validates the entire shorter history before commit. */
    val canUndo: Boolean get() = actions.isNotEmpty() && status != Status.WON
    fun undo(): Boolean {
        if (!canUndo) return false
        return restore(GameSnapshot(emptyList(), null, actions.dropLast(1)))
    }
    fun restart() { current = initial.toList(); history = emptyList(); actions = emptyList(); held = emptyList(); selected = null }
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
