package com.tilebloom.game.domain

import java.util.Random

data class TileSlot(val id: Int, val x: Double, val y: Double, val layer: Int)
data class BoardLayout(val id: String, val slots: List<TileSlot>)
data class CandidateQuality(val index: Int, val consequenceChoices: Int, val riskyChoices: Int,
    val searchNodes: Int, val unknownAlternatives: Int, val targetMet: Boolean)
data class LevelDefinition(val id: Int, val seed: Long, val version: Int, val layout: BoardLayout,
    val tiles: List<Tile>, val witness: List<PairMove>, val attempts: Int,
    val qualityTargetMet: Boolean = false, val candidatesEvaluated: Int = 0,
    val candidateQuality: List<CandidateQuality> = emptyList())

object Layouts {
    private fun layout(id: String, rows: List<Pair<Int, Int>>, top: List<Pair<Double, Double>>): BoardLayout {
        val slots = mutableListOf<TileSlot>()
        rows.forEachIndexed { y, (start, count) -> repeat(count) { x ->
            slots.add(TileSlot(slots.size, (start + x).toDouble(), y.toDouble(), 0))
        } }
        top.forEach { (x, y) -> slots.add(TileSlot(slots.size, x, y, 1)) }
        return BoardLayout(id, slots)
    }
    val winding = layout("winding", listOf(0 to 3, 0 to 4, 1 to 4, 1 to 4, 2 to 3),
        listOf(.5 to .5, 1.5 to .5, 2.5 to .5, 1.5 to 2.5, 2.5 to 2.5, 3.5 to 2.5))
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

/** Version 1 is retained unchanged for explicit historical generation.
 * Version 2 evaluates at most six verified v1 candidates, each with at most 64 geometry attempts.
 * Quality search is capped at 2000–5000 nodes per candidate and never runs during board rendering.
 */
object LevelGenerator {
    const val VERSION = 2
    const val MAX_ATTEMPTS = 64
    fun generate(id: Int, seed: Long, layout: BoardLayout, version: Int = VERSION): LevelDefinition =
        generateWithProfile(id, seed, layout, version)
    // Internal profile override exercises fallback in tests; live identity always uses the versioned defaults.
    internal fun generateWithProfile(id: Int, seed: Long, layout: BoardLayout, version: Int = VERSION,
        qualityAttempts: Int = 6, minimumChoices: Int? = null): LevelDefinition {
        require(version == 1 || version == VERSION)
        require(qualityAttempts in 1..6)
        if (version == VERSION) {
            var best: LevelDefinition? = null
            var bestScore = Int.MIN_VALUE
            val quality = mutableListOf<CandidateQuality>()
            for (candidate in 0 until qualityAttempts) {
                val level = generate(id, seed + candidate * 7919L, layout, 1)
                val a = PuzzleAnalyzer.analyze(level, if (id == 3) 5000 else if (id == 4) 4000 else 2000)
                val score = a.consequenceChoiceStates * 3 + a.riskyChoiceStates * 5 - a.forced - (if (id == 3) maxOf(0, a.riskyChoiceStates - 2) * 20 else 0)
                if (score > bestScore) { best = level; bestScore = score }
                // Require visible unlock choices and at least one proven consequential risk.
                // Compare at least two candidates; cap observed risk for the branching tutorial.
                val target = minimumChoices ?: (if (id <= 2) 3 else if (id == 3) 5 else 6)
                val targetMet = a.consequenceChoiceStates >= target && a.forced <= a.branching.size / 2 &&
                    a.riskyChoiceStates >= (if (id == 5) 2 else 1) && (id != 3 || (a.complete && a.riskyChoiceStates <= 2))
                quality.add(CandidateQuality(candidate, a.consequenceChoiceStates, a.riskyChoiceStates,
                    a.searchNodes, a.unknownAlternatives, targetMet))
                if (candidate >= 1 && targetMet) {
                    return level.copy(seed = seed, version = VERSION, qualityTargetMet = true, candidatesEvaluated = candidate + 1, candidateQuality = quality.toList())
                }
            }
            return checkNotNull(best).copy(seed = seed, version = VERSION, candidatesEvaluated = qualityAttempts, candidateQuality = quality.toList())
        }
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
    const val INTRODUCTORY_VERSION = 3
    // CODEX 09 fixture: kept for migration audits, never used to replay v2 moves on v3.
    val legacyIntroductory: LevelDefinition by lazy {
        // Candidate 12 of 24 offline symbol assignments (Random(108)); selected for a
        // two-pair opening and one proven risky witness state, rather than four.
        val symbols = listOf(0, 1, 2, 3, 4, 4, 5, 5, 0, 1, 3, 2)
        val assignments = mutableMapOf<Int, Symbol>()
        LevelOne.witness.forEachIndexed { i, p ->
            assignments[p.first] = Symbol.entries[symbols[i]]
            assignments[p.second] = Symbol.entries[symbols[i]]
        }
        LevelDefinition(1, 0, 2, Layouts.original,
            LevelOne.tiles().map { it.copy(symbol = assignments.getValue(it.id)) },
            LevelOne.witness, 0, qualityTargetMet = true, candidatesEvaluated = 24)
            .also(LevelValidator::validate)
    }
    private val introductory: LevelDefinition by lazy {
        // Offline portfolio: 60 candidates / 4 silhouettes; see CODEX_10_REPORT.md.
        // Freeze v1 construction and its seed. No quality search on application launch.
        LevelGenerator.generate(1, 123L, Layouts.winding, version = 1)
            .copy(version = INTRODUCTORY_VERSION, qualityTargetMet = true, candidatesEvaluated = 60)
            .also(LevelValidator::validate)
    }
    private val generated by lazy { mapOf(
        2 to LevelGenerator.generate(2, 202L, Layouts.terrace),
        3 to LevelGenerator.generate(3, 303L, Layouts.woodland),
        4 to LevelGenerator.generate(4, 404L, Layouts.courtyard),
        5 to LevelGenerator.generate(5, 511L, Layouts.woodland)) }
    fun get(id: Int): LevelDefinition = when (id) {
        1 -> introductory
        2 -> generated.getValue(2)
        3 -> generated.getValue(3)
        4 -> generated.getValue(4)
        5 -> generated.getValue(5)
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
        // v3 changes Level 1 footprints and associations. Never replay a v2 ID history
        // against it. ProgressController recovers only that attempt; completions survive.
        // Levels 2–5 deliberately keep their v2 identities and exact definitions.
        if (candidate.seed != saved.seed || candidate.version != saved.version) return false
        val restored = Game(candidate.tiles)
        if (!restored.restore(saved.game)) return false
        level = candidate; game = restored
        return true
    }
}
