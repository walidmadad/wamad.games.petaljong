# CODEX 55 device verification — pending

No Android screenshots were captured in this restricted environment. No new APK was built or installed. Do not treat the saved source or fixtures as visual approval.

The pre-change production renderer is preserved in `MahjongBoardView.before.kt.txt`. To obtain a controlled old/new comparison on an authorized host, capture the same isolated test board with that renderer first, then restore the new renderer and repeat. Do not overwrite unrelated working-tree changes.

1. Use JDK 17 and run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.
2. Verify APK application ID `com.tilebloom.game` and SHA-256 signer `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6` using SDK apkanalyzer/apksigner before installing.
3. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Never uninstall or clear data.
4. Install the androidTest APK and run `adb shell am instrument -w -e class com.tilebloom.game.MysteryTileVisualTest com.tilebloom.game.test/android.test.InstrumentationTestRunner`.
5. Pull fixtures from the target app private `files/codex55-visual` directory with `adb exec-out run-as com.tilebloom.game cat files/codex55-visual/<name>.png`. Fixtures use isolated Game objects and do not write player progression.
6. Compare available, laterally blocked, partially covered and revealed mystery tiles alongside ordinary ivory tiles. Review all five `happy-garden-<theme>-backs/revealed` pairs at normal phone size. Capture the OLD renderer separately before claiming before/after evidence.
7. Launch the normal app and capture actual gameplay with multiple mysteries using `adb exec-out screencap -p`. Test first reveal, second collect, switching, direct match, mismatch, Hint and Undo. Check saved progress after a normal restart.

Rendering uses cached gradients and paths; no bitmap decoding, blur, animation dependency, or geometry change was added. Theme contrast and frame pacing still require actual device review.
