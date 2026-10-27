# TileBloom — Mahjong Garden Puzzle

CODEX 06 / application version 0.1: an offline, portrait Android Mahjong Solitaire prototype with **one deterministic level**. No accounts, permissions, services, advertising, analytics, or network access.

## Stack

Kotlin 2.2.10, Java 17, Android Gradle Plugin 8.10.0, Gradle wrapper 8.13, compile/target SDK 36, minimum SDK 26, Build Tools 35.0.0. The diagnostic fallback intentionally retains its cached Kotlin 1.9.24 compiler; it does not verify the configured Gradle compiler. Surrounding controls use platform Android Views; the board uses Canvas/Path and six original botanical silhouettes. JUnit 4.13.2 tests require no Android runtime. No production third-party dependencies beyond Kotlin.

## Structure

- `app/src/main/java/com/tilebloom/game/domain/Game.kt`: immutable tile values, encapsulated engine, geometry, legal pairs, selection, status, fixed level and solution witness.
- `app/src/main/java/com/tilebloom/game/presentation/MainActivity.kt`: screen, counter, terminal feedback, restart, saved-instance restoration.
- `app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt`: responsive rendering, original symbols, explicit drawing order and reverse-order touch testing.
- `app/src/main/res/values/`: English strings, theme and view IDs.
- `app/src/test/`: 67 JVM tests: all original 64 plus three focused match-effect regressions.
- `app/src/androidTest/`: nine compiled-only Android tests for launch, coordinate-tapped victory/deadlock, selection/drag, restart during touch, and activity recreation.
- `tools/host_verify.sh`: standard Linux host verification with logs, exit codes and APK checks.
- `tools/test_host_verify.py`: isolated workflow checks using fake tools, not Android evidence.
- `tools/verify_offline.py`: fallback compiler/JUnit/APK pipeline for this restricted environment.
- `VALIDATION.md`: actual verification outcomes and environment limitations.
- `HOST_VERIFICATION.md`: exact unrestricted Linux host commands and manual device checklist.

## Current verification status

CODEX 06 uses **user-supplied observations of three emulator screenshots** (approximately 382 × 898 pixels): running screen, gold selection, changing board, counters at 10 and 7 pairs, and removed upper tiles. No screenshot files were available for direct inspection. This establishes externally observed execution/progression of an earlier build, not verification of the newly polished APK.

The UI now has a platform-serif title, separate level/pair strip, responsive compact spacing, ivory ceramic rims and quieter shadows, and a rounded forest-green/gold Restart button with ripple feedback. A 150 ms fade/shrink uses visual-only copies after immediate engine removal. Restart, resize and detachment clear effects; recreation restores only game state. Disabled system animators skip the effect. Frame requests stop when copies expire.

All **67 JVM tests pass** using the standalone diagnostic compiler. Nine Android methods compile; **zero have executed here**. The new fallback APK passes alignment/signature checks. Standard Gradle/Kotlin 2.2.10, Lint, new-APK installation, visual review, animation smoothness and compact/font-scale usability remain **NOT VERIFIED**. See VALIDATION.md for exact artifact details and acceptance results. No engine, level, symbol shape or touch geometry changed.

Run this from a normal Linux terminal, with Java 17 and the SDK configured:

```sh
bash tools/host_verify.sh
```

The script runs standard Gradle version/JVM/build/Lint checks, verifies the APK, records command exit codes and preserves logs in a unique `app/build/host-verification/run-*` directory. It returns nonzero on failure and never substitutes the diagnostic fallback. It does not run ADB or claim device success. See [HOST_VERIFICATION.md](HOST_VERIFICATION.md) for installation, launch, instrumentation and collecting device evidence. Ten isolated fake-tool workflow tests pass; these are separate from Android/Gradle verification.

Then complete [PLAYTEST_01.md](PLAYTEST_01.md). Keep human observations empty until a real person plays. Actual results and historical iteration reports are in [VALIDATION.md](VALIDATION.md). CODEX 05 should consume those host logs, Android screenshots and feedback, rather than repeat the diagnosed sandbox startup failures.

## Activity and process state

Ordinary background/foreground retains the existing activity state when Android keeps it alive. Saved instance state encodes accepted pairs plus selection for activity/task recreation; domain replay is JVM-tested across all witness prefixes, victory and a real Level 1 deadlock. Actual Bundle/OS restoration is only compiled-test coverage, not device-verified. There is no file/database persistence: force-stop, task dismissal or a fresh launch need not restore progress.

## Play

Tap an available tile, then another with the same symbol. Tap the selected tile again to deselect; tap a different available symbol to move selection. Unavailable tiles have muted faces and two engraved corner bars. Selection has a gold outline. Matched tiles leave logical state immediately, then fade and shrink visually for 150 ms when system animators are enabled. The counter tracks remaining tile pairs, not currently legal moves. Clear all tiles to see Garden Cleared. A board without legal matching pairs shows No More Matches. Restart / Play Again restores all 24 original tiles and clears selection.

## Authentic availability rules

Every logical footprint is a 1 × 1 rectangle. A tile is covered if **any** remaining tile on **any higher layer** has positive horizontal AND vertical footprint overlap. Same-layer neighbors block the left or right side only when their edges are adjacent and their vertical spans overlap positively. Both sides blocked makes the tile unavailable; one open side suffices. Edge-only and corner-only contacts are not top overlaps. Numerical tolerance is 0.000001 board units.

All selection and matching decisions live in the pure Kotlin engine. Invalid pair requests leave both board and selection untouched. Removed tiles cannot be selected or removed again. Availability and legal pairs are computed from the current tiles, preventing stale caches. Empty board status takes priority over no-moves detection.

## Level 1 and its executable witness

Lower layer: 4 × 4, x/y = 0, 1, 2, 3. Upper layer: 4 × 2, x = 0.5, 1.5, 2.5, 3.5; y = 0.5, 1.5. Upper tiles genuinely cover lower footprints. Six botanical symbols each occur four times.

`LevelOne.witness` pairs upper IDs (16,20), (17,21), (18,22), (19,23), then lower columns left to right: (0,4), (8,12), (1,5), (9,13), (2,6), (10,14), (3,7), (11,15). Symbols cycle through the six types over these twelve pairs. The JVM witness test uses production `Game.match`, checks availability and symbol equality on each move, and requires exactly twelve successes and WON with zero remaining tiles. This proves at least one solution; **other choices can still cause a deadlock**. There is no procedural generator.

## Build and test

Install Java 17 and Android SDK 36 / Build Tools 35.0.0. Set `ANDROID_HOME` or an untracked `local.properties` containing `sdk.dir=/your/sdk`. Dependencies must be cached or downloadable.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The last task needs a running emulator/device with USB debugging enabled. APK: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

For this sandbox only, `python3 tools/verify_offline.py` directly invokes cached Kotlin 1.9.24, JUnitCore, AAPT2, javac, D8, zipalign and apksigner. It produces a debug-signed APK at the same path. It does **not** stand in for a passing Gradle build, Lint, device run or instrumentation run. It resolves the SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or the conventional `$HOME/Android/Sdk`. It discovers all current JVM test classes and clears stale compiled artifacts before verification, preserving the local debug key.

## Limitations and next improvements

One fixed level, no sound, hints, undo, shuffle or progression. English resources establish a translation foundation; no additional locales are included. Custom-board screen-reader navigation is not implemented. Basic standard controls, contrast, silhouettes and selection cues are present. The updated layout, Android lifecycle and animation smoothness still need device validation; no completed human playtest has been received.

The next iteration should first execute the compiled Android interaction tests and inspect compact/large-phone screenshots on an unrestricted host, then address confirmed touch, layout, lifecycle or accessibility defects before adding levels or boosters.

## Git in this managed workspace

The supplied `.git` is an empty, read-only placeholder, so conventional initialization is blocked. The actual repository is stored locally in `.tilebloom-git` (excluded from source control). Use:

```sh
git --git-dir=.tilebloom-git --work-tree=. status
git --git-dir=.tilebloom-git --work-tree=. log --oneline
```

Nothing was pushed or published. On a normal writable filesystem the metadata can be copied into an ordinary `.git` directory, or a normal checkout can be created from this repository. Do not remove the managed placeholder inside this sandbox.

## CODEX 07 — five-garden progression (2026-10-27)

This section supersedes earlier single-level status descriptions. The production `Game.kt`, original LevelOne assignments, solution and deadlock witnesses, and all 67 original JVM tests remain unchanged.

### Architecture and guarantee

`domain/Levels.kt` contains TileSlot, BoardLayout, LevelDefinition, LevelGenerator, LevelValidator, Levels and GardenSession. Android dependencies remain outside this domain. A definition records level ID, seed, generator version, layout, assigned tiles, complete witness and attempt count.

Generation chooses pairs of geometrically available slots using production `Rules.available`, then assigns a balanced shuffled bag of the six existing botanical symbols to those pairs. Validation checks slot identity, finite coordinates, unique IDs, even counts, same-layer non-overlap, supported upper tiles and genuine interlayer overlap. Every witness is replayed using production `Game.match`; acceptance requires an empty board and WON. Every pair must therefore match symbols, contain distinct IDs and be legal before removal. Initial solvability guarantees at least one complete solution; player choices can still deadlock.

Version 1 uses java.util.Random seeded by seed XOR level ID, ordered slot lists and explicit Fisher-Yates library shuffle. The same ID, seed, layout and version reproduce assignments and witness on JVM/Android. Changes to layouts, ordering, random consumption or algorithm require a version change for existing content. This is a bounded foundation, not an unlimited production generator.

There are at most 64 attempts and at most 16 pair removals per attempt for accepted input sizes of 2–32 slots. There is no recursion, exhaustive solver or frame-time generation. Each attempt discards a stuck ordering or invalid candidate. Exhaustion raises a controlled error; there is no unrelated fallback board. The Next action retains the current completed board if preparation fails and shows an error. The verified original level is the restoration fallback. Tiny supported definitions are prepared synchronously; phone timing remains unmeasured.

### Introductory definitions

| Level | Seed | Version | Layout | Tiles | Design intent |
|---|---:|---:|---|---:|---|
| 1 | 0 | 0 (fixed) | Original | 24 | Unchanged introductory reference |
| 2 | 202 | 1 | Terrace | 24 | Wider stepped rows, shallow upper patch |
| 3 | 303 | 1 | Woodland | 28 | Asymmetric rows and distributed upper blocking |
| 4 | 404 | 1 | Courtyard | 30 | Broader upper cover and more spatial observation |
| 5 | 505 | 1 | Woodland | 28 | Return to asymmetric geometry with different pairing decisions |

Four silhouettes including the original are implemented. All use two layers. Counts and cover patterns are difficulty proxies; human difficulty and monotonic strategic progression are NOT established. Level 5 deliberately reduces tile count. No timer, shuffle, undo, fake locked content or durable progression storage is added.

### Navigation, restoration and presentation

Only winning enables explicit Next Level. A completed board remains until Next, Replay Level or the existing restart action. The fifth victory says Garden Journey Complete and hides Next. Restart and replay retain the exact definition and seed. NO_MOVES retains the level, explains that no legal matching pairs remain and offers Restart.

The centered forest-green victory card uses the existing leaf artwork, ivory serif title, encouragement and accessible standard buttons with a restrained 200 ms entrance fade. Existing 150 ms match effects remain, with ghosts outside hit testing. Replacing the board clears effects, pending touch and blocked feedback before binding the next game. Drawing and hit testing use the same geometry, fitted to all initial slots including negative coordinates; removed tiles do not change the transform.

Activity state stores ID, seed, version, accepted move history and selection. Restoration first reconstructs the catalog definition, verifies seed/version and atomically replays moves in Game. Status is derived from the restored engine rather than trusted serialized input. Invalid state falls back to a fresh Level 1. Legacy bundles without identity keys migrate to fixed Level 1 where their history is valid. This is session-based progression with activity recreation support, not durable completion saving.

### Verification scope and limitations

The original nine Android test methods remain; a tenth uses coordinate taps derived from actual view geometry to complete all five levels, advance, restart each new board and check terminal navigation/effect clearing. It does not directly match through the domain engine. Runtime verification is still required for visual balance, compact screen readability, accessibility, centered panel placement, multi-level activity recreation and touch behavior on physical devices.

JVM coverage adds seed determinism/variation, invalid candidates, bounded impossible geometry, all five witnesses, transition/deadlock/final state, restoration, renderer/hit geometry across three viewport sizes and a 120-board matrix (40 seeds × three generated layouts). Every accepted matrix candidate is replayed fully. Existing host scripts are preserved.

Next iteration should prioritize measured puzzle quality: bounded branch analysis, deadlock-risk proxies, representative human playtests, gentler seed curation and phone touch-size measurements before adding more content.

### CODEX 07 executed verification

- Diagnostic production JVM suite: **89 passed, 0 failed** (67 preserved + 22 new); JUnit execution 0.394 s on this host.
- Representative matrix: 120 accepted boards, all replayed through Game; 89 ms total on this host, maximum two attempts. These are JVM host timings, not Android device measurements.
- Initial available tiles / matching choices: L1 6/3, L2 11/6, L3 10/5, L4 9/5, L5 10/6. These counts do not prove human difficulty or a monotonic challenge curve.
- Host verification script regression suite: **10 passed, 0 failed**.
- Standard Gradle request (`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`) could not start: wrapper lock under ~/.gradle is read-only. Standard tests, assemble and lint are **NOT EXECUTED**; the wrapper invocation failed before task execution.
- ADB device discovery failed because its server cannot bind a socket (Operation not permitted). Updated Android launch, installation, connected instrumentation and visual review are **NOT EXECUTED**. Earlier user screenshots cover CODEX 06 only.
- An initial overlapping diagnostic build caused shared-output errors; that result is discarded. A later compact-scroll compile error was corrected; only the final successful serial diagnostic build is artifact evidence.
- Victory content is scrollable within the play area for compact displays; actual phone readability remains NOT VERIFIED.

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
