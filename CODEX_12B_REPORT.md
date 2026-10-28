# CODEX 12B — BLOCKED Android delivery

2026-10-28. No publication, uninstall, data clearing, or installation occurred.

## A. Root cause

Commit `929fe41876e89784fe2561f62016c1d905021ab2` implemented the source but did not produce an ads-enabled Android APK. The existing local APK predates that commit. This explains why installing that artifact cannot deliver CODEX 12; the actual installed package could not be inspected because no Android device was connected. A separate source defect hid the credit balance on victory; fixed here.

## B. Source audit

Reviewed the requested Gradle files, manifest, MainActivity, PlayerProgress, ProgressRepository, UndoCreditWallet, RewardedAdManager, GameAds, GameStore, UndoCreditTest, related progress/Undo/presentation tests, instrumentation suite, and CODEX_12_REPORT.md against the existing implementation commit. The working tree was clean at start, using `.tilebloom-git` (the exposed `.git` directory is not a repository).

- The actual main Android source set includes RewardedAdManager, with `RewardedAd.load()` and `RewardedAd.show(Activity, rewardListener)`. No fake SDK or simulated production ad implementation exists.
- App implementation dependencies remain Google Mobile Ads **25.5.0** and UMP **4.0.0**. Google's current documentation lists these versions. Ads requires min API 24 and compile API 35; this app uses 26/36. AGP 8.10.0 supports API 36, Build Tools 35.0.0 and JDK 17; wrapper Gradle 8.13 exceeds its 8.11.1 minimum. Compatibility of the actual resolved dependency graph is still unverified.
- `settings.gradle.kts` configures `google()` and `mavenCentral()` for dependency resolution and plugin management. AndroidX is enabled.
- Manifest has INTERNET, ACCESS_NETWORK_STATE and application-level `com.google.android.gms.ads.APPLICATION_ID` metadata. Placeholder is Google's `ca-app-pub-3940256099942544~3347511713`. BuildConfig rewarded unit is Google's `ca-app-pub-3940256099942544/5224354917`.
- SDK resources/transitive manifests must be merged by standard Gradle. No generated/merged manifest or SDK packaging success is claimed.

Official sources checked:

- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/privacy
- https://developers.google.com/admob/android/rel-notes
- https://developer.android.com/build/releases/agp-8-10-0-release-notes

## C. Gradle results

All three requested wrapper commands were attempted independently and exited 1 before compilation:

```text
./gradlew --console=plain :app:testDebugUnitTest
./gradlew --console=plain :app:assembleDebug
./gradlew --console=plain :app:lintDebug
java.io.FileNotFoundException: /home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck (Read-only file system)
```

The installed Gradle 8.13 distribution was then invoked directly with a writable `/tmp/tilebloom12b-gradle` home and `--no-daemon`, requesting unit tests, APK, lint and instrumentation APK. Both default JDK 25 and explicit JDK 17 attempts exited 1:

```text
Gradle could not start your build.
> Could not create service of type FileLockContentionHandler using BasicGlobalScopeServices.createFileLockContentionHandler().
   > Could not determine a usable wildcard IP for this machine.
```

Raw logs are in `docs/codex12b/`. These failures occur before project configuration/dependency resolution. They do not establish Kotlin compilation success or failure. No unrestricted-host logs were supplied. Sandbox escalation is unavailable under this session's permission policy. Android delivery is **BLOCKED**.

## D. Confirmed defects and fixes

No Android compiler diagnostics reached the compiler, so no Kotlin compilation errors are claimed fixed. Kept verified SDK versions and toolchain configuration rather than guessing at dependency changes.

- Keep `Undo (N)` visible before any move and after victory.
- Show unavailable feedback when a zero-credit player has undoable history but inventory is unavailable, including consent-blocked states. Keep the optional ad action visible.
- Permit an explicit ad request to retry SDK initialization after an initialization exception, still respecting `canRequestAds()` and existing load cooldowns.
- Log initialization exceptions and SDK load/display error domain, code and message for host diagnosis.

## E. Undo implementation

Existing wallet starts at three; the controller only deducts after successful engine restoration. Empty/failed Undo and zero balance do not modify board or credits. Main-thread controller changes are captured as a complete board/history/wallet snapshot and submitted to the existing ordered AtomicFile writer. Restart, replay and level navigation do not replace the wallet. Disk failure reporting remains intact. Engine, generator, artwork and animations were not modified.

## F. Reward integration

Default provider remains the real SDK adapter. Ads load only with consent permission, initialize once, consume loaded inventory once, guard concurrent presentations, expire stale inventory and reload after dismissal. Only the official earned callback delivers a single-use ticket for exactly one credit; duplicate earned events do nothing. Dismissal/display/load failures do not award credits. An earned credit is persisted before dismissal and is not revoked on dismissal. Process-owned state and detached Activity UI callbacks preserve an earned credit through Activity replacement. These are source observations and independent test evidence, not real SDK runtime verification.

## G. Consent/privacy

Retained UMP information update at launch, required consent form flow, `canRequestAds()` gates for initialization/loading/presentation, and required privacy options entry point. No consent bypass, production IDs, or privacy policy URL was added. Account-side consent behavior with Google's sample application ID needs device verification. Gameplay remains independent of ad requests.

## H. Migration

Schema 1 receives three credits; encoding writes schema 2 into the existing `player-progress-v1` file. Valid completion prefix, current identity, accepted history and generator version survive decoding and production-engine restoration. Schema 2 zero remains zero. Invalid schema-2 balances recover conservatively to zero. Existing ordered writes, owner tokens and atomic replacement remain unchanged. Independent JVM migration/round-trip tests pass. Added Android migration test uses a valid schema-1 board/history, checks displayed three, consumes one, reloads storage and checks two plus retained progression/identity. Android execution remains pending.

## I. JVM results

**159 independent JVM tests PASS**, rerun using JDK 17 and `tools/verify_offline.py --jvm-only`. Includes economy, exactly-once reward tickets, migration, explicit zero, rejection, restart/navigation, deadlock and existing Mahjong regressions. **10 Python host-workflow tests PASS**. Logs retained. This fallback compiles only independent Kotlin domain/presentation models; it does not compile MainActivity, the Google adapter, or Android instrumentation.

## J. Instrumentation results

**29 test methods present; zero compiled or executed here.** Existing tests cover reward/cancellation, deadlock, restart, Activity recreation, storage reload, progression, touches and animations. Extended 3→2→1→0 assertions, disabled zero-credit attempt and initial visibility; added schema-1 migration/persisted relaunch, victory visibility and unavailable-ad UI tests. FakeGameAds is in androidTest only; debug injection is ignored in release builds. Activity relaunch plus storage-cache reset tests are not proof of OS process termination/relaunch; the host manual procedure below covers that separately.

## K. Emulator results

`adb devices -l` succeeded and returned an empty device list. No emulator test, test ad playback, screenshot, runtime launch or installed save inspection occurred. No stale APK was installed.

## L. APK evidence

**No new APK.** New path, size, timestamp, SHA-256, package, merged metadata, alignment and signature: unavailable until standard Gradle succeeds. Existing **STALE diagnostic artifact**, deliberately not delivered:

```text
app/build/outputs/apk/debug/app-debug.apk
745875 bytes
2026-10-27 23:25:35.694855155 +0100
SHA-256 f246a09c203308e1e900f2e48a92ec00f061183f144c65133701d68895a7205b
```

That file is older than commit 929fe41 (23:42:31 +0100). Its relationship to the device's currently installed package cannot be confirmed without device access. It is not evidence of this iteration's build.

## M. Git

Parent commit: `929fe41876e89784fe2561f62016c1d905021ab2`. Changes are limited to the existing ad/UI adapter, instrumentation assertions, this report and evidence logs. Use `git --git-dir=.tilebloom-git log -1 --oneline` and `git --git-dir=.tilebloom-git status --short` for the final commit/status. No history rewrite or push.

## N. Remaining problems

Unrestricted standard Gradle compilation, resolved SDK compatibility, lint, Android tests, new APK verification, upgrade installation, real Google test ad, consent behavior, process relaunch, live save preservation and visual Mahjong regression verification remain pending. CODEX 12B does **not** meet release acceptance. Send exact host diagnostics if compilation fails; compilation errors cannot be inferred from this sandbox's pre-build failure.

## O. Exact host build/install/verification

Run in this project on an unrestricted Linux host with JDK 17, Android SDK 36, Build Tools 35.0.0 and network access to Google Maven/Maven Central. Keep the existing signing identity. Do not use the diagnostic APK or offline APK builder. The marker and forced packaging below prevent an old APK being mistaken for a fresh output.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p docs/codex12b/host
set -euo pipefail
./gradlew --console=plain :app:testDebugUnitTest 2>&1 | tee docs/codex12b/host/unit.log
touch /tmp/tilebloom12b-build-start
./gradlew --console=plain --rerun-tasks :app:assembleDebug 2>&1 | tee docs/codex12b/host/assemble.log
./gradlew --console=plain :app:lintDebug 2>&1 | tee docs/codex12b/host/lint.log
./gradlew --console=plain :app:assembleDebugAndroidTest 2>&1 | tee docs/codex12b/host/instrumentation-build.log
APK=app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"
test "$APK" -nt /tmp/tilebloom12b-build-start
stat -c '%n %s bytes %y' "$APK" | tee docs/codex12b/host/apk-stat.txt
sha256sum "$APK" | tee docs/codex12b/host/apk-sha256.txt
"$ANDROID_HOME/build-tools/35.0.0/aapt2" dump badging "$APK" | tee docs/codex12b/host/apk-badging.txt
"$ANDROID_HOME/build-tools/35.0.0/aapt2" dump xmltree "$APK" --file AndroidManifest.xml | tee docs/codex12b/host/apk-manifest.txt
"$ANDROID_HOME/build-tools/35.0.0/zipalign" -c -v 4 "$APK" | tee docs/codex12b/host/alignment.txt
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$APK" | tee docs/codex12b/host/signature.txt
```

Confirm package `com.tilebloom.game`, manifest App ID above, SDK manifest components/resources and dex contents, and unit/lint/build success before installing. Connect a Google Play-enabled emulator or authorized device and replace SERIAL below. Capture the pre-install package update time and installed certificate. The new APK must be newer and signed with the same certificate; if not, recover the original signing key and rebuild. An `INSTALL_FAILED_UPDATE_INCOMPATIBLE` is a blocker; do not uninstall or clear data to bypass it.

```bash
ADB="$ANDROID_HOME/platform-tools/adb"
"$ADB" devices -l
DEVICE=SERIAL
"$ADB" -s "$DEVICE" shell dumpsys package com.tilebloom.game > docs/codex12b/host/package-before.txt
"$ADB" -s "$DEVICE" shell pm path com.tilebloom.game
# Pull the base.apk path printed above, then inspect its signer:
# "$ADB" -s "$DEVICE" pull /printed/path/base.apk /tmp/tilebloom12b-installed.apk
# "$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --print-certs /tmp/tilebloom12b-installed.apk
"$ADB" -s "$DEVICE" install -r "$APK"
"$ADB" -s "$DEVICE" shell dumpsys package com.tilebloom.game > docs/codex12b/host/package-after.txt
"$ADB" -s "$DEVICE" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$ADB" -s "$DEVICE" shell am instrument -w com.tilebloom.game.test/android.test.InstrumentationTestRunner | tee docs/codex12b/host/instrumentation-runtime.txt
"$ADB" -s "$DEVICE" shell am force-stop com.tilebloom.game
"$ADB" -s "$DEVICE" shell am start -n com.tilebloom.game/.presentation.MainActivity
```

Instrumentation uses a separate test save, not the player's save. Require all 29 methods to pass, not merely an instrumentation APK build. Finish instrumentation before manual testing to ensure the default real provider is used.

Manual sequence on the existing installation:

1. Verify completed/unlocked levels and current board against the pre-upgrade game. A schema-1 player sees `Undo (3)` immediately, even with no accepted move. An already migrated player keeps their actual balance, including zero; do not reset their save to manufacture three credits.
2. For an initial-three player, make a match then Undo three times; verify 3→2→1→0 and correct restored tiles. Make another match; Undo is disabled and does not change board/balance. Record a screenshot.
3. Verify `Watch Ad +1 Undo` remains visible, with loading/unavailable feedback as appropriate. Handle any required consent normally; never bypass `canRequestAds()` to force a load. Restart must remain usable if consent/network blocks ads.
4. Explicitly request a real Google test rewarded ad. Confirm it is labeled as a test ad. Complete it; balance becomes one. Dismissal retains one; Undo spends it to zero. Repeat a voluntary ad request and close without earning; balance must remain zero. Ad durations/close availability vary: verify reward callback behavior rather than assuming every close cancels the reward.
5. Restart the level, advance/replay where available, and force-stop/start using the commands above. Verify the balance is unchanged and accepted history/progression remain valid. Never clear app storage. If the original player is already migrated at zero, use a separate emulator/test installation for the initial-three demonstration, not destructive edits to their save.
6. Verify Activity recreation, deadlock recovery at zero and with a rewarded credit, required privacy options when UMP reports REQUIRED, and existing selection/matching/animations on device. Record any failures.

Capture screenshots at launch, zero credits, real test-ad display, earned credit and post-relaunch. Example:

```bash
"$ADB" -s "$DEVICE" exec-out screencap -p > docs/codex12b/host/credits-zero.png
"$ADB" -s "$DEVICE" logcat -d -v threadtime TileBloomAds:V TileBloomSave:V Ads:V UserMessagingPlatform:V AndroidRuntime:E '*:S' > docs/codex12b/host/runtime.log
```

Do not mark complete until the new artifact and each device acceptance observation are supported by these host outputs.
