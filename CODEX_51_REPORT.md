# CODEX 51 — Interactive Level 1 tutorial

Implementation is finished. Final cached JVM regressions and Android source/resource compilation pass. Standard Gradle tasks, device interaction, screenshots, and a fresh signed APK remain blocked by the host restrictions described below. The interrupted session's 318-test result is not used as final evidence.

| Requested item | Implementation and verification |
| --- | --- |
| A. Overview | A short tutorial uses the existing Level 1 board, production selection/matching engine, tray, Undo and Hint controls, artwork, audio policy and animations. Botanical spotlight, ivory bubble, gold pulsing outline, fade, and 48dp action buttons. |
| B. Introduction | Compact welcome with Start and Skip appears when a player with no completed levels enters a pristine Level 1. Saved games above Level 1 and already-played attempts do not receive mandatory guidance. No level regeneration or frontier reset. |
| C. Selection | The spotlight accepts the highlighted tile's real touch sequence. Both availability and the tutorial target must pass. Selection is never automatic. |
| D. Tray | The actual four-slot tray is highlighted after the first collection. Short text explains four slots and two identical tiles. Continue preserves the collected tile. |
| E. Undo | The real Undo button restores the first collection through the engine. The controller grants the exemption only during DEMONSTRATE_UNDO and only with the exact expected board history. The step changes atomically, preventing repeated free Undo. |
| F. Hint | The real Hint button runs existing HintPolicy and uses the normal hint animation/audio. Controller authorization requires DEMONSTRATE_HINT, the expected snapshot, and the actual policy suggestion. Credits are unchanged; no balance is topped up. |
| G. Pair | The tutorial opening is derived from the real Hint result. Its matching partner must be legal, identical, and allow a complete replay of the remaining verified witness. The shipped Level 1 uses 18 → 17. Both collections and automatic matching are real. Completion leaves the same board in play. |
| H. Skip | Skip is available throughout guidance. It preserves accepted moves, puzzle identity and wallets, records SKIPPED, and cancels outstanding Hint requests so an asynchronous guided result cannot become a paid normal Hint. Android Back skips guidance; Back/Exit in practice closes practice. |
| I. How to Play | Settings retains Sound Effects, Language and Privacy, and adds a help entry. It launches another MainActivity with a temporary ProgressController, keeping the previous Activity/navigation intact. Practice finishes or exits back to that screen. |
| J. Persistence | Save schema 8 adds stable tutorial step names alongside the board snapshot. Schemas 1–7 remain supported. Restoration validates each unfinished step against the board; inconsistent guidance exits without rebuilding the puzzle. FINISHED/SKIPPED never automatically begin again. Practice state uses its own saved-instance Bundle. |
| K. Credit/advertising safety | Guided actions bypass consumption only in their exact controller steps. Normal consumption resumes after finish/skip. Reward grants and reward UI are blocked during guidance. Practice does not attach ad SDK hosts, request rewards, save exposure, record victories, advance GameStore, or persist its controller. Launching practice also preserves a real game's currently revealed mystery tile rather than invoking the ordinary background hide. Normal background behavior remains in place otherwise. |
| L. Languages | All 19 tutorial/help strings exist in English, French, Spanish, Brazilian Portuguese, Arabic, Hindi, Japanese and German. Text uses resources and inherited RTL; the board retains its existing geometry direction. Bubbles scroll and choose the side with room; narrow/enlarged text layouts stack actions. Arabic rendering still requires device acceptance. |
| M. Android screenshots | NOT VERIFIED. No screenshots were captured or inspected in this run. French/Arabic instrumentation scenarios capture welcome, tile, tray, Undo, Hint, pair, completion and Settings, assert target/bubble separation, and repeat practice. They also include a Level 37 revealed-mystery preservation case. These scenarios compile but could not execute. |
| N. Automated checks | Final cached JUnit run: **320 tests passed, 145.038 seconds**, including nine tutorial tests. Final main + all Android test sources compile using cached Kotlin/JDK 17; AAPT2 compiles/links all resources. These independent diagnostics do not execute Gradle or Android instrumentation. Existing gameplay device tests use returning-player fixtures so their original gameplay assertions remain applicable. |
| O. APK | No fresh APK was generated. The pre-existing APK predates the resumed work and is not a CODEX 51 deliverable. Fresh timestamp, size, SHA-256, package, alignment, signature and packaged tutorial resources are NOT VERIFIED. Application ID remains `com.tilebloom.game`; signing/build configuration was not changed. Required certificate remains `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. |
| P. Git | Existing `.tilebloom-git` history is used. Only CODEX 51 source, tests, tutorial string additions and evidence/report are staged for `feat: add interactive level one tutorial with hint and undo`. The eight pre-existing `continue_garden` edits and unrelated reports/docs remain outside this commit. No push or publication. |
| Q. Limitations | Standard unit-test task, assembly and lint cannot start; device lifecycle/touch/audio/RTL acceptance and screenshot inspection remain outstanding. Practice lifecycle preservation is source-checked and covered by compiled device tests, not a claimed device pass. An incompatible old tutorial puzzle fails safely to ordinary play. |

The separate production-controller replay passed selection → free Undo → free Hint → identical pair → encode/decode → remaining legal selections → WON. Its complete sequence is:

`18, 17, 23, 15, 21, 16, 11, 19, 14, 0, 7, 1, 3, 10, 20, 4, 6, 5, 22, 2, 13, 9, 12, 8`

The controller remains on Level 1; both credit balances stay at 3. This replay proves the remaining puzzle is solvable; tutorial completion itself only removes the demonstrated pair.

Evidence is in [docs/codex51](docs/codex51/): `jvm-tests-final.log`, `diagnostic-compile-final.log`, `production-replay.txt`, three required Gradle task logs, `gradle-writable.log` and `adb.log`. `WitnessReplayTest.kt` is a helper file without a test class; the cached runner excludes it as a runnable class while compiling it with the suite.

All three requested wrapper commands were attempted independently. Each fails before task execution with:

```
java.io.FileNotFoundException:
/home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck
(Read-only file system)
```

An installed-Gradle attempt with a writable `/tmp` cache also fails before building: `Could not determine a usable wildcard IP for this machine` while creating `FileLockContentionHandler`. ADB reports `could not install *smartsocket* listener: Operation not permitted`. No install, uninstall, data clearing, signing-key change or progress reset was performed.

On an unrestricted host with JDK 17, run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --console=plain :app:testDebugUnitTest
./gradlew --no-daemon --console=plain :app:assembleDebug
./gradlew --no-daemon --console=plain :app:lintDebug
./gradlew --no-daemon --console=plain :app:assembleDebugAndroidTest
bash tools/host_verify.sh
```

The existing host verification script checks the fresh APK, package/ad resources, alignment and required signing certificate. To inspect the outputs directly:

```bash
stat app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk
"$HOME/Android/Sdk/build-tools/35.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
"$HOME/Android/Sdk/build-tools/35.0.0/zipalign" -c -v 4 app/build/outputs/apk/debug/app-debug.apk
"$HOME/Android/Sdk/build-tools/35.0.0/aapt" dump badging app/build/outputs/apk/debug/app-debug.apk
"$HOME/Android/Sdk/build-tools/35.0.0/aapt2" dump resources app/build/outputs/apk/debug/app-debug.apk
"$HOME/Android/Sdk/build-tools/35.0.0/aapt2" dump configurations app/build/outputs/apk/debug/app-debug.apk
```

After the certificate matches, install with `adb install -r` to retain data; install the matching test APK the same way. On a narrow emulator, execute:

```bash
adb shell am instrument -w -e class com.tilebloom.game.TutorialInteractionTest com.tilebloom.game.test/android.test.InstrumentationTestRunner
adb pull /sdcard/Android/data/com.tilebloom.game/files/tutorial51 docs/codex51/screenshots
```

Inspect those actual screenshots before claiming French/Arabic visual acceptance. Test actual activity recreation, app interruption/resume, Skip during pending Hint, and returning from practice to Home/gameplay on device. The test suite uses separate progress/exposure namespaces, not player-data resets.
