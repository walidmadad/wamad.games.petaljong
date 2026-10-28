# CODEX 13 — current verification

- JVM: **168 tests PASS**, 14.658 seconds in the final diagnostic run. Includes a 49-identity deterministic generation matrix (1–40 and large identifiers through Long.MAX_VALUE), full witness replay for every accepted matrix board, fallback, invalid candidates, geometric bounds/hit testing, exact next-level commits, deadlock, restarts, migration, pending victory, history and credit restoration.
- Host workflow: **10 tests PASS** (`python3 tools/test_host_verify.py`).
- Standard Gradle: **BLOCKED** before build: wrapper cannot create its lock in the read-only `/home/walid/.gradle/wrapper/dists` directory. Evidence: [gradle.txt](docs/codex13/gradle.txt).
- Diagnostic compiler/build: production Kotlin/resources, Android instrumentation source compilation, D8, alignment, and debug signing. Final APK details appear in [CODEX_13_REPORT.md](CODEX_13_REPORT.md).
- Android instrumentation/runtime: **NOT EXECUTED**. ADB server cannot install its socket listener (`Operation not permitted`). No installation, launch, device screenshots, performance claim or visual PASS is inferred from JVM/build results.
- Instrumentation uses actual tile coordinate taps through Levels 1–7, Next double taps, generated save/relaunch/restart/Undo and pending-victory recreation. Compiled coverage is not execution evidence.
- Android timing logging is available under `TileBloomGeneration` when the Next Level worker runs. Host measurements are separate and do not establish phone responsiveness.
- Unchanged production files: Game/Rules, botanical tile drawing, board geometry, match effects and Undo wallet. No advertising dependencies added.

See the complete [30-item acceptance table](CODEX_13_REPORT.md#q-acceptance-criteria) and current [device checklist](PLAYTEST_01.md). Historical validation below is superseded where it conflicts with CODEX 13 (including all ads, level selection, Replay and Level 5 ending claims).

---

# Historical validation

# CODEX 12 — Current status

Latest evidence: **159 JVM tests PASS; 10 host-workflow tests PASS**. Android Gradle was attempted twice and blocked by sandbox filesystem/network-interface restrictions. **26 instrumentation methods present, not compiled or executed** in this iteration. No new APK or real Google ad test is claimed. Logs are in `docs/codex12/`; [CODEX_12_REPORT.md](CODEX_12_REPORT.md) contains the full acceptance matrix. Previous iteration APK/instrumentation results below are historical.

---

# CODEX 11 — current verification (2026-10-27)

- JVM: 149 executed / 149 passed / 0 failed, including all 139 prior tests and 10 Undo methods. Five complete witnesses and deterministic Level 1 v3 remain valid.
- Host workflows: 10 executed / 10 passed / 0 failed.
- Instrumentation: 18 methods COMPILED BUT NOT EXECUTED (15 retained, 3 added); 0 executed. No device runtime claim.
- Standard Gradle unit/build/Lint invocation: blocked at read-only wrapper lock before task execution. Lint NOT EXECUTED.
- Diagnostic pipeline: PASS with JDK 17; default Java 25 was incompatible with cached Kotlin compiler. APK: `app/build/outputs/apk/debug/app-debug.apk`, 745,875 bytes.
- APK SHA-256: `f246a09c203308e1e900f2e48a92ec00f061183f144c65133701d68895a7205b`.
- Alignment PASS (`zipalign -c 4`); signature PASS (v2/v3, one debug signer). Packaged Undo resources confirmed.
- ADB: sandbox socket listener denied; no installation, launch, connected tests or visual inspection executed. Persistence across actual app relaunch and compact-screen usability NOT VERIFIED.
- Host performance: 1,000 combined late-Level-5 restore+Undo cycles in 104.747208 ms. No Android latency measurement.
- Evidence: `app/build/codex11/diagnostic.log`, `host-tests.log`, `gradle.log`, `adb.log`. Full architecture and all 28 criteria: [CODEX_11_REPORT.md](CODEX_11_REPORT.md).

Undo uses accepted-history candidate replay, clears selection/effects and saves the shorter history through the existing ordered writer. Schema, puzzle identity and completed records are preserved. No Undo after victory; Replay/Restart start fresh attempts. Human Level 1 assessment and real durable storage checks remain pending; see current PLAYTEST_01.md additions. Historical verification entries below remain evidence for earlier iterations, not current runtime results.

---

# CODEX 10 validation — current evidence

This section supersedes all historical artifact/count claims below.

- Diagnostic Kotlin 1.9.24, Java 17, JUnitCore: **139 PASS** (127 previous purposes retained, 12 new tests). Final suite 6.31 s; 60-candidate portfolio 328 ms; 120-board matrix 3,766 ms. Single desktop timings.
- Host workflow regression tests: **10 PASS**, fake-tool coverage only.
- Android instrumentation: **15 compiled, zero executed**. Fourteen prior methods adapted/preserved; one old-version save upgrade launch test added. Gameplay uses real view taps, not direct engine moves.
- Standard Gradle compilation, Gradle JVM tests and Lint: **NOT EXECUTED**. Previously diagnosed read-only wrapper cache and restricted listener failures were not repeatedly retried in the unchanged sandbox.
- Android installation, current rendering, connected tests, process restoration, permanent save runtime, real update migration and human difficulty: **NOT VERIFIED / NOT EXECUTED**. No screenshot or device result establishes them.
- Portfolio: all 60 boards validate and finish WON through production witness replay. All portfolio alternatives resolve within 10,000 nodes per candidate; zero UNKNOWN. Separate existing 16-board/1,000-node analyzer regression records two exhausted analyses and one candidate-quality exhaustion; these are UNKNOWN, not UNSOLVABLE.
- CODEX 09 schema-1 migration preserves valid completion/unlock records, resets only incompatible old Level 1 history, and round-trips new saves. Compatible Level 2 history remains byte-identical. JVM evidence only.

New debug APK: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`, **745,875 bytes**, built from current source using `tools/verify_offline.py` with Java 17 / cached Kotlin 1.9.24 / SDK 36 / Build Tools 35.0.0. This is a diagnostic build, not Gradle. Size coincides with the previous artifact but SHA-256 differs: **32747acd23b102a57b22afe42b5fda73cd341187e759e4c6573d2f79a403eb6b**. New DEX includes `winding`, `INTRODUCTORY_VERSION` and `newlyAvailable`; stale artifact not reused.

`zipalign -c 4`: PASS. `apksigner verify --verbose`: PASS, one signer, v2/v3 signatures. aapt2 badging: package `com.tilebloom.game`, debug enabled, versionCode 1 / versionName 0.1, min SDK 26 / target SDK 36. Actual Android runtime: **NOT EXECUTED**.

Logs: `/tmp/tilebloom10-build.log`, `/tmp/tilebloom10-tests.log`, `/tmp/tilebloom10-host-tests.log`. Tracked reproducible evidence: [candidate CSV](docs/codex10-candidates.csv), [selected-board analysis](docs/codex10-analysis.txt), [report and acceptance table](CODEX_10_REPORT.md). On an authorized host use the exact commands in report section L and HOST_VERIFICATION.md; complete the CODEX 10 checklist in PLAYTEST_01.md without clearing application data.

---

Historical evidence below does not verify CODEX 10.

# CODEX 09 validation — current evidence

This section supersedes earlier iteration counts and artifact evidence below.

- Diagnostic Kotlin 1.9.24/JUnitCore: **127 PASS**, all original 104 plus 23 new progress/restore tests. Original 120-board generator regression retained.
- Host-workflow tests: **10 PASS**, tools/test_host_verify.py; fake-tool workflow coverage only.
- Android instrumentation: **14 compiled, 0 executed**; original ten retained, four new durable-storage/relaunch/failure cases. Uses isolated test storage and geometry-derived board taps. A fresh activity relaunch is distinct from process restart; manual fresh-process commands are in HOST_VERIFICATION.md.
- Standard tools/host_verify.sh: exit 1 at wrapper version preflight; wrapper cache lock is read-only. Standard Gradle JVM/build/Lint tasks **NOT EXECUTED**, rather than represented by diagnostic results.
- ADB: cannot start listener (Operation not permitted). Installation, connected instrumentation, UI screenshots, current CODEX 08 victory fix, new selector, runtime restoration and latency **NOT VERIFIED**.
- No human five-level playtest results supplied. PLAYTEST_01.md is a blank structured worksheet.

Updated debug APK: `app/build/outputs/apk/debug/app-debug.apk` — **745,875 bytes**. Build: `python3 tools/verify_offline.py`, cached Kotlin 1.9.24 diagnostic compiler, SDK 36 / Build Tools 35.0.0; not a standard Gradle build. Package: `com.tilebloom.game`, versionCode 1 / versionName 0.1, min SDK 26 / target 36 (aapt2 badging verified). zipalign 4-byte check PASS; apksigner verification PASS, one signer, v2 and v3 signatures. SHA-256: `4e112e3896c5844f7aac2b0afb48f32782aba94d3dffb4eb448ecf84468b29a7`. No installation or runtime verification performed.

Logs for this iteration: /tmp/tilebloom09-verified.log, /tmp/tilebloom09-workflow.log, /tmp/tilebloom09-host.log. Standard preflight evidence: app/build/host-verification/run-20261027T210731Z-6PK65F/. These generated logs/artifacts are not committed. The complete 28-criterion evaluation and A–S development report are in CODEX_09_REPORT.md.

---

Historical evidence follows. Earlier screenshots or APKs do not establish runtime verification of CODEX 09.

# CODEX 06 verification — 2026-10-27

## Outcome and evidence

Continued `48c9098b20d914c74dd594b1bdb91e2ae5cd5a40`; CODEX 05 made no changes. Preserved existing history, engine, Level 1 positions/symbol assignments, solution and deadlock witnesses, shared hit geometry, gesture behavior, restart and saved-state replay. No dependency changes or publication.

New external evidence: the user describes three emulator screenshots, approximately 382 × 898 pixels, showing TileBloom's green/ivory screen, layered tiles, gold selection, counters at 10 then 7 pairs, and upper tiles disappearing. **No screenshot files were accessible or directly inspected.** These concrete user-supplied observations establish earlier-build Android rendering/progression. They do not establish standard Gradle success, instrumentation, terminal states, persistence or reliability of this iteration's APK. No unrestricted-host verification logs were received. No completed human playtest or enjoyment/retention feedback was supplied.

## Screen and artwork changes

Platform-serif title (36 sp; 30 sp on compact screens or increased font scale), understated subtitle and a rounded muted-green level/pair strip replace the tightly grouped header. The board retains the flexible central area, original transform and insets. Margins reduce below 640 dp screen height or above 1.3 font scale. Text uses sp and can wrap; there are no fixed text heights. Footer reserves two text lines for instruction or terminal feedback, avoiding a normal-size board jump on victory/deadlock. Existing positive victory text and clear replay/restart actions remain.

Restart remains an accessible native Button, now content-width, at least 48 dp tall, with green surface, ivory text, rounded gold border and masked ripple. Tile faces gain an inset ivory rim, warmer face/side colors and lower-opacity shadows. Symbols, gold selection and engraved blocked markers remain; leaf Path and fixed symbol coordinate lists are cached without changing shapes. Decorative shadows remain excluded from hit targets.

No updated Android screenshots were captured. Readability, composition, shadow quality, contrast on real displays and compact/font-scale usability require direct inspection; implementation alone is not visual acceptance.

## Match feedback and code review

After `Game.select` validates a match, authoritative tiles/history/counter update immediately. `MatchEffects` records only newly removed visual copies. Canvas merges these with remaining tiles in original layer order and fades 100% to 0% while shrinking 100% to 90% over 150 ms. Tile-bounded alpha layers avoid full-screen buffers. No particles, external framework, delayed engine mutation or animation-completion callback is used.

Touch uses only original `game.tiles` through unchanged BoardGeometry; removed copies cannot receive touch or change legality. Rapid successive pairs have independent timestamps. Restart, size change and detachment clear copies and pending gesture/blocked feedback. Copies are not saved, so recreation restores only accepted game state. Disabled Android animators skip/clear the effect. `postInvalidateOnAnimation` runs only while live copies remain; expiry uses monotonic uptime. Smoothness and real lifecycle integration remain runtime-unverified.

Reviewed ordering, save/restore balance, original hit bounds, snapshot ownership, duplicate input, effect lifetime and resource cleanup. No engine or gameplay defect is claimed from screenshots. First fallback compile exposed an integer/Float mismatch in the new surface helper; corrected to integer dp before the successful final build.

## Automated verification

`python3 tools/verify_offline.py` — exit **0**, `OK (67 tests)`; all production and instrumentation Kotlin compiled; D8/APK packaging completed.

| JVM suite | Executed | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| GameTest (unchanged) | 31 | 31 | 0 | 0 |
| BoardGeometryTest (unchanged) | 16 | 16 | 0 | 0 |
| GameReliabilityTest (unchanged) | 10 | 10 | 0 | 0 |
| TileTapGestureTest (unchanged) | 7 | 7 | 0 | 0 |
| MatchEffectsTest | 3 | 3 | 0 | 0 |
| **Total** | **67** | **67** | **0** | **0** |

New JVM coverage exercises immediate single engine removal, removed-ID hit exclusion, independent rapid matches, timed expiry, restart clearing, disabled effects and restored game state. Existing solution/deadlock and all shared-geometry/gesture tests pass. JVM geometry is not an Android layout test.

`python3 tools/test_host_verify.py -v`: **10 executed, 10 passed, 0 failed** using isolated fake tools; these are workflow tests, not Gradle or Android verification. Diff whitespace check passed.

All eight existing instrumentation methods are preserved. One new method dispatches real view touches to check immediate removal, rapid pairs, restart during effects, expiry and later matching. The recreation test also checks no stale effects. **Nine methods compiled; 0 executed, 0 passed, 0 failed, 9 unexecuted.** Existing deprecated platform test API warning remains. No assertion was weakened.

Standard Gradle/Kotlin 2.2.10 tasks, Lint and connected tests were not retried inside the previously diagnosed restricted environment. No Gradle success is claimed. Actual screen sizes tested for the updated APK: **none**; original mathematical geometry tests ran. The externally reported ~382 × 898 pixels do not determine dp, density, font scale or API.

## New APK

- Path: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`.
- Size: **713107 bytes**.
- SHA-256: `d91c182931af7297bf3a2dc4e39ea2a71c595d97101f010e7b663971d8353ea9`.
- Method: `python3 tools/verify_offline.py`, cached Kotlin **1.9.24**, Java 17, SDK 36 / Build Tools 35.0.0, AAPT2, javac, D8, zipalign and local debug signing. This does not verify configured Gradle/Kotlin 2.2.10.
- `zipalign -c 4`: passed. `apksigner verify --verbose`: passed, v2/v3, one signer.
- AAPT2 metadata checked: `com.tilebloom.game`, version 0.1/code 1, min API 26, target 36, debuggable, portrait and expected MainActivity.
- Final log: `app/build/offline/verification.log` (ignored build artifact).

This is a fresh diagnostic build of CODEX 06 sources, not an old APK and not standard Gradle output. New-APK installation, launch, crash inspection, UI victory/deadlock, animation smoothness and real lifecycle restoration are **NOT VERIFIED**.

## Acceptance criteria for the updated APK

| # | Criterion | Result | Evidence / limit |
| --- | --- | --- | --- |
| 1 | Engine preserved | PASS | No domain changes. |
| 2 | All previous JVM tests preserved | PASS | All 64 plus three new tests pass. |
| 3 | Level solution valid | PASS | Existing complete witness replay. |
| 4 | Known deadlock valid | PASS | Existing deterministic regression. |
| 5 | Readable updated header | NOT VERIFIED | Implemented; device review pending. |
| 6 | Visually organized status | NOT VERIFIED | Separate status strip; device review pending. |
| 7 | Restart matches identity | NOT VERIFIED | Custom styling implemented; device review pending. |
| 8 | Board remains focal point | NOT VERIFIED | Flexible central board retained; visual review pending. |
| 9 | Six distinct symbols | NOT VERIFIED | Original shapes preserved; updated display review pending. |
| 10 | Better depth, same hit testing | NOT VERIFIED | Geometry unchanged and passing; visual improvement pending. |
| 11 | Clear selection | NOT VERIFIED | Gold outlines preserved; new-APK review pending. |
| 12 | Recognizable blocked tiles, legally unavailable | NOT VERIFIED | Rules pass and markers preserved; recognizability pending. |
| 13 | Smooth, safe match feedback | NOT VERIFIED | JVM state protection passes; Android smoothness/integration pending. |
| 14 | Restart handles effects safely | NOT VERIFIED | Model cleanup passes; Android regression compiled, unexecuted. |
| 15 | Usable compact portrait layout | NOT VERIFIED | Responsive implementation; no new-APK layout run. |
| 16 | Shared touch geometry tests | PASS | All 16 unchanged geometry tests pass. |
| 17 | Fresh debug APK | PASS | Final fallback build. |
| 18 | Accurate build/validation reporting | PASS | Fallback and runtime limits explicitly separated. |
| 19 | Honest instrumentation results | PASS | Nine compiled; zero executed. |
| 20 | Git history preserved | PASS | Descendant of 48c9098; no rewrite/push. |

## Remaining actions and CODEX 07

On an unrestricted Linux host, run `bash tools/host_verify.sh` (standard version, testDebugUnitTest, assembleDebug and lintDebug checks). After success select a booted authorized device, follow HOST_VERIFICATION.md for install/launch/crash logs/screenshots, and run `./gradlew --console=plain :app:connectedDebugAndroidTest`. Retain commands, exit codes and actual nine-test XML counts.

Test compact (~320 × 568 dp), typical (~360 × 800 dp) and larger (~412 × 915 dp) portrait layouts at normal/increased font scale, system insets, enabled/disabled animations, rapid matches, restart and recreation mid-fade. Complete PLAYTEST_01.md with a real player; no reactions are invented. Verify title/strip wrapping, tile readability, terminal messages and bottom controls on the new build.

CODEX 07 should first verify the polished APK and resolve observed failures. If stable, use actual player feedback to plan handcrafted puzzle variety and more meaningful strategic choices. No procedural generation, ads, online systems or monetization is proposed. Existing accessibility limitations (no individual screen-reader tile navigation), one fixed level and lack of durable progression remain.

---

# CODEX 04 verification — 2026-10-27

## Actual outcome

Prepared a repeatable **unrestricted Linux host** workflow instead of retrying diagnosed sandbox Gradle/ADB failures. No unrestricted host logs, Gradle test/Lint reports, connected test results, Android screenshots or completed human feedback were found in the project, including ignored build output. No standard Gradle or ADB startup was retried in this iteration. Their prior blockers remain documented below as historical findings, not newly executed checks.

Continued commit `a031e1c8857f0688e3de75c0f6703336b03dcdb0`; initial working tree was clean. Reviewed all four project documents, Gradle/wrapper/SDK configuration, manifests/resources, engine and fixed level, renderer, shared geometry, gesture handling, activity/Bundle restoration, all four JVM suites, eight instrumentation methods and offline pipeline. No new confirmed application defect or runtime evidence justified changing game code. All application sources, artwork, rules, solution/deadlock witnesses, exact move-history restoration and existing tests remain unchanged.

## Host workflow implementation and verification

New `tools/host_verify.sh`:

- Resolves the project root; checks the same Java executable as the wrapper, Java 17, wrapper files, SDK platform 36 and Build Tools 35.0.0 tools.
- Reads ordinary Linux SDK paths from local.properties or SDK environment variables without executing the properties file. Does not install tooling, alter global settings or disable security.
- Runs these actual standard commands **when the user executes it on the host**:

```sh
./gradlew --version
./gradlew --console=plain :app:testDebugUnitTest
./gradlew --console=plain :app:assembleDebug
./gradlew --console=plain :app:lintDebug
```

- Saves context/commit, exact commands, exit codes in results.tsv, individual logs, available reports/JVM XML and a summary under a unique `app/build/host-verification/run-*` directory.
- Stops before tasks on failed preflight/version startup. Otherwise records independent JVM/build/Lint failures, returns nonzero for failed checks, and never calls the fallback automatically.
- Checks successful assemble output for existence, size/hash, package/API metadata, alignment and signature. Archives it with the run. A failed build never accepts a pre-existing fallback APK as its output.
- Explicitly leaves Android installation, launch, instrumentation, visual inspection and human playtest outside its build success status. Archived historical reports do not establish current execution; inspect commands, timestamps and XML together.

`bash -n tools/host_verify.sh` passed. `python3 tools/test_host_verify.py -v` passed **10/10 isolated workflow tests**, using fake Java/Gradle/SDK tools and temporary fixture projects. They protect startup/preflight failures, SDK path precedence/quoting, non-execution of properties text, preservation of per-task failures, stale/missing APK rejection and signature failure reporting. These are workflow tests, **not real Gradle, Android, or additional game JVM tests**. Temporary fixture logs are not supplied as host evidence.

Toolchain unchanged: Java 17 (locally OpenJDK 17.0.20.1), KGP 2.2.10, AGP 8.10.0, Gradle 8.13, compile/target 36, minimum 26, Build Tools 35.0.0. The diagnostic compiler remains 1.9.24. Standard KGP 2.2.10 compilation, dependency resolution and Android Lint remain NOT VERIFIED.

## Regression results — actual execution

`python3 tools/verify_offline.py` exited **0**, `OK (64 tests)`:

| JVM suite | Executed | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| GameTest | 31 | 31 | 0 | 0 |
| BoardGeometryTest | 16 | 16 | 0 | 0 |
| GameReliabilityTest | 10 | 10 | 0 | 0 |
| TileTapGestureTest | 7 | 7 | 0 | 0 |
| **Game JVM total** | **64** | **64** | **0** | **0** |

The full twelve-pair solution and known deadlock pass. Domain snapshot restoration, legal matches, counts, selection, duplicate protection, gesture cancellation and shared geometry remain protected. These facts do not prove real Android touch delivery, rendering, feedback timing or lifecycle behavior.

**Android instrumentation: NOT EXECUTED.** Eight existing methods compiled against Android APIs; executed **0**, passed **0**, failed **0**, skipped **0**. The existing deprecated platform test API warning remains. No valid assertion was removed or weakened.

**Installation/launch/UI victory/UI restart/visual inspection/human playtest: NOT VERIFIED.** No app launch, screenshot, crash-log inspection or player session occurred. Symbol distinction, touch comfort, visual hierarchy, strategic interest and match satisfaction remain unknown. No animation or speculative redesign was introduced.

## APK — actual diagnostic artifact

Rebuilt through `python3 tools/verify_offline.py`, **not standard Gradle**:

- Path: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`
- Size: **709011 bytes**.
- SHA-256: `444188ca459614fc8a8a6fd86a395f5780eaf9e63f05fea6d909747ef0751a87`.
- AAPT2 metadata confirms `com.tilebloom.game`, code 1/version 0.1, minimum API 26, target API 36, debuggable, expected launch activity and portrait requirement.
- `zipalign -c 4` passed; `apksigner verify --verbose` passed with v2 and v3 signatures, one signer.
- Actual fallback log retained at `app/build/offline/verification.log` (ignored build output).

The APK has not been installed or runtime-tested. Its existence and signature do not establish standard Gradle or Android success.

## CODEX 04 acceptance criteria

| # | Criterion | Result | Evidence / limitation |
| --- | --- | --- | --- |
| 01 | Existing engine preserved | PASS | No production source changes. |
| 02 | Existing 64 JVM tests passing | PASS | All 64 executed through diagnostic Kotlin/JUnit pipeline. |
| 03 | Complete solution witness preserved | PASS | Unchanged; JVM replay passed. |
| 04 | Deadlock witness preserved | PASS | Unchanged; JVM replay passed. |
| 05 | Unrestricted standard Gradle verification | NOT VERIFIED | No host run/results received. |
| 06 | Android Lint completed | NOT VERIFIED | Host execution pending. |
| 07 | Valid debug APK generated | PASS | Diagnostic fallback, aligned and signature-verified. |
| 08 | Android installation completed | NOT VERIFIED | No installation evidence. |
| 09 | Successful application launch | NOT VERIFIED | No launch evidence. |
| 10 | Actual Android touch interaction | NOT VERIFIED | JVM coverage only. |
| 11 | Android instrumentation executed | NOT VERIFIED | Eight compiled, zero executed. |
| 12 | Victory through Android UI | NOT VERIFIED | Engine witness only. |
| 13 | Restart through Android UI | NOT VERIFIED | Engine/geometry coverage only. |
| 14 | Visually inspected screen layout | NOT VERIFIED | No Android screenshot. |
| 15 | Human playtest completed | NOT VERIFIED | Checklist remains blank. |
| 16 | Confirmed defects corrected | NOT VERIFIED | No new confirmed application defect or runtime failure available to correct. |
| 17 | Git history preserved | PASS | Existing alternative metadata retained; iteration committed as descendant of a031e1c. |
| 18 | Accurate documentation | PASS | Host workflow, separate evidence categories and outstanding actions recorded. |

## Exact next external actions

1. In a **regular Linux terminal**, configure JDK 17 and the actual SDK path, then run `bash tools/host_verify.sh`. Inspect its summary/results and retain its run directory. This is the next milestone, not another sandbox attempt.
2. After successful standard build verification, follow HOST_VERIFICATION.md to select a booted authorized device, install the APK, launch, capture the initial screen, execute `./gradlew --console=plain :app:connectedDebugAndroidTest` and collect XML/crash logs. Record real counts and failures.
3. Complete PLAYTEST_01.md with a real player. It now distinguishes unaided understanding/help, objective technical observations and subjective impressions. No human answer has been filled in.
4. Return host logs, device/API/font details, screenshots and real feedback for CODEX 05. Fix the highest-impact observed failures before changing gameplay. Evaluate a subtle match transition only after actual runtime stability is established.

The signed diagnostic artifact and instructions are ready for an exploratory first playtest, conditional on successful host installation/launch. Android readiness is not yet established. No new features, publication, push, Git reinitialization or history rewrite occurred.

---

# Historical CODEX 03 results (current status is above)

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


## CODEX 08 — final verification (2026-10-27)

Final serial `python3 tools/verify_offline.py`: exit **0**, **104 JVM tests passed, 0 failed**, 6.663 s JUnit execution. The existing 89 regressions remain; live Level 1 fixture expectations were intentionally updated. Added 13 analyzer/quality and two presentation tests. All ten host-workflow tests passed (`python3 tools/test_host_verify.py`).

The preserved 120-board matrix replayed every complete witness: 4,690 ms host time, max one geometric attempt for selected candidates. The separate 16-board/all-four-family quality matrix evaluated 58 candidates: 14 target acceptances and two verified, explicitly flagged fallback selections; 42 candidates not selected. One internal candidate search and two separate 1,000-node selected-board audits exhausted budgets and reported UNKNOWN. Generation including quality evaluation: 596 ms; separate audit: 120 ms. These are JVM host timings, not Android performance results.

Ten Android instrumentation methods **compiled, zero executed**. Coordinate victory/deadlock coverage now checks mutually exclusive footer/panel states, visible Next/Replay on victory and hidden victory actions on deadlock. Existing gesture/effect/recreation/progression tests remain. ADB discovery failed: server socket listener not permitted. Updated installation, launch, gameplay, screenshots and visual review: **NOT EXECUTED**. Previous user feedback validates older animations, not this artifact.

Standard Gradle JVM tests, assemble and Lint: **NOT EXECUTED**. The known sandbox startup restrictions have not changed; no redundant Gradle retry was made. Diagnostic success is not standard Gradle success. An intermediate overlapping diagnostic run produced shared-output errors, and a provisional fixture deadlock expectation failed; both are superseded by the final successful serial build.

- Path: `app/build/outputs/apk/debug/app-debug.apk` (also archived at `app/build/codex08/app-debug.apk`).
- Method: final serial diagnostic offline pipeline, exit 0; not standard Gradle.
- Size: **737,683 bytes**.
- SHA-256: `6cb49ea56e63fbf3a41310a4608e9a9334d6200df2b327eaa6cd8aaca3fda37b`.
- Alignment: PASS (`zipalign -c 4`). Signatures: PASS, v2/v3, one debug signer. v1/v3.1/v4 and SourceStamp absent.
- Metadata: `com.tilebloom.game`, debug enabled, min API 26, target/compile API 36, launch activity `.presentation.MainActivity`.
- Installation, launch, instrumentation execution and updated visual playtest: NOT EXECUTED.

Live seeds/version: L1 0/v2, L2 202/v2, L3 303/v2, L4 404/v2, L5 511/v2. Incompatible saved histories are rejected atomically; activity falls back to a fresh Level 1. New coordinate deadlock path: `(16,20), (0,4), (8,12), (17,21), (1,5), (9,13), (15,18), (19,23)` leaves eight tiles/four pairs. Historical fixture tests remain on `LevelOne`.

Evidence: `app/build/codex08/verification.log`, `quality-matrix.tsv`, `host-workflow-tests.log`, `adb-server.log`, archived APK. Full metrics and all 26 acceptance criteria: [CODEX_08_REPORT.md](CODEX_08_REPORT.md). Human difficulty and device latency remain unverified.

## CODEX 14 — final host validation

- PASS: 174 JVM tests (168 existing tests retained, six new tier/catalog/matrix/save/
  progression/fallback tests); final suite 28.213 seconds. Historical v4 reconstruction
  tested at levels 6, 20, 21, 40, 101 and 10000.
- PASS: 10 host-workflow tests.
- PASS: deterministic matrix at every introduction and nearby boundaries, through
  levels 200, 500, 1000, 10000 and Long.MAX_VALUE. Complete engine witness replay,
  even paired symbols, bounded attempts/nodes and compact/medium/wide hit testing.
- Host generation at the required 17 representative levels: 16–180 ms in this run;
  subsequent analysis measured separately. These are single host observations, not
  device latency or statistically stable benchmarks. See `docs/codex14/host-matrix.txt`.
- Standard Gradle: blocked by read-only wrapper cache lock. Android Lint NOT EXECUTED.
- Diagnostic Java 17 pipeline: production code, instrumentation compilation and fresh
  aligned/signed debug APK. See final APK evidence in `CODEX_14_REPORT.md`.
- Android instrumentation: sources updated; execution NOT EXECUTED. ADB listener bind
  denied by the sandbox. No device runtime, performance or visual claims.
- Human challenge, artwork recognizability, font scaling and insets: NOT VERIFIED;
  follow the CODEX 14 milestone checklist in `PLAYTEST_01.md`.

Reproduce with Java 17: `python3 tools/verify_offline.py`, then
`python3 -m unittest tools/test_host_verify.py`. Standard Gradle remains the preferred
normal-host build. Environment diagnostics: `docs/codex14/environment.txt`.

## CODEX 15 validation

The active rules are individual tile collection, automatic pairs and four-distinct-symbol
loss. Historical pair-selection UI expectations above describe older builds. Current
JVM tests replay witnesses through production `Game.select`, preserve geometry tests,
and cover tray ordering, duplicate rejection, defeat Undo, atomic replay, credits,
schema 4 restoration and legacy migration. `TrayAnalyzer` keeps budget exhaustion UNKNOWN.

Standard Gradle attempt: blocked by read-only wrapper lock under ~/.gradle. Offline
verification command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py`.
Android instrumentation and runtime are NOT EXECUTED: ADB cannot bind port 5037 in this
sandbox. No Android Lint result is claimed. See CODEX_15_REPORT.md and docs/codex15 for
final test/build evidence and APK digest. Small-phone layout and animation quality
remain device playtest items; the compact header hides decorative subtitle and reduces
status/footer height to make room for the tray.

Final CODEX 15 results: **180 JVM tests PASS** (52.162 s); instrumentation sources
compile successfully, NOT EXECUTED. Fresh APK: 774,547 bytes, 2026-10-28 19:27:57 +01:00,
package `com.tilebloom.game`, aligned and verified with signatures v2/v3.
SHA-256: `d8aebcc2ac8ef94f32c1f0c4aca1721b697224334187045819ae1d5e9df73527`.
APK DEX contains the new tray renderer, analyzer and selection-history methods. Evidence:
`docs/codex15/offline-verification.txt`, `apk-verification.txt`, `gradle.txt` and
`android-runtime.txt`. No Android execution or Lint claim is made.

## CODEX 16 — start screen and icon controls

The engine, generator, difficulty policy, symbols, save schema/codec, repository and
Undo wallet are unchanged. `LevelNavigation` is separate presentation state: fresh
entry START, explicit Play GAME, Home START, recreation restores only a matching
level/screen, and a newly committed identity synchronizes to START. Navigation never
mutates a puzzle or grants credits. The existing next-level durable-save transaction
and forward-only completion checks are retained.

All 180 prior JVM regressions are preserved, plus six combined navigation/lifecycle
regressions (186 tests). Android source coverage adds `LevelStartTest` and updates
legacy board helpers to explicitly press Play. Icon assertions inspect the numeric
badge rather than the old Undo text. Board taps still use transformed geometry.

Standard Gradle remains blocked by a read-only ~/.gradle wrapper lock. The established
Java 17 offline builder is used. ADB cannot bind port 5037 in the sandbox; Android
instrumentation, screenshots, installation, interaction, font/width matrix, TalkBack
and animation appearance are **NOT EXECUTED / NOT VERIFIED**. Android Lint is also
NOT EXECUTED. See `docs/codex16/gradle.txt`, `android-runtime.txt`, the final offline
verification log and `CODEX_16_REPORT.md` for the exact build and APK results.

Final CODEX 16 verification: **186 JVM tests PASS**, 30.672 seconds. All Android
instrumentation sources compile (39 test methods), **NOT EXECUTED**. APK generated at
2026-10-28 19:44:01 +01:00: 783,092 bytes, package `com.tilebloom.game`, aligned, valid
APK v2/v3 signatures, same local debug certificate as CODEX 15.
SHA-256: `7c261fc59eb9496417d7b3c96f7acc097b76823490429accde62acb5eebd0104`.
DEX startup/navigation/control classes and all five compiled vector resources verified.
The APK is newer than every production Kotlin/XML file. Verification evidence is saved
in `docs/codex16/apk-verification.txt`. No Android runtime success is claimed.

## CODEX 25 verification

Evidence: `docs/codex25/`. Requested standard Gradle unit-test, assembleDebug and lintDebug commands were attempted; each fails at the read-only wrapper lock. Java 17 / installed Gradle 8.13 / writable temporary user-home retry fails at wildcard IP initialization. These are prerequisite failures, not task success. ADB cannot bind its server socket, so instrumentation and manual Android verification are NOT VERIFIED. No fresh APK was generated; its timestamp, size, SHA-256, package contents, alignment and signature are unavailable.

Cached Kotlin/JUnit verifies generator v6, full production witnesses, new-symbol pairs/Hint/Undo, historical/current save restoration, progression and the capability/600ms handover state policy. Main and Android test sources also compile diagnostically with cached official SDK dependencies and metadata checking bypassed; this does not replace Gradle or Lint. Historical v5 level fingerprints match the pre-change compiled generator for 6, 11, 21, 41, 101 and 10000. The existing locally configured certificate matches the required fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6; no fresh signed artifact is claimed.

`ConnectivityInteractionTest` adds offline home/Play, consent-independent online access, action guards, tray/credit preservation, reconnect, victory gating, recreation and renderer-scale coverage. Tests are updated and compiled, not run. Real Wi-Fi/mobile, captive network, handover, callback registration under recreation, compact/large-font layout and optional rewarded-ad playback remain pending. See CODEX_25_REPORT.md for the 30-item acceptance matrix.

Final CODEX 25 JVM result: **216 tests, 0 failures**, 30.099 seconds. Diagnostic main/Android test compilation: PASS. Standard Gradle, Lint, emulator and fresh APK remain unverified/blocked as described above.
