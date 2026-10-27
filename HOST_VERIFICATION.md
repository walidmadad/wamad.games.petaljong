# CODEX 03 — Linux host verification

Run from the existing TileBloom directory in a normal Linux terminal outside the Codex sandbox. Do not change operating-system security settings. Java 17, SDK platform 36 and Build Tools 35.0.0 are required. Gradle dependencies must be cached or downloadable. Configured versions: Kotlin 2.2.10, AGP 8.10.0, Gradle 8.13. The standalone diagnostic compiler is separately pinned to 1.9.24.

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

Stop and investigate actual configuration/compiler errors. Sandbox startup errors are not source errors. This session failed at wrapper lock creation (read-only ~/.gradle), then at FileLockContentionHandler / usable wildcard IP with writable temporary Gradle home. No Gradle task ran. The host build may need to download AGP 8.10.0; it was not cached in this sandbox.

## Device, installation and launch

```sh
adb version
adb devices -l
emulator -list-avds
```

If no device has status `device`, connect and authorize a USB-debugging phone or start an installed AVD in Android Studio. Alternatively, run `emulator -avd YOUR_AVD_NAME` in another terminal, using a name actually listed above. Previously listed here: 11.0, Medium_Phone, Medium_Tablet_2. Wait for boot, unlock the screen and rerun adb devices. `unauthorized` requires accepting the phone's prompt; `offline` is not a usable device. If no device/AVD is available, stop device checks and record NOT EXECUTED; install a system image/AVD through SDK Manager first.

If several devices exist, set ANDROID_SERIAL to the chosen serial for ADB commands. Gradle connected tests may run on all connected devices; disconnect other targets for a single-device run.

```sh
adb shell getprop sys.boot_completed
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
adb shell pidof com.tilebloom.game
adb exec-out screencap -p > /tmp/tilebloom03-initial.png
./gradlew :app:connectedDebugAndroidTest
adb logcat -b crash -d > /tmp/tilebloom03-crashes.txt
adb logcat -d -s AndroidRuntime > /tmp/tilebloom03-runtime.txt
```

Installation must print `Success`. Launch should report `Status: ok`, show the game and leave its process running; inspect the screen and crash logs too. A successful am start alone does not prove gameplay. Do not clear existing logs before collecting a failure. If `INSTALL_FAILED_UPDATE_INCOMPATIBLE` occurs, fallback and Gradle debug keys differ. Keep the same key, or intentionally uninstall with `adb uninstall com.tilebloom.game` and install again; uninstall loses task state.

Reports: JVM `app/build/reports/tests/testDebugUnitTest/index.html`, Lint `app/build/reports/lint-results-debug.html`, Android `app/build/reports/androidTests/connected/debug/index.html`. Read XML/results for actual executed, passed, failed and ignored counts.

Eight instrumentation methods cover launch, coordinate-tapped victory and deadlock, selection/blocked/drag behavior, restart during touch/feedback, activity recreation, repeated taps during feedback/after restart, and batched release movement. The last test dispatches a constructed historical MotionEvent through the real view; other gameplay tests inject screen coordinates. None invoke engine select/match directly. They are compiled only in this sandbox.

## Screen and state checks

Use PLAYTEST_01.md for the human session. Capture initial, selected, victory and deadlock screens on compact (~320×568 dp), typical (~360×800 dp), and larger (~412×915 dp) portrait phones. Record actual dimensions, density, API and font scale. Repeat at increased system font size. Inspect all controls, system bars, symbol readability and overlap order. Geometry-only JVM tests do not establish phone usability.

For a reproducible deadlock from Restart, match IDs (12,15), (14,23), (16,20), (13,21), (0,4). Lower IDs = row*4+column; upper IDs = 16+row*4+column, using zero-based rows/columns. Expect fourteen tiles, seven pairs, No More Matches and working Restart.

Check recreation on a fresh board, after several moves, with selection, victory and deadlock. The automated recreation test covers one move plus selection. For other states, use the developer option Don't keep activities, leave and return, then restore the previous option setting. Check exact board, selection, counts, terminal status and working Restart.

For process recreation: make a match and select a tile, press Home, run `adb shell am kill com.tilebloom.game`, then return through Recents. Do not force-stop. Recovery depends on Android retaining saved task state. Force-stop, task dismissal, reboot and fresh launch have no durable-save guarantee. No persistent progression exists.

Individual screen-reader tile actions are not implemented; assess TalkBack announcements separately. Save actual failures and screenshots in VALIDATION.md. Do not claim full Android validation from an APK, source review or a compiled test suite.
