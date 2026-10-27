# TileBloom — Mahjong Garden Puzzle

CODEX 01 / version 0.1: an offline, portrait Android Mahjong Solitaire prototype with **one deterministic level**. No accounts, permissions, services, advertising, analytics, or network access.

## Stack

Kotlin 1.9.24, Java 17, Android Gradle Plugin 8.12.1, Gradle wrapper 8.13, compile/target SDK 36, minimum SDK 26, Build Tools 35.0.0. Kotlin 1.9.24 was selected to match the locally available standalone compiler as well as the Gradle plugin. Surrounding controls use platform Android Views; the board uses Canvas/Path and six original botanical silhouettes. JUnit 4.13.2 tests require no Android runtime. No production third-party dependencies beyond Kotlin.

## Structure

- `app/src/main/java/com/tilebloom/game/domain/Game.kt`: immutable tile values, encapsulated engine, geometry, legal pairs, selection, status, fixed level and solution witness.
- `app/src/main/java/com/tilebloom/game/presentation/MainActivity.kt`: screen, counter, terminal feedback, restart, saved-instance restoration.
- `app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt`: responsive rendering, original symbols, explicit drawing order and reverse-order touch testing.
- `app/src/main/res/values/`: English strings, theme and view IDs.
- `app/src/test/`: 31 JVM rule and interaction-state tests.
- `app/src/androidTest/`: launch/restart smoke test using the platform instrumentation runner.
- `tools/verify_offline.py`: fallback compiler/JUnit/APK pipeline for this restricted environment.
- `VALIDATION.md`: actual verification outcomes and environment limitations.

## Play

Tap an available tile, then another with the same symbol. Tap the selected tile again to deselect; tap a different available symbol to move selection. Unavailable tiles have muted faces and two engraved corner bars. Selection has a gold outline. Tiles disappear immediately after a validated match. The counter tracks remaining tile pairs, not currently legal moves. Clear all tiles to see Garden Cleared. A board without legal matching pairs shows No More Matches. Restart / Play Again restores all 24 original tiles and clears selection.

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

For this sandbox only, `python3 tools/verify_offline.py` directly invokes cached Kotlin 1.9.24, JUnitCore, AAPT2, javac, D8, zipalign and apksigner. It produces a debug-signed APK at the same path. It does **not** stand in for a passing Gradle build, Lint, device run or instrumentation run. Its SDK default is environment-specific; override with `ANDROID_HOME`.

## Limitations and next improvements

One fixed level, immediate removal, no sound, hints, undo, shuffle or progression. English resources establish a translation foundation; no additional locales are included. Custom-board screen-reader navigation is not implemented. Basic standard controls, contrast, silhouettes and selection cues are present. Runtime layout, Android lifecycle and touch usability still need device validation; no human playtesting has occurred.

CODEX 02 should prioritize device/emulator validation on compact and large portrait screens, automated actual-touch witness playback, lifecycle restoration tests, clearer blocked-tile feedback, and accessibility exploration before adding levels or boosters.

## Git in this managed workspace

The supplied `.git` is an empty, read-only placeholder, so conventional initialization is blocked. The actual repository is stored locally in `.tilebloom-git` (excluded from source control). Use:

```sh
git --git-dir=.tilebloom-git --work-tree=. status
git --git-dir=.tilebloom-git --work-tree=. log --oneline
```

Nothing was pushed or published. On a normal writable filesystem the metadata can be copied into an ordinary `.git` directory, or a normal checkout can be created from this repository. Do not remove the managed placeholder inside this sandbox.
