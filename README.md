# TileBloom — Mahjong Garden Puzzle

CODEX 03 / application version 0.1: an offline, portrait Android Mahjong Solitaire prototype with **one deterministic level**. No accounts, permissions, services, advertising, analytics, or network access.

## Stack

Kotlin 2.2.10, Java 17, Android Gradle Plugin 8.10.0, Gradle wrapper 8.13, compile/target SDK 36, minimum SDK 26, Build Tools 35.0.0. The diagnostic fallback intentionally retains its cached Kotlin 1.9.24 compiler; it does not verify the configured Gradle compiler. Surrounding controls use platform Android Views; the board uses Canvas/Path and six original botanical silhouettes. JUnit 4.13.2 tests require no Android runtime. No production third-party dependencies beyond Kotlin.

## Structure

- `app/src/main/java/com/tilebloom/game/domain/Game.kt`: immutable tile values, encapsulated engine, geometry, legal pairs, selection, status, fixed level and solution witness.
- `app/src/main/java/com/tilebloom/game/presentation/MainActivity.kt`: screen, counter, terminal feedback, restart, saved-instance restoration.
- `app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt`: responsive rendering, original symbols, explicit drawing order and reverse-order touch testing.
- `app/src/main/res/values/`: English strings, theme and view IDs.
- `app/src/test/`: 64 JVM tests: 31 original engine tests, 16 shared-geometry tests, 10 reliability/restoration tests, and 7 gesture tests.
- `app/src/androidTest/`: eight compiled-only Android tests for launch, coordinate-tapped victory/deadlock, selection/drag, restart during touch, and activity recreation.
- `tools/verify_offline.py`: fallback compiler/JUnit/APK pipeline for this restricted environment.
- `VALIDATION.md`: actual verification outcomes and environment limitations.
- `HOST_VERIFICATION.md`: exact unrestricted Linux host commands and manual device checklist.

## CODEX 02 changes and actual verification

The original engine rules, all 31 original tests, exact 24-tile layout, six symbols and twelve-pair witness are preserved. No dependencies or board balance changed.

- Shared pure Kotlin `presentation/board/BoardGeometry.kt` supplies face, edge, shadow, scale, centering, padding, coordinate transforms and deterministic stacking order. Rendering and hit testing consume the same geometry. The visible topmost physical tile consumes the tap even when legally unavailable. Decorative shadows do not receive touches.
- `TileTapGesture.kt` requires the same pointer and visible tile at press/release. Dragging beyond slop (including a move away and back), cancellation, pointer changes, and restart invalidate the gesture. Android also checks MotionEvent historical samples.
- Selection has a restrained double gold outline inside the tile face. Blocked faces are lighter, retain engraved corner markers, and show a 160 ms border response on tap. Restart, resize and detachment clear transient feedback and pending gestures.
- Matching removal stays immediate. No disappearance animation or animation-dependent game state was introduced.
- Saved instance state now records the exact legal move history and selection. Restoration replays production validation atomically on a candidate board instead of guessing pairings from a removed-ID set. Corrupt snapshots do not partially restore the game. The volatile CODEX 01 removed-ID Bundle format is not migrated; this is not durable saving or an upgrade-persistence guarantee.
- Instruction text is 16 sp; counter and status are polite accessibility live regions. The board exposes a localized remaining/selected summary. Individual screen-reader tile navigation remains unimplemented, and TalkBack behavior is unverified.

**Verification:** 63/63 JVM tests passed. Six Android test methods compiled but **were not executed**. A valid debug APK was built through the standalone fallback; Gradle and ADB remain blocked by sandbox filesystem/socket restrictions. Full Android launch, touch delivery, screenshots and lifecycle behavior remain unverified. See [VALIDATION.md](VALIDATION.md) and [HOST_VERIFICATION.md](HOST_VERIFICATION.md).

## CODEX 03 result

64/64 JVM tests passed with the diagnostic compiler; eight Android methods compiled, **zero executed**. Standard Gradle tasks and Android Lint remain blocked before configuration; ADB server socket creation is denied. No Android launch, screenshot, human playtest or responsive device validation is claimed.

Fixed historical movement handling on final touch release. Added regression checks for batched release movement, rapid touches during feedback/after restart, and invalid/removed selections. Gold selection and blocked feedback are preserved. Removal remains immediate until Android runtime evidence supports a safe animation.

Configured Kotlin 2.2.10 supports Gradle through 8.14 and AGP through 8.10.0 according to the [Kotlin compatibility matrix](https://kotlinlang.org/docs/gradle-configure-project.html). [AGP 8.10](https://developer.android.com/build/releases/agp-8-10-0-release-notes) supports API 36, Java 17 and requires Gradle at least 8.11.1. Gradle remains 8.13. This fixes the prior unsupported matrix without changing the engine or SDK. AGP 8.10.0 is not cached here; the host may need to download it. Actual Gradle configuration/compilation remains unverified.

See [PLAYTEST_01.md](PLAYTEST_01.md) for the first human session and [HOST_VERIFICATION.md](HOST_VERIFICATION.md) for build/install commands. Earlier CODEX 02 details below describe that iteration, not new runtime evidence.

## Activity and process state

Ordinary background/foreground retains the existing activity state when Android keeps it alive. Saved instance state encodes accepted pairs plus selection for activity/task recreation; domain replay is JVM-tested across all witness prefixes, victory and a real Level 1 deadlock. Actual Bundle/OS restoration is only compiled-test coverage, not device-verified. There is no file/database persistence: force-stop, task dismissal or a fresh launch need not restore progress.

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

For this sandbox only, `python3 tools/verify_offline.py` directly invokes cached Kotlin 1.9.24, JUnitCore, AAPT2, javac, D8, zipalign and apksigner. It produces a debug-signed APK at the same path. It does **not** stand in for a passing Gradle build, Lint, device run or instrumentation run. It resolves the SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or the conventional `$HOME/Android/Sdk`. It discovers all current JVM test classes and clears stale compiled artifacts before verification, preserving the local debug key.

## Limitations and next improvements

One fixed level, immediate removal, no sound, hints, undo, shuffle or progression. English resources establish a translation foundation; no additional locales are included. Custom-board screen-reader navigation is not implemented. Basic standard controls, contrast, silhouettes and selection cues are present. Runtime layout, Android lifecycle and touch usability still need device validation; no human playtesting has occurred.

The next iteration should first execute the compiled Android interaction tests and inspect compact/large-phone screenshots on an unrestricted host, then address confirmed touch, layout, lifecycle or accessibility defects before adding levels or boosters.

## Git in this managed workspace

The supplied `.git` is an empty, read-only placeholder, so conventional initialization is blocked. The actual repository is stored locally in `.tilebloom-git` (excluded from source control). Use:

```sh
git --git-dir=.tilebloom-git --work-tree=. status
git --git-dir=.tilebloom-git --work-tree=. log --oneline
```

Nothing was pushed or published. On a normal writable filesystem the metadata can be copied into an ordinary `.git` directory, or a normal checkout can be created from this repository. Do not remove the managed placeholder inside this sandbox.
