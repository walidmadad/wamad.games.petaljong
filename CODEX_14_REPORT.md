# CODEX 14 — TileBloom development report

## A. Executive summary

Added deterministic twenty-level difficulty profiles and ten original botanical tile
symbols. Generator v5 retains v4 restoration. Existing Mahjong rules, five introductory
puzzles, persistent forward-only progression and the three-credit wallet remain intact.
No advertising, publishing, pushing, history rewriting or data deletion.

## B–D. Difficulty architecture, milestones and profiles

`DifficultyPolicy` is pure Kotlin. Tier is `(level - 1) / 20`; positive Long levels are
required. First level is `tier * 20 + 1`, which cannot exceed the input. Last level adds
at most `min(19, Long.MAX_VALUE - first)`. Long.MAX_VALUE is supported without wrapping.
Thus 20/21 → tiers 0/1, 40/41 → 1/2, 120/121 → 5/6.

| Tier | Levels | Tile target | Active types | Blocking target | Unlock-choice target | Risk-state allowance | Nodes/candidate |
|---|---|---|---|---|---|---|---|
| 0 | 1–20 | 24–28 | 6 | 12 | 2 | 2 | 1500 |
| 1 | 21–40 | 28–32 | 8 | 14 | 3 | 2 | 1750 |
| 2 | 41–60 | 32–36 | 10 | 16 | 4 | 3 | 2000 |
| 3 | 61–80 | 36–40 | 12 | 18 | 5 | 3 | 2250 |
| 4 | 81–100 | 40–44 | 14 | 20 | 6 | 4 | 2500 |
| 5 | 101–120 | 40–48 | 16 | 22 | 7 | 4 | 2750 |
| 6+ | 121+ | 40–48 | 16 maximum | 24 | 8, rising to 14 | 5 | 3000 |

All profiles allow eight existing families and layers 0–1. Four candidates maximum.
Actual generation caps at 40 tiles; the original five puzzles keep historical counts.
Within a tier the second ten levels gain four tiles where the cap permits. Upper-slot
selection varies with identity; half-tile cover offsets rotate across advanced tiers.
Strategic scoring emphasis rotates each tier beyond the finite count/catalog caps.
Unlock-choice targets saturate to avoid impossible perpetual growth. This is an endless
tier policy with bounded complexity, not a claim of indefinitely increasing human difficulty.

## E–G. Catalog, original artwork and distribution

| Stable IDs | Symbols | Introduction |
|---|---|---|
| 0–5 | Leaf, Lotus, Fern, Flower, Berries, Sunflower | original |
| 6–7 | Mushroom, Butterfly | 21 |
| 8–9 | Bamboo, Acorn | 41 |
| 10–11 | Maple Leaf, Pinecone | 61 |
| 12–13 | Iris, Ginkgo Leaf | 81 |
| 14–15 | Cherry Blossom, Dragonfly | 101 |

Explicit IDs are append-only; original enum ordering is preserved. Original six drawing
branches remain intact. New Canvas paths, ovals, arcs and lines create caps/stems,
symmetrical wings, bamboo joints, acorn cap, pointed maple silhouette, cone scales,
upright iris petals, ginkgo fan, notched blossom petals and narrow dragonfly wings/body.
Muted colors complement ivory faces. Accessibility symbol names were added.
Silhouette recognition and small-screen visual quality still need human review.

Each active symbol receives a witness pair before assignments are deterministically
shuffled. Counts are even and at least two; active types never exceed pair count.
All unlocked types currently fit the sixteen-type cap, including both milestone types.
Catalog rotation is deterministic and supports selecting a bounded subset when future
catalogs grow. No artwork beyond the current sixteen is claimed.

## H–J. Generator, analysis and solvability

The pipeline selects the profile, compact family, upper slots, bounded removal witness,
eligible symbols and pair assignments. Every candidate is replayed through `Game` by
`LevelValidator`, then scored through `PuzzleAnalyzer`. Selection considers geometric
blocking, initial pairs, forced moves, meaningful unlock differences, branching along
the witness, upper unlocks, safe alternatives, confirmed deadlocks and unknown analyses.
A quality target requires complete bounded analysis, limited risk and forced moves.
No candidate is accepted without a complete production-engine solution witness.

Four candidates each permit at most 64 construction attempts and 1500–3000 shared
analysis nodes. Construction exhaustion uses an ordered top-layer-first pair-removal
fallback through `Rules.available`, with no search. Analysis or quality exhaustion
selects the best verified candidate and reports `qualityTargetMet=false` and per-candidate
diagnostics. Quality relaxation never relaxes solvability. Forced fallback is tested.
Eight distinct consecutive families provide recent geometry novelty; regression tests
compare the previous four geometry fingerprints. There is no history-dependent seed.

Automated scores are difficulty proxies. They neither establish human-perceived
challenge nor guarantee that every alternative has been classified when a budget ends.
Detailed representative analysis and timings are in `docs/codex14/host-matrix.txt`.

## K–M. Determinism, compatibility, progression and Undo

New endless boards use explicit version 5. `LegacyEndlessGenerator` retains v4 seed,
layout and candidate policy. Historical `LevelGenerator` assignment uses exactly the
original six symbols; expanding the enum therefore cannot change historical boards.
Saved v4 moves are reconstructed and replayed on v4 boards, then the next level uses
v5. Introductory versions and schema 3 remain unchanged. Unknown or corrupt snapshots
use the existing safe current-frontier recovery, preserving the level and wallet.

Future artwork additions require appended unique IDs and introduction levels, a drawing
branch and accessibility label, regression tests, and an explicit generation version
change if boards change. Earlier reconstructors must remain available.

No progression or wallet algorithms changed. Existing durable Next transition remains
asynchronous, guarded, forward-only and +1. Milestone tests exercise 20,40,60,80,100,120,
double commits, restart, save/reload and unchanged credits. Brief Toast notifications
announce new tiers; only actual catalog introductions mention new tiles. No credits
are granted and no extra progression step is introduced.

## N–Q. Performance, tests, instrumentation and APK

Final verification evidence follows below. Host timings are
not Android device timings. The diagnostic pipeline compiles production Android and
instrumentation Kotlin, builds dex/resources, aligns and signs a fresh debug APK.
Standard Gradle was attempted but its wrapper cache lock is read-only. Android Lint
was consequently not executed. ADB failed to bind its listener (`Operation not permitted`).
Android installation, instrumentation execution, runtime performance, screenshot review,
font-scale/inset usability and human playtesting are NOT EXECUTED / NOT VERIFIED.

Instrumentation coverage adds isolated 20→21 and 40→41 fixtures, geometry-derived tile
taps, double Next, all new-level witness pairs, Canvas drawing, restart, wallet checks,
and Level 21 recreation/relaunch. Existing compact-board/animation/progression coverage
is retained. Notification text is asserted through the policy; actual Toast appearance
needs device verification.

### Final verification evidence

- Java 17 diagnostic pipeline: PASS; 174 JVM tests in 28.213 seconds, including all
  168 retained regressions and six new test methods with multi-level matrices.
- Host-workflow tests: PASS, 10 tests.
- Instrumentation compilation: PASS (existing deprecated Android test-base warning).
  Instrumentation execution: NOT EXECUTED.
- Representative required-level generation: 16–180 ms; timings include cold/cached
  effects and are single observations. Additional analysis is separately timed.
- Matrix covers 31 milestone/nearby/large identities; all selected boards met their
  configured bounded quality targets in this run. This is sampled evidence only.
  Confirmed risky states and branching vary; no strict adjacent difficulty ordering.
- Fresh APK: `app/build/outputs/apk/debug/app-debug.apk`; package `com.tilebloom.game`.
- Timestamp: 2026-10-28 18:10:03 UTC; size: 766355 bytes.
- SHA-256: `e3ca7d2f33b0f2e91c04803b119ab29037bbc3382be1a8e56848ca6b30b18a3e`.
- Alignment: PASS (`zipalign -c 4`); signature: PASS (v2 and v3).
- APK dex contains the Canvas renderer, ten new symbols, difficulty policy and legacy
  reconstructor. Compiled renderer has sixteen-case dispatch. Evidence:
  `docs/codex14/apk-verification.txt`.
- Full diff whitespace check: PASS. Legacy v4 source compared to the parent revision:
  unchanged except object name. No Mahjong rule or wallet behavior changes.

## R. Git

History begins this iteration at `73e42fa`. Changes are committed with the requested
feature message; see `git --git-dir=.tilebloom-git log -1`. No push or publication.

## S. Acceptance criteria

| # | Criterion | Result |
|---|---|---|
| 1 | Existing Mahjong rules preserved | PASS |
| 2 | Endless progression preserved | PASS |
| 3 | Forward-only progression preserved | PASS |
| 4 | Tier every twenty levels | PASS |
| 5 | Deterministic tier calculations | PASS |
| 6 | Beginner profiles and original introductions preserved | PASS |
| 7 | 21–40 profile applied | PASS |
| 8 | Higher strategic profiles applied | PASS (human difficulty NOT VERIFIED) |
| 9 | Original six available | PASS |
| 10 | Ten new botanical symbols implemented | PASS |
| 11 | Level 21 introduces two | PASS |
| 12 | Level 41 introduces two | PASS |
| 13 | Level 61 introduces two | PASS |
| 14 | Level 81 introduces two | PASS |
| 15 | Level 101 introduces two | PASS |
| 16 | Stable symbol IDs | PASS |
| 17 | Visually distinguishable at device sizes | NOT VERIFIED (original silhouettes implemented) |
| 18 | Matching all symbols | PASS |
| 19 | Valid even distribution | PASS |
| 20 | Verified complete witness for accepted boards | PASS |
| 21 | Deterministic generation | PASS |
| 22 | Bounded generation | PASS |
| 23 | Android readability | NOT VERIFIED (host geometry checks pass) |
| 24 | Save restoration/safe recovery | PASS (host; device NOT VERIFIED) |
| 25 | Undo unchanged | PASS |
| 26 | No tier level skipping | PASS |
| 27 | No completed replay | PASS |
| 28 | Existing JVM regressions | PASS |
| 29 | Tier tests | PASS |
| 30 | Instrumentation coverage updated | PASS; execution NOT EXECUTED |
| 31 | Fresh APK verified | PASS |
| 32 | History preserved | PASS |
| 33 | Nothing published | PASS |

## T–U. Limitations and CODEX 15

Actual playtesting is needed to tune challenge, recognize similar floral silhouettes,
and confirm animation and accessibility on devices. Finite compact geometry and sixteen
symbols intentionally cap complexity. Some boards can miss quality targets and still
be safely solvable; consult diagnostics. Original Level 4 remains 30 tiles as a frozen
introductory exception. New glyph paths allocate small objects during drawing; device
profiling should determine whether caching is useful. There are no new illustrations
after 101, and tier changes after 120 do not advertise new tiles.

For CODEX 15, execute the milestone instrumentation and manual checklist on compact and
medium phones, gather genuine difficulty/fairness feedback, profile cold generation and
rendering, then tune a new versioned policy using those observations. Add future curated
artwork only with stable-ID and historical reconstruction tests.
