package com.wamad.petaljong.domain

import kotlin.math.abs

/** Append only. Explicit IDs are persistence contracts, independent of catalog ordering. */
enum class Symbol(val stableId: Int, val introducedAt: Long) {
    LEAF(0, 1), LOTUS(1, 1), FERN(2, 1), FLOWER(3, 1), BERRIES(4, 1), SUNFLOWER(5, 1),
    MUSHROOM(6, 21), BUTTERFLY(7, 21), BAMBOO(8, 41), ACORN(9, 41),
    MAPLE_LEAF(10, 61), PINECONE(11, 61), IRIS(12, 81), GINKGO_LEAF(13, 81),
    CHERRY_BLOSSOM(14, 101), DRAGONFLY(15, 101),
    CACTUS(16, 6), LAVENDER(17, 11), LADYBUG(18, 21), SNAIL(19, 41),
    VEINED_LEAF(20, 8), DOUBLE_LEAF(21, 8), YELLOW_DAISY(22, 16),
    PINK_PEONY(23, 8), PINK_CAMELLIA(24, 8), PURPLE_CROCUS(25, 16), PURPLE_BELLFLOWER(26, 16),
    YELLOW_MARIGOLD(27, 16), GOLDEN_CHRYSANTHEMUM(28, 16), MONSTERA_LEAF(29, 8),
    EUCALYPTUS_LEAF(30, 8), RED_JAPANESE_MAPLE(31, 31), ORANGE_SUGAR_MAPLE(32, 31),
    OLIVE_BRANCH(33, 31), ROSEMARY_BRANCH(34, 31)
}
enum class Status { PLAYING, WON, LOST, NO_MOVES }
data class Tile(val id: Int, val symbol: Symbol, val x: Double, val y: Double, val layer: Int, val removed: Boolean = false, val mystery: Boolean = false, val revealed: Boolean = false) {
    val concealed: Boolean get() = mystery && !revealed
}
data class PairMove(val first: Int, val second: Int)
enum class ActionKind { REVEAL, COLLECT, LEGACY_COLLECT }
data class GameAction(val kind: ActionKind, val tileId: Int)
data class GameSnapshot(val moves: List<PairMove>, val selectedId: Int?, val selections: List<Int>? = null, val rulesVersion: Int = 2, val actionLog: List<GameAction>? = null)

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

class Game private constructor(definition: List<Tile>, private val legacyMysteries: Boolean,
    private val availability: AvailabilityIndex) {
    constructor(definition: List<Tile>, legacyMysteries: Boolean = false) :
        this(definition, legacyMysteries, AvailabilityIndex(definition))
    private val initial = definition.map { it.copy(removed = false, revealed = false) }
    init {
        require(initial.map { it.id }.distinct().size == initial.size)
        require(initial.all { it.x.isFinite() && it.y.isFinite() && it.layer >= 0 })
    }
    private var current = initial.toList()
    private var history = emptyList<PairMove>()
    private var actions = emptyList<Int>()
    private var log = emptyList<GameAction>()
    private var held = emptyList<Tile>()
    private var availabilityState: List<Tile>? = null
    private var cachedAvailable = emptyList<Tile>()
    private var cachedAvailableIds = emptySet<Int>()
    /** Current is immutable: reference changes invalidate all derived availability, including
     * reveal/undo/restore. Search forks safely share the read-only cache until their next action. */
    internal fun availableTiles(): List<Tile> {
        if (availabilityState !== current) {
            cachedAvailable = availability.free(current)
            cachedAvailableIds = cachedAvailable.map { it.id }.toSet()
            availabilityState = current
        }
        return cachedAvailable
    }
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
    fun isAvailable(id: Int): Boolean { availableTiles(); return id in cachedAvailableIds }
    fun matchingPairs(): List<PairMove> {
        val free = availableTiles().filter { !it.concealed }
        return free.flatMapIndexed { i, a -> free.drop(i + 1).filter { !it.concealed && it.symbol == a.symbol }.map { PairMove(a.id, it.id) } }
    }
    val status: Status get() = when {
        held.size == 4 -> Status.LOST
        remaining.isEmpty() -> if (held.isEmpty()) Status.WON else Status.LOST
        availableTiles().isEmpty() -> Status.LOST
        else -> Status.PLAYING
    }
    fun match(a: Int, b: Int): Boolean {
        val first = current.find { it.id == a } ?: return false
        val second = current.find { it.id == b } ?: return false
        if (first.concealed || second.concealed || a == b || first.symbol != second.symbol || !isAvailable(a) || !isAvailable(b)) return false
        if (held.isNotEmpty() || status != Status.PLAYING) return false
        return select(a) && select(b)
    }
    @Synchronized
    fun reveal(id: Int): Boolean {
        if (status != Status.PLAYING || !isAvailable(id)) return false
        val tile = current.first { it.id == id }
        if (!tile.concealed) return false
        current = current.map { if (legacyMysteries) { if (it.id == id) it.copy(revealed = true) else it } else it.copy(revealed = it.id == id) }
        if (!legacyMysteries) log = log.dropLastWhile { it.kind == ActionKind.REVEAL }
        log = log + GameAction(ActionKind.REVEAL, id)
        return true
    }
    @Synchronized
    fun interact(id: Int): Boolean = if (current.any { it.id == id && it.concealed }) reveal(id) else select(id)
    /** One atomic selection. Matching resolves before terminal-state evaluation. */
    @Synchronized
    fun select(id: Int): Boolean = collect(id, legacyMysteries)
    private fun collect(id: Int, legacy: Boolean): Boolean {
        if (status != Status.PLAYING || !isAvailable(id)) return false
        val tile = current.first { it.id == id }
        if (tile.concealed) return false
        val active = current.firstOrNull { it.mystery && it.revealed && !it.removed && it.id != id }
        if (!legacy && !tile.mystery && active?.symbol == tile.symbol && isAvailable(active.id)) {
            current = current.map { if (it.id == id || it.id == active.id) it.copy(removed = true, revealed = false) else it.copy(revealed = false) }
            history = history + PairMove(active.id, id)
            actions = actions + id
            log = log + GameAction(ActionKind.COLLECT, id)
            selected = null
            return true
        }
        if (!legacy) clearMystery()
        current = current.map { if (it.id == id) it.copy(removed = true) else it }
        val partner = held.firstOrNull { it.symbol == tile.symbol }
        if (partner == null) held = held + tile else {
            held = held.filterNot { it.id == partner.id }
            history = history + PairMove(partner.id, id)
        }
        actions = actions + id
        log = log + GameAction(if (legacy) ActionKind.LEGACY_COLLECT else ActionKind.COLLECT, id)
        selected = null
        return true
    }
    /** Immutable lists make a search branch cheap; selections still use the same controller. */
    internal fun fork(): Game = Game(initial, legacyMysteries, availability).also {
        it.current = current; it.history = history; it.actions = actions; it.held = held
        it.selected = selected; it.log = log
        it.availabilityState = availabilityState; it.cachedAvailable = cachedAvailable
        it.cachedAvailableIds = cachedAvailableIds
    }
    /** Temporary information stays in the controller. Only revelations needed to replay a
     * permanent collection are retained; switching cannot grow the durable log without bound. */
    val activeMysteryId: Int? get() = current.firstOrNull { it.mystery && it.revealed && !it.removed }?.id
    @Synchronized
    fun hideMystery() {
        if (activeMysteryId != null) {
            clearMystery()
            log = log.dropLastWhile { it.kind == ActionKind.REVEAL }
        }
    }
    private fun clearMystery() { current = current.map { if (it.mystery) it.copy(revealed = false) else it } }
    fun snapshot() = GameSnapshot(history.toList(), null, actions.toList(), if (initial.any { it.mystery }) { if (legacyMysteries) 3 else 4 } else 2, if (initial.any { it.mystery }) log.toList() else null)
    // Replay exact accepted moves; arbitrary re-pairing of removed IDs can deadlock.
    // Validate on a candidate first so corrupt state never partially changes this game.
    @Synchronized
    fun restore(snapshot: GameSnapshot, preserveTemporary: Boolean = false): Boolean {
        val candidate = Game(initial, false, availability)
        if (snapshot.rulesVersion !in 2..4) return false
        val replay = snapshot.selections ?: snapshot.moves.flatMap { listOf(it.first, it.second) }
        if (snapshot.actionLog != null) {
            if (!snapshot.actionLog.all {
                if (snapshot.rulesVersion == 3) {
                    // Old saves permitted multiple reveals and always collected through the tray.
                    if (it.kind == ActionKind.REVEAL) {
                        val t = candidate.current.firstOrNull { t -> t.id == it.tileId }
                        if (t == null || !t.concealed || !candidate.isAvailable(t.id)) false else {
                            candidate.current = candidate.current.map { t -> if (t.id == it.tileId) t.copy(revealed = true) else t }
                            candidate.log = candidate.log + it
                            true
                        }
                    } else {
                        val t = candidate.current.firstOrNull { t -> t.id == it.tileId }
                        if (t?.mystery == true && t.concealed) candidate.reveal(t.id)
                        candidate.collect(it.tileId, true)
                    }
                } else when (it.kind) {
                    ActionKind.REVEAL -> candidate.reveal(it.tileId)
                    ActionKind.COLLECT -> candidate.select(it.tileId)
                    ActionKind.LEGACY_COLLECT -> {
                        candidate.current = candidate.current.map { t -> if (t.id == it.tileId && t.mystery) t.copy(revealed = true) else t }
                        candidate.collect(it.tileId, true)
                    }
                }
            }) return false
            if (snapshot.selections != null && candidate.actions != snapshot.selections) return false
        } else if (!replay.all { candidate.select(it) }) return false
        if (snapshot.rulesVersion == 3) candidate.log = candidate.log.filter { it.kind != ActionKind.REVEAL }
        if (!preserveTemporary) candidate.hideMystery()
        current = candidate.current.toList()
        history = candidate.history.toList()
        selected = null
        actions = candidate.actions.toList()
        held = candidate.held.toList()
        log = candidate.log.toList()
        return true
    }
    /** Accepted individual selections only; replay validates the entire shorter history before commit. */
    val canUndo: Boolean get() = actions.isNotEmpty() && status != Status.WON
    @Synchronized
    fun undo(): Boolean {
        if (!canUndo) return false
        val cut = log.indexOfLast { it.kind == ActionKind.COLLECT || it.kind == ActionKind.LEGACY_COLLECT }
        return restore(GameSnapshot(emptyList(), null, actions.dropLast(1), 4, log.take(cut)))
    }
    @Synchronized
    fun restart() { current = initial.toList(); history = emptyList(); actions = emptyList(); log = emptyList(); held = emptyList(); selected = null }
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
