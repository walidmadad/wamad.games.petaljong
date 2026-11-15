package com.wamad.petaljong.domain

import java.util.Random

/** v6 introduces four symbols; historical v4 and v5 definitions remain reconstructible. */
object VersionSixGenerator {
    const val VERSION = 6
    const val RECENT_WINDOW = 4
    fun seed(id: Long) = LegacyEndlessGenerator.seed(id) xor 4L xor VERSION.toLong()
    fun layout(id: Long): BoardLayout {
        val profile = HistoricalDifficultyPolicy.profile(id)
        val old = LegacyEndlessGenerator.layout(id)
        val base = old.slots.filter { it.layer == 0 }
        // Twenty base tiles in eight compact silhouettes; at most a half-tile cover offset.
        val count = minOf(40, profile.targetTiles.first + if (profile.phase >= 10) 4 else 0)
        val ordered = base.sortedWith(compareBy<TileSlot> { it.y }.thenBy { it.x })
        val shift = ((id - 1) % base.size).toInt()
        val upper = List(count - base.size) { n -> ordered[(n + shift) % base.size] }
        return BoardLayout(old.id, base + upper.mapIndexed { i, slot -> slot.copy(id = base.size + i, x = slot.x + profile.coverOffset, layer = 1) })
    }
    fun geometryFingerprint(layout: BoardLayout) = LegacyEndlessGenerator.geometryFingerprint(layout)
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
        val profile = HistoricalDifficultyPolicy.profile(id)
        val layout = layout(id)
        val seed = seed(id)
        check(layout.id.substringBefore('-') in profile.allowedFamilies)
        check(layout.slots.all { it.layer in profile.allowedLayers })
        val quality = mutableListOf<CandidateQuality>()
        var best: LevelDefinition? = null
        var bestScore = Int.MIN_VALUE
        repeat(if (forceFallback) 1 else profile.candidateBudget) { index ->
            val built = if (forceFallback) fallback(id, seed, layout) else runCatching {
                LevelGenerator.generate(id, seed + index * 7919L, layout, version = 1)
            }.getOrElse { fallback(id, seed, layout) }
            val catalog = HistoricalDifficultyPolicy.symbols(id)
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
            LevelValidator.validate(candidate)
            val a = PuzzleAnalyzer.analyze(candidate, if (forceFallback) 0 else profile.analysisBudget)
            val met = !forceFallback && a.complete && a.consequenceChoiceStates >= profile.meaningfulChoices &&
                a.initialBlocked >= profile.targetBlocked && a.riskyChoiceStates <= profile.maximumRiskStates &&
                a.forced <= a.pairRemovals / 2
            // Reward unlock differences and safe alternatives; penalize traps and unresolved analysis.
            val score = a.consequenceChoiceStates * (12 + profile.strategicEmphasis) + a.upperUnlockMoves * (2 + profile.strategicEmphasis) + a.multiple * 2 -
                a.forced * 3 - maxOf(0, a.riskyChoiceStates - profile.maximumRiskStates) * 30 -
                a.unknownAlternatives / 10 - maxOf(0, a.initialPairs - 8) * 5 -
                a.unsolvableAlternatives + minOf(10, a.solvableAlternatives) + if (met) 1000 else 0
            quality.add(CandidateQuality(index, a.consequenceChoiceStates, a.riskyChoiceStates, a.searchNodes, a.unknownAlternatives, met))
            if (best == null || score > bestScore) { best = candidate; bestScore = score }
        }
        return checkNotNull(best).copy(attempts = checkNotNull(best).attempts,
            qualityTargetMet = quality.any { it.targetMet }, candidatesEvaluated = quality.size,
            candidateQuality = quality).also(LevelValidator::validate)
    }
}
