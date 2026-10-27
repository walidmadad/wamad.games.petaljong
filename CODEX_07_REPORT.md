# CODEX 07 development report

## A. Executive summary

Implemented five introductory gardens, deterministic constructive generation, complete production-engine witness validation, explicit progression and a centered victory card. Preserved Game.kt, LevelOne, six botanical drawings, the 150 ms match effect implementation, all original tests and Git history. Nothing was published or pushed. Android runtime acceptance remains outstanding.

## B. Level generation architecture

`Levels.kt` adds TileSlot, BoardLayout, LevelDefinition, LevelGenerator, LevelValidator, the five-entry Levels catalog and GardenSession. Definitions include ID, seed, version, layout, assigned tiles, solution witness and attempt count. Generation has no Android dependencies and uses existing Rules; Game remains the authority for matching, victory and deadlock.

## C. Supported levels

| Level | Layout | Tiles | Seed | Version |
|---|---|---:|---:|---:|
| 1 | Original rectangle | 24 | 0 | 0, fixed |
| 2 | Stepped terrace | 24 | 202 | 1 |
| 3 | Asymmetric woodland | 28 | 303 | 1 |
| 4 | Layered courtyard | 30 | 404 | 1 |
| 5 | Woodland, new arrangement | 28 | 505 | 1 |

All four geometries use two layers. Level 1's tile data and both existing witnesses remain unchanged.

## D. Solvability guarantee

The generator removes geometrically free pairs using production Rules, then assigns matching botanical symbols along that ordering. A balanced shuffled symbol bag keeps counts even and avoids a dominant symbol. The validator checks geometry, slot identity and distribution, then replays every move using Game.match. Acceptance requires every move to succeed and the final state to be WON with no remaining tiles. Invalid candidates are discarded. This guarantees one solution from the initial state; other choices may deadlock.

## E. Determinism

Version 1 uses explicitly seeded java.util.Random, stable ordered slots and a deterministic shuffle. The same catalog ID, seed and version reproduce the same definition and witness. Existing seed outputs require the layouts, ordering and versioned algorithm to remain stable. Any future change affecting those outputs must introduce a new version. Unsupported versions are rejected. Restart never generates a new seed.

## F. Difficulty progression

Terrace introduces a wider silhouette; woodland distributes cover asymmetrically; courtyard expands upper-layer cover. The last garden revisits woodland with different assignments and fewer tiles than Level 4. Initial free tiles / matching choices are 6/3, 11/6, 10/5, 9/5 and 10/6. These are proxies, not objective human difficulty scores. A monotonic challenge curve has not been established.

## G. Level navigation

Next is enabled only after winning and before Level 5. Advancing constructs the next definition before replacing the session, binds a fresh game and clears pending gestures, blocked feedback and match ghosts. Replay and Restart restore the same initial board. Deadlock keeps the current identity and never enables advancement. Level 5 hides Next and displays a terminal journey message. Generation exhaustion leaves the completed level in place and displays a controlled error; no unrelated fallback is mislabeled.

## H. Victory presentation

A centered rounded forest-green card uses the existing original leaf drawing, ivory serif title, encouraging copy, Next Level and Replay Level. It fades in over 200 ms when animations are enabled. A scroll container keeps actions reachable in a short play area. The existing restart control remains available. Actual appearance and compact-screen readability need Android review.

## I. State restoration

Activity bundles now save catalog ID, seed, version, accepted move history and selection. Restoration checks identity and replays moves atomically through Game; status is derived. Corruption falls back safely to fresh Level 1. Legacy identity-free bundles are treated as Level 1. The session model is tested across all five levels, but real multi-level Android recreation is not yet executed. Progression is session-based, with no durable completion store.

## J. Automated test results

89 production JVM tests pass: all 67 original tests plus 22 new tests. The matrix covers 40 seeds for each of three generated geometries (120 boards), with full production-engine replay for every accepted candidate. Additional tests cover impossible layouts, corrupt candidates, restart, next/final/deadlock behavior, restoration and shared drawing/hit geometry across three viewport sizes. The existing host verification script regression suite passes all 10 tests. Final execution timing is recorded in VALIDATION.md.

## K. Android runtime testing

All nine original instrumentation methods are preserved. A tenth uses geometry-derived coordinate taps to complete all five gardens, advance, restart each new board and check final navigation and cleared effects. All ten methods compile; zero were executed. ADB cannot start its server because socket binding is denied. Installation, launch, visual review and connected instrumentation are NOT EXECUTED. Previous user screenshots validate the prior iteration only.

## L. Performance and limits

Generation permits 2–32 slots, at most 64 attempts and at most 16 pair removals per attempt. There is no recursion, unbounded solver or per-frame generation. The 120-board JVM matrix needed at most two attempts. Host timing is recorded in VALIDATION.md; no device performance claim is made. There is no stored generated fallback; exhaustion produces an error, never an unverified playable board.

## M. APK output

`app/build/outputs/apk/debug/app-debug.apk` is generated using the existing `tools/verify_offline.py` diagnostic pipeline: cached Kotlin compiler, aapt2, javac, D8, zipalign and apksigner. Exact final size and signing results are recorded in VALIDATION.md. This is a diagnostic debug APK, not a successful standard Gradle build.

The standard Gradle invocation failed before task execution because its wrapper lock path is read-only. Unit-test, assemble and lint Gradle tasks are NOT EXECUTED. Lint is not supplied by the fallback. Initial overlapping fallback output and a subsequent compact-UI compilation failure were discarded; only the final successful serial run is artifact evidence.

## N. Git status

History from 07ca78c0f465865715adeaa3812154441ac1c11e is retained in .tilebloom-git. The implementation is committed with `feat: add solvable Mahjong levels and garden progression`. The final response supplies the resulting commit ID and clean-status check. No reinitialization, history rewrite, push or publication occurred.

## O. Acceptance criteria

Runtime-dependent criteria remain NOT VERIFIED even where corresponding JVM behavior passes.

| # | Criterion | Result | Evidence / limit |
|---:|---|---|---|
| 1 | Existing engine preserved | PASS | Game.kt unchanged |
| 2 | Level 1 unchanged | PASS | Tile data, solution and deadlock regressions |
| 3 | Original JVM tests preserved | PASS | All 67 retained and passing |
| 4 | Five playable introductory levels | NOT VERIFIED | Five engine-solvable definitions; Android play unexecuted |
| 5 | Three distinct layout families | PASS | Four geometries including original |
| 6 | Seed reproducibility | PASS | Seed/version tests |
| 7 | Complete introductory witnesses | PASS | All five production-engine replays |
| 8 | Invalid candidates rejected | PASS | Corrupt candidate tests |
| 9 | Bounded generation | PASS | Fixed attempt/size limits, impossible-layout test |
| 10 | Honest difficulty progression | PASS | Spatial design progression; human difficulty unmeasured |
| 11 | Exact restart | NOT VERIFIED | JVM passes; new Android flow unexecuted |
| 12 | Correct deadlock handling | NOT VERIFIED | JVM passes; updated Android flow unexecuted |
| 13 | Improved victory panel | NOT VERIFIED | Implemented and compiled; no visual runtime review |
| 14 | Correct Next Level | NOT VERIFIED | Session tests pass; UI tests unexecuted |
| 15 | Final-level behavior | NOT VERIFIED | Session tests pass; UI tests unexecuted |
| 16 | Multi-level restoration | NOT VERIFIED | All five session restores pass; Android unexecuted |
| 17 | Multi-layout geometry | PASS | Bounds and shared hit-test tests at three viewport sizes |
| 18 | Correct touch selection | NOT VERIFIED | Geometry tests pass; Android input unexecuted |
| 19 | Safe match animations | NOT VERIFIED | Original JVM effect tests pass; Android transitions unexecuted |
| 20 | JVM regressions pass | PASS | 89/89 |
| 21 | Valid updated APK | PASS | Final diagnostic build, alignment and signature verification |
| 22 | Accurate standard Gradle reporting | PASS | Wrapper prerequisite failure explicitly distinguished |
| 23 | Accurate instrumentation reporting | PASS | Compiled only, NOT EXECUTED |
| 24 | Git history preserved | PASS | Original HEAD remains an ancestor |

## P. Known limitations

Android runtime checks and standard Gradle/Lint are outstanding. Human puzzle quality, monotonic difficulty, minimum comfortable phone touch size and actual device generation timing remain unverified. The generator supports a small curated geometry catalog and bounded retries, not unlimited content. It has no exhaustive strategy analyzer, undo, shuffle or durable progression saving. Canvas tile-level accessibility remains inherited; a full accessible tile-navigation interface is not implemented.

## Q. Recommendation for CODEX 08

First run the new APK and coordinate tests on Android, review compact-screen targets and tune the victory card. Then use bounded branch/deadlock analysis and human playtests to curate fair seeds and a smoother strategic progression. Prefer deeper puzzle quality over increasing level counts.

### Final CODEX 07 artifact

Final serial `python3 tools/verify_offline.py` completed with exit code 0. All 89 JVM tests passed (0 failures; 0.394 s JUnit execution), all 10 instrumentation methods compiled (0 executed), and the 120-board seed matrix took 89 ms on this host with maximum two attempts.

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Size: **721,299 bytes**
- SHA-256: `87672898c9e7ca4a705755ccc2701dbd9e7a375791735709e94cd5e8e14c0964`
- Method: diagnostic offline pipeline, **not standard Gradle**.
- Alignment: PASS. Debug signing: PASS, APK signature schemes v2 and v3 verified, one signer. v1/v3.1/v4 and SourceStamp are absent; this does not establish publisher provenance.
- Android installation/launch/instrumentation/visual review: **NOT EXECUTED**.
- Full A–Q report and all 24 acceptance criteria: `CODEX_07_REPORT.md`.
- Local diagnostic evidence: `app/build/codex07/` (ignored build output).
