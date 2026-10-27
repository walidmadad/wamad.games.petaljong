# CODEX 03 verification — 2026-10-27

## Outcome and audit

Continued commit `855673d4c0cb35205d450beb2ac37b323b8180b6` with a clean initial working tree. Reviewed README, prior validation/host instructions, all production Kotlin, manifests/resources, Gradle/wrapper configuration, standalone build pipeline, all 63 JVM tests and six original instrumentation tests before changes. Original engine, symbols, board geometry, level witness and history remain intact.

Confirmed weaknesses: Kotlin 1.9.24 was outside its fully supported Gradle/AGP range; final ACTION_UP ignored historical movement while ACTION_MOVE checked it. The latter allowed a constructed batched release containing an out-and-back movement to complete selection. No Android-observed visual defect is claimed.

Fixed release handling to consume historical samples before completing a tap. Added an Android regression that dispatches a historical release through the actual view, and another for repeated screen touches during feedback/after restart. Added one JVM regression checking repeated removed/invalid selections preserve accepted move history and selection, then a match after restart. Original tests were retained. No second legality system, animation dependency, new symbol or level was added. Gold selection and 160 ms blocked feedback are unchanged. Pair removal stays immediate because Android animation/cancellation reliability cannot be verified here.

## Toolchain and compatibility

- Runtime Java: OpenJDK 17.0.20.1+1-1-24.04-Ubuntu.
- Configured Kotlin Gradle plugin: **2.2.10** (was 1.9.24).
- Configured Android Gradle Plugin: **8.10.0** (was 8.12.1).
- Gradle wrapper/distribution: **8.13**, verified with writable temporary Gradle home. Its embedded Kotlin 2.0.21 is Gradle's DSL compiler, not the app plugin.
- Compile/target API **36** (installed platform revision 2, extension 17), minimum **26**, Build Tools **35.0.0**, JVM target **17**.
- Diagnostic fallback compiler/stdlib remain cached Kotlin **1.9.24**, JUnit **4.13.2**. This does not test KGP 2.2.10.

Official [Kotlin matrix](https://kotlinlang.org/docs/gradle-configure-project.html) lists KGP 2.2.0–2.2.10 with Gradle 7.6.3–8.14 and AGP 7.3.1–8.10.0. [AGP 8.10 requirements](https://developer.android.com/build/releases/agp-8-10-0-release-notes) cover API 36, Java 17, Build Tools 35.0.0 and Gradle minimum 8.11.1. Thus the configured versions fall within the published ranges. Two plugin pins changed; wrapper/SDK/engine remain unchanged. AGP 8.10.0 is not cached locally and may require host download. Actual dependency resolution and Gradle compilation are NOT VERIFIED.

## Gradle results

Each initial command exited 1 before configuration:

```sh
./gradlew --version
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Exact blocker: wrapper cannot create `/home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck` (Read-only file system).

Changed the relevant filesystem condition using the existing cached distribution in writable `/tmp/tilebloom-gradle`:

```sh
GRADLE_USER_HOME=/tmp/tilebloom-gradle ./gradlew --version
GRADLE_USER_HOME=/tmp/tilebloom-gradle ./gradlew --offline --no-daemon \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest
```

Version command succeeded (Gradle 8.13). Task command exited 1: `Could not create service of type FileLockContentionHandler` / `Could not determine a usable wildcard IP for this machine.` The task attempt was performed before and after final toolchain/source changes; both failed before configuration. No source compilation error or successful Gradle build is inferred. Lint: NOT EXECUTED. Session policy automatically rejects sandbox approvals; no supported execution escalation is available. No security restriction was bypassed.

## Android runtime and instrumentation

`/home/walid/Android/Sdk/platform-tools/adb devices -l` exited 1. ADB 1.0.41 / 37.0.1-15733141 could not install its smartsocket listener: `Operation not permitted`; it could not connect to its daemon. Device availability cannot be established. Emulator profiles listed: `11.0`, `Medium_Phone`, `Medium_Tablet_2`. `/dev/kvm` is absent. Starting an emulator cannot supply a usable ADB connection under this socket restriction, so no emulator launch/install was attempted through the blocked connection.

**INSTRUMENTATION TESTS: NOT EXECUTED.** Eight methods compiled; executed 0, passed 0, failed 0, skipped 0. Compilation is not execution. All original six tests remain. Instrumentation compilation emits the existing deprecated platform test API warning.

Actual application installation, Android launch, complete UI witness/deadlock, crash inspection, screenshot review, compact/typical/large phone usability, font scaling, background return and Bundle/process recreation: **NOT VERIFIED**. No screenshot was captured; no visual observations or human feedback are invented.

## Regression and artifact results

Command: `python3 tools/verify_offline.py` — exit 0, `OK (64 tests)`.

| Suite | Executed | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Original GameTest | 31 | 31 | 0 | 0 |
| BoardGeometryTest | 16 | 16 | 0 | 0 |
| GameReliabilityTest | 10 | 10 | 0 | 0 |
| TileTapGestureTest | 7 | 7 | 0 | 0 |
| Total JVM | 64 | 64 | 0 | 0 |

Twelve-pair Level 1 solution and known five-pair deadlock continue passing. Geometry proves topmost hit ownership, no blocked-upper tap-through, edges/boundaries, removal/restart and coordinate replay. Gesture tests prove slop, cancellation, pointer and target consistency. Engine tests prove legal-only removal, exact history, invalid state rejection, duplicate protection and restoration at all witness prefixes, victory and deadlock. These are JVM facts; actual Android delivery, feedback timing and OS restoration remain unverified.

Final debug APK:

- `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`
- **709011 bytes**, fallback build (not Gradle).
- SHA-256 `5df5cbb551d37d48956570d7c44f72991cb03a5f2710b95357f17f69eecc64d2`.
- `zipalign -c 4` passed; `apksigner verify --verbose` passed v2/v3, one signer. v1/v4 absent; minimum API 26 supports v2.
- AAPT2 badging confirms package `com.tilebloom.game`, version code 1/name 0.1, minimum 26, target 36, debuggable, portrait, launch activity `com.tilebloom.game.presentation.MainActivity`, no requested permissions.

## CODEX 03 acceptance criteria

Device-dependent behavior is deliberately NOT VERIFIED even where JVM coverage passes.

| # | Criterion | Result | Evidence / limitation |
| --- | --- | --- | --- |
| 01 | Original JVM tests | PASS | All 63 retained and passing. |
| 02 | Complete Level 1 solution | PASS | Engine and geometry witness. |
| 03 | Known deadlock | PASS | Exact fixed-level sequence. |
| 04 | Touch geometry | PASS | 16 shared geometry JVM tests; Android delivery unverified. |
| 05 | No visible-upper tap-through | NOT VERIFIED | Shared production hit testing JVM coverage; actual UI unverified. |
| 06 | Selection/deselection | NOT VERIFIED | Engine/gesture JVM coverage; actual UI unverified. |
| 07 | Rapid input cannot corrupt game | NOT VERIFIED | Repeated engine selections/duplicate protection; Android delivery unverified. |
| 08 | Restart restores board | NOT VERIFIED | Exact engine/geometry tests; button execution unverified. |
| 09 | Activity state restoration | NOT VERIFIED | Domain replay passes; actual Bundle/OS unexecuted. |
| 10 | Standard Gradle or exact blocker | PASS | Both filesystem and startup/socket blockers recorded; Gradle did not build. |
| 11 | Valid debug APK | PASS | Fallback artifact aligned/signature checked. |
| 12 | Instrumentation execution | NOT VERIFIED | Eight compiled; zero executed. |
| 13 | Android launch | NOT VERIFIED | ADB socket denied. |
| 14 | Android visual inspection | NOT VERIFIED | No screenshot. |
| 15 | Compact-screen usability | NOT VERIFIED | Mathematical viewport coverage only. |
| 16 | Gameplay feedback reliability | NOT VERIFIED | Timer/real touch runtime unexecuted. |
| 17 | Existing Git history | PASS | Alternative metadata preserved; new commit descends from 855673d. |
| 18 | Accurate documentation | PASS | Current results, host steps and blank human checklist recorded. |

## Remaining priorities and playtest readiness

1. Run standard Gradle tests/build/Lint on host, then install/launch and execute eight instrumentation tests. Resolve actual failures before extending scope.
2. Inspect compact/typical/large Android screenshots and large fonts; verify activity/process restoration and real feedback timing. No device-dependent acceptance is claimed.
3. Conduct PLAYTEST_01.md with a person; evaluate symbol distinction, touch comfort, selection clarity and meaningful decisions. The signed APK and instructions are ready for a first exploratory human playtest, conditional on successful host installation/launch. The application is not fully Android-validated.
4. Individual screen-reader tile actions remain absent; no durable save, animation or progression exists.

CODEX 04 should close Android runtime evidence gaps first and make focused changes driven by observed usability and human feedback. Add a short removal transition only after runtime cancellation/restart behavior can be verified. No new levels or boosters are recommended yet.

Updated README.md, HOST_VERIFICATION.md and PLAYTEST_01.md. Git continues using `.tilebloom-git`; no reinitialization, rewrite, push or publication. This report is committed with the implementation; see `git --git-dir=.tilebloom-git --work-tree=. log -1` for the resulting hash.

---

# Historical results (superseded by CODEX 03 above)

# CODEX 02 verification — 2026-10-27

## Summary

Continued CODEX 01, preserving commit `f451d028cfbbc48940ee232ef25807205b039b4d`, all 31 original tests, original Mahjong rules, deterministic geometry, six symbols and solution witness. No dependency upgrade, new level, booster, analytics or external service was introduced.

The final source was independently compiled and packaged, and **63 JVM tests passed**. Android runtime execution and standard Gradle verification remain blocked. This is not a production-readiness claim.

## Initial audit and changes

The original engine's overlap, side-adjacency, match validation, state counts, status ordering and witness were retained. Drawing and reverse hit ordering already respected tile layers. The main geometry risk was duplicated render/hit calculations with no direct tests; a shared pure Kotlin geometry component now supplies both. It includes padding, rounded faces, visible solid edges, noninteractive shadows, and zero-size protection.

Confirmed gesture weakness: CODEX 01 checked only press/release displacement, allowing a drag out and back to count as a tap. The new gesture tracks movement and identity for the full gesture, including Android historical samples. Pointer changes, cancellation, changing targets, restart, resize and detachment prevent a stale tap.

Selection has two inset gold borders; blocked tiles retain readable lighter faces and engraved markers. A blocked tap briefly changes its border for 160 ms. No match disappearance animation was added; pair removal is immediate and authoritative in the engine. A callback clears only transient presentation state, never board legality or counts.

Saved state records exact accepted pair history instead of re-pairing a set of removed IDs. Restoration validates the entire history and selection on a candidate game before applying it. The new volatile Bundle format does not migrate CODEX 01 saved-instance Bundles. There is no durable save system. Added localized board summaries, polite counter/status live regions, and 16 sp instructions; individual accessible tile actions and TalkBack verification remain outstanding.

## Final JVM and compile results

Command: `python3 tools/verify_offline.py`.

| Suite | Executed | Passed | Failed | Skipped | Method |
| --- | ---: | ---: | ---: | ---: | --- |
| Original GameTest | 31 | 31 | 0 | 0 | Kotlin 1.9.24 + JUnitCore 4.13.2 |
| BoardGeometryTest | 16 | 16 | 0 | 0 | JVM, shared production geometry |
| GameReliabilityTest | 9 | 9 | 0 | 0 | JVM, production game engine |
| TileTapGestureTest | 7 | 7 | 0 | 0 | JVM, production gesture component |
| **JVM total** | **63** | **63** | **0** | **0** | `OK (63 tests)` |
| Android instrumentation | 0 | 0 | 0 | 0 | Six methods compiled; **NOT EXECUTED**, not reported as skipped/passed |

Geometry checks exercise visible centers/borders/edges, rounded-corner and shadow exclusions, gaps/empty space, upper-over-lower ordering, unavailable upper targets, transform round-trips, padding, scale changes, removal, restart and a full coordinate-to-selection witness. The tested board viewports are 284×260, 376×480 and 720×960 view-local units, with an additional padded viewport. These are mathematical layout tests, not Android screenshots or complete phone-UI measurements.

Reliability checks include repeated restart after one/several matches and selection, victory, a real Level 1 deadlock, exact state restoration across all thirteen witness prefixes, selection/history preservation, invalid snapshot rejection and defensive history copies. The fixed-level deadlock `(12,15), (14,23), (16,20), (13,21), (0,4)` is verified by the engine and leaves fourteen tiles. The twelve-pair winning witness still passes.

Six instrumentation methods compile against the target app and Android test libraries: launch/restart, full coordinate-tapped witness and victory/Play Again, coordinate-tapped deadlock/restart, selection/blocked taps/drag cancellation, restart during a pending gesture/feedback, and activity recreation. They inject screen coordinates calculated from shared geometry plus view location, and do not invoke engine match/select methods. The existing deprecated platform instrumentation API produces a compile warning; no new Android test dependency was added.

## Gradle attempts and classification

Executed each requested command once:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

All exited 1 before any project/task execution because the shared wrapper lock path is read-only:

```text
java.io.FileNotFoundException:
/home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck
(Read-only file system)
```

One diagnostic retry used the already-prepared writable temporary Gradle home and cached distribution:

```sh
GRADLE_USER_HOME=/tmp/tilebloom-gradle ./gradlew --offline --no-daemon \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

It exited 1 before compilation:

```text
Could not create service of type FileLockContentionHandler
Could not determine a usable wildcard IP for this machine.
```

Classification: **E — development environment filesystem/socket restrictions**. This result establishes neither project configuration success nor a configuration/source/dependency failure. No Gradle task completed; Android Lint is NOT VERIFIED. The session permission policy automatically rejects sandbox-approval requests, so no authorized escalation mechanism was available. No security setting or global configuration was changed.

Compatibility review: Java 17, Gradle 8.13, Build Tools 35.0.0 and API 36 align with [AGP 8.12 requirements](https://developer.android.com/build/releases/agp-8-12-0-release-notes). Kotlin 1.9.24 is beyond its [fully supported Gradle/AGP version matrix](https://kotlinlang.org/docs/gradle-configure-project.html). This remains a compatibility caveat for the unrestricted host build; versions were preserved rather than upgrading without a demonstrated failure.

## Device/emulator result

`/home/walid/Android/Sdk/platform-tools/adb devices -l` exited 1:

```text
could not install *smartsocket* listener: Operation not permitted
adb: failed to check server version: cannot connect to daemon
```

`emulator -list-avds` lists `11.0`, `Medium_Phone`, `Medium_Tablet_2`; `/dev/kvm` is absent. No device could be enumerated and no usable ADB connection existed. Installation, launch, instrumentation, screenshots, logcat and real touch/lifecycle checks were not performed in CODEX 02. Retrying install/launch through the known unavailable server would not provide new evidence.

Use [HOST_VERIFICATION.md](HOST_VERIFICATION.md) for exact unrestricted-host Gradle, ADB, installation, launch, instrumentation, screenshot and crash-log commands, plus compact/large phone and lifecycle procedures.

## APK output

- Path: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`
- Size: **709011 bytes**.
- SHA-256: `092fffca8784a13bc6e05a653fb1168662b177e7b96e8d59ee250ee33654d3ec`.
- Method: cached Kotlin 1.9.24 / javac 17, AAPT2, D8, zipalign, apksigner via `tools/verify_offline.py`.
- ZIP alignment passed; APK v2 and v3 signatures verified.
- AAPT metadata: `com.tilebloom.game`, version 0.1/code 1, minimum API 26, target 36, debuggable, expected launch activity, no requested permissions.

This valid APK is a fallback artifact, **not evidence of a passing Gradle build or successful Android launch**. The fallback now clears stale compiled output, discovers all current JVM test classes, and keeps its debug signing identity in ignored build output.

## CODEX 02 acceptance audit

PASS below means the stated automated evidence passed. Pure JVM evidence does not establish actual Android delivery, drawing, accessibility or OS behavior.

| # | Criterion | Status | Evidence / limit |
| --- | --- | --- | --- |
| 01 | Original JVM tests | PASS | All 31 unchanged original tests passed. |
| 02 | Level 1 solution | PASS | Twelve production-engine matches and shared-geometry selection replay. |
| 03 | New touch geometry tests | PASS | Sixteen geometry and seven gesture tests passed. |
| 04 | Blocked tiles cannot select | PASS | Engine and unavailable-upper-target JVM tests; Android delivery unverified. |
| 05 | No upper-tile tap-through | PASS | Production shared geometry returns topmost tile regardless of legality; Android delivery unverified. |
| 06 | Selection/deselection consistency | PASS | Engine/gesture tests; Android test compiled only. |
| 07 | Remove exactly once | PASS | Original invalid/duplicate/self-match and complete replay tests. |
| 08 | Accurate pair counter | PASS | Production counts tested; Android text binding compiled but not run. |
| 09 | Victory/no moves | PASS | Original tests plus fixed-level win and deadlock; Android feedback unverified. |
| 10 | Complete restart | PASS | Repeated exact-board comparisons, history/selection/count/status checks. |
| 11 | Visual consistency during effects | NOT VERIFIED | No removal animation; brief blocked-border timer/cleanup awaits Android execution. |
| 12 | Valid debug APK | PASS | Compiled, packaged, aligned and signature-verified fallback APK. |
| 13 | Gradle or exact blocker | PASS | Required commands attempted, exact external blocker and host handoff documented; Gradle itself did not pass. |
| 14 | Android runtime verification | NOT VERIFIED | ADB socket denied; no launch. |
| 15 | Honest instrumentation reporting | PASS | Six compiled methods, zero executed. |
| 16 | Preserve history / commit | PASS | Existing alternative repository retained; CODEX 02 committed as a child of CODEX 01 after available verification. |

## Remaining limitations and priorities

1. **High:** unrestricted-host Gradle/Lint and actual Android installation, launch and coordinate tests still need execution. No startup-crash or human usability evidence exists.
2. **Medium:** Kotlin plugin compatibility beyond the published fully supported range requires a real host Gradle result. Activity/Bundle/process recreation, feedback timing and actual visual fit remain unverified.
3. **Medium:** individual screen-reader tile navigation/actions are absent. Live-region/summary behavior is unverified with TalkBack.
4. **Scope limitations:** one deterministic level, intentional possible deadlocks, immediate match removal, no durable save, no hints/shuffle/undo. No human playtest findings are claimed.

Git continues using `.tilebloom-git` because `.git` is a read-only placeholder. No metadata replacement, global Git change, history rewrite, push or publication occurred. Build outputs and signing keys remain ignored.

---

# Historical CODEX 01 verification

The following is retained as the original iteration's historical report; its APK size/hash and test counts are not the current CODEX 02 results.

# CODEX 01 verification — 2026-10-27

## Completed

`python3 tools/verify_offline.py` completed against the final sources, using Kotlin 1.9.24, OpenJDK 17.0.20.1, Android SDK 36, Build Tools 35.0.0, and JUnit 4.13.2.

- JVM: **31 executed, 31 passed, 0 failed, 0 skipped**. Final JUnitCore output: `OK (31 tests)`.
- Full executable Level 1 witness: 12 successful validated matches, 24 removed tiles, final WON.
- Main sources and instrumentation smoke-test sources compiled. Instrumentation uses the deprecated platform test API deliberately to avoid additional downloaded test dependencies. It is compiled, not runtime verified.
- AAPT2 linked actual English resources and Android manifest; javac/Kotlin compiled the actual application; D8 produced executable DEX; zipalign passed; apksigner verified v2 and v3 signatures.
- Final debug APK: `app/build/outputs/apk/debug/app-debug.apk`, **700819 bytes**.
- SHA-256: `00f4f056530b2c697e5c9c2ec809bf3ca08a62fafd967f937d9f33977eddd7f8`.
- AAPT metadata: package `com.tilebloom.game`, version 0.1/code 1, minimum 26, target 36, launch activity `com.tilebloom.game.presentation.MainActivity`, debuggable, portrait, no requested permissions.

The APK was generated by the documented standalone fallback, **not by a successful Gradle task**. It is debug signed and intended for local testing only. Build outputs and the debug key are ignored by Git.

## Gradle attempts — blocked before any task ran

Initial command:

```sh
./gradlew --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The shared Gradle home is read-only; wrapper lock creation failed. Retried the installed Gradle binary with a writable `/tmp/tilebloom-gradle` home, then retried the wrapper using the cached distribution from that writable home:

```sh
GRADLE_USER_HOME=/tmp/tilebloom-gradle ./gradlew --offline --no-daemon \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest
```

Both reached Gradle but failed during startup:

```text
Could not create service of type FileLockContentionHandler
Could not determine a usable wildcard IP for this machine.
```

The environment denies socket operations. No Gradle task completed, and Android Lint remains **unverified**. This is an environment failure, not a reported passing Gradle build. The Kotlin DSL configuration still needs execution in a normal Android development environment.

## Device attempts — unavailable under current restrictions

SDK emulator and AVD definitions exist (Medium_Phone and others), but `/dev/kvm` is absent. More decisively, `adb devices`, `adb install -r ...`, and `adb shell am start -n com.tilebloom.game/.presentation.MainActivity` all fail because the ADB server cannot bind its socket: `could not install *smartsocket* listener: Operation not permitted`.

Instrumentation: **0 executed**; 1 smoke test defined and compiled, no passing runtime result claimed. Installation, launch, crash/logcat inspection, actual touches, screenshot/layout review, and lifecycle restoration were **not verified**. No human playtesting occurred.

## Acceptance criteria audit

| Criterion | Evidence / status |
| --- | --- |
| A — build / APK | Actual compiled, aligned, signature-verified APK exists via fallback; Gradle build unverified. |
| B — app opens playable board | Implemented; device launch unverified. |
| C — 24 tiles / two overlapping layers | Verified by engine tests; actual rendering unverified. |
| D — blocked selection rejected | Engine tested; actual touch behavior unverified. |
| E — pairs disappear | Engine removal tested; Canvas behavior unverified at runtime. |
| F — uncover lower tiles | Engine tested. |
| G — invalid matches preserve state | Engine tested. |
| H — correct remaining pairs | Engine tested; screen binding inspected and compiled. |
| I — victory | Engine tested; visible terminal feedback unverified at runtime. |
| J — no moves | Engine tested; visible terminal feedback unverified at runtime. |
| K — restart | Engine tested; button smoke test compiled but not run. |
| L — witness solution | Passed production-engine replay of all 12 pairs. |
| M — readable portrait UI | Responsive implementation compiled; screen/device validation unverified. |
| N — meaningful tests | 31 JVM tests passed. |
| O — maintainable separation | Pure Kotlin domain; Android presentation separate. |
| P — docs / commit | README and this audit included; repository metadata uses `.tilebloom-git` because `.git` is read-only. |

## Final code review

Reviewed top-overlap tolerance, same-layer side adjacency, all-higher-layer blocking, removed tile filtering, duplicate/self/invalid matches, selection transitions, empty-board status priority, current-state pair enumeration, restart, draw order and reverse hit order. Corrected rounded-face and visible-thickness touch coverage during review. Added API 30+ system-bar/cutout insets with an API 26–29 fallback. The board geometry is centered and scales to fit both dimensions; its actual usability on small phones still requires runtime testing.

Known limitations: one deterministic level; player choices can deadlock; no hints/shuffle/undo; custom board lacks screen-reader tile navigation; no runtime usability evidence. Source compilation and engine correctness are not a substitute for Android gameplay verification.
