# CODEX 34 — strategic difficulty

This iteration changes puzzle construction and measures actual individual tray play. It does not establish human-perceived difficulty. APK delivery remains blocked by the build environment; the old APK is not a CODEX 34 build.

## A–B. Why the previous puzzles were easy

The frozen [v8 generator](app/src/main/java/com/tilebloom/game/domain/VersionEightGenerator.kt) is byte-for-byte the previous committed generator except for its object name. Its symbol-assignment loop assigns the same symbol to both members of every PairMove in its construction witness. LevelGenerator v1 chooses both tiles from the currently free set. The structural fallback also peels two free tiles at a time and later assigns a matching symbol to both. Consequently **every generated v8 board has a complete path consisting entirely of immediately available matching pairs**, irrespective of its tile count, symbols, layers, or silhouette.

The old PuzzleSolver searched only whole matching-pair removals; its solvability claims applied to that restricted move space. Candidate scoring rewarded pair unlock differences, available alternatives, and three-held states observed during partly random tray probes. Those random probes sometimes deliberately ignored an available tray match. Their losses/pressure therefore did not show that a sensible player needed to plan. The pair witness itself always returned the tray to zero after each pair. The scheduled last candidate used the simple structural fallback even without construction failure. Visual overlap targets could be met while retaining that easy pair-only path.

The benchmark preserves that implementation and runs both versions with the same production Game controller and simulated players. Historical pair analysis remains available because changing its ordering or score would change v8 save regeneration; current individual-witness boards take a full-state analysis path.

## C–D. Updated model and metrics

Difficulty boundaries remain Normal 1–7, Hard 8–15, Very Hard 16–30, Expert 31+. Tile-count ranges, catalog, artwork and layout definitions are unchanged.

PuzzleAnalyzer exposes strategic analysis plus bounded full-state TrayAnalyzer results. Current-board analysis samples individual witness states every six actions and shares one strict search-node budget. Keys include remaining IDs and unmatched symbol identities. States classify as SOLVABLE, UNSOLVABLE, or UNKNOWN_BUDGET_EXCEEDED; unsearched/exhausted continuations remain unknown. Complete witnesses independently certify initial solvability even if a search cannot find them within its budget.

Metrics distinguish:

- Initial selectable tiles and immediately available matching pairs.
- Covered/blocked counts and overlap relationships.
- Blocking depth: rounds of removing all currently available tiles needed to expose the deepest positions, using Rules.available. This is a structural dependency proxy, not minimum human planning depth.
- Witness states holding one, two, and three unmatched symbols; peak occupancy; delayed matches; largest action gap between partners.
- Deterministic greedy, simple, and depth-limited lookahead outcomes and node usage.
- Sampled verified safe, verified losing, and unresolved continuations. Different tray/unlock results measure consequential branching; a losing-state count is not rewarded as difficulty.

Witness pressure describes a verified route; it does not prove every winning route must reach that occupancy. Simulation differences are the stronger practical filter. Partner delay is not presented as a proof of required planning depth.

## E. Simulated players and information fairness

[StrategicAnalysis.kt](app/src/main/java/com/tilebloom/game/domain/StrategicAnalysis.kt) defines deterministic strategies:

- Greedy: take a tray match, then an available board matching opportunity, then a stable tie break.
- Simple: prefer tray matches, available board partners, and immediate geometric unlocks.
- Lookahead: evaluate individual selections up to six actions ahead under a shared maximum of 1,800 expanded states for the entire playthrough, sharing work across roots and remaining moves. Prefer completion, avoid defeat, then value progress and lower unmatched occupancy. Search exhaustion is a heuristic cutoff, not an unsolvability proof.

Simulated players use no Hint or Undo credits. Every transition uses Game.select; branches use an immutable-state Game.fork, which is covered by exact state/Undo comparisons. No solver receives the winning witness.

Lookahead freezes observed identities at each real decision using the production BoardGeometry/TileVisibility center visibility test, plus currently selectable tiles. Hypothetical search can select only these known identities. A newly exposed identity becomes usable after an actual move and observation. Hidden tiles are filtered **before** symbol grouping and ordering so they cannot affect the budget-limited choice. A regression permutes covered symbols and checks the first decision remains unchanged. Center visibility is a conservative observer approximation, not a model of human recognition of partially obscured botanical artwork.

Selection evaluates three deterministic tie breaks for both shallow players. Four independent tile-ID relabelings are reserved for benchmark holdouts. Relabeling changes priority ties without changing symbols, geometry, or the puzzle; every relabeled witness is validated. Exploratory single-player scoring looked strong but generalized poorly to those holdouts; selection was revised to include the shallow ensemble before final measurements.

## F–G. Symbol distribution and the four-slot tray

[v9 construction](app/src/main/java/com/tilebloom/game/domain/EndlessGenerator.kt) first creates an individually legal geometric removal order. It then opens and closes symbol obligations along that order, maintaining at most three unmatched identities and closing all obligations before the board ends. Some partners occur after other selections and unlocking dependencies, rather than being assigned to successive free-tile pairs. Active symbols and milestone introductions retain complete even counts.

Assignments are controlled by a fixed seeded schedule with explicit tray constraints, not uncontrolled random placement. Each result replays all individual actions through Game before it is scored. There is no tray-size change, triple matching, availability change, or new gameplay rule. The resulting witness is stored separately as selectionWitness; winningSelections expands historical pair witnesses or returns the new individual sequence.

## H. Candidate selection, cost and fallback

At most 24 candidates are constructed. Candidates already failing the shallow-player filter receive a 100-node lookahead probe; eligible candidates retain up to 1,800 nodes. This avoids spending a full search on already rejected profiles. Search outcomes remain actual simulated play, and the independent benchmark always uses the same 1,800-node budget for both versions. Search stops after at least two candidates when the target is met. The current layout family schedule, real terraces and preceding ten-level geometry novelty checks remain. Geometry candidates stay within the existing family for that level. Selection records complete strategic measurements per candidate and reuses the selected result.

Hard requires greedy failure plus failure in at least two of three greedy tie breaks and nonzero witness pressure. Very Hard additionally requires default simple failure, no more than two wins in either shallow ensemble, a lookahead win, delayed matches, three held symbols, at least 30 blocked positions and structural depth at least three. Expert requires at least two failures in each three-player shallow ensemble, a lookahead win, at least six delayed matches, three held symbols, at least 34 blocked positions and structural depth at least four.

The score rewards meeting the tier profile, stronger-player success and delayed-match pressure; penalizes shallow wins and excessive initial matching opportunities; includes bounded blocking metrics and lookahead work. It does not reward random losses or call unknown branches losing. If no candidate qualifies, the best verified board is returned with qualityTargetMet=false. Forced structural fallbacks also retain false. This flag records a limitation, not Expert-quality certification. Some levels can still be easy for players or policies outside the probe set.

## I–L. Benchmark, solvability, variety and performance

Final measured tables and limitations are in [benchmark-summary.md](docs/codex34/benchmark-summary.md); all per-puzzle records are in [benchmark.csv](docs/codex34/benchmark.csv). The benchmark includes every Normal level, representative Hard/Very Hard levels, levels 31–50, 51–100 and higher Expert levels 101/501/1001. Both versions run in the same Java 17 JVM after warming the paths. Introductory fixtures are unchanged and already cached for both measurements.

Each record includes level/tier, layout, layers, tiles, legal selections, immediate pairs, blocking metrics, tray distribution and delays, all player outcomes, search outcomes/budgets, witness validation, candidate count, novelty limitation, fresh generation time and independent analyzer time. Node counts are expanded states, not elapsed-time guarantees. No timeout is an UNSOLVABLE result.

Measured Expert results (nine original boards; 36 independent relabeling holdouts):

| Policy | v8 original wins | v9 original wins | v8 holdout wins | v9 holdout wins |
|---|---:|---:|---:|---:|
| Greedy | 4/9 | 1/9 | 21/36 | 10/36 |
| Simple | 7/9 | 0/9 | 36/36 | 18/36 |
| Lookahead | 9/9 | 9/9 | 36/36 | 36/36 |

Depth-one lookahead wins 1/9 updated Expert boards; depth six wins 9/9 under the same global cap. The selected Expert boards carry 15–22 delayed matches and a witness peak of three unmatched symbols. Level 31 misses the profile and remains a limitation. Very Hard holdout simple success falls from 12/12 to 9/12 while lookahead wins 11/12; that smaller gap needs further human-guided tuning.

Expert median cold generation was 223.4 ms for v8 and 579.5 ms for v9; observed maxima were 1,076.1 and 4,048.4 ms, respectively, during concurrent verification work. This is a real cost increase, with bounded background generation and result reuse rather than an intentional wait. Independent v9 Expert strategic analysis had a 226.0 ms median on this host. All 44 sampled boards and 96 holdout witnesses passed controller verification. The continuation sampler retained 146 unresolved v9 alternatives as unknown; it did not report them as losses.

No tile-count or artwork increase contributes to the result. All eight families remain scheduled. All accepted boards and relabeled holdouts replay to WON with no tray overflow. The broader regression matrices check hit testing, genuine multilayer support, determinism, even symbol counts, novelty and exact individual Undo.

Generation remains in the existing MainActivity initialization worker / GameStore LevelPreparation executor; the existing loading screen and deadline remain. A synchronized twelve-entry LRU reuses recent immutable LevelDefinitions without holding its lock during generation. Cold generation now does substantially more work than v8. Host timings, including concurrent-load limitations, must not be treated as Android-device latency measurements.

## M. Save compatibility and preserved systems

Generator VERSION advances from 8 to 9. GardenSession restores version 8 via the frozen VersionEightGenerator, alongside unchanged v4–v7 routes. Existing schema 5 already saves individual accepted selections, so its envelope and credit fields need no migration. v9 boards regenerate deterministically; existing v8 boards retain their geometry, assignments, seed and accepted history, including a partly occupied tray. Future levels use v9. Long.MAX_VALUE completion recovery now uses the individual witness rather than assuming pairs.

No player save file was edited or cleared. Forward-only progression and exactly +1 advancement remain. AdMob, UMP, interstitial frequency, online-only behavior, garden themes, artwork, animation, visibility, Hint economy and Undo economy production files were not changed. Their existing regression tests remain.

## N–P. JVM, Android and APK verification

**Final isolated JVM result: 264 tests passed.** Production Android and instrumentation sources also compile with the cached compiler, without executing instrumentation.

See [verification.md](docs/codex34/verification.md), the isolated [JVM output](docs/codex34/tests-final.txt), and [Android compilation output](docs/codex34/android-final.txt). The isolated runner avoids the old offline tool's shared-directory deletion. Exploratory overlapping runs are not valid final evidence.

Standard JVM Gradle, assembleDebug and lintDebug were attempted and blocked before execution by wrapper write/network/socket restrictions. Independent cached-compiler JVM tests and Android source compilation are reported separately, without calling them Gradle or device verification. ADB's socket listener is also forbidden; no instrumentation was executed.

**No fresh APK could be built in this environment.** The existing artifact is unchanged: timestamp 2026-10-29 01:20:01 +0100, 7,565,391 bytes, SHA-256 a27abc37b9ba483b186436d1df556cf5e5839eaffe999eacefb9678c5b1244c7. Its certificate SHA-256 is fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6, matching the required certificate. These identify an older APK, not this source revision. Signing configuration was not modified. No uninstall, data clearing, push, publication or installation occurred.

## Q. Git

The established repository is .tilebloom-git because the exposed .git directory is read-only and does not contain a usable checkout. Existing history is retained. Only CODEX 34 changes and final evidence are committed; unrelated pre-existing untracked files are excluded. The final response records the new commit ID. No reinitialization, history rewrite or push was performed.

## R–S. Remaining limitations and human playtest

Automated evidence supports an improvement in strategic difficulty proxies, not a conclusion that the player's complaint is fully resolved. Some candidate budgets miss their profile, some shallow holdout players still win, and bounded searches leave alternatives unknown. Probe selection can still overfit. Greater witness pressure is not necessarily unavoidable pressure; structural depth is not a proof of human planning depth. Covered-information fairness is improved by the observer restriction but still needs actual observation and human feedback. Cold Android generation time, device rendering, and fresh signed APK verification remain unverified here.

[The playtest protocol](docs/codex34/playtest.md) compares all four tiers and asks about planning, obvious pairs, move order, tray management, understandable losses, fairness, enjoyment and willingness to continue. Responses are blank. No player comments or observations have been invented. Use actual action traces and feedback, especially hidden-information losses and profile-limited boards, to guide the next tuning pass.
