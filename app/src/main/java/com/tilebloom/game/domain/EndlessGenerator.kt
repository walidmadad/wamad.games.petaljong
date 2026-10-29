package com.tilebloom.game.domain

import java.util.Random

/** v11 verifies Option C mystery interactions with one temporary reveal.
 * Construction uses individual legal selections and a bounded three-unmatched symbol schedule.
 * A witness certifies solvability; it is never supplied to simulated players. */
object EndlessGenerator {
    const val VERSION = 11
    const val RECENT_WINDOW = 10
    const val CANDIDATES = 24
    fun seed(id: Long) = LegacyEndlessGenerator.seed(id) xor 4L xor VERSION.toLong()
    fun layout(id: Long) = AdvancedLayouts.layout(id)
    fun geometryFingerprint(layout: BoardLayout) = AdvancedLayouts.fingerprint(layout)
    fun generate(id: Long, forceFallback: Boolean = false): LevelDefinition {
        require(id >= 6)
        if (DifficultyPolicy.difficulty(id) == Difficulty.NORMAL || forceFallback) {
            return MysteryPlacement.apply(VersionEightGenerator.generate(id, forceFallback).copy(seed = seed(id), version = VERSION))
        }
        val tier = DifficultyPolicy.difficulty(id)
        var best: LevelDefinition? = null
        var bestScore = Int.MIN_VALUE
        val qualities = mutableListOf<CandidateQuality>()
        val recent = (maxOf(6L, id - RECENT_WINDOW) until id).flatMap { previous ->
            (0 until CANDIDATES).map { AdvancedLayouts.fingerprint(AdvancedLayouts.layout(previous, it)) }
        }.toSet()
        val layouts = (0 until CANDIDATES).map { AdvancedLayouts.layout(id, it) }
        val novel = layouts.filter { AdvancedLayouts.fingerprint(it) !in recent }.ifEmpty { layouts }
        for (attempt in 0 until CANDIDATES) {
            PreparationMetrics.checkBudget()
            val random = Random(seed(id) + attempt * 7919L)
            val layout = novel[attempt % novel.size]
            var remaining = layout.slots.map { Tile(it.id, Symbol.LEAF, it.x, it.y, it.layer) }
            val order = mutableListOf<Int>()
            while (remaining.isNotEmpty()) {
                val free = remaining.filter { Rules.available(it, remaining) }
                check(free.isNotEmpty())
                val tile = free[random.nextInt(free.size)]
                order.add(tile.id)
                remaining = remaining.filter { it.id != tile.id }
            }
            val catalog = DifficultyPolicy.symbols(id)
            val active = (SymbolFamilies.required(id) + catalog.filter { it.introducedAt == id } + catalog.filter { it.stableId >= 16 } +
                catalog.drop((id % catalog.size).toInt()) + catalog).distinct()
                .take(DifficultyPolicy.profile(id).activeSymbols)
            val held = mutableListOf<Symbol>()
            val assignments = mutableMapOf<Int, Symbol>()
            var opened = 0
            order.forEachIndexed { index, tile ->
                val left = order.size - index
                val unused = active.filter { it !in held }
                val close = held.isNotEmpty() && (held.size == 3 || left == held.size ||
                    unused.isEmpty() || random.nextInt(100) < 30)
                val symbol = if (close) held.removeAt(random.nextInt(held.size)) else {
                    val next = (0 until active.size).map { active[(opened + it) % active.size] }.first { it !in held }
                    opened++; held.add(next); next
                }
                assignments[tile] = symbol
            }
            check(held.isEmpty())
            val candidate = MysteryPlacement.apply(LevelDefinition(id, seed(id), VERSION, layout,
                layout.slots.map { Tile(it.id, assignments.getValue(it.id), it.x, it.y, it.layer) },
                emptyList(), attempt + 1, selectionWitness = order))
            LevelValidator.validate(candidate)
            val analysis = PreparationMetrics.phase("strategicAnalysis") { StrategicAnalyzer.analyze(candidate, adaptive = true) }
            val met = analysis.meets(tier) && ExpertProgression.targets(id).accepts(analysis)
            qualities.add(CandidateQuality(attempt, 0, 0, analysis.lookahead.nodes,
                0, met, analysis.coverRelationships, analysis.witnessThreeHeld, analysis.probes - analysis.greedyProbeWins, analysis))
            val score = analysis.score(tier) + ExpertProgression.targets(id).score(analysis)
            if (score > bestScore) { best = candidate; bestScore = score }
            if (attempt >= 1 && met) break
        }
        val result = checkNotNull(best)
        return result.copy(qualityTargetMet = qualities[result.attempts - 1].targetMet,
            candidatesEvaluated = qualities.size, candidateQuality = qualities, noveltyLimited = AdvancedLayouts.fingerprint(result.layout) in recent, strategicAnalysis = qualities[result.attempts - 1].strategic).also(LevelValidator::validate)
    }
}
