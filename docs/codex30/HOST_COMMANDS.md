# CODEX 30 host verification

Run from the project root in a normal terminal, with JDK 17 selected and the existing local signing configuration. Do not uninstall or clear application data.

```bash
./gradlew --no-daemon --console=plain :app:testDebugUnitTest
./gradlew --no-daemon --console=plain :app:assembleDebug
./gradlew --no-daemon --console=plain :app:lintDebug
./gradlew --no-daemon --console=plain :app:assembleDebugAndroidTest
stat -c '%n %s bytes %y' app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk
/home/walid/Android/Sdk/build-tools/35.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
/home/walid/Android/Sdk/build-tools/35.0.0/zipalign -c -v 4 app/build/outputs/apk/debug/app-debug.apk
/home/walid/Android/Sdk/build-tools/35.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Before installation, confirm the signer SHA-256 is exactly `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. If it differs, stop and restore the original signing configuration. Do not regenerate a key.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.tilebloom.game.AdvancedBoardVisualTest com.tilebloom.game.test/android.test.InstrumentationTestRunner
adb exec-out run-as com.tilebloom.game tar -cf - files/codex30-visual > docs/codex30/android-captures.tar
```

Extract and examine actual Android PNGs at 320, 360, 400 and 600dp. The capture test uses isolated Game instances, never writes player progress, and covers all eight families and five themes. Check gameplay screen composition separately with `adb exec-out screencap -p > docs/codex30/gameplay.png`; board-only captures cannot establish tray/control fit. Review available/blocked contrast, symbol privacy, layering and small-screen legibility before approval.

The JVM matrix writes `app/build/codex30-matrix.csv` under Gradle (or `build/codex30-matrix.csv` for the diagnostic JVM runner). Diagnostics do not replace Gradle/Lint, APK, signing or device evidence.
