# CODEX 35 — mystery tiles and evolving Expert difficulty

## A. Executive summary

Adds engine-owned deterministic mystery tiles, original jade Canvas artwork, deliberate two-tap interaction, ordered reveal persistence, three new stable botanical variants, explicit family co-occurrence, and bounded evolving Expert targets. Existing four-slot pair gameplay, themes, progression and advertising code are retained. Nothing was published, pushed, uninstalled or cleared.

A fresh APK and executed Android visual verification are **blocked by this environment**. The existing APK is not this implementation. See O and Q; source compilation and JVM results are recorded separately from Gradle/device verification.

## B–C. Gameplay architecture and interaction

`Tile` retains stable ID/symbol/geometry and adds `mystery`, `revealed`, and derived `concealed`. `Game.reveal` validates playing status, identity, and the same `Rules.available` used by collection. It changes only revelation and the ordered action log. It leaves board membership, blocking, tray, accepted selections and pairs unchanged. Repeating a reveal is rejected.

`Game.interact` chooses exactly one transition. `Game.select` is explicitly collection and rejects concealed tiles. A later collection revalidates geometry, removes the tile, transfers it to the unchanged four-slot tray, and matches exact Symbol identity before defeat evaluation. `Game.match` rejects concealed members before any partial pair selection.

## D–E. Rendering and animation

The original jade back uses a green gradient, ceramic side depth and existing shadows, inset light rim, botanical gold sprig medallion, and an available corner facet. It never calls the underlying symbol renderer while concealed. Ordinary blocked tiles retain their existing ceramic faces; blocked mystery backs use the same blocking overlay and engraved corner bars.

A 260 ms Canvas horizontal flip shows the back for its first half and the assigned face for its second half. No animation callback controls game state. System animator disabling reveals immediately. A separate 420 ms boundary checks the **start** of a subsequent touch, preventing a rapid double tap from collecting even with motion disabled. Existing single-pointer/drag/uppermost-hit rules remain. Load, navigation feedback cleanup, size changes and detachment discard obsolete visual timing. Actual Android visual approval remains outstanding.

## F. Similar-symbol mixing

The existing catalog had 20 symbols, but no explicit same-level family contract. Stable IDs 20–22 append Veined Leaf, Twin Leaves and Yellow Daisy. Existing Leaf is the classic form. These differ by broad silhouettes, branching, veins, petal count and center size; they share the existing board/tray renderer. Matching still uses full enum identity.

`SymbolFamilies` explicitly maps leaves, flowers, yellow flowers, greenery and purple flowers. Hard deliberately includes Leaf/Veined Leaf/Twin Leaves. Very Hard additionally includes Sunflower/Yellow Daisy. Expert additionally includes Lotus/Lavender. Every selected identity participates in even counts; witness scheduling opens every active identity. Verified forced fallbacks remap whole identities, preserving pairs while retaining the family contract. Existing artwork is reused for all other members.

## G–H. Difficulty and strategic analysis

Boundaries remain Normal 1–7, Hard 8–15, Very Hard 16–30, Expert 31+. Expert windows 31–50, 51–75, 76–100 and subsequent 25-level windows increase delayed-match, tray-pressure and dependency-depth targets and eventually require zero shallow ensemble wins. Targets saturate at practical limits rather than increasing tile count or shrinking artwork. Seeded geometry/assignments retain local variation and all eight layout families.

Candidate selection still requires the previous tier's actual shallow-player/lookahead profile, and now includes the Expert-window acceptance/score. Best verified candidates can miss quality targets; `qualityTargetMet=false` reports this honestly. Stronger targets do not prove monotonically harder human play.

Mystery counts are bounded: zero in Normal; 2–3 Hard; 4–6 Very Hard; 6–8 early Expert; 7–10 later Expert. Placement spreads over the legal geometric witness, including initially accessible positions and later blocking dependencies, across the existing families. Every candidate is analyzed with its actual mystery flags. Simulated players freely reveal all legally available mysteries before choosing a collection; lookahead cannot use a newly exposed concealed identity until a real observation/reveal. **Free revelation is not credited as intrinsic difficulty:** strategic immediate-match scoring first performs safe accessible reveals. Geometry, tray obligations, delayed partners and policy comparisons provide the strategic evidence.

Full-state solvability probes legally reveal then collect in each hypothetical continuation. Their memo intentionally collapses information-only differences; they are omniscient continuation analysis, not a human-information model. A reported immediate collection loss never means revelation alone loses.

## I. Solvability verification

`winningSelections` retains its collection-only contract. `winningActions` explicitly inserts REVEAL before each initially concealed tile's COLLECT. `LevelValidator` replays these through production Game methods, checks reveal leaves tray, board count and pair history unchanged, verifies all collection actions and exact matches, and requires final WON with every tile removed. No concealed collection bypass is permitted. All returned candidates and forced fallbacks pass this verification.

## J. Performance and benchmark

Final measurements and window summaries: `docs/codex35/benchmark-final.csv` and `benchmark-summary.md`. Expert cold generation measured **2,029.3 ms median / 4,558.5 ms maximum**; Hard median was **451.3 ms**. The additional regression matrix verified 77 boards across all eight layout families (`layout-matrix.csv`). Timings are cold generation in this host JVM, not Android-device latency. Candidate construction remains capped at 24; lookahead remains capped at 1,800 expanded states, with the existing 100-node early rejection path. The 30-second worker deadline, animated loading UI, background executor and twelve-entry immutable definition cache remain.

The benchmark covers 23 representative boards from level 1 through 1001, including all requested progression windows. It records actual geometry, blocking depth, immediate opportunities after free reveals, tray pressure, delayed matches, family/mystery counts, player outcomes, quality flags, witness validation and generation time. Some higher windows miss their acceptance targets; targets evolve, but this sample does not establish monotonically increasing achieved difficulty or human-perceived challenge.

## K. Save compatibility

Generator version advances from 9 to 10; schema from 5 to 6; mystery snapshots identify rules version 3. Schema 6 appends an ordered typed `R:id` / `C:id` log while preserving accepted collections, seed, generator version, frontier and wallet fields. No symbol is rerolled on reveal. Normal resume replays legal reveals/collections; restart restores the original backs. Existing schema 1–5 decoding remains.

Frozen `VersionNineGenerator` and `VersionNineDifficultyPolicy` preserve v9 attempts. v8 uses the same frozen pre-expansion policy; v4–v7 catalog filters explicitly exclude appended IDs. Historical v9 geometry, symbols, seed and witnesses match SHA-256 fingerprints against the **actual prior compiled implementation** at levels 8/12/31/75/101/1001 (`v9-old-fingerprints.txt`, `v9-new-fingerprints.txt`). Old attempts keep their generator until completion; future levels use v10. The extreme completed-frontier recovery path also replays full reveal actions. Advertising state remains in its existing separate storage.

## L. Undo and Hint

Only accepted collections enable/spend paid Undo. Undo replays the log immediately before the last collection, discarding later information actions while preserving revelations already made before that collection. Board, tray, pairs and reveal state restore together. Reveal alone cannot spend either wallet.

Hint recommends an accessible REVEAL first when present, with an explicit action kind and separate user message. Otherwise it searches collection continuations. Accepting a Hint validates on a private candidate and consumes the established one Hint credit without changing the live board. A reveal recommendation is safe information, not a false proof that an already damaged board is solvable. Hint and Undo reward/economy/advertising implementations are unchanged.

## M. Android visual verification

`MysteryTileVisualTest` prepares isolated pixel-concealment, touch, tray and stage screenshots without loading or editing player progression. `AdvancedBoardVisualTest` explicitly reveals mysteries before its existing collection/geometry checks. See `docs/codex35/visual-verification.md` for fixtures and reproducible device commands. No screenshots were fabricated and no device tests are claimed: ADB cannot start its listener here.

## N. Automated verification

Final suite and compilation evidence are in `docs/codex35/tests-final.txt`, `tests-focused.txt`, and `android-final.txt`. The focused engine/mystery/strategy run passed 21 tests. The first full run passed 269/273; four older Hint/navigation tests still collected concealed tiles without revealing them. Their transitions were corrected, and all 15 tests in those two affected classes then passed (`tests-corrected-regressions.txt`). **The final complete rerun passed all 273 tests in 153.536 seconds** (`tests-final.txt`). Existing generated-witness regressions now explicitly call production reveal before production collection; new mystery tests directly assert that collection before revelation fails. The utility performs no state bypass.

All three requested standard Gradle commands were attempted; each failed before task execution at the read-only wrapper lock. Direct installed Gradle 8.13 with Java 17 and a writable temporary home also failed before task execution because its lock service could not determine/open a wildcard IP. Independent cached Kotlin compilation and JUnit are not labeled Gradle success or Android Lint.

## O. APK

**No fresh CODEX 35 APK could be generated with the required Android Gradle build.** The old artifact's exact timestamp, size and SHA-256 are in `existing-apk.txt`. Its package is `com.tilebloom.game`; existing alignment/signature checks are recorded separately. Its certificate SHA-256 is `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`, the requested compatible identity. The signing configuration/keys were not changed or committed. New mystery/generator code is source-compiled, but cannot be asserted present in that old APK.

Run the standard Gradle tasks on a host permitting their lock/socket operations, then verify new timestamp, size, digest, package, alignment, certificate and dex contents before an update installation. Do not distribute the old artifact as this iteration.

## P. Git

Existing history is preserved in the established `.tilebloom-git` repository (the exposed `.git` directory is not a usable checkout). Only this iteration's source/tests/report/evidence are committed. Pre-existing unrelated untracked files are excluded. No repository reinitialization, rewrite, push or publication occurred. The final response records the commit ID.

## Q–R. Remaining limitations and next iteration

Complete Gradle/Lint/fresh APK and actual device screenshot/flip/recreation verification on an unrestricted host. Then perform human playtests of exact-family recognition at small tile sizes and Expert planning. Treat free mysteries as information discovery rather than a difficulty multiplier. Investigate profile misses and shallow-player holdouts; use observed human decisions to tune increasing targets and mystery quantities. Current bounded target saturation is deliberate, not an assertion of unlimited mathematical difficulty. Actual later-window outcomes vary and some verified boards remain below target.
