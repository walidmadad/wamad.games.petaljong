# CODEX 31 host verification

Use a normal host terminal with JDK 17, Android SDK and the existing signing configuration. Never uninstall or clear data.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
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

Require package `com.tilebloom.game` and certificate SHA-256 `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. Stop on a mismatch; restore the original signing configuration rather than replacing keys. Compare the installed certificate before updating.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.tilebloom.game.TileOcclusionVisualTest,com.tilebloom.game.AdvancedBoardVisualTest com.tilebloom.game.test/android.test.InstrumentationTestRunner
adb exec-out run-as com.tilebloom.game tar -cf - files/codex31-visual > docs/codex31/android-captures.tar
```

Extract and inspect captures. Board captures use isolated games and never write player progress. The `upper-removed` capture removes exactly one upper tile; `opened` follows three pairs. Compare Levels 17–20 at all four widths. Verify no symbol differences bleed through opaque upper faces; inspect exposed lower artwork and muted side-blocked symbols.

Also capture the complete gameplay screen at each width with `adb exec-out screencap -p > <output.png>` on an isolated emulator/test profile. Board-only images cannot verify header/tray/footer fit or system insets. Review all five themes, visible blocked neighbors, dense stacks, Hint, Undo, victory/defeat, brightness settings and ad dismissal/restoration. Do not modify real player saves to reach levels. Use existing test-storage seams when preparing an emulator fixture.
