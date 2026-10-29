# TileBloom — Mahjong Garden Puzzle

Native Kotlin Android tile collection puzzle with a Zen Garden identity. CODEX 16 opens on a botanical level-start screen with the actual saved level and a Play button. Play resumes the same attempt; Home returns to this screen without resetting it. Rounded vector controls replace text-heavy Undo/Restart buttons, with a persistent numeric Undo badge. CODEX 15 added a four-slot tray above the Mahjong board. One legal tap transfers a tile; identical symbols clear in pairs; four different held symbols lose immediately. Undo reverses one tile selection, including matching and defeat. Endless generation provides on-demand, verified levels beyond the five preserved introductory boards. The only playable board is the current unfinished level. Victory offers **Next Level →**, advancing exactly one; completed boards cannot be selected or replayed. Restart repeats the same unfinished puzzle. No ads, accounts, publishing, or new dependencies.

Three initial Undo credits remain global and persistent. Successful Undo costs one; failed Undo costs nothing. Restart, victory, advancement, and relaunch never refill credits.

Level identities are positive Kotlin `Long` values, up to 9,223,372,036,854,775,807. The last representable identity is guarded against overflow; this is practical endless progression, not mathematical infinity. Levels 1–5 retain their seeds/versions and boards. Version 4 generates later levels deterministically using eight silhouettes, reflection and upper-terrace placement variants, 24–28 tiles, bounded quality analysis, and production-engine witness replay. Boards are generated on entry, not stored in a growing catalog.

Schema 4 (gameplay rules 2) stores a single completion frontier, active identity/seed/version, accepted individual-selection history, and credits. A verified winning history represents pending victory. Old schemas migrate from their contiguous completion prefix; the first missing level is never skipped. Compatible unfinished histories survive. Completed legacy selections move to the first unfinished level. Invalid histories recover without reopening a completed board. Private storage remains `player-progress-v1`; ordinary upgrades retain data, while uninstall/data clearing removes it.

Cold restoration and next-board generation run off the main thread. Next Level is guarded across Activity recreation; its AtomicFile transaction completes before the new board is installed. Failed generation/saving retains victory for retry. Botanical drawing, authentic Mahjong rules, touch geometry, and removal animations remain intact.

See [CODEX_16_REPORT.md](CODEX_16_REPORT.md), [VALIDATION.md](VALIDATION.md), and [PLAYTEST_01.md](PLAYTEST_01.md) for architecture, measured verification, limitations, and device checks. Earlier iteration reports are historical.

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
# Diagnostic builder for restricted environments (cached Kotlin 1.9.24):
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py
python3 tools/test_host_verify.py
```

Normal toolchain: Kotlin 2.2.10, AGP 8.10.0, Gradle 8.13, Java 17, SDK 36, min SDK 26, build tools 35.0.0. The diagnostic builder compiles instrumentation sources but does not execute them or run Android Lint.

## CODEX 14: twenty-level garden challenges

Difficulty is selected by `(level - 1) / 20` using positive Long identifiers. Profiles:

| Tier | Levels | Tile target | Active symbols |
|---|---|---|---|
| Beginner | 1–20 | 24–28 | 6 |
| Beginner Plus | 21–40 | 28–32 | 8 |
| Intermediate | 41–60 | 32–36 | 10 |
| Advanced | 61–80 | 36–40 | 12 |
| Expert | 81–100 | 40–44 | 14 |
| Master | 101–120 | 40–48 | 16 |
| Continuing | 121+ (20 levels each) | 40–48 | at most 16 |

The five existing introductory puzzles remain frozen (including their historical tile
counts). New boards use at most 40 tiles within eight compact silhouette families;
target ranges permit this conservative device-size cap. Within each tier the second
half adds four tiles where the cap permits. Profiles also control cover offsets,
unlocking-choice targets, risk allowance, candidate and analysis budgets. Higher tiers
rotate strategic emphasis and geometry; finite board and symbol limits mean challenge
cannot grow without bound. Scores measure proxies, not proven human difficulty.

New original Canvas artwork: mushroom and butterfly (21), bamboo and acorn (41), maple
leaf and pinecone (61), iris and ginkgo (81), cherry blossom and dragonfly (101).
Every milestone board includes a complete pair of each new type. Original IDs 0–5 stay
fixed; appended explicit stable IDs 6–15 identify the new catalog. Matching still uses
symbol identity through the existing engine. No downloaded art or image engine.

Generation v5 retains v4 reconstruction for ongoing saved puzzles. Historical assignment
is frozen to six symbols, including introductory versions. Saves migrate to schema 4 and retain the seed, generator version, completion frontier, moves and global Undo balance.
An unrecognizable snapshot follows the existing safe recovery at the current frontier,
preserving level and credits. New levels use v5; no data deletion is required.
Future catalog additions must append a unique stable ID and introduction milestone,
provide a renderer/accessibility name, bump the generation version when output changes,
and retain earlier reconstructors. The curated catalog currently ends at sixteen;
there is no unlimited artwork. At 121+ tier notifications do not claim new tiles.

See [CODEX_14_REPORT.md](CODEX_14_REPORT.md) for build evidence and limitations.

## CODEX 15 save compatibility and tray analysis

Tray contents, cleared pairs and status are reconstructed by production action replay.
Schemas 1–3 contain ordered pair actions: migration expands each pair into two taps and
validates them. Compatible puzzle seeds and generator versions remain unchanged. If
replay fails, only the unfinished attempt resets; completion frontier and Undo balance
survive. Schema 4 explicitly stores rules version 2. AtomicFile writes remain ordered.
All generated candidates replay their witnesses through `Game.select`, ending with an
empty board, empty tray and WON. Existing tier quality scores remain frozen for seed
compatibility; `TrayAnalyzer` adds bounded individual-order analysis, occupancy, imminent
losses and matching reveals. Its UNKNOWN result never stops gameplay. Live play declares
only explicit tray loss, exhausted unmatched board, or absence of any legal tile as lost.

The tray uses four responsive ivory/gold slots in a green container. Board geometry and
system-inset handling are unchanged; the tray consumes 58–78 dp above the flexible board.
Device playtesting is still required for smallest supported screens and animation polish.

## CODEX 16: start, resume and icon controls

A fresh entry without restored Activity state shows **LEVEL X** and **PLAY**. Gameplay
views are created once on Play and reused after Home. UI navigation never changes the
puzzle, seed, accepted selections, tray, credits or completion frontier. Returning from
background keeps the active screen. Activity/process recreation with a matching saved
level restores START or GAME from a Bundle; stale navigation opens the current level start.

Pending victory stays completed: Play opens its outcome, and Next Level performs the
existing durable one-level transition before showing the new start screen. A saved
defeat stays lost until the player explicitly chooses Undo or Restart after Play.
Home is unavailable on victory. Completed levels cannot be restarted or selected.

Original lightweight VectorDrawables provide Play, Next, Undo, Restart and Home. The
Undo icon carries a compact numeric badge, including zero; accessibility descriptions
and tooltips identify all secondary actions. All touch targets are at least 56 dp.
The welcome layout scrolls for compact/large-font screens; level/title text uses bounded
auto sizing, and safe system-bar/cutout insets apply to both screens. Static botanical
Canvas artwork reuses the existing symbol illustrations. No image/icon library was added.

Save schema 4/gameplay rules 2 remain unchanged. Navigation is separate presentation
state, so no new save migration or puzzle generation version is needed. All domain
engine, generator, tiers, symbols, wallet and repository logic are retained. See the
CODEX 16 report for exact test/APK evidence and pending Android playtests.

## CODEX 25 — online garden and botanical expansion

Gameplay now requires Android's default network to have both INTERNET and VALIDATED capabilities. A lifecycle-managed callback suspends input immediately on validation loss; a 600ms handover window stabilizes the connection panel. Retry and Network Settings are available. Home keeps the saved level and privacy entry visible. Reconnection resumes the same board/tray and credits without restarting. This is local connectivity detection, not remote authentication or a tamper-proof policy. Consent and ad availability never unlock or block gameplay.

New v6 boards introduce Cactus (ID 16) at level 6, Lavender (17) at 11, Ladybug (18) at 21 and Snail (19) at 41. All eligible new types receive real witness pairs. Existing IDs, levels 1–5, 20-level tiers and four-slot rules remain. Historical generator v4/v5 saves restore their original boards; schema 5 and balances are unchanged. Home spacing, the secondary privacy action, tray outlines and control badge separation are refined.

See [CODEX_25_REPORT.md](CODEX_25_REPORT.md) for acceptance results and limitations. Cached JVM and diagnostic compilation checks are available; normal Gradle and ADB are blocked by host restrictions. **No fresh CODEX 25 APK or emulator verification is available.** Existing APKs must not be treated as this delivery.

## CODEX 29: advanced Mahjong

The historical CODEX 29 baseline used generator v7: Normal 1–7, Hard 8–15, Very Hard 16–30, Expert 31+. Alternating classic side-terrace and compact center-stack families reach four real blocking layers with at most 48 botanical tiles. Six bounded candidates combine pair-choice analysis and actual four-slot tray probes; every returned board replays a complete winning witness through Game. Old in-progress v4/v5/v6 puzzles reconstruct with historical definitions. Blocked faces are darker; overhead coverage hides artwork; ceramic edges and shadows are deeper.

See [CODEX_29_REPORT.md](CODEX_29_REPORT.md) for measured baseline, targets, compatibility and limitations. Android visual acceptance and a fresh APK remain pending because Gradle/ADB are blocked in this environment. `tools/benchmark29.py historical` measures frozen v6; omit the argument for the current generator (Java 17 required). AdvancedBoardVisualTest prepares Android board captures without touching saved progress.

## CODEX 30 garden layouts and themes

New endless puzzles use generator v8 with eight scheduled footprints: turtle, butterfly, flower, pyramid, bridge, spiral, crescent and islands. Branch relocations and supported terrace arrangements vary deterministically. Ten-level geometry lookback compares normalized coordinates, independently of symbols; bounded generation reports novelty limitations rather than claiming global uniqueness. Historical v4–v7 puzzles remain reconstructible, including active tray/history state.

Five stable garden themes tint ceramic surfaces, board gradients and tray containers. Available faces remain ivory; blocked faces use lit theme ceramics, opaque upper tiles retain symbol privacy, and rendering continues to use the shared touch transform. Difficulty boundaries remain 1–7 / 8–15 / 16–30 / 31+.

See [CODEX_30_REPORT.md](CODEX_30_REPORT.md) for measured results and limitations, and [host commands](docs/codex30/HOST_COMMANDS.md) for standard Android verification. A source/diagnostic check is not a fresh verified APK.

CODEX 30 final diagnostic result: **244/244 JVM tests PASS**, plus **6/6 focused Endless reruns PASS** after stricter duplicate/normalization assertions. Main and instrumentation sources compile against cached Android dependencies. The 77-board matrix has 77 validated wins, eight families and zero observed exact duplicates within ten levels; strategic targets are met on 75/77 (21 and 29 miss). Actual Android screenshots, Gradle/Lint, fresh APK and signer verification remain blocked. See CODEX_30_REPORT.md and docs/codex30 for evidence.

### CODEX 31 garden rendering

The existing five level themes now supply an immutable screen palette: full-screen gradient, status panel, tray, empty holders, controls, ceramics, shadows and Hint. The board itself is transparent. Side-blocked botanical symbols remain visible with muted faces; opaque upper tiles cover lower artwork in physical draw order. Generator v8, historical generators, saves, credits and ads are unchanged. Android screenshots and a fresh standard APK could not be verified inside the restricted sandbox; see [CODEX 31 report](CODEX_31_REPORT.md) and [host commands](docs/codex31/HOST_COMMANDS.md).

### CODEX 32: next-level preparation

Next Level now shares a background preparation task begun after durable victory. Slow preparation shows a theme-aware botanical loading surface with the real next level and Retry; ready boards skip it. Search/geometry optimizations retain generator version, difficulty and witness validation. See [CODEX_32_REPORT.md](CODEX_32_REPORT.md) and [host JVM measurements](docs/codex32/isolated-performance.md). Android runtime, fresh standard APK and screenshots remain pending because Gradle cannot run in this sandbox.


## CODEX 34 strategic difficulty

Generator v9 constructs verified individual tray selections with delayed partners. Frozen v8 regeneration preserves existing in-progress saves. Candidate evaluation uses production-rule greedy, simple and visibility-restricted lookahead players; all eight layout families and the four-slot rules remain.

[Measured before/after results](docs/codex34/benchmark-summary.md), [full report](CODEX_34_REPORT.md), and [human playtest protocol](docs/codex34/playtest.md). Expert holdout simple wins fall from 36/36 to 18/36 while lookahead remains 36/36; some profiles still miss their target. Gradle/APK verification is blocked by this environment, and the existing APK does not contain these changes. Reproduce JVM checks with Java 17 using `python3 tools/test34.py`; run `python3 tools/benchmark34.py` for the fixed-seed matrix.
