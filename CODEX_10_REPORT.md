# CODEX 10 — Level 1 redesign

## A. Executive summary

Level 1 now uses a winding, asymmetric 24-tile terrace, with 18 base tiles over five rows and six caps over two separated rows. Tile count, six botanical symbols, production engine, artwork, animations, progression and storage implementation are preserved. This is a geometry change, not a symbol-only rebalance. [Geometry comparison](docs/level1-layout.svg) is a schematic, not an Android screenshot.

## B. Original Level 1 weaknesses

The CODEX 09 baseline is retained as `Levels.legacyIntroductory`. Its lower 4×4 rectangle and upper 4×2 cap repeated the prototype silhouette. The witness repeatedly paired corresponding rows, then worked column by column. Initial matches were (16,20) and (19,23); each upper row had identical symbol order. Removing (16,20) made eight associations legal, followed by ten. Many were interchangeable clearing actions rather than planning problems. Only two witness moves removed caps and unlocked lower tiles. Both openings were safe; most of the witness required little anticipation. The one confirmed risky witness state and sampled random failures did not establish interesting difficulty.

Baseline: 24 tiles, six free, two initial pairs, branching `2,8,10,7,8,4,8,4,4,3,2,1`; one forced state; eight states with different immediate unlock consequences; one risky state; 60 solvable / one unsolvable / zero unknown alternatives; 555 search nodes. Full symbol/geometry fixture and witness remain in source. The original 127 tests passed with the exploratory portfolio test (128 total) before activating the redesign.

## C. New Level 1 design

Deterministic offline portfolio: 60 assignments, four silhouettes. Every candidate has even symbols, unique IDs, valid two-layer geometry and a complete witness replayed through `Game.match` to WON by `LevelValidator`. Each receives a shared 10,000-node analyzer budget. See [all candidate measurements](docs/codex10-candidates.csv), including forced sequences through full witness branching profiles. All 60 initial boards are SOLVABLE by witness, and all portfolio alternative queries completed with zero UNKNOWN results. This does not enumerate every reachable state.

| Silhouette | Seeds | Reason for selection/rejection |
|---|---|---|
| Sprout | 100–111 | 6–11 initial pairs; too many exposed associations |
| Steps with short caps | 100–111 | 4–8 initial pairs; short caps too immediately accessible |
| Crescent | 100–111 | 4–8 initial pairs; six-tile width reduces compact face width to about 42 dp |
| Winding with three-tile caps | 100–123 | Six free tiles; more sustained cap blocking; selected seed 123 |

Selection was a design review of the bounded portfolio, not a maximum-risk score. Seed 123 meets these explicit targets: two opening pairs, maximum five witness associations, at least eight different-unlock choice states, at least four upper unlock moves, at most three forced witness states, one confirmed risky state, no unknown witness alternatives, compact face width at least 48 dp. It satisfies all targets. Other winding candidates were rejected for excessive opening matches, larger association floods, too much risk, or less sustained layered unlocking. Seed 115, for example, had no risky witness states but a larger seven/eight-association middle game; seed 111 had five risky states.

## D. Geometry and symbol distribution

Logical positions are tile footprints, one unit square. Base rows at y=0..4 have `(start x, count)` = `(0,3), (0,4), (1,4), (1,4), (2,3)`. Caps are at `(0.5,0.5), (1.5,0.5), (2.5,0.5), (1.5,2.5), (2.5,2.5), (3.5,2.5)`. IDs follow base rows then caps. Middle caps 19 and 22 are laterally blocked. Caps overlap lower footprints and no same-layer footprints overlap. No third layer was needed.

| Symbol | IDs |
|---|---|
| Leaf | 1,7,17,18 |
| Lotus | 2,3,10,22 |
| Fern | 0,8,12,14 |
| Flower | 11,15,19,23 |
| Berries | 5,6,16,21 |
| Sunflower | 4,9,13,20 |

All six symbols occur four times. Matching upper tiles are not arranged as mirrored or corresponding rows. Some simple remaining base pairs are intentional relief near completion.

## E. Strategic decision analysis

Initial free IDs: 15,17,18,20,21,23. The two legal pairs are flower (15,23) and leaf (17,18). Both are SOLVABLE. Flower unlocks IDs 10,14,16,22; leaf unlocks 0,3,16,19. This gives different visible routes through the stacks without an opening trap.

After witness pairs (23,15) and (21,16), associations are:

| Pair | Newly available IDs | Bounded result |
|---|---|---|
| Leaf (7,17) | none | UNSOLVABLE, 12 nodes |
| Leaf (7,18) | 0,3,19 | SOLVABLE |
| Lotus (10,22) | 9 | SOLVABLE |
| Leaf (17,18) | 0,3,19 | SOLVABLE |

Removing cap 18 visibly opens the upper-left group; consuming the two base leaves instead keeps that cap blocking it. This is the intended anticipation lesson. Hidden symbols still require a fairness review by people; no claim that every trap is self-evident.

New witness branching: `2,3,4,5,4,4,3,1,2,1,2,1`. Nine multiple-choice states, three forced states (zero-based steps 7,9,11; longest forced run one), eight distinct-unlock choice states, five upper unlock moves, one risky state; 31 solvable / one unsolvable / zero unknown alternatives in 270 nodes. Five extra associations share an immediate unlock signature with another association. This measures local equivalence, not equivalence of all future consequences. The new `PuzzleAnalyzer.alternatives` exposes IDs and outcomes and is checked against production state transitions.

Sampled random play wins: old 19/32, new 29/32. This is evidence of greater forgiveness, not increased objective difficulty. The design improvement is a different silhouette, smaller decision sets and more frequent meaningful stack releases, not more traps. Human enjoyment and whether Level 1 is sufficiently challenging remain unverified.

## F. Solvability verification

Complete production-validated witness:

`(23,15), (21,16), (18,17), (11,19), (14,0), (7,1), (3,10), (20,4), (6,5), (22,2), (13,9), (12,8)`.

Every pair has different IDs, equal symbols and two available tiles at removal. Both blocking rules apply; final state is WON with zero remaining tiles. Restart restores the exact definition. Initial solvability does not guarantee arbitrary play succeeds.

Confirmed deadlock UI fixture: `(15,23), (10,22), (9,20), (6,16), (17,18), (0,14)`, leaving 12 tiles and NO_MOVES. This is a different route from the witness risk example. Solvers retain separate SOLVABLE, UNSOLVABLE and UNKNOWN_BUDGET_EXCEEDED outcomes; budget-zero and shared-budget regressions remain passing.

## G. Levels 1–5 difficulty progression

| Level | Tiles | Initial pairs | Different unlock states | Upper unlock moves | Risk states | Forced states | Random wins /32 |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 new | 24 | 2 | 8 | 5 | 1 | 3 | 29 |
| 2 | 24 | 7 | 10 | 2 | 1 | 2 | 21 |
| 3 | 28 | 8 | 11 | 5 | 1 | 1 | 22 |
| 4 | 30 | 7 | 11 | 6 | 1 | 2 | 18 |
| 5 | 28 | 6 | 11 | 6 | 4 | 2 | 18 |

Levels 2–5 remain byte-for-byte definitions with their existing identities and verified witnesses. Level 2 already has more simultaneous associations and unlock-choice states than the new introduction, with lower sampled random success. No Level 2 adjustment was justified by these measurements. The curve is not objectively monotonic: Level 3 has slightly more random wins than Level 2, and Level 1 has more cap unlock moves than Level 2. The intended progression remains a human-testing hypothesis, not a certified ranking.

## H. Generator versioning

Level 1 identity is now `(id=1, seed=123, version=3)`, layout `winding`. Construction is frozen to deterministic generator v1 and this seed; at most 64 geometry attempts (selected board uses one). No quality search occurs on Level 1 launch. `LevelGenerator.VERSION` stays 2 so that Levels 2–5 do not change. `INTRODUCTORY_VERSION` explicitly versions the new puzzle. Future changes to frozen generation must increment affected identities. Portfolio metadata describes offline evaluation, not live launch work.

## I. Persistent save compatibility

Schema 1 and filename `player-progress-v1` remain unchanged. Identity validation compares ID, seed and version before replay. CODEX 09 Level 1 `(1,0,2)` histories are intentionally incompatible with the new geometry and restart only that attempt on verified v3. Validated contiguous completion records and derived unlocks remain; replay cannot revoke completion. A valid v2 Level 2–5 attempt restores normally. Historical v2 Level 1 is retained for regression/audit rather than offered for continued play, so updating actually delivers the redesigned board.

Regression envelope `1\n1,2\n1,0,2\n16,20;0,4` retains completions {1,2}, unlocks through 3, starts fresh v3 Level 1, then round-trips without a second recovery. Another regression preserves a compatible CODEX 09 Level 2 envelope byte-for-byte. Incompatible session restores are atomic. Storage writer, AtomicFile rollback, owner tokens and lifecycle behavior are unchanged. Durable history takes precedence over Bundle; a Bundle selection is accepted only on exact identity and exact moves, so stale v2 selection cannot leak into v3. No application-data wipe is required or performed.

## J. Android rendering and touch geometry

Production BoardGeometry already fits active initial footprints and orders upper tiles last. No renderer/effect changes were necessary. Full selected witness is tapped through JVM geometry/selection at board viewports 284×360, 360×520 and 412×650. Faces are approximately 48.17, 62.58 and 71.62 units wide respectively, with all shadows inside bounds, correct topmost hits after removals and exact restart. These are board viewport tests, not full phone screen tests. A shorter 284×300 viewport still fits but reduces faces below 48; existing constrained-view regression remains passing. Android font size, actual board allocation, symbol readability and touch comfort need device observation. See schematic and PLAYTEST_01.md.

## K. Automated test results

Diagnostic Kotlin 1.9.24 / Java 17 / JUnitCore: **139 PASS**, original 127 test purposes preserved, 12 redesign tests added. Historical engine/geometry/effect fixtures remain. Host-workflow tests: **10 PASS**. Full 120-board generator matrix preserved: 3,766 ms, maximum one geometry attempt. Sixty-candidate portfolio: 328 ms. Entire JVM suite: 6.31 s in final build. Timings are single desktop runs, not Android performance claims.

The independent existing 16-board/1,000-node analysis matrix has two exhausted analyses and one candidate-quality exhaustion; those remain UNKNOWN, not confirmed failures. Fourteen quality profiles met targets and two used verified solvable fallback. These are separate from the fully resolved Level 1 portfolio.

Java 25 initially failed the cached Kotlin compiler (`IllegalArgumentException: 25.0.4.1`); rerunning with the project's required Java 17 resolved that toolchain mismatch. No source workaround or global toolchain change was made.

## L. Android runtime results

NOT EXECUTED: installation, launch, connected instrumentation, process restart, actual persistence, update migration, screenshots and human playtesting. The unchanged restricted sandbox previously diagnosed wrapper-lock and ADB-listener failures; these were not repeatedly retried. Standard Gradle build/JVM/Lint are NOT EXECUTED; diagnostic results do not substitute for them.

Instrumentation preserves all fourteen methods, adapts ID interactions to the new witness and adds one real-store v2-upgrade launch case: **15 methods compiled, zero executed**. Actual gameplay assertions inject coordinates into the view, including full completion, deadlock, restart, recreation and five-level transitions. Repository/controller inspection verifies storage contents but does not substitute for UI taps. Fresh-activity relaunch is not fresh-process verification.

Run on an authorized unrestricted host with Java 17:

```sh
bash tools/host_verify.sh
./gradlew :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
# After playing several pairs and pressing Home:
adb shell am force-stop com.tilebloom.game
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
adb shell run-as com.tilebloom.game cat files/player-progress-v1
```

Use PLAYTEST_01.md for persistence and real-upgrade steps. Preserve application data and matching signing identity; do not reinstall to conceal a migration failure. HOST_VERIFICATION.md supplies detailed logs/device commands; its older iteration sections are historical.

## M. APK output

New-source diagnostic APK: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`, 745,875 bytes. Built with `tools/verify_offline.py`, Java 17, Kotlin 1.9.24, SDK 36 / Build Tools 35.0.0. Alignment check PASS; signature verification PASS (one signer, v2/v3). SHA-256 `32747acd23b102a57b22afe42b5fda73cd341187e759e4c6573d2f79a403eb6b`. Package com.tilebloom.game, min/target 26/36. DEX checked for new layout/analyzer identifiers. File size matches the earlier APK by coincidence; hash differs. Runtime NOT EXECUTED. See VALIDATION.md.

## N. Git status

History retained in `.tilebloom-git`; parent `fc75a29e04ae2602cff9348da4bee88814211154`. No reinitialization, rewrite, push or publication. Commit message: `feat: redesign TileBloom Level 1 for strategic gameplay`. Generated APKs/logs remain untracked build artifacts. Final commit identity is reported to the user.

## O. Acceptance criteria

PASS below means source/automated evidence unless explicitly stated; it does not imply Android execution.

| # | Criterion | Result |
|---|---|---|
| 1 | Existing engine preserved | PASS |
| 2 | Geometry visibly redesigned in definition/schematic | PASS |
| 3 | Not merely symbol reshuffle | PASS |
| 4 | Layout valid and readable | NOT VERIFIED — valid geometry passes; phone readability pending |
| 5 | Complete verified solution | PASS |
| 6 | Initial solvability | PASS |
| 7 | Deterministic generation | PASS |
| 8 | Meaningful branching measured | PASS — unlock differences, not enjoyment proof |
| 9 | Forced and equivalent choices analyzed | PASS |
| 10 | Difficulty not solely tile count | PASS |
| 11 | Coherent human introductory progression | NOT VERIFIED |
| 12 | Existing animations preserved | PASS — unchanged source and regression tests |
| 13 | Restart correctness | PASS — JVM; Android execution pending |
| 14 | Victory/deadlock correctness | PASS — production replay |
| 15 | Permanent saves compatible or safely migrate | PASS — JVM; Android persistence pending |
| 16 | Completed records preserved | PASS — JVM |
| 17 | Level selection functional | PASS — domain tests; Android execution pending |
| 18 | Existing JVM tests passing | PASS |
| 19 | New puzzle regressions | PASS |
| 20 | Instrumentation coverage preserved | PASS — compiled only |
| 21 | New debug APK generated/verified | PASS — diagnostic; runtime pending |
| 22 | Build/runtime accurately separated | PASS |
| 23 | Git history preserved | PASS |
| 24 | Actual human difficulty explicitly unverified | PASS |

## P. Known limitations

This iteration proves a distinct, solvable board with specific blocking decisions. It cannot prove that the recurring “too easy” complaint is resolved without a player trying it. The random policy performs better than before; risk was deliberately not maximized. Analysis follows alternatives at witness states, not all possible histories. Immediate unlock signatures do not measure long-range strategic equivalence. Android execution, full-screen readability and actual save migration remain unverified.

## Q. Recommendation for CODEX 11

Install this exact build with matching signature on an authorized device, verify process persistence and upgrade without clearing data, then watch a player solve Level 1 unaided and compare Level 2. Record which choices led to observation, blind matching or confusing traps. Tune only from that evidence; preserve this versioned fixture if another puzzle change becomes necessary.
