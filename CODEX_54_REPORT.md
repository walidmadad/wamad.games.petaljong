# CODEX 54 — Petaljong

30 October 2026. Working-tree implementation and validation report. Google Play title remains **Petaljong: Mahjong Pair Match**. Nothing was published, pushed, installed, uninstalled or cleared.

The catalog and generator overhaul is implemented. Android build/device sign-off and human playtesting remain outstanding; **there is no fresh APK** from this environment.

## A. Why previous Expert puzzles were still easy

The inspected catalog contained 23 identities. Expert typically reused 14–16 identities across roughly 46–56 tiles, leaving interchangeable copies that could bypass the intended partner dependency. The construction schedule closed held symbols with a 30% discretionary probability; its pressure measurements described one constructed route, rather than proving that ordinary players needed that route.

Acceptance tested only three fixed tie breaks. Independent tie breaks expose the gap: the corrected baseline player wins 32/144 Expert runs even though the old internal acceptance often reported success. Geometry and layer counts alone did not establish consequential play. No minimum physical pair-separation constraint existed; proximity alone was not established as the cause.

Mysteries were distributed by position along the witness. The old simulated player revealed every legal mystery before choosing, although Option C retains only one active reveal. Its information handling and pair counts therefore did not reflect the approved interaction. Continuation analysis could also omit the safe reveal-mystery + matching-normal direct removal at a three-symbol tray. Both issues could exaggerate difficulty or misclassify continuations.

An additional preparation bug continued searching after a qualifying first candidate when subsequent candidates failed. Level 1001 evaluated all 24 candidates and returned candidate 1, whose target already passed. Version 12 now stops after the required two-candidate comparison once the retained winner qualifies. Historical reconstruction is frozen separately.

## B. The 12 new symbols

| Stable ID | New illustration | Distinguishing design |
|---:|---|---|
| 23 | Pink Peony | Full ruffled, overlapping broad petals |
| 24 | Pink Camellia | Ordered smooth-petal concentric rosette |
| 25 | Purple Crocus | Upright cup, orange-gold stamens, blade leaves |
| 26 | Purple Bellflower | Three hanging pointed bells on an arching stem |
| 27 | Yellow Marigold | Dense scalloped yellow pompom |
| 28 | Golden Chrysanthemum | Long curling radial golden petals |
| 29 | Monstera Leaf | Heart silhouette, deep splits and natural holes |
| 30 | Eucalyptus Leaf | Rounded blue-sage leaves on a curved stem |
| 31 | Red Japanese Maple | Seven narrow, deeply divided pointed lobes |
| 32 | Orange Sugar Maple | Five broad lobes and shallower sinuses |
| 33 | Olive Branch | Slender silver-green leaves and purple olives |
| 34 | Rosemary Branch | Needle clusters and small lilac flowers |

An iris and generic maple already existed, so crocus and distinct maple species were selected. IDs 0–22, their introductions and their artwork remain unchanged. The catalog now contains 35 unique IDs. Only identical IDs match.

## C. Artwork comparison

All 12 were generated individually with the built-in image_gen tool, then packaged as transparent, lossless 256×256 WebP resources. Exact prompts and source/package hashes are in [art-prompts.json](docs/codex54/art-prompts.json), [art-sources.json](docs/codex54/art-sources.json) and [art-manifest.json](docs/codex54/art-manifest.json). Packaging adds 715,062 bytes. The existing 23 illustrations and approved launcher files remain byte-identical to their recorded baselines.

Visual asset inspection checked the existing pink blossom, approved icon, and the new illustrations at 24/32/48-pixel ink sizes. Gentle gradients, botanical colors, gold details and strong silhouettes follow the established direction. Related shapes differ beyond recoloring: six related-pair silhouette comparisons differ in 9.9–26.0% of the 32-pixel square. This is an automated shape check, not proof of human recognition.

[All 12, with small-size samples](docs/codex54/new-symbols-asset-review.png) · [Existing art beside new variants](docs/codex54/similar-symbols-asset-review.png). These are explicitly labeled asset composites, **not Android screenshots**. Existing board/tray drawing paths and ivory/jade tile styling are retained; the bitmap cache grows from 23 to 35 entries, bounded at 8.75 MiB of decoded pixels.

## D. Similar-symbol mixing

Version 12 uses explicit co-occurrence groups: pink flowers, yellow flowers, related leaves, purple flowers, maples and botanical branches. It retains historical recognition-family requirements as well.

Normal 1–7 keeps the introductory catalog. Hard 8–15 requires one eligible narrow related group; Very Hard 16–30 requires two; early Expert requires three, and later Expert four, subject to introduction and pair capacity. Every required identity has at least one identical partner, including a normal partner for every mystery identity. Selection rotates eligible groups; it does not force all 12 additions into every puzzle.

The historical catalog policies remain separate so this expansion cannot change old saved puzzles. All eight languages have names for the new symbols.

## E. Difficulty model

The tier boundaries remain 1–7 Normal, 8–15 Hard, 16–30 Very Hard and 31+ Expert. Expert targets continue through 31–50, 51–100, 101–200 and later bounded windows. Existing 25-level targets saturate; tiles and layers do not grow indefinitely.

Analysis now combines legal choices, initially visible and freely inspectable pairs, blocking depth/overlaps, witness partner delay, tray occupancy, production-verified four-distinct alternatives, immediate-pair-only search, and ordinary-player outcomes. Greedy and one-step strategies must fail the selected higher-tier targets while a bounded planning player succeeds. Short-lookahead failure is a preference, not a mandatory condition that can force slow or unfair selection.

Full continuation analysis normalizes legal reveal/collection sequences, including mystery + normal direct removal and switching an active reveal. Two mysteries cannot clear each other by revelation. SOLVABLE, UNSOLVABLE and UNKNOWN_BUDGET_EXCEEDED remain distinct. Unfinished searches propagate UNKNOWN. Restricted pair-only UNSOLVABLE is never presented as four-slot unsolvability. Neither witness occupancy nor a short-policy failure proves a minimum human planning depth.

## F. Symbol placement

The legal removal order is still constructed first, with an even opening/closing assignment and at most three unmatched symbols in its witness. Discretionary closing drops from 30% to 22% in Hard, 10% through early Expert and 4% later. Identity capacity increases beyond the old fixed Expert palette, while reserving at least two extra pairs so repeated-copy choices remain possible.

This reduces interchangeable shortcuts and encourages holding a symbol while unlocking its partner. No artificial distance or tile-size penalty is used. All eight layout families and the existing silhouettes, offsets, asymmetric holes, terraces and multilayer geometry remain available. Sampled Expert tile counts remain bounded and readable; the board geometry and touch-size policy were not redesigned.

## G. Mystery integration

Version 12 chooses at most one mystery per identity, leaving normal matching copies. It prefers inspectable mysteries with partners behind dependencies and tiles involved in coverage relationships. Counts remain bounded: 2 in Hard, 4 in Very Hard, 6 in early Expert and 8 later, rather than simply increasing green-card density.

Production reveal, switch, second-tap collection, normal direct matching and mismatch behavior are unchanged. Players now scan using real reveal taps, retain memory and respect the single active reveal. They can re-reveal a known mystery for a normal direct match. They receive no free hide action, no covered identities, no construction witness and no mystery-to-mystery shortcut.

## H. Four-slot strategy

The four-slot tray and four-different defeat rule remain unchanged. Mean three-held selections along the Expert witnesses rise from 14.7 to 20.9. Exact four-distinct alternatives are counted only at valid three-held states and checked against production transitions; a regression compares the totals against every production alternative.

The separate [actual player-pressure run](docs/codex54/player-pressure.csv) uses tie break 10: stronger planning wins all 12 sampled Expert boards, reaches three held symbols on 10, and has pressure states on its real played routes. Every saved player trace replays through the production controller. These are route observations, not a proof that every possible solution must reach three held symbols.

## I. Candidate selection and work bounds

At most 24 candidates are considered. At least two are compared; once the retained winner qualifies, generation stops. Acceptance and scoring prefer verified solutions, observed-information planning advantage, consequential tray choices, family mixing and useful geometry. The short/deeper simulations share at most 1,800 expanded nodes per candidate; shallow-player rejects receive reduced work, and restricted pair-only analysis adds at most 64 nodes. Board size bounds branching.

Immutable geometry masks and per-state availability caches accelerate the controller and search forks. Cache invalidation follows immutable board-state changes, including reveal, collect, undo, restore and restart. Tests compare availability against the original Rules implementation across witness states, 256 removal masks, the signed 64th bit and larger-fixture fallback. No global cross-puzzle search memo is introduced.

A missed quality target remains false in metadata. Level 501 is a verified, challenging returned board but misses its full late-Expert target; it is not reported as an Expert-quality success. The explicit verified fallback also remains marked as a target miss. Background preparation, the 30-second safety budget and existing loading-screen integration are preserved.

## J. Reproducible before/after results

The baseline is pinned to `8b318b7d283aebcb70144670bb81fe7ac53784d1`. Both puzzle versions are evaluated with the same corrected observed-information players. Held-out greedy tie breaks 10–21 and planning tie breaks 10–13 are not used for acceptance. There are 20 fixed puzzles, including 12 Expert puzzles. Short planning uses depth 3 / 600 expanded nodes; stronger planning uses depth 6 / 1,800. These policies are not human subjects and never use an omniscient winning witness.

| Levels | Puzzles | Greedy wins old → new | Short wins old → new | Strong wins old → new | Mean three-held old → new | Generation median ms old → new | New quality targets |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1–7 | 2 | 24/24 → 24/24 | 8/8 → 8/8 | 8/8 → 8/8 | 0.0 → 0.0 | 401 → 426 | 2/2 |
| 8–15 | 3 | 11/36 → 16/36 | 12/12 → 12/12 | 12/12 → 12/12 | 9.0 → 11.3 | 786 → 1078 | 3/3 |
| 16–30 | 3 | 4/36 → 4/36 | 12/12 → 12/12 | 12/12 → 12/12 | 12.7 → 19.0 | 1991 → 1937 | 3/3 |
| 31–50 | 3 | 10/36 → 1/36 | 8/12 → 12/12 | 8/12 → 12/12 | 13.3 → 21.7 | 1383 → 2366 | 3/3 |
| 51–100 | 3 | 11/36 → 1/36 | 12/12 → 11/12 | 11/12 → 12/12 | 14.3 → 21.3 | 996 → 871 | 3/3 |
| 101–200 | 3 | 7/36 → 0/36 | 12/12 → 11/12 | 11/12 → 12/12 | 14.7 → 21.3 | 553 → 1479 | 3/3 |
| 201+ | 3 | 4/36 → 0/36 | 12/12 → 10/12 | 11/12 → 12/12 | 16.3 → 19.3 | 1204 → 1028 | 2/3 |

Expert greedy wins fall from **32/144 (22.2%) to 2/144 (1.4%)**. Stronger planning rises from **41/48 to 48/48**. Aggregate short-lookahead wins remain 44/48; there is no claim of a universal increase in minimum required depth. Hard is not uniformly harder, and the Very Hard greedy aggregate is unchanged. The measurable strategic improvement is concentrated in Expert.

[Full baseline rows](docs/codex54/baseline.csv) · [Full new rows](docs/codex54/after.csv) · [Methods and limitations](docs/codex54/benchmark-summary.md). Every sampled board passes its complete winning witness. CSV generation times compare the two generators under the final controller; the separate repeated timing below compares the original and final engine implementations.

## K. Winning witnesses

Every candidate is replayed by LevelValidator through production Game actions before scoring and before return. Mystery reveals remain on the board, second taps collect, matching resolves before terminal evaluation, and completion requires an empty board and tray. The complete regression matrix includes tutorial, high levels, forced fallbacks, save replay and Long.MAX_VALUE cases. Search timeouts never substitute for a witness.

## L. Generation performance

Three repeated, interleaved measurements compare original pinned v11 code and final v12 code in one JVM. The original code is relocated only into a diagnostic package so both engines can coexist. JDK 17, two processors and a fixed 512 MiB heap are used, with a warmup. The final timing pass ran after other diagnostics finished. These are host measurements, not Android timings.

| Level | v11 median / max ms | v12 median / max ms | v12 candidates |
|---|---:|---:|---:|
| 8 | 236 / 315 | 633 / 675 | 3 |
| 16 | 1090 / 1701 | 414 / 537 | 2 |
| 31 | 907 / 931 | 474 / 491 | 2 |
| 75 | 331 / 338 | 502 / 506 | 2 |
| 150 | 369 / 373 | 794 / 1121 | 3 |
| 501 | 1743 / 1829 | 2982 / 3050 | 24 |
| 1001 | 1541 / 1636 | 250 / 252 | 2 |

[Raw repeated timings](docs/codex54/performance.csv) · [Timing interpretation](docs/codex54/performance-summary.md). Each repeat has the same board fingerprint as the corresponding benchmark row and passes its witness. Level 1001 returns the same accepted board after two comparisons instead of 24. Level 501 remains the slow full-budget outlier and needs device measurement; performance is not claimed uniform across all future levels.

## M. Save compatibility

Application ID **com.tilebloom.game**, save schema, storage keys, old symbol IDs, credits, language/audio preferences, advertising/consent state and tutorial completion paths remain unchanged. Existing version 11 sessions explicitly reconstruct through VersionElevenGenerator; version 10 also uses the frozen 23-symbol policy. Versions 4–10 keep their existing restore routes. Version 12 applies to newly prepared puzzles.

An independent comparison recompiles the pinned original engine and checks eight v11 levels: seeds, exact stable symbol IDs, positions, mystery flags and full winning selections match. A fixed SHA-256 oracle and save tests also preserve tray, history, completed frontier and Hint/Undo balances. Existing temporary-reveal-on-restoration behavior is retained. No real player data was opened or edited, and no application was uninstalled or cleared. An actual installed-app upgrade comparison remains unverified because ADB is unavailable.

[Compatibility evidence](docs/codex54/compatibility-verification.txt) · [Original hashes](docs/codex54/compatibility-head.csv) · [Retained-generator hashes](docs/codex54/compatibility-retained.csv).

## N. Android visuals

ADB cannot create its listener: Operation not permitted. Therefore no Android screenshots, actual-device clipping/touch checks, runtime typography checks or installed gameplay verification are claimed.

Botanical54VisualTest prepares isolated production MahjongBoardView / TileTrayView captures for all 12 symbols, related variants, Hard 12, Very Hard 23, Expert 40/75/150/501, and three-held tray states. It does not open MainActivity or read/write the player's save. Its sources compile, but instrumentation was not executed. Asset comparison sheets above provide review material only.

The launcher icon, ivory ceramic tiles, jade backs, gold rims, shadows, gameplay controls, Home theme, Settings gear and victory LEVEL card remain unchanged. Source/icon hashes are recorded; visual sign-off still requires the Android fixtures.

## O. Automated validation

**333 independent cached-Kotlin/JUnit tests pass**, comprising all 320 existing tests plus 13 new tests. Catalog-size/profile-cap assertions were updated for the expansion; old identity prefixes are explicitly pinned and no behavior regressions were removed. New checks cover assets/translations, strict matching, co-occurrence, mysteries/normal partners, controller witnesses, old saves/credits, bounded/reproducible players, UNKNOWN vs UNSOLVABLE, hidden identity isolation, held-out difficulty improvement, legal player-trace replay, direct-mystery escape, exact trap counts, availability equivalence and the early-exit bug.

[Complete test output](docs/codex54/unit-tests.txt) · [Artwork checks](docs/codex54/art-validation.txt). All 23 old assets and all approved launcher files remain byte-identical. Independent AAPT2 resource compilation/linking and main + Android-test Kotlin compilation pass, including the new artwork references and unchanged AdMob/UMP integrations. This does not constitute Gradle, Android Lint or executed instrumentation.

## P. APK and build status

All three requested commands were attempted with JDK 17:

- `./gradlew --no-daemon --console=plain :app:testDebugUnitTest`
- `./gradlew --no-daemon --console=plain :app:assembleDebug`
- `./gradlew --no-daemon --console=plain :app:lintDebug`

They fail before task execution because the Gradle wrapper cache lock is read-only. The installed distribution with a writable temporary cache also fails to determine a usable wildcard IP. See [test command](docs/codex54/gradle-tests.txt), [assembly command](docs/codex54/gradle-assemble.txt), [lint command](docs/codex54/gradle-lint.txt), [writable-cache attempt](docs/codex54/gradle-writable.txt) and [ADB evidence](docs/codex54/adb.txt).

**No fresh APK was produced.** Its build timestamp, size, content, package and signature cannot be verified as a new deliverable. The pre-existing 9,123,232-byte APK predates this overhaul and is not offered for installation. Its metadata/hash are recorded separately as stale evidence. Its certificate matches the required `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`; signing configuration is unchanged and no private keys are committed. A complete build, fresh certificate/content verification and install-over-existing test remain required on a capable host.

## Q. Git

Verified CODEX54 changes are committed with `feat: expand botanical tiles and improve expert puzzle difficulty`; the final response supplies the commit hash. The existing .tilebloom-git history is preserved. No reinitialization, history rewriting, push or publication occurs. Pre-existing branding/loading/launcher/flag edits remain outside this commit and untouched. Validation applies to the full working tree, including that pre-existing Petaljong work; source hashes record those inputs. Signing files and unrelated changes are excluded.

## R. Remaining limitations and human playtest

No human feedback has been collected. [The playtest checklist](docs/codex54/PLAYTEST.md) compares Hard, Very Hard, early/advanced Expert and later windows for obvious pairs, planning, tray use, variant recognition, mystery memory, understandable failures and enjoyment. Use isolated fixtures or a test profile, never reset the player's save.

The portfolio is fixed and limited; tie breaks are policy variants, not independent human participants. Symbolic solvers do not measure human visual confusion. No universal minimum planning depth or mandatory three-held solution is proven. Level 501 misses its full target and remains a preparation-time outlier. Android generation latency, rendering, actual save migration, ads/consent runtime behavior, build/lint and a fresh signed APK remain unverified here. Existing Hint priorities and credit policy remain unchanged; bounded hints are not a guarantee of winning every continuation.

## S. Readiness

The catalog expansion and measurable Expert improvement are ready for source/art review. The reduction in held-out greedy success is real in this portfolio, while all accepted boards retain verified solutions. **Gameplay stabilization sign-off is pending** a fresh official Android build/lint, save-preserving installation, Android visual/runtime checks and real player testing. No publishing action has been taken.
