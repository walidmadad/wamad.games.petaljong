# CODEX 08 — strategic puzzle quality

## A. Executive summary

The engine, botanical art, layout geometry and tile-match effects are preserved. Victory now has one presentation. The live Level 1 is a versioned symbol fixture with an approachable opening and a proven consequential choice. The generator compares bounded, witness-verified candidates; a new Android-independent analyzer separates legal move counts, unlock differences and demonstrated solvability risks.

## B. Confirmed UI fixes

`MainActivity.update()` derives mutually exclusive visibility from `ScreenPresentation`: PLAYING shows instruction/Restart; WON shows only the centered victory panel; NO_MOVES shows the deadlock explanation/Restart. The old victory footer is empty and GONE, and its obsolete Play Again control is GONE. Replay and restart clear board feedback; advancing loads a fresh board; restoration derives visibility from the restored domain state. Existing animation files/timings are unchanged.

Next Level has a forest-green full-width surface, ivory label, muted-gold outline, 24 dp rounded corners and restrained gold ripple. Replay has a darker secondary surface and the same minimum 48 dp height. The scrollable central panel stays inside the board area. Actual compact-screen and large-font visual review remains outstanding.

## C. Original puzzle analysis

All five originals have two layers, six symbols and verified solutions. Tile count alone is a poor difficulty predictor. L1 has 11 multiple-choice witness states, yet no inspected witness alternative loses solvability. L4 likewise has zero risky witness states despite its larger board. L3 already has demonstrable risks. The original boards are not globally immune to deadlocks: random-move rollouts reach paths that the witness-local audit never visits.

The measurement tables below are generated from production-rule replay. “Blocked” includes side blocking and covering. “Unlock moves” counts pair removals involving an upper tile that make at least one lower tile newly available. The branching lists exclude the empty terminal state. “Risk” requires both a proven solvable and proven unsolvable successor at the same witness state.

### Original measured boards

All rows: two layers, six symbols.

|Level|Tiles|Removals|Free|Blocked|Pairs|Matching symbols|Forced/multiple|Unlock moves|Different consequences|Risk states|Arbitrary wins|
|---|---|---|---|---|---|---|---|---|---|---|---|
|1|24|12|6|18|3|3|1/11|2|8|0|20/32|
|2|24|12|11|13|6|4|1/11|3|9|0|29/32|
|3|28|14|10|18|5|5|1/13|6|11|2|23/32|
|4|30|15|9|21|5|3|2/13|6|12|0|24/32|
|5|28|14|10|18|6|4|3/11|5|11|0|21/32|

|Level|Legal matching choices before each witness removal|
|---|---|
|1|[3, 6, 6, 5, 12, 8, 4, 4, 4, 3, 2, 1]|
|2|[6, 5, 5, 6, 8, 3, 4, 2, 2, 2, 2, 1]|
|3|[5, 4, 9, 8, 3, 5, 6, 8, 4, 5, 3, 2, 2, 1]|
|4|[5, 4, 7, 3, 5, 9, 4, 3, 2, 3, 2, 2, 1, 2, 1]|
|5|[6, 8, 3, 8, 8, 7, 7, 9, 5, 4, 3, 1, 1, 1]|

|Level|Solvable successors|Unsolvable successors|Unknown successors|Search nodes|
|---|---|---|---|---|
|1|58|0|0|457|
|2|46|0|0|376|
|3|63|2|0|2031|
|4|53|0|0|1372|
|5|71|0|0|800|

### Updated measured boards

All rows: two layers, six symbols.

|Level|Tiles|Removals|Free|Blocked|Pairs|Matching symbols|Forced/multiple|Unlock moves|Different consequences|Risk states|Arbitrary wins|
|---|---|---|---|---|---|---|---|---|---|---|---|
|1|24|12|6|18|2|2|1/11|2|8|1|19/32|
|2|24|12|11|13|7|5|2/10|2|10|1|21/32|
|3|28|14|10|18|8|3|1/13|5|11|1|22/32|
|4|30|15|9|21|7|3|2/13|6|11|1|18/32|
|5|28|14|10|18|6|4|2/12|6|11|4|18/32|

|Level|Legal matching choices before each witness removal|
|---|---|
|1|[2, 8, 10, 7, 8, 4, 8, 4, 4, 3, 2, 1]|
|2|[7, 9, 8, 8, 8, 4, 5, 3, 3, 2, 1, 1]|
|3|[8, 3, 3, 5, 4, 6, 2, 2, 4, 3, 3, 2, 2, 1]|
|4|[7, 7, 9, 4, 2, 2, 5, 4, 3, 2, 1, 3, 3, 2, 1]|
|5|[6, 5, 6, 4, 10, 10, 11, 4, 2, 2, 1, 3, 2, 1]|

|Level|Solvable successors|Unsolvable successors|Unknown successors|Search nodes|
|---|---|---|---|---|
|1|60|1|0|555|
|2|58|1|0|768|
|3|47|1|0|1090|
|4|54|1|0|950|
|5|62|5|0|1887|

Arbitrary wins use 32 fixed Random seeds (0–31), uniformly sampling a legal pair each turn. They measure a particular uninformed policy, not players. L1 changes from 20/32 to 19/32: a modest change, not evidence of a major difficulty increase. L3 deliberately becomes gentler than the sharp candidate rejected during development. L4 and L5 both yield 18/32, despite different strategic structures.

All live boards meet their configured profile: candidate counts L2/L3/L4/L5 = 2/3/2/5. L1 was curated offline from 24 assignments. All five 10,000-node audits classify every inspected successor; no UNKNOWN in those five audits.

## D. Puzzle analyzer

`PuzzleAnalyzer` validates geometry, IDs, symbol parity and the complete witness before analysis; an empty definition is explicitly supported. It uses `Game.matchingPairs()` and `Rules.available()` as the only move/availability rules. It does not mutate the definition. Output includes layout/seed, counts, witness branching, forced/multiple choices, upper unlock moves, consequence-choice states, alternative classifications, node usage and completeness.

A consequence-choice state has alternatives with different sets of newly unlocked tile IDs. Removing interchangeable exposed pairs without unlocking anything yields the same empty signature. This is a useful geometric proxy, not proof of human interest. A risky state is stronger evidence of consequences, but says nothing about whether the mistake is visually fair.

`PuzzleSolver` performs deterministic depth-first search with memoized sets of remaining tile IDs. It supports more than 64 IDs without truncation. Small-board lists/sets favor clarity over a specialized bitset implementation. It returns SOLVABLE after finding a complete path; UNSOLVABLE only after every successor is proven unsolvable; otherwise UNKNOWN_BUDGET_EXCEEDED. Unknown states are never memoized as failures. Each analyzer call shares one budget across all alternatives. Completeness means all inspected witness successors were classified, not exhaustive coverage of every reachable board state.

## E. Level 1 balancing

The live fixture retains 24 tiles, two layers, six symbols with four copies each, the original positions and the original complete witness. Only symbol relationships below the opening upper pairs change. Twenty-four deterministic assignments were evaluated offline using Random(108); candidate 12 was selected for a two-pair opening and one risky witness state. Other candidates had four such states and were rejected as unnecessarily sharp for an introduction. The selected fixture is pinned in source; startup does not repeat that search.

Witness-pair symbol ordinals are `0,1,2,3,4,4,5,5,0,1,3,2`. The witness remains legal and complete rather than being replaced unnecessarily. `LevelOne` intentionally retains the historical v0 fixture for engine regressions. Live gameplay uses `Levels.get(1)`, seed 0/version 2. Android tests use the live witness and updated coordinate deadlock path. Old v0 snapshots are rejected atomically and the activity opens a fresh puzzle.

## F. Levels 2–5

The intended progression is clear unlock decisions (terrace), branching with observable consequences (woodland), additional overlapping relationships (courtyard), and anticipation in the final woodland. Measurements support these design directions, not a strictly monotonic human difficulty scale. Candidate filtering caps observed risky witness states for the branching tutorial to avoid the much sharper alternative found during development. Level 5 uses curated seed 511/version 2, replacing seed 505/version 1. Geometry and tile counts are unchanged throughout.

## G. Generation quality filtering

Version 1 generation remains explicitly callable with its original Random ordering. Version 2 uses at most six candidates at `seed + index * 7919`, each with at most 64 geometric construction attempts and 16 removals. Every candidate passes the existing complete-witness validator. At least two candidates are considered. Thresholds require 3/5/6 unlock-choice states for L2/L3/L4–5, no more than half forced states, and proven risk (two states for L5). The branching tutorial also caps observed risk at two and requires complete classification of its witness alternatives. These are deliberately modest design targets.

Each candidate records consequence/risk counts, node usage, unknown counts and target status. The first suitable candidate after the minimum comparison is accepted. Otherwise the highest-scoring verified candidate is returned with `qualityTargetMet=false`; no claim is made that it satisfies the target. An impossible geometry still fails explicitly after the construction bound, never fabricating a solution. Tests exercise an impossible quality target with two attempts. Internal test profile overrides are separate from the public versioned generation entry point.

## H. Solvability guarantees and compatibility

Every accepted and fallback board is replayed completely through production `Game.match()`. The witness guarantee is independent of any bounded solver result. A player can still deadlock through a different pairing. Restart reuses the exact accepted tiles and clears history/selection; it does not generate a new puzzle.

All live fixtures now have version 2. Seeds for L1–4 remain 0/202/303/404; L5 is 511. The old generated identities remain reproducible by explicit version 1 calls; the old L1 fixture remains in `LevelOne`. Session restoration checks level, seed and version before replaying saved accepted moves, on a temporary game. Incompatible histories are rejected without partial mutation. There is no attempt to migrate a move history between incompatible boards; activity fallback starts Level 1 safely. Valid v2 sessions retain exact restoration behavior.

## I. Performance

Generation analysis runs only during level creation, never on a frame or touch callback. Definitions are cached after creation. Production candidate budgets are 2,000 shared nodes for L2/L5, 5,000 for the branching tutorial (L3) and 4,000 for courtyard (L4); diagnostic audits use 10,000 shared nodes per board, and the representative quality matrix uses 1,000. Memoization is per solver invocation rather than a global cache. Counts and host timings below are measurements, not Android performance claims.

Final host JUnit execution: **6.663 s**, 104 tests. Preserved 120-board matrix: **4,690 ms**, maximum one geometric attempt for each selected candidate (quality attempts are counted separately). Extra 16-board matrix: **596 ms** generation including internal quality evaluation, **120 ms** separate 1,000-node audit. These aggregate timings do not isolate raw geometric generation cost.

The 16-board matrix evaluated **58 candidates**, selected 16 verified boards, met the target in **14**, and used explicitly flagged fallbacks in **2**. At the individual-candidate level, **20** met the profile and **38** did not. **42** candidates were not selected; this includes candidates considered during the minimum comparison, not necessarily 42 failed profiles. One candidate analysis exhausted its 5,000-node budget; two selected-board 1,000-node audits exhausted theirs. Full candidate outcomes are recorded in `app/build/codex08/quality-matrix.tsv`, alongside the final verification log. UNKNOWN counts are preserved. These are local JVM timings, not Android latency measurements.

## J. Regression tests

Final serial diagnostic command: `python3 tools/verify_offline.py`. **104 JVM tests passed, 0 failed**: all 89 existing regressions retained (two live-fixture expectations deliberately updated), 13 analyzer/quality tests and two presentation tests added. All ten host-workflow tests passed via `python3 tools/test_host_verify.py`. Ten Android instrumentation methods compiled; **0 executed**. Standard Gradle tests/assemble/Lint: NOT EXECUTED under unchanged restrictions.

During development, one fixture-dependent session deadlock test failed when a provisional symbol candidate had no deadlock; it was replaced with bounded discovery on the accepted live fixture plus an explicit coordinate-path regression. Overlapping intermediate diagnostic invocations also caused shared-output compilation errors; these are discarded, and the final artifact is built serially. No valid regression was removed to hide a failure.

Coverage includes empty/invalid input, no-pair states, solvability, forced and multiple choices, unknown results, shared budgets, immutability, deterministic generation, bounded fallback, live L1 parity/deadlock fixture, snapshot version rejection, every witness and transition, restart, geometry/touch hit testing and unchanged match-effect tests. The original 120-board matrix remains. A separate 16-board matrix samples four seeds across all four families and records quality results and budgets.

## K. Android verification

Ten existing instrumentation methods are preserved and extended. Coordinate taps complete the live witness and reproduce the new deadlock. Victory assertions check one visible title, hidden footer/Restart, and visible Next/Replay. Deadlock asserts hidden victory actions and visible Restart. Progression, counts, touch gestures, effects and recreation remain covered.

Instrumentation is compiled only, not executed. ADB discovery failed with `could not install *smartsocket* listener: Operation not permitted`. No updated APK was installed/launched, no updated runtime or visual validation is claimed. Standard Gradle/JVM/assemble/Lint tasks were not reattempted: unchanged sandbox restrictions and missing host evidence are already established in HOST_VERIFICATION.md. Diagnostic compilation does not establish standard Gradle or Lint success.

## L. APK output

- Path: `app/build/outputs/apk/debug/app-debug.apk` (also archived at `app/build/codex08/app-debug.apk`).
- Method: final serial diagnostic offline pipeline, exit 0; not standard Gradle.
- Size: **737,683 bytes**.
- SHA-256: `6cb49ea56e63fbf3a41310a4608e9a9334d6200df2b327eaa6cd8aaca3fda37b`.
- Alignment: PASS (`zipalign -c 4`). Signatures: PASS, v2/v3, one debug signer. v1/v3.1/v4 and SourceStamp absent.
- Metadata: `com.tilebloom.game`, debug enabled, min API 26, target/compile API 36, launch activity `.presentation.MainActivity`.
- Installation, launch, instrumentation execution and updated visual playtest: NOT EXECUTED.

## M. Git status

The repository uses `.tilebloom-git`; parent commit is `756551106fc1e246900d6cdb350b8bff660c7593`. No history rewrite, new repository, push or publication. The CODEX 08 commit contains this report; retrieve its hash using `git --git-dir=.tilebloom-git --work-tree=. log -1`. CODEX_07_REPORT.md remains historical and unmodified.

## N. Acceptance criteria

PASS below denotes implemented behavior verified by source/JVM tests unless explicitly qualified. Android execution remains separate.

| # | Criterion | Result |
|---|---|---|
|1|Existing engine preserved|PASS|
|2|Existing animations preserved|PASS — source unchanged; effect regressions pass|
|3|Duplicate victory fixed|PASS — visibility regression; device review pending|
|4|Garden victory styling|PASS — implemented; device visual review pending|
|5|All five levels playable|NOT VERIFIED — updated device gameplay pending; witnesses/hit tests pass|
|6|All five initially solvable|PASS|
|7|Complete witnesses verified|PASS|
|8|Useful analysis metrics|PASS|
|9|Forced states identified|PASS|
|10|Meaningful branching analyzable|PASS — documented proxies|
|11|Search budgets respected|PASS|
|12|Unknown results not mislabeled|PASS|
|13|Level 1 feedback addressed|PASS — versioned redistribution; human assessment pending|
|14|L2–5 progression documented|PASS — intended profiles and measurements|
|15|Deterministic filtering|PASS|
|16|Bounded generation|PASS|
|17|Seed/version compatibility|PASS — explicit rejection of incompatible histories|
|18|Reliable restart|PASS — domain/effect regressions; device pending|
|19|Correct deadlock behavior|PASS — live fixture/domain; device pending|
|20|Safe restoration|PASS — atomic replay/version checks|
|21|Existing JVM regressions|PASS|
|22|New analyzer tests|PASS|
|23|Android tests preserved|PASS — ten compiled, zero executed|
|24|Valid updated APK|PASS — diagnostic aligned/signed artifact|
|25|Accurate build/runtime reporting|PASS|
|26|Git history preserved|PASS|

## O. Known limitations

No broad player study or scientific difficulty validation. Random policy results are 32 deterministic simulations per board, not player win rates. Witness-local risk is a lower bound; no risky witness alternatives does not imply arbitrary play is safe. Search budgets can hide additional risk, and quality targets do not prove fairness. Some metrics remain nonmonotonic across levels. Set/list allocation is suitable for current tiny boards but should be profiled before larger layouts. Initial generation runs synchronously; Android latency is not measured. TalkBack tile actions and device/font-size coverage remain pending. No Undo, Hint, Shuffle, timers, penalties or monetization were added.

## P. Recommendation for CODEX 09

Play the updated five-level sequence on a real device or emulator, record confusing pair decisions and restart frequency, and tune curated seeds from those observations. Check victory layout at compact sizes and enlarged fonts; run the ten UI tests plus standard Gradle/Lint outside this sandbox. If generation is visibly slow, move level preparation off the UI thread. Prioritize fairness/readability and enjoyable anticipation before more content or assistance features.
