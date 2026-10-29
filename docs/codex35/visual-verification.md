# Android visual verification — prepared, not executed

ADB cannot start its listener in this environment (`adb.txt`). No device captures or visual approval are claimed. No player level, storage, installation or app data was changed.

`MysteryTileVisualTest` renders isolated Canvas fixtures and writes PNGs under the target app's `files/codex35-visual` directory. It does not acquire GameStore or ProgressRepository. Fixtures cover available and laterally blocked jade backs, an authoritative reveal remaining on the board, rapid-tap rejection, later collection, all three leaf forms and the daisy inside the small tray, identical pixels for concealed Leaf/Sunflower, and Hard/Very Hard/Early Expert/Advanced Expert/higher Expert boards (12/23/40/88/151). Separate recognition captures expose all faces in a dedicated fixture, not a real saved game.

On a host with working Gradle and ADB:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew --no-daemon --console=plain :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.tilebloom.game.MysteryTileVisualTest com.tilebloom.game.test/android.test.InstrumentationTestRunner
```

Never uninstall or clear app data. Verify the app's signing certificate before updating. Copy fixture captures with `adb exec-out run-as com.tilebloom.game cat files/codex35-visual/NAME.png`.

Review green ceramic contrast across all five themes, blocked corner marks, physical upper-layer occlusion, and the three leaf silhouettes at 320dp and in the tray. Observe an actual reveal at approximately 0/130/260 ms with animations enabled; repeat with system animations disabled. Confirm a rapid double tap reveals only, then a fresh tap after 420 ms collects. Verify recreation/navigation during a flip leaves the saved face revealed. These human/device checks remain outstanding.
