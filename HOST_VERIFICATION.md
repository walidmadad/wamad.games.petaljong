# CODEX 12 — Current status

CODEX 12 needs an unrestricted Linux host with JDK 17, Android SDK 36, Google Maven access and a working ADB server. Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Alternatively use `./tools/host_verify.sh` for logged build verification, then run connected tests separately. Install only the newly verified Gradle APK using `adb install -r app/build/outputs/apk/debug/app-debug.apk`; preserve installed data and signing identity. Instrumentation uses a deterministic androidTest-only fake. Separately launch the normal application (without test injection) and earn one **Google test** reward. Test IDs: app `ca-app-pub-3940256099942544~3347511713`, rewarded `ca-app-pub-3940256099942544/5224354917`. Verify consent and required Privacy options under a properly configured UMP test account/device. The sample app may not have a required form configured; do not interpret that as consent validation.

Current attempts: wrapper lock is read-only; direct installed Gradle with temporary home fails on unavailable wildcard IP. ADB listener creation is prohibited. No new APK is verified. The standalone fallback supports JVM tests only and explicitly refuses to build an ads APK. Do not install or label stale artifacts as CODEX 12. See [report](CODEX_12_REPORT.md).

---

# CODEX 11 verification addendum

Use JDK 17 for the cached Kotlin 1.9.24 diagnostic pipeline; default Java 25 is incompatible. Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py` if standard Gradle is unavailable. This fallback does not run Gradle or Lint.

The CODEX 11 sandbox standard build stops at a read-only wrapper lock. ADB cannot install its socket listener (Operation not permitted). Do not repeat these unchanged failures. Run the established host script and connected tests outside this restriction, then the CODEX 11 checklist in PLAYTEST_01.md. Save-schema 1 has durable restoration; older statements below denying persistent progress are historical and superseded by CODEX 09–11.

---

# CODEX 10 supplement

Current evidence and exact APK identity are in VALIDATION.md. Level 1 is now winding v3, seed 123; all historical Level 1 coordinate/ID sequences below are obsolete. Use the current witness from Levels.get(1), or CODEX_10_REPORT.md section F, for verification. Instrumentation now has 15 methods (compiled only here). Actual runtime and save persistence remain unverified. Use the CODEX 10 checklist in PLAYTEST_01.md, including real process restart and a same-signature update from CODEX 09 without clearing data. Schema 1 remains durable; old statements below claiming no persistent progression are historical and no longer apply.

Standard Gradle and ADB failures were already diagnosed in this unchanged sandbox and were not repeatedly retried. Run the host commands below only in an authorized unrestricted environment with Java 17. Do not uninstall a player installation to work around a signing mismatch; preserve its app data.

---

# CODEX 09 persistence verification supplement

This section supersedes historical runtime status below. Current sandbox: wrapper cannot create its cache lock (read-only filesystem); ADB cannot open its server listener (Operation not permitted). Standard Gradle tasks, Lint and connected instrumentation were NOT EXECUTED. Diagnostic builds are separate evidence.

On an unrestricted authorized host, from this repository:

```sh
bash tools/host_verify.sh
python3 tools/test_host_verify.py
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
adb devices
./gradlew :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

Instrumentation uses private test-smoke-progress storage and cleans it before/after each test; the storage-failure test uses test-blocked. Production progression is not loaded or deleted by tests. Tests include a fresh activity launch without Bundle and real AtomicFile reload after dropping the process cache; these are not fresh-process tests.

Manual fresh-process check (developer/playtest data only): play several pairs, note level/seed identity and counter, press Home, then:

```sh
adb shell am force-stop com.tilebloom.game
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
adb logcat -d -s TileBloomSave AndroidRuntime
adb shell run-as com.tilebloom.game cat files/player-progress-v1
```

Check exact removed tiles, remaining pairs and absence of old effects. Repeat after Level 2 victory before Next, after Level 5 victory, and during a deadlock. Verify replay/restart keep completions and reproduce the same definition. Install an update with matching signature using adb install -r and repeat. Do not clear application data. Different debug signing identities require special care; do not uninstall a real player installation to bypass a signature mismatch.

Check the level panel: current Continue, locked rows disabled, completed rows marked, explicit confirmation for replacing an attempt, cancellation, system Back, bottom navigation insets. Complete all five witness paths through geometry-derived instrumentation helpers. Visually inspect the revised Level 1 arrangement, single centered victory panel, gold/green buttons, Next/Replay, and final-level handling. No current screenshots establish these checks yet.

---

# TileBloom — Linux host verification

Run from the existing TileBloom directory in a normal Linux terminal outside the Codex sandbox. Do not change operating-system security settings. Java 17, SDK platform 36 and Build Tools 35.0.0 are required. Gradle dependencies must be cached or downloadable. Configured versions: Kotlin 2.2.10, AGP 8.10.0, Gradle 8.13. The standalone diagnostic compiler is separately pinned to 1.9.24.

## Run the host workflow

From a normal Linux terminal in this project:

```sh
bash tools/host_verify.sh
```

First configure Java 17 and the SDK as described below. The script finds the project root even when invoked from another directory. It reads the SDK from ordinary Linux `sdk.dir` entries in local.properties (including escaped spaces, colons and backslashes), then ANDROID_HOME or ANDROID_SDK_ROOT. It does not source local.properties or change SDK/JDK/Gradle installations, global configuration or security settings. Use a plain absolute Linux SDK path; Java-properties Unicode escapes and continued lines are not supported by the script's path reader.

Each run writes a unique `app/build/host-verification/run-*` directory containing context, exact commands, `results.tsv` exit codes, individual logs and `summary.txt`. Successful assemble output is checked for size, package/API metadata, alignment and signature, then copied there as `app-debug.apk`. Existing reports are also copied for inspection; copied historical reports do not establish execution in this invocation. Use commands/exit codes and report timestamps together.

Exit 0 means the requested host build/artifact checks passed; it does **not** mean Android launched or a person played. Exit 1 means a check failed; inspect its log. Preflight or wrapper-version failure stops prerequisite-dependent work. Otherwise JVM, build and Lint run independently so failures remain visible. A failed assemble never labels an existing fallback APK as its output. No fallback is automatically invoked. Standard Gradle may download its declared dependencies as usual.

Do not run the script inside the known restricted sandbox to rediscover the same startup errors. No unrestricted host run has been received as of CODEX 04. The script itself was checked with `bash -n` and ten temporary fake-tool tests (`python3 tools/test_host_verify.py`); those tests provide no Android or real Gradle success evidence.

## Environment and standard build

```sh
java -version
printf 'JAVA_HOME=%s\nANDROID_HOME=%s\nANDROID_SDK_ROOT=%s\n' "$JAVA_HOME" "$ANDROID_HOME" "$ANDROID_SDK_ROOT"
cat local.properties
```

Find the SDK path in Android Studio's SDK Manager. Set ANDROID_HOME to that actual path, or keep an untracked local.properties containing sdk.dir=/actual/sdk/path. Do not assume another machine has the same SDK path. For the commands below set ANDROID_HOME to the path even when local.properties is used.

```sh
export ANDROID_HOME=/actual/sdk/path
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
ls "$ANDROID_HOME/platforms/android-36/android.jar"
ls "$ANDROID_HOME/build-tools/35.0.0"
./gradlew --version
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Stop and investigate actual configuration/compiler errors. Sandbox startup errors are not source errors. CODEX 03 failed at wrapper lock creation (read-only ~/.gradle), then at FileLockContentionHandler / usable wildcard IP with writable temporary Gradle home. No Gradle task ran. The host build may need to download AGP 8.10.0; it was not cached in this sandbox.

## Device, installation and launch

```sh
adb version
adb devices -l
emulator -list-avds
```

If no device has status `device`, connect and authorize a USB-debugging phone or start an installed AVD in Android Studio. Alternatively, run `emulator -avd YOUR_AVD_NAME` in another terminal, using a name actually listed above. Previously listed here: 11.0, Medium_Phone, Medium_Tablet_2. Wait for boot, unlock the screen and rerun adb devices. `unauthorized` requires accepting the phone's prompt; `offline` is not a usable device. If no device/AVD is available, stop device checks and record NOT EXECUTED; install a system image/AVD through SDK Manager first.

If several devices exist, set ANDROID_SERIAL to the chosen serial for ADB commands. Gradle connected tests may run on all connected devices; disconnect other targets for a single-device run.

After the host script passes, use its standard Gradle APK for the following commands. Run each step in order; stop and inspect its log if it fails. Replace YOUR_SERIAL with the single selected, booted device. Keep other devices disconnected for the Gradle connected test.

```sh
export ANDROID_SERIAL=YOUR_SERIAL
device_evidence=$(mktemp -d "$PWD/app/build/host-verification/device-XXXXXX")
adb devices -l > "$device_evidence/devices.txt" 2>&1
adb shell getprop ro.build.version.sdk > "$device_evidence/android-api.txt"
adb shell wm size > "$device_evidence/screen-size.txt"
adb shell wm density > "$device_evidence/screen-density.txt"
adb shell settings get system font_scale > "$device_evidence/font-scale.txt"
adb shell getprop sys.boot_completed
sha256sum app/build/outputs/apk/debug/app-debug.apk > "$device_evidence/apk-sha256.txt"
adb install -r app/build/outputs/apk/debug/app-debug.apk > "$device_evidence/install.log" 2>&1
install_code=$?
printf 'install exit code: %s\n' "$install_code" | tee "$device_evidence/install-exit.txt"
cat "$device_evidence/install.log"
```

Continue only after installation prints `Success` and exits 0:

```sh
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity > "$device_evidence/launch.log" 2>&1
launch_code=$?
printf 'launch exit code: %s\n' "$launch_code" | tee "$device_evidence/launch-exit.txt"
cat "$device_evidence/launch.log"
adb shell pidof com.tilebloom.game
adb exec-out screencap -p > "$device_evidence/initial.png"
```

Confirm the game is visible and usable. Capture this initial screenshot before running instrumentation, which restarts/changes the board. Then:

```sh
./gradlew --console=plain :app:connectedDebugAndroidTest > "$device_evidence/instrumentation.log" 2>&1
instrumentation_code=$?
printf 'instrumentation exit code: %s\n' "$instrumentation_code" | tee "$device_evidence/instrumentation-exit.txt"
adb logcat -b crash -d > "$device_evidence/crashes.txt"
adb logcat -d -s AndroidRuntime > "$device_evidence/runtime.txt"
```

Collect crash logs even when launch/tests fail. Do not interpret command exit 0 alone as nine passing tests; inspect Gradle output and connected test XML for executed/pass/failure/skipped counts. Preserve `app/build/outputs/androidTest-results/connected` and `app/build/reports/androidTests/connected` with these device logs if generated. If installation or launch fails, record its exit code/log and do not mark subsequent steps as executed.

Installation must print `Success`. Launch should report `Status: ok`, show the game and leave its process running; inspect the screen and crash logs too. A successful am start alone does not prove gameplay. Do not clear existing logs before collecting a failure. If `INSTALL_FAILED_UPDATE_INCOMPATIBLE` occurs, fallback and Gradle debug keys differ. Keep the same key, or intentionally uninstall with `adb uninstall com.tilebloom.game` and install again; uninstall loses task state.

Reports: JVM `app/build/reports/tests/testDebugUnitTest/index.html`, Lint `app/build/reports/lint-results-debug.html`, Android `app/build/reports/androidTests/connected/debug/index.html`. Read XML/results for actual executed, passed, failed and ignored counts.

Nine instrumentation methods cover launch, coordinate-tapped victory and deadlock, selection/blocked/drag behavior, restart during touch/feedback, activity recreation, repeated taps during feedback/after restart, batched release movement, and match-effect completion/restart. The batched release test dispatches a constructed historical MotionEvent through the real view; gameplay tests inject screen coordinates or dispatch local MotionEvents through the real view. None invoke engine select/match directly. They are compiled only in this sandbox.

## Screen and state checks

Use PLAYTEST_01.md for the human session. Capture initial, selected, victory and deadlock screens on compact (~320×568 dp), typical (~360×800 dp), and larger (~412×915 dp) portrait phones. Record actual dimensions, density, API and font scale. Repeat at increased system font size. Inspect all controls, system bars, symbol readability and overlap order. Geometry-only JVM tests do not establish phone usability.

For a reproducible deadlock from Restart, match IDs (12,15), (14,23), (16,20), (13,21), (0,4). Lower IDs = row*4+column; upper IDs = 16+row*4+column, using zero-based rows/columns. Expect fourteen tiles, seven pairs, No More Matches and working Restart.

Check recreation on a fresh board, after several moves, with selection, victory and deadlock. The automated recreation test covers one move plus selection. For other states, use the developer option Don't keep activities, leave and return, then restore the previous option setting. Check exact board, selection, counts, terminal status and working Restart.

For process recreation: make a match and select a tile, press Home, run `adb shell am kill com.tilebloom.game`, then return through Recents. Do not force-stop. Recovery depends on Android retaining saved task state. Force-stop, task dismissal, reboot and fresh launch have no durable-save guarantee. No persistent progression exists.

Individual screen-reader tile actions are not implemented; assess TalkBack announcements separately. Save actual failures and screenshots in VALIDATION.md. Do not claim full Android validation from an APK, source review or a compiled test suite.

## Return evidence for the next iteration

Keep the host run directory, device evidence, connected test XML/reports, screenshots and the completed PLAYTEST_01.md. Record installation and launch exit codes, observed screen behavior, exact APK hash, device/API/font scale, nine-test counts and any reproduction steps in VALIDATION.md. Host scripts and old fallback logs alone are not runtime evidence.

The next external action is to run `bash tools/host_verify.sh` in the regular Linux terminal, then follow the device steps above. Return the resulting logs/screenshots and genuine player feedback for the next iteration. CODEX 06 added visual polish and a new diagnostic APK; prior user-reported emulator screenshots do not verify that updated APK. If no device is available, complete host build verification and explicitly leave Android steps NOT EXECUTED. Do not schedule another identical sandbox build attempt.
