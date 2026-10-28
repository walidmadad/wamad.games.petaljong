# CODEX 30 — colorful gardens and structural variety

## A. Executive summary

Source implementation introduces generator v8 and five stable garden themes without replacing the project or changing the collection engine. Android build/device validation is blocked in this sandbox; this report does not certify an APK or visual result. Nothing was published, uninstalled or cleared.

## B. Confirmed repetition root causes

V7 `AdvancedLayouts` chose `classic` on even levels and `compact` on odd levels. Their base coordinates were constant within each branch. Candidate index altered reflection and tiny upper offsets, so six expensive candidate analyses could not select a different silhouette. `RECENT_WINDOW = 4` was a constant without an implemented lookback. The difficulty profile explicitly permitted only classic/compact. Symbols changed, but the meaningful base footprints did not.

The exception fallback used the layout it was passed; it did not independently rotate geometry. Thus it preserved the two-footprint limitation, rather than being proven to cause it. There is no evidence that candidate rejection secretly selected another family: none existed in the v7 path. Quality scoring could select different terraces, not a new base silhouette.

Blocked faces were hardcoded gray-green `(153,163,139)`, with another gray-green artwork overlay. Conservative symbol concealment left many exposed ceramic rectangles without artwork. Both shadow bands and tile transforms already existed; this iteration strengthens the bands and changes ceramic lighting rather than claiming that depth rendering was absent.

## C–F. Layout architecture, families, novelty and fallback

Eight coordinate masks provide turtle, butterfly, flower, pyramid, bridge, spiral, crescent and islands. Family scheduling is independent of the stage. Hard and later windows span all eight families; Normal retains the five authored boards plus the turtle and butterfly at levels 6 and 7. These are compact garden interpretations, not full 144-tile traditional layouts. Base holes, narrow connections, separate components and broad/stepped formations differ. Within-family cycles relocate two positions into negative space; terrace candidates rotate supported pair groups. Outward extensions were rejected during development because they reduced scale on short screens.

Scheduling uses `(level - 6) % 8`. A bounded twelve-cycle parameter and candidate index determine geometry. Version 8 derives stable symbol seeds from the historical level seed XOR version; no time or ambient randomness participates. V7 source, footprints and policy are frozen separately for restoration.

Lookback checks the ten preceding level numbers, across their bounded six candidate geometries. Translation-normalized fingerprints include every x/y/layer coordinate and exclude IDs and artwork. No previous puzzle analysis or full-history generation is needed. Current candidates prefer fingerprints absent from that bounded portfolio. If none qualifies, the selected validated puzzle records `noveltyLimited`; global uniqueness is explicitly not promised. Family scheduling guarantees eight families per eight procedural levels, including fallback, irrespective of quality rejection.

Search remains six candidates, each with bounded geometry attempts and analysis nodes. The final candidate uses deterministic top-layer peeling, so search sees a structured witness as well as random legal-removal witnesses. Forced fallback uses the scheduled family and its supported terraces, rather than a universal rectangle. Every fallback is recolored with the stable botanical catalog and replayed through the production validator. A valid witness and a met strategic-quality target are separate facts.

## G–I. Garden palette and tile rendering

Centralized `GardenTheme` tokens define stable IDs `spring`, `sunrise`, `lotus`, `twilight`, `autumn`. `(level-1)%5` selects the same theme after restart and relaunch, including historical puzzles without changing their identity. Forest/mint/rose, teal/honey/coral, jade/turquoise/rose, blue-green/lavender and olive/terracotta palettes tint board gradients, ceramic shading and tray surfaces. The surrounding app chrome remains the existing design.

Available tiles retain warm ivory, botanical artwork and inset highlights. Blocked tiles use theme ceramic-to-accent lighting rather than a flat gray face, retain corner blocking marks, and apply a lighter artwork veil. Opaque upper faces and `TileVisibility` continue to conceal hidden identities. Body and edge colors use centralized accent/gold tokens. Stronger penumbra/contact bands improve separation without changing the physical face, body offsets or hit regions. Theme gradients introduce no animation or photographic resources.

## J–L. Difficulty, solvability and compatibility

Boundaries stay Normal 1–7, Hard 8–15, Very Hard 16–30, Expert 31+. The initial five authored boards remain unchanged. Procedural tile-count envelopes accommodate different silhouettes. Stages add supported terraces; the existing consequence-choice, blocked-position, layer-count, forced-move and three-held-tray metrics remain. New acceptance also checks eighteen covered positions for Hard+ and seventy overlap relationships for Expert. Analysis is bounded at 4,000/5,000/6,000/7,000 nodes per candidate; failure to meet a quality target is recorded, not presented as a proof of difficulty. Human difficulty is not established by these proxies.

`LevelValidator` still constructs `Game`, selects each witness tile, requires automatic pair cancellation after each pair, and checks empty board/tray with WON. No solver-only shortcut replaces production replay. Rules, four-slot loss, undo, hints, progression, credit/cooldown serialization and ad code remain intact.

Restore explicitly routes v7 to `VersionSevenGenerator` with frozen `HistoricalAdvancedLayouts` and `VersionSevenDifficultyPolicy`. V4–v6 routes remain. Existing valid active puzzles retain seed/version, tray and accepted actions; new frontier puzzles use v8. No save or wallet format was reset.

## M–N. Measurements and JVM results

See the validation evidence added below after execution. The requested matrix test covers 8–60, 100–120, 250, 500 and 1000; records family, variation, tile/layer counts, SHA-256 geometry fingerprint, stable theme ID, witness outcome, generation duration, strategic target, blocking and overlap counts. Separate tests cover within-family changes, ten-level duplicates, deterministic definitions, v7 active tray restoration and stable theme tokens. Existing JVM tests are retained.

## O–Q. Android, APK and signing

NOT VERIFIED. Wrapper startup cannot write the read-only home lock. A direct installed Gradle invocation with a writable cache fails creating its lock service: `Could not determine a usable wildcard IP for this machine`. ADB fails opening its smart socket: `Operation not permitted`. Therefore Android instrumentation was updated but not executed, no actual Android screenshots were examined, and no fresh APK was produced. Subjective attractiveness and screenshot readability are not certified.

Expected output after a successful host build is `app/build/outputs/apk/debug/app-debug.apk`, package `com.tilebloom.game`. Fresh size, timestamp, SHA-256, alignment, signature and packaged v8/theme bytecode are unavailable. Signing configuration and keys were not modified; installed-certificate compatibility is NOT VERIFIED. Required signer is `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`.

Exact host build, signer, installation and capture commands are in [HOST_COMMANDS.md](docs/codex30/HOST_COMMANDS.md). They use replacement installation only after checking the signer and never uninstall or clear data.

## R–T. Git, limits and CODEX 31

History is preserved in the existing `.tilebloom-git` repository; the sandbox's `.git` mount is empty. No repository was initialized, rewritten or pushed. Unrelated pre-existing untracked reports are excluded from the commit.

Limits: finite deterministic geometry portfolio, no global uniqueness guarantee; quality targets may be missed within budget; relative human difficulty needs playtesting; full app chrome is not comprehensively themed; no Android visual, Lint, Gradle, fresh APK or signer certification in this environment. Recommended CODEX 31: execute the host verification/capture matrix, tune themes using actual device images and benchmark cold generation before expanding the finite geometry portfolio.

## Acceptance audit

PASS below denotes source/diagnostic evidence, not Android device approval.

| # | Criterion | Result | Evidence / limit |
|---|---|---|---|
| 1 | Mahjong rules preserved | PASS | Production Rules/Game unchanged; witness replay retained |
| 2 | Four-slot tray | PASS | Capacity and loss rules unchanged |
| 3 | Undo and Hint | PASS | Controller and credit logic unchanged; regressions retained |
| 4 | AdMob | PASS | SDK/configuration/ad service sources unchanged; device behavior pending |
| 5 | Existing saves | PASS | Frozen v7 source and restoration regression; v4–v6 retained |
| 6 | Garden themes | PASS | Five centralized stable theme definitions |
| 7 | Brighter and attractive | NOT VERIFIED | Palette changed; actual Android image review blocked |
| 8 | Readable botanical symbols | NOT VERIFIED | Artwork preserved; device contrast/legibility pending |
| 9 | Flat gray blocked faces removed | PASS | Theme ceramic gradients replace gray fill |
| 10 | Available distinction | NOT VERIFIED | Ivory/ceramic tokens and marks differ; device appearance pending |
| 11 | Concealed identities | PASS | Visibility guard and opaque face rendering retained |
| 12 | Depth/shadows | PASS | Existing geometry retained; stronger shadow bands, lit ceramics |
| 13 | Eight families | PASS | Eight different coordinate masks |
| 14 | Structural differences | PASS | Different silhouettes, holes, components and terraces |
| 15 | Family variations | PASS | Base relocation and terrace candidate tests |
| 16 | Consecutive diversity | PASS | Eight-family scheduling; bounded normalized-geometry tests |
| 17 | Repetitive fallbacks | PASS | Fallback inherits scheduled family; no universal rectangle |
| 18 | Deterministic new puzzles | PASS | Level/version seed and fixed candidate budgets |
| 19 | Exact restart | PASS | Game restart and session definition retained |
| 20 | Difficulty boundaries | PASS | 7/15/30 boundaries retained; strategic proxies separately measured |
| 21 | Validated witness | PASS | Every returned definition calls production LevelValidator |
| 22 | Rendering/touch consistency | PASS | Shared geometry unchanged; responsive JVM tests retained |
| 23 | Forward-only progression | PASS | Progression logic untouched; regressions retained |
| 24 | JVM regressions | PASS | 244-test suite and six focused final novelty regressions pass |
| 25 | Instrumentation updated | PASS | All families/themes at four widths; compiled, not executed |
| 26 | Android screenshots reviewed | NOT VERIFIED | ADB socket prohibited; no Android captures available |
| 27 | Fresh verified APK | FAIL | Gradle startup blocked; no fresh APK generated |
| 28 | Signing compatibility | NOT VERIFIED | Keys/configuration unchanged; no fresh APK/device certificate check |
| 29 | Git history | PASS | Existing `.tilebloom-git` used; no reinitialization or rewriting |
| 30 | Nothing published | PASS | No push, release, upload or publication |

### Geometry inspection artifact

[Production geometry contact sheet](docs/codex30/geometry-models.png) was generated from accepted v8 `Levels.get(30..37)` coordinates and examined. It shows the broad turtle, split wings, radial branches, stepped pyramid, narrow bridge, winding spiral, open crescent and separated islands. [Raw model coordinates](docs/codex30/geometry-models.csv) retain the evidence. Colors identify layers in this diagnostic diagram; it is **not an Android screenshot or a preview of the final themed renderer**. Device visual acceptance remains pending.

### Final matrix measurements

[Generation matrix](docs/codex30/generation-matrix.csv): **77/77 winning witnesses**, eight families (9–11 occurrences each), no exact fingerprint duplicates at distances 1–10 in the tested matrix, and no reported novelty limitation. All eight families occur in every full eight-level scheduled window. This is a bounded sampled result, not a claim of endless global uniqueness.

| Tier | Samples | Tile range | Mean initially blocked | Mean overlap relationships |
|---|---:|---:|---:|---:|
| Hard | 8 | 32–42 | 26.12 | 49.50 |
| Very Hard | 15 | 38–48 | 31.40 | 74.80 |
| Expert | 54 | 46–56 | 39.98 | 106.02 |

Expert's lowest measured overlap count was 87. Strategic quality targets were met on **75/77** sampled boards; levels **21 and 29** missed them within budget. Both still have complete production-validated winning witnesses. These misses are a known tuning limitation, not a reason to alter saved puzzles or mislabel solvability.

| Level | Family | Variation | Tiles / layers | Theme | Witness |
|---|---|---|---|---|---|
| 13 | Islands | 0-2 | 32 / 3 | Lotus | PASS |
| 14 | Turtle | 1-0 | 42 / 3 | Twilight | PASS |
| 16 | Flower | 1-1 | 40 / 4 | Spring | PASS |
| 17 | Pyramid | 1-0 | 40 / 4 | Sunrise | PASS |

Matrix generation mean was 598.4 ms, maximum 2,101 ms in this diagnostic host run. This is not an Android performance benchmark; concurrent compiler/test load affects timing. Complete fingerprints and per-level durations are in the CSV.

[Android diagnostic compilation](docs/codex30/android-compilation.log) passes for final main and instrumentation sources, using cached SDK/dependency jars. [Gradle startup evidence](docs/codex30/gradle-blocker.log) records the wildcard-IP service failure. Neither establishes an APK or device result.

### Final JVM regression result

**PASS: 244 tests**, 257.931 seconds, through cached Kotlin 1.9.24/JDK 17/JUnit 4.13.2. [Full log](docs/codex30/jvm-tests.log). The final strengthened duplicate-detection, translation-normalization and varied-fallback assertions were then compiled and rechecked in the six-test Endless suite: **6/6 PASS**, 69.445 seconds ([focused log](docs/codex30/novelty-tests.log)). These six are reruns, not six additional unique tests. The final production engine and final renderer/instrumentation sources compile. Standard Gradle unit test/Lint/assemble and device execution remain blocked.

Original regression assertions that explicitly required only two families and at most 48 tiles were updated to v8's eight families and explicit per-tier tile/node envelopes. Introductory boards retain their historical 5,000-node test allowance. The original covered-position, Expert overlap, three-held-tray, touch, undo, hint, advertising, progression and historical-save checks remain and pass. V7 generator/layout/policy bodies were also compared against the preceding Git revision and match exactly apart from renamed frozen object references.

Acceptance totals: **24 PASS, 5 NOT VERIFIED, 1 FAIL**. The failure is fresh APK delivery; unverified items concern device appearance and signing. This iteration is source-verified and committed, not Android release-approved.
