package com.wamad.petaljong.domain

import com.wamad.petaljong.presentation.board.BoardGeometry
import com.wamad.petaljong.presentation.board.TileVisibility

/** Observed-information player. Free scans use actual reveal taps and explicit memory.
 * Covered identities are never scored. Search cannot collect artwork unseen at its root.
 * No witness, assignment seed, hidden symbol catalog or omniscient solver is consulted. */
object PlanningPlayer {
    fun play(level: LevelDefinition, strategy: SimulatedPlayer.Strategy, budget: Int = 1800,
        depth: Int = 6, tieBreak: Int = 0): SimulatedPlayer.Result {
        val game = Game(level.tiles)
        val geometry = BoardGeometry(360f, 520f, tiles = level.tiles)
        val memory = mutableSetOf<Int>()
        var nodes = 0; var peak = 0; var pressure = 0
        fun free(g: Game) = g.availableTiles()
        fun collect(g: Game, id: Int) {
            val tile=g.remaining.first { it.id==id }
            val direct=if(tile.mystery || g.tray.any { it.symbol==tile.symbol }) null else
                free(g).firstOrNull { it.mystery && it.id in memory && it.symbol==tile.symbol }
            if(direct!=null) {
                if(g.remaining.first { it.id==direct.id }.concealed) check(g.reveal(direct.id))
            } else if(tile.concealed) check(g.reveal(id))
            check(g.select(id))
        }
        fun step(g: Game, id: Int): Game = g.fork().also { collect(it,id) }
        fun rank(t: Tile): Long = if (tieBreak == 0) t.id.toLong() else {
            val m = (t.id.toLong() + tieBreak * 7919L) * -7046029254386353131L
            m xor (m ushr 32)
        }
        while (game.status == Status.PLAYING) {
            // A player may inspect available mysteries at zero tray cost, one at a time.
            for (t in free(game).filter { it.mystery && it.id !in memory }.sortedBy { it.id }) {
                check(game.reveal(t.id)); memory.add(t.id)
            }
            memory.addAll(game.remaining.filter { t ->
                !t.mystery && geometry.face(t).center.let { c ->
                    TileVisibility.faceVisibleAt(t, game.tiles, geometry, c.x, c.y)
                }
            }.map { it.id })
            memory.addAll(free(game).map { it.id })
            val observed = memory.toSet()
            fun ordered(g: Game): List<Tile> {
                val available = free(g).filter { it.id in observed }
                val counts = available.groupingBy { it.symbol }.eachCount()
                return available.sortedWith(compareByDescending<Tile> { t -> g.tray.any { it.symbol == t.symbol } }
                    .thenByDescending { t -> !t.mystery && available.any { it.mystery && it.symbol==t.symbol } }
                    .thenByDescending { counts.getValue(it.symbol) > 1 }.thenBy(::rank))
            }
            fun value(g: Game): Int {
                val status = g.status
                if (status == Status.WON) return 100000
                if (status == Status.LOST) return -100000
                val available = free(g)
                val known = available.filter { it.id in observed }
                val held = g.tray.map { it.symbol }
                val reachable = known.count { it.symbol in held }
                return -g.remaining.size * 100 - g.tray.size * 65 + reachable * 45 +
                    available.count { it.id !in observed } * 18
            }
            val choices = ordered(game)
            val selected = when (strategy) {
                SimulatedPlayer.Strategy.GREEDY -> choices.first()
                SimulatedPlayer.Strategy.SIMPLE -> choices.maxBy { t ->
                    val next = step(game, t.id)
                    (if (game.tray.any { it.symbol == t.symbol }) 1000 else 0) +
                    (if (choices.any { it.id != t.id && it.symbol == t.symbol }) 100 else 0) +
                    free(next).count { !game.isAvailable(it.id) }
                }
                SimulatedPlayer.Strategy.LOOKAHEAD -> {
                    val allowance = maxOf(0, (budget - nodes) / maxOf(1, game.remaining.size / 2))
                    val perRoot = allowance / maxOf(1, choices.size)
                    choices.maxBy { t ->
                        val ceiling = minOf(budget, nodes + perRoot)
                        val memo = mutableMapOf<Triple<Set<Int>, List<Int>, Int>, Int>()
                        fun search(g: Game, left: Int): Int {
                            if (left == 0 || g.status != Status.PLAYING || nodes >= ceiling) return value(g)
                            val key = Triple(g.remaining.map { it.id }.toSet(), g.tray.map { it.symbol.stableId }.sorted(), left)
                            // Direct matches remove two tiles, so depth must be explicit in memo identity.
                            memo[key]?.let { return it }
                            nodes++; PreparationMetrics.checkBudget()
                            val known = ordered(g)
                            val v = if (known.isEmpty()) value(g) else known.maxOf { search(step(g, it.id), left - 1) }
                            memo[key] = v
                            return v
                        }
                        search(step(game, t.id), depth - 1)
                    }
                }
            }
            collect(game,selected.id)
            peak = maxOf(peak, game.tray.size)
            if (game.tray.size in 2..3) pressure++
        }
        val snapshot=game.snapshot()
        return SimulatedPlayer.Result(game.status == Status.WON, snapshot.selections.orEmpty(), nodes, peak, pressure, nodes >= budget, snapshot)
    }
}
