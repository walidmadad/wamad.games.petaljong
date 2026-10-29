# CODEX 34 measured benchmark

Java 17, Kotlin 1.9.24, fixed levels/seeds; frozen v8 and current v9 in one JVM after warming both paths. Generation is timed before separate analysis and holdout probes. Each lookahead playthrough has at most 1,800 expanded states and depth six. Depth-one results use the same policy without future search. Full-state continuation sampling has a shared 1,000-node budget per puzzle. Four independent ID relabelings test tie-break generalization on levels 16+; they were not used for selection. These are puzzle design proxies, not human difficulty measurements.

| Tier | Version | Boards | Greedy wins | Simple wins | Lookahead wins | Depth-one wins |
|---|---:|---:|---:|---:|---:|---:|
| NORMAL | 8 | 7 | 6/7 (85.7%) | 7/7 (100.0%) | 7/7 (100.0%) | 6/7 (85.7%) |
| NORMAL | 9 | 7 | 6/7 (85.7%) | 7/7 (100.0%) | 7/7 (100.0%) | 6/7 (85.7%) |
| HARD | 8 | 3 | 2/3 (66.7%) | 3/3 (100.0%) | 3/3 (100.0%) | 2/3 (66.7%) |
| HARD | 9 | 3 | 0/3 (0.0%) | 3/3 (100.0%) | 3/3 (100.0%) | 0/3 (0.0%) |
| VERY_HARD | 8 | 3 | 1/3 (33.3%) | 3/3 (100.0%) | 3/3 (100.0%) | 1/3 (33.3%) |
| VERY_HARD | 9 | 3 | 0/3 (0.0%) | 0/3 (0.0%) | 3/3 (100.0%) | 0/3 (0.0%) |
| EXPERT | 8 | 9 | 4/9 (44.4%) | 7/9 (77.8%) | 9/9 (100.0%) | 4/9 (44.4%) |
| EXPERT | 9 | 9 | 1/9 (11.1%) | 0/9 (0.0%) | 9/9 (100.0%) | 1/9 (11.1%) |

| Holdout tier | Version | Greedy wins | Simple wins | Lookahead wins |
|---|---:|---:|---:|---:|
| VERY_HARD | 8 | 4/12 (33.3%) | 12/12 (100.0%) | 12/12 (100.0%) |
| VERY_HARD | 9 | 5/12 (41.7%) | 9/12 (75.0%) | 11/12 (91.7%) |
| EXPERT | 8 | 21/36 (58.3%) | 36/36 (100.0%) | 36/36 (100.0%) |
| EXPERT | 9 | 10/36 (27.8%) | 18/36 (50.0%) | 36/36 (100.0%) |

## Generation and analyzer cost

These host timings were measured during other verification work and include CPU contention. They are not Android latency measurements. Levels 1–5 share unchanged, warmed cached fixtures; exclude them from cold timing comparisons. No artificial delay is added.

| Tier (levels 6+) | Version | Median generation ms | Maximum generation ms | Median independent analysis ms |
|---|---:|---:|---:|---:|
| NORMAL | 8 | 389.8 | 585.2 | 191.3 |
| NORMAL | 9 | 365.5 | 572.1 | 189.5 |
| HARD | 8 | 201.9 | 367.7 | 188.6 |
| HARD | 9 | 512.1 | 829.0 | 235.8 |
| VERY_HARD | 8 | 516.1 | 525.2 | 324.8 |
| VERY_HARD | 9 | 951.1 | 2535.9 | 255.2 |
| EXPERT | 8 | 223.4 | 1076.1 | 264.6 |
| EXPERT | 9 | 579.5 | 4048.4 | 226.0 |

## Verification and interpretation

- All 44 original sampled boards and all 96 relabeled holdout witnesses passed production-controller verification. No witness overflowed.
- v9 boards missing the strategic profile: 31. Historical v8 target flags use its old geometry/pair metrics and are not comparable strategic certifications.
- v8 witness peak occupancy is always one. v9 Expert witness peak occupancy is 3–3, delayed matches 15–22 per board. This describes a certified route, not minimum required pressure.
- Maximum expanded lookahead states: 1686 / 1,800. Maximum full-state search expansions: 1000 / 1,000.
- Sampled continuation totals v8: safe 922, losing 0, unknown 263; v9: safe 896, losing 10, unknown 146. These sample different certified routes and are not exhaustive loss probabilities. Unknown is never counted as losing.
- Distinct scheduled advanced families in sampled v9 boards: 8. Novelty-limited sampled v9 boards: 0.
- Holdouts expose residual shallow wins and selection-policy overfitting. Expert generalization improves, but several boards remain easy for some tie breaks. Very Hard holdout separation is weaker and needs more tuning/playtest evidence.
- Visibility-restricted lookahead cannot use a newly uncovered identity until a real observation. This reduces hidden-information peeking; it does not prove every loss is fair or every choice is inferable to a human.
- The broader JVM matrix contains additional profile misses; this is not a claim that every Expert level meets the target. Human playtest and device performance remain unverified.

[Per-puzzle measurements](benchmark.csv) · [Playtest protocol](playtest.md) · [Build limitations](verification.md)
