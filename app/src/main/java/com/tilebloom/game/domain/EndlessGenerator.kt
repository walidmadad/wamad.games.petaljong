package com.tilebloom.game.domain

import java.util.Random

/** v8: scheduled garden footprints. Historical versions retain frozen geometry. */
object EndlessGenerator {
    const val VERSION = 8
    const val RECENT_WINDOW = 10
    fun seed(id: Long) = LegacyEndlessGenerator.seed(id) xor 4L xor VERSION.toLong()
    fun layout(id: Long): BoardLayout = AdvancedLayouts.layout(id)
    fun geometryFingerprint(layout: BoardLayout) = AdvancedLayouts.fingerprint(layout)
    /** No search: peel each even-sized layer using the authoritative availability rule. */
    private fun fallback(id: Long, seed: Long, layout: BoardLayout): LevelDefinition {
        var remaining = layout.slots.map { Tile(it.id, Symbol.LEAF, it.x, it.y, it.layer) }
        val witness = mutableListOf<PairMove>()
        while (remaining.isNotEmpty()) {
            val layer = remaining.maxOf { it.layer }
            val free = remaining.filter { it.layer == layer && Rules.available(it, remaining) }.sortedBy { it.id }
            check(free.size >= 2)
            val pair = PairMove(free.first().id, free.last().id)
            witness.add(pair); remaining = remaining.filter { it.id != pair.first && it.id != pair.second }
        }
        return LevelDefinition(id, seed, VERSION, layout,
            layout.slots.map { Tile(it.id, Symbol.LEAF, it.x, it.y, it.layer) }, witness, 0).also(LevelValidator::validate)
    }
    fun generate(id: Long, forceFallback: Boolean = false): LevelDefinition {
        require(id >= 6)
        val profile = PreparationMetrics.phase("configuration") { DifficultyPolicy.profile(id) }
        val seed = seed(id)
        check(layout(id).id.substringBefore('-') in AdvancedLayouts.families)
        check(layout(id).slots.all { it.layer in profile.allowedLayers })
        val quality = mutableListOf<CandidateQuality>()
        var best: LevelDefinition? = null
        var bestScore = Int.MIN_VALUE
        var bestIndex = 0
        val recent = PreparationMetrics.phase("noveltyGeometry") {
            (maxOf(6L, id - RECENT_WINDOW) until id).flatMap { previous ->
                (0 until profile.candidateBudget).map { AdvancedLayouts.layout(previous, it) }
            }
        }
        val recentFingerprints = recent.map(AdvancedLayouts::fingerprint).toHashSet()
        val proposals = (0 until profile.candidateBudget).map { AdvancedLayouts.layout(id, it) }
        val fingerprints = proposals.map(AdvancedLayouts::fingerprint)
        repeat(if (forceFallback) 1 else profile.candidateBudget) { index ->
            val layout = PreparationMetrics.phase("noveltySelection") {
                if (fingerprints[index] !in recentFingerprints) proposals[index]
                else proposals.indices.firstOrNull { fingerprints[it] !in recentFingerprints }?.let { proposals[it] } ?: proposals[index]
            }
            val built = PreparationMetrics.phase("witnessConstruction") { if (forceFallback || index == profile.candidateBudget - 1) fallback(id, seed, layout) else runCatching {
                LevelGenerator.generate(id, seed + index * 7919L, layout, version = 1)
            }.getOrElse { fallback(id, seed, layout) } }
            val catalog = DifficultyPolicy.symbols(id)
            val offset = ((id - 1) % catalog.size).toInt()
            val required = catalog.filter { it.introducedAt == id } + catalog.filter { it.stableId >= 16 }
            val active = (required + List(catalog.size) { catalog[(it + offset) % catalog.size] })
                .distinct().take(minOf(profile.activeSymbols, built.witness.size))
            // Every active type receives a complete witness pair, including all milestone unlocks.
            val assignments = MutableList(built.witness.size) { active[it % active.size] }
            java.util.Collections.shuffle(assignments, Random(seed + index))
            val symbols = mutableMapOf<Int, Symbol>()
            built.witness.forEachIndexed { i, pair -> symbols[pair.first] = assignments[i]; symbols[pair.second] = assignments[i] }
            val candidate = built.copy(seed = seed, version = VERSION,
                tiles = built.tiles.map { it.copy(symbol = symbols.getValue(it.id)) })
            PreparationMetrics.phase("witnessVerification") { LevelValidator.validate(candidate) }
            val a = PreparationMetrics.phase("difficultyAnalysis") { PuzzleAnalyzer.analyze(candidate, if (forceFallback) 0 else profile.analysisBudget) }
            val tray = PreparationMetrics.phase("trayReplay") { TrayPressure.measure(candidate) }
            val geometry = GeometryMetrics.measure(candidate.tiles)
            val met = !forceFallback && a.complete && a.consequenceChoiceStates >= profile.meaningfulChoices &&
                a.initialBlocked >= profile.targetBlocked && a.layers >= minOf(4, profile.tierIndex.toInt() + 2) &&
                geometry.overlapRelationships >= maxOf(profile.targetBlocked, if (profile.tierIndex == 3L) 70 else 0) &&
                (profile.tierIndex == 0L || geometry.coveredPositions >= 18) &&
                a.forced <= a.pairRemovals / 2 && (profile.tierIndex == 0L || tray.threeHeldStates >= 6)
            // Reward unlock differences and safe alternatives; penalize traps and unresolved analysis.
            val score = minOf(30, tray.threeHeldStates) + a.consequenceChoiceStates * (12 + profile.strategicEmphasis) + a.upperUnlockMoves * (2 + profile.strategicEmphasis) + a.multiple * 2 -
                a.forced * 3 - maxOf(0, a.riskyChoiceStates - profile.maximumRiskStates) * 30 -
                a.unknownAlternatives / 10 - maxOf(0, a.initialPairs - 8) * 5 -
                a.unsolvableAlternatives + minOf(10, a.solvableAlternatives) + if (met) 1000 else 0
            quality.add(CandidateQuality(index, a.consequenceChoiceStates, a.riskyChoiceStates, a.searchNodes, a.unknownAlternatives, met,
                GeometryMetrics.measure(candidate.tiles).overlapRelationships, tray.threeHeldStates, tray.losses))
            if (best == null || score > bestScore) { best = candidate; bestScore = score; bestIndex = index }
        }
        return checkNotNull(best).copy(attempts = checkNotNull(best).attempts,
            qualityTargetMet = quality.first { it.index == bestIndex }.targetMet, candidatesEvaluated = quality.size,
            candidateQuality = quality, noveltyLimited = recent.any {
                AdvancedLayouts.fingerprint(it) == AdvancedLayouts.fingerprint(checkNotNull(best).layout)
            }).also(LevelValidator::validate)
    }
}
