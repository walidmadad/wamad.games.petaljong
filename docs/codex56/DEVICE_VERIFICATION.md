# CODEX 56 — device verification pending

This environment cannot run Gradle or ADB sockets. No Android screenshot, fresh APK or signer verification was obtained. The user’s Level 45 screenshot is not present in the supplied workspace/conversation. Do not consider this change visually approved.

On a host with JDK 17 and Android access:

1. Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.
2. Check application ID `com.tilebloom.game` and verify the APK signer SHA-256 is `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6` using SDK apkanalyzer/apksigner. Stop before installing if the signer differs.
3. Use `adb install -r app/build/outputs/apk/debug/app-debug.apk` to preserve player data. Never uninstall or clear data.
4. Install `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` and run `adb shell am instrument -w -e class com.tilebloom.game.MysteryTileVisualTest com.tilebloom.game.test/android.test.InstrumentationTestRunner`.
5. Pull isolated fixture images with `adb exec-out run-as com.tilebloom.game cat files/codex56-visual/<name>.png`. Review all five `natural-green-<theme>-backs.png` fixtures, available/lateral/top blocking, and the added Level 45 fixture. These Canvas fixtures do not write progression and do not replace a gameplay screenshot.
6. Capture normal Level 45 gameplay with `adb exec-out screencap -p` and compare with the user’s original screenshot. Confirm a visibly medium-green face, distinct blocked tiles, readable golden flower, ceramic depth and contrast against brown, forest-green, turquoise, purple and gold backgrounds.

The previous production renderer is saved as `MahjongBoardView.before.kt.txt` for a controlled comparison in a separate checkout. Do not overwrite unrelated source changes or player saves to stage a comparison.
