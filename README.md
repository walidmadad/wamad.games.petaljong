# TileBloom — Mahjong Garden Puzzle

Native Kotlin Android tile collection puzzle with a Zen Garden identity. CODEX 15 adds a four-slot tray above the Mahjong board. One legal tap transfers a tile; identical symbols clear in pairs; four different held symbols lose immediately. Undo reverses one tile selection, including matching and defeat. Endless generation provides on-demand, verified levels beyond the five preserved introductory boards. The only playable board is the current unfinished level. Victory offers **Next Level →**, advancing exactly one; completed boards cannot be selected or replayed. Restart repeats the same unfinished puzzle. No ads, accounts, publishing, or new dependencies.

Three initial Undo credits remain global and persistent. Successful Undo costs one; failed Undo costs nothing. Restart, victory, advancement, and relaunch never refill credits.

Level identities are positive Kotlin `Long` values, up to 9,223,372,036,854,775,807. The last representable identity is guarded against overflow; this is practical endless progression, not mathematical infinity. Levels 1–5 retain their seeds/versions and boards. Version 4 generates later levels deterministically using eight silhouettes, reflection and upper-terrace placement variants, 24–28 tiles, bounded quality analysis, and production-engine witness replay. Boards are generated on entry, not stored in a growing catalog.

Schema 4 (gameplay rules 2) stores a single completion frontier, active identity/seed/version, accepted individual-selection history, and credits. A verified winning history represents pending victory. Old schemas migrate from their contiguous completion prefix; the first missing level is never skipped. Compatible unfinished histories survive. Completed legacy selections move to the first unfinished level. Invalid histories recover without reopening a completed board. Private storage remains `player-progress-v1`; ordinary upgrades retain data, while uninstall/data clearing removes it.

Cold restoration and next-board generation run off the main thread. Next Level is guarded across Activity recreation; its AtomicFile transaction completes before the new board is installed. Failed generation/saving retains victory for retry. Botanical drawing, authentic Mahjong rules, touch geometry, and removal animations remain intact.

See [CODEX_15_REPORT.md](CODEX_15_REPORT.md), [VALIDATION.md](VALIDATION.md), and [PLAYTEST_01.md](PLAYTEST_01.md) for architecture, measured verification, limitations, and device checks. Earlier iteration reports are historical.

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
