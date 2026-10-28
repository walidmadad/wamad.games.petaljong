# CODEX 29 — Advanced Mahjong difficulty and multilayer boards

## A. Executive summary

Implemented generator v7, the requested four-stage policy, two genuinely distinct geometry families, three/four-layer terraces, conservative covered-symbol privacy and stronger ceramic shading/depth. Existing botanical IDs, four-slot Game controller, Hint/Undo wallets, progression codec, rewarded ads, UMP and timed victory interstitials are preserved. No publication, push, uninstall or player-data clearing.

Delivery is incomplete: this environment blocks Gradle and ADB before task/device execution. No fresh APK, executed instrumentation, examined Android screenshots or live advertising result is claimed. The existing APK predates this work.

## B. Previous weaknesses and baseline

Inspected production generator v6, historical v4/v5, DifficultyPolicy, PuzzleAnalyzer/TrayAnalyzer, witness validation, Game/Rules, BoardGeometry, MahjongBoardView, HintPolicy/HintPresentation, ProgressCodec/ProgressController, GameStore, ad integration, existing JVM/Android tests and `.tilebloom-git` history. Prior head: `8701d8b`. The conventional `.git` is a restricted placeholder; the existing real history is `.tilebloom-git`.

The old policy advanced every 20 levels. V6 selected 20 base positions and placed more tiles on only layer 1, often at exactly the same coordinates. Four candidates varied pair assignments but not geometry. Late boards capped at 40 tiles and two layers. Pair-only synthetic scores could miss individual tray danger; most sampled levels had zero proven risky pair-choice states.

Actual frozen-v6 baseline (JVM, analysis budget 2000; timing includes generation and analysis):

| Level | Tiles | Layers | Overlap relationships | Blocked | Available | Consequence states | Pair risk states | ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 24 | 2 | 23 | 18 | 6 | 8 | 1 | 362 |
| 7 | 24 | 2 | 4 | 13 | 11 | 9 | 0 | 510 |
| 8 | 24 | 2 | 4 | 12 | 12 | 10 | 0 | 385 |
| 15 | 28 | 2 | 8 | 17 | 11 | 12 | 0 | 348 |
| 16 | 28 | 2 | 8 | 16 | 12 | 12 | 0 | 390 |
| 30 | 28 | 2 | 8 | 17 | 11 | 11 | 2 | 324 |
| 31 | 32 | 2 | 12 | 22 | 10 | 12 | 0 | 358 |
| 50 | 32 | 2 | 21 | 21 | 11 | 13 | 0 | 207 |
| 100 | 40 | 2 | 36 | 32 | 8 | 18 | 0 | 161 |
| 500 | 40 | 2 | 20 | 32 | 8 | 17 | 0 | 236 |
| 1000 | 40 | 2 | 35 | 30 | 10 | 18 | 0 | 261 |

Reproduce with Java 17: `python3 tools/benchmark29.py historical`. Timing is host-specific, includes cold/JIT effects and must not be interpreted as Android latency.

## C. New progression and quality targets

Pure Kotlin `DifficultyPolicy.difficulty(Long)` implements exact inclusive boundaries, including overflow-safe Expert classification at Long.MAX_VALUE. The first five introductory puzzle identities remain unchanged.

| Difficulty | Levels | Tile range | Actual layers | Minimum blocked target | Minimum consequence states | Pair analysis nodes/candidate |
|---|---|---|---|---:|---:|---:|
| NORMAL | 1–7 | 24–28 | 2 | 12 | 2 | 2000 |
| HARD | 8–15 | 32–36 | 3 | 22 | 4 | 2500 |
| VERY_HARD | 16–30 | 40–44 | 4 | 30 | 6 | 3000 |
| EXPERT | 31+ | 44–48 | 4 | 34 | 8 | 3500 |

Targets are calibrated above the measured early flat-board baseline. All stages require complete candidate pair analysis, at most half forced witness states, and at least as many overlap relationships as their blocked target. Hard+ additionally requires at least six three-held-tray observations across 12 probes. Expert matrix regression requires at least 70 overlap relationships, 34 blocked positions and eight consequential states. These are measurable engineering proxies, not a claim of strict human difficulty ordering.

## D. Type A: classic multilayer

Even generated levels use a six-column, five-row outline with a hollow middle, two separated side structures and bridging end rows. Upper terraces occupy separate groups; staggered intermediate and cap layers create multiple opening directions. Counts: Normal 28, Hard 36, Very Hard 42, Expert 48. This is the wider family without copying reference artwork.

## E. Type B: compact stacked

Odd generated levels use a four-column, five-row base, then narrower three-column terraces and two-column upper structures/caps. Counts: Normal 24, Hard 36, Very Hard 42, Expert 44. Deeper center positions unlock progressively. Both families use deterministic reflection and horizontal/vertical offsets. Geometry fingerprints ignore IDs; the recent-window regression checks actual geometry rather than names. Candidate selection considers six deterministic geometry/assignment candidates. Expert footprints stay bounded indefinitely.

## F. Geometric blocking

`Rules.available` remains authoritative for generation, witnesses, controller, Hint and Undo. No generator-specific blocking implementation exists. Each upper position intersects a supporting lower terrace; no same-layer intersection is legal. `LevelValidator` now accepts layers 0–3 and still rejects invalid geometry/counts and replays the entire witness through `Game.select`. GeometryMetrics counts directed upper/lower overlaps, covered positions, blocked/free counts and density. PuzzleAnalyzer records unlocks of intermediate terraces as well as the base.

## G. Blocked rendering

Available faces remain warm ivory RGB(250,244,224). Blocked faces are opaque gray-green RGB(153,163,139), with a muted rim and existing engraved bars. Exposed blocked artwork gets an opaque-color tint overlay; selection styling requires availability. The tile body stays opaque. Low-brightness recognition still requires device inspection.

## H. Depth and hidden identity

Shared BoardGeometry increases per-layer lift from .09 to .12 tile units, lower edge from .045 to .065, body thickness from .085 to .115 and shadow offset from .14 to .18. Existing two directional shadow bands remain. The vertical origin was shifted upward within the existing viewport margin to contain both deeper shadow bands while preserving introductory touch scale. Face/edge targets still exclude shadows; draw/hit order remains layer/y/x/id.

TileVisibility suppresses all artwork on any tile intersected by a higher live tile. This deliberately also hides artwork on partially covered positions: visible ceramic edges communicate presence without disclosing identity through rounded gaps. Removing coverage restores the symbol. Canvas draws lower layers first and upper faces opaquely; Hint remains a final foreground pass and only accepts legally available tiles.

## I. Strategic analysis and bounded fallback

Six candidates maximum, 64 geometry peeling attempts maximum each, bounded pair solver budgets above, and 12 individual-action tray probes of at most tile-count selections per candidate. Probes run the actual four-slot Game: sometimes prefer a currently held matching symbol, otherwise choose a deterministic random legal selection. Record losses, wins and three-held states. Probe losses measure susceptibility to poor choices, not unavoidable traps or proof of unsolvability. Candidate scoring rewards consequence/unlock diversity and limited tray pressure; pair-risk excess, forced moves, unknown analysis and excessive immediate pairs are penalized. CandidateQuality records overlap, tray pressure and probe losses.

If no candidate meets all soft targets, retain the best *verified board of the same tier geometry* and set qualityTargetMet=false. There is no substitution with a smaller/easier-tier layout. This is a documented quality fallback, not a claim of satisfied Expert targets. A forced fallback uses bounded highest-layer peeling, then the same botanical assignment and controller verification; forced-fallback quality remains false. The quality flag belongs to the selected candidate rather than any discarded candidate. No strict guarantee that every possible level meets every soft strategic target is made.

## J. Solvability

Each geometry peel constructs pairs of simultaneously available positions using production Rules. Pair symbols are assigned in complete even counts, using the existing catalog and milestone requirements. LevelValidator replays each pair as individual selections, requires an empty tray after each match, all tiles cleared and WON. Generation never accepts a witness with an illegal selection or tray overflow. Every returned definition is validated again. This guarantees one safe initial sequence; arbitrary player orders can lose. The witness is deliberately a conservative immediate-pair winning path; probes assess individual tray choices separately.

## K. Saves

Generator v7 uses its own version/seed. Frozen HistoricalDifficultyPolicy and VersionSixGenerator preserve exact previous v6 definitions; VersionFiveGenerator now also uses the frozen policy. Existing v4 restoration remains. Versions 1–3 introductory definitions are preserved. Session restoration explicitly dispatches saved versions 4, 5 and 6 before current generation, validates identity and replays accepted individual actions. Schema 5 is unchanged. Level/frontier, tray, histories, credits and ad preferences retain existing storage paths/semantics. Regression tests restore partially filled historical trays across the new boundaries. Corrupt-save recovery remains the existing validated current-frontier restart strategy.

## L. Hint, Undo, progression and ads

Hint still runs bounded individual-action search, prefers tray matches, filters availability and never decorates a covered tile. Undo replays the exact accepted history and reconstructs coverage. Three initial Hint and Undo credits, persistent spending/rewards, +1 forward-only advancement, duplicate commit protection and current-only restart are unchanged. Existing tests cover all botanical additions and higher-level Hint selection. Ads/UMP code, test IDs, frequency, privacy and Internet policy are untouched. JVM ad regressions are relevant evidence; live AdMob behavior is NOT VERIFIED.

## M. Performance and viewport

Generation and Hint continue on existing MainActivity worker threads, including cold progress loading and next-level preparation. No new rendering library or continuous idle redraw loop. Geometry is at most 48 positions/four layers; rendering follows the existing per-draw allocation pattern rather than introducing caches with stale-state risks. Further allocation profiling requires Android. Matrix tests fit 320/360/400/800 dp-equivalent widths and existing narrow/landscape viewports, check shadows in bounds and replay every witness through shared hit geometry. Minimum matrix tile unit is 38 dp; wide boards on 320 dp devices may remain below a 48 dp face target. Layering was preferred over increasing the six-column limit.

Final measured v7 results, test counts and completion evidence follow below after verification. JVM benchmark duration includes analysis; matrix tests also include deterministic regeneration and touch replay and are not generation-only measurements. Android timings are NOT VERIFIED.

## N. JVM results

See final verification addendum below. Existing semantic gameplay, credits, migration and advertising regressions remain. Obsolete 20-level/eight-family/40-tile expectations were updated for the requested specification. The botanical Hint regression now accepts any legally available matching symbol rather than requiring one particular duplicate partner ID.

## O. Android instrumentation

Added AdvancedBoardVisualTest: real Android Canvas captures for levels 1/8/16/31/32 at 320/360/400/600 dp, before/after upper removals, plus actual view-local touch insertion, Undo and tray checks. Uses a standalone board and isolated `files/codex29-visual`, without modifying player progress. Run with standard Android instrumentation on an unrestricted host; pull that directory with `run-as com.tilebloom.game`. Compiling test sources does not mean executing them.

## P. Screenshot verification

NOT VERIFIED. ADB fails before connecting: `could not install *smartsocket* listener: Operation not permitted`. No updated Android screenshot was captured or examined. No reference screenshots are present as accessible workspace files in this session. Do not infer visual acceptance from geometry statistics or treat the capture harness as screenshot evidence.

## Q. APK

FAIL: no fresh APK could be built here. Requested wrapper unit/assemble/lint tasks fail on the read-only default Gradle lock. Installed Gradle 8.13 with Java 17 and writable /tmp user home fails before tasks: `Could not determine a usable wildcard IP for this machine`. Logs are in docs/codex29. No unrestricted terminal tool is available; no approval or sandbox bypass was attempted.

The existing `app/build/outputs/apk/debug/app-debug.apk` is stale and is not CODEX 29. New creation time, size, SHA-256, alignment, package inspection, packaged code and signature are unavailable. Local signing configuration remains untouched. Keytool rechecked the existing configured keystore certificate (docs/codex29/signing.txt); it matches `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. An actual newly built signature and in-place upgrade must still be verified. No keys or credentials are committed.

## R. Git

Existing history is preserved through `.tilebloom-git`. Unrelated pre-existing CODEX_18_REPORT.md and docs/codex26 are excluded. Implementation is committed after verification; commit/status recorded below. No push or publishing.

## S. Acceptance criteria

PASS here means executed JVM behavior/source review; visual/device requirements require independent evidence.

| # | Criterion | Result |
|---|---|---|
| 1–5 | Exact Normal/Hard/Very Hard/Expert ranges and deterministic policy | PASS |
| 6–9 | Both distinct families and real upper blocking | PASS |
| 10–11 | Blocked faces visibly darker; bright available tiles | NOT VERIFIED visually; rendering policy implemented |
| 12 | Hidden identities concealed | PASS policy/geometry tests; Android pixels NOT VERIFIED |
| 13 | Improved thickness/shadows | NOT VERIFIED visually; code implemented |
| 14 | Readability | NOT VERIFIED on Android |
| 15 | Touch accuracy | PASS JVM shared-geometry replay; Android NOT VERIFIED |
| 16–17 | Verified winning witnesses/four-slot tray | PASS |
| 18–21 | Hint, Undo, forward-only progression, saved progress | PASS JVM |
| 22 | Functional AdMob | NOT VERIFIED live; code unchanged/JVM regressions retained |
| 23–24 | Existing and new JVM regressions | PASS — 240/240 |
| 25 | Android visual verification | NOT VERIFIED (ADB blocked) |
| 26 | Fresh verified APK | FAIL (Gradle blocked) |
| 27 | Signing compatibility | NOT VERIFIED for a fresh APK; local configuration preserved |
| 28–29 | Git history preserved/nothing published | PASS |

## T. Limitations

Human difficulty has not been playtested. Pair solver deadlocks can be recoverable through tray moves; never equate pair-risk counts with actual four-slot forced losses. Tray probes are bounded policy samples, not an exhaustive probability model. Soft quality fallback is explicitly reported rather than claiming perfect ordering. Expert symbol diversity grows only to the existing bounded catalog/profile limit; physical board size stays capped. Geometry variation is a bounded portfolio and can recur after the recent window. Partially covered artwork is intentionally hidden more conservatively than required. Android render readability, performance, pixels, touch, ads, build/Lint and update installation remain pending.

## U. CODEX 30 recommendation

On an unrestricted Java 17 Android host, run standard Gradle unit/assemble/lint and connected tests; inspect the new capture matrix and actual full-screen screenshots before approving visuals. Verify both families at 320 dp, blocked/available contrast, coverage transitions and Hint overdraw. Profile generation/rendering on a real phone. Build/sign/align/hash the APK with the existing local key and update in place without uninstalling/clearing data. Then conduct human difficulty playtests with tray-choice telemetry and tune bounded candidate targets rather than increasing tile counts.

## Final measured v7 matrix

Java 17 cached-compiler benchmark, same representative levels; separate generation/analysis times, milliseconds. Run concurrently with validation on this host; timings are observations, not a latency guarantee. Pair analysis here uses 2000 nodes. Candidate acceptance uses the larger stage-specific budgets, so a selected target-met Hard/Very Hard board can have unknown alternatives in this lower-budget independent measurement.

| Level | Tiles | Layers | Overlaps | Blocked | Available | Consequences | Pair risk | Unknown alternatives | Generation ms | Analysis ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 24 | 2 | 23 | 18 | 6 | 8 | 1 | 0 | 28 | 34 |
| 7 | 24 | 2 | 16 | 16 | 8 | 10 | 0 | 0 | 224 | 12 |
| 8 | 36 | 3 | 44 | 28 | 8 | 16 | 2 | 45 | 258 | 56 |
| 15 | 36 | 3 | 80 | 32 | 4 | 15 | 1 | 0 | 149 | 24 |
| 16 | 42 | 4 | 75 | 34 | 8 | 18 | 1 | 46 | 345 | 59 |
| 30 | 42 | 4 | 75 | 34 | 8 | 18 | 2 | 40 | 408 | 28 |
| 31 | 44 | 4 | 146 | 38 | 6 | 19 | 4 | 0 | 154 | 17 |
| 50 | 48 | 4 | 120 | 42 | 6 | 22 | 2 | 0 | 196 | 22 |
| 100 | 48 | 4 | 114 | 42 | 6 | 19 | 3 | 0 | 178 | 19 |
| 500 | 48 | 4 | 114 | 42 | 6 | 22 | 2 | 0 | 150 | 19 |
| 1000 | 48 | 4 | 114 | 42 | 6 | 21 | 0 | 0 | 179 | 16 |

Expert generation in this run: 150–196 ms; four layers, 114–146 overlap relationships, 38–42 blocked tiles, six initial selections and 19–22 consequence states. Very Hard generation: 345–408 ms. These synthetic observations support stronger geometry and choice structure; they do not prove subjective ordering. Individual-action probes and all quality-target flags are recorded in final.log. Every representative selected board meets its production profile target in the executed matrix. No probe win is needed to prove solvability because the full certified witness already proves it.

Diagnostic Android main/resource/Android-test compilation: PASS (diagnostic-pass.log); final shared geometry is additionally recompiled in final.log. This includes the new visual capture harness. Tests were compiled, not run on Android. Existing signing certificate: PASS match (signing.txt). Gradle unit/build/lint: FAIL before tasks. Android runtime, captures, live ads, performance and fresh APK: NOT VERIFIED/blocked as described above.

## Final verification and repository result

**JVM: PASS, 240/240 tests, 66.007 seconds** on final sources/shared geometry. Full cached-compiler build previously passed 240/240 in 66.136 seconds; the last penumbra-origin adjustment was recompiled into both JVM and Android diagnostic classes and the entire suite replayed successfully. Seven new AdvancedMahjongTest scenarios supplement the existing 233 tests. These are actual JUnit executions, not a standard Gradle test task.

Evidence committed: final.log, baseline.log, benchmark-final.log, diagnostic-pass.log, default Gradle unit/assemble/lint failures, writable Gradle failure, ADB failure and signing.txt. `git diff --check` passed. No new APK exists for this iteration. Acceptance 23–24 is PASS; 25/27 remain NOT VERIFIED and 26 FAIL for the stated host restrictions.

Implementation is committed as `feat: introduce advanced Mahjong difficulty and multilayer boards`. The final response records its commit ID. Only pre-existing unrelated CODEX_18_REPORT.md and docs/codex26 remain untracked; no user changes were included or removed. Nothing was pushed or published.
