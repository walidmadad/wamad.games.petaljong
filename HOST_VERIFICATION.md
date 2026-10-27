# CODEX 02 — unrestricted Linux host verification

Run these commands **outside the restricted Codex sandbox**, from the existing TileBloom directory. They require Java 17, Android SDK platform 36 / Build Tools 35.0.0, and access to cached dependencies or Google's/Maven repositories. Do not change host security settings.

Set `ANDROID_HOME` to your actual SDK path if not already configured. For example, the conventional per-user SDK directory is `$HOME/Android/Sdk`. Then:

```sh
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
java -version
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
adb devices -l
emulator -list-avds
```

Launch an installed compact phone AVD through Android Studio or `emulator -avd YOUR_AVD_NAME` if no device is connected. Unlock its screen and leave it foregrounded. If multiple devices are connected, set `ANDROID_SERIAL` to the chosen serial from `adb devices -l` for ADB commands. The Gradle connected task runs on connected devices; disconnect extra devices if you want one target.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
adb exec-out screencap -p > /tmp/tilebloom02-initial.png
./gradlew :app:connectedDebugAndroidTest
adb logcat -b crash -d > /tmp/tilebloom02-crashes.txt
adb logcat -d -s AndroidRuntime > /tmp/tilebloom02-runtime.txt
```

If signing identities differ between a fallback APK and a later Gradle APK, installation can return `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Keep one signing identity, or explicitly uninstall this prototype with `adb uninstall com.tilebloom.game` before installing again. Uninstall clears its task state; no durable saves are implemented.

Reports:

- JVM: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`
- Instrumentation: `app/build/reports/androidTests/connected/debug/index.html`

## Automated Android checks

Six instrumentation test methods use the platform test runner:

1. Launch and Restart smoke check.
2. All twelve witness pairs via injected **screen-coordinate** DOWN/UP events, selection assertions, tile/counter assertions, victory, Play Again.
3. A reproducible coordinate-tapped no-moves sequence and Restart.
4. Selection, deselection, replacement, blocked upper-tile taps, and drag-out-and-back cancellation.
5. Restart while a touch is pending and blocked feedback is scheduled.
6. Activity recreation after one match and a selection, followed by another coordinate-tapped match.

Coordinates come from the view's shared tile geometry plus its actual screen location. Tests do not invoke `Game.match` or `Game.select`. Read-only presentation queries support assertions. These tests have only been compiled in the sandbox; run them here to establish actual Android results.

## Manual screen and lifecycle checks

Repeat on a compact phone (approximately 320 × 568 dp) and a larger phone (approximately 412 × 915 dp). Verify the complete screen, including system bars, rather than assuming the JVM board-viewport tests cover text layout.

- Initial board: 24 tiles, 12 remaining pairs, two clearly stacked layers, visible Restart.
- Tap upper left tile, then tap it again: select, deselect. Its matching partner is directly below it on the upper layer.
- Tap an inner upper tile while a free tile is selected: no selection replacement, removal or tap-through; brief restrained border feedback.
- Tap a different free symbol: selection moves. Tap empty space: board remains intact.
- Drag away from a pressed tile and return, cancel a touch, and try two fingers: no unintended selection/match.
- Remove upper pairs from left to right; then lower columns left to right, pairing rows 0/1 and 2/3 in each column. Expect exactly twelve matches and Garden Cleared.
- Play Again and repeated Restart restore 24 tiles, twelve pairs and no selection, including immediately after a blocked tap.
- For a repeatable no-moves state, use tile IDs `(12,15), (14,23), (16,20), (13,21), (0,4)`. Lower IDs are `row*4+column`; upper IDs are `16+row*4+column`. Expect fourteen tiles, seven remaining pairs, No More Matches, and working Restart. This sequence is JVM-engine verified; manually check the Android feedback.
- Background via Home and return: board/selection should remain intact.
- Enable the developer option “Don't keep activities,” leave and return, then disable it: inspect activity restoration. Also use the compiled recreation instrumentation check.
- To check process recreation: make a match and select a tile; press Home; run `adb shell am kill com.tilebloom.game`; return using Recents (do not force-stop). Inspect board, selection and counter. State recovery depends on Android retaining the task's saved instance state.
- A force-stop, task dismissal, reboot or fresh launch is **not** guaranteed to restore progress. There is no durable storage.
- Inspect large font settings, TalkBack status announcements, symbol legibility, rounded borders and tile thickness. The board still lacks individual accessible tile navigation/actions.
- Capture screenshots at initial, selected, victory and no-moves states. Record device/API/density/font scale and actual failures in `VALIDATION.md` before calling runtime verification complete.
