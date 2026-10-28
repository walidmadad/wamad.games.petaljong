# CODEX 12C delivery report

A. Previous failure: the emulator was still running an older build. Standard Gradle could not write its wrapper lock in the restricted home cache; the standalone builder had an explicit ads-enabled abort. Thus the prior source changes never reached a new APK.

B. Removed active dependencies `com.google.android.gms:play-services-ads:25.5.0` and `com.google.android.ump:user-messaging-platform:4.0.0`, AdMob build constants, manifest metadata and network permissions. Removed the SDK manager, activity initialization, rewarded-ad/consent controls and associated resources. The old implementation remains in repository history. The legacy fake and adapter interface live only in instrumentation sources. No advertising SDK is in the APK.

C. Reused UndoCreditWallet and ProgressController. A new wallet starts at three. Successful engine restoration consumes exactly one; failed restoration, empty history and zero balance consume none. No production UI grants credits or replenishes them. Engine rules and level definitions are unchanged.

D. Reused schema-2 ProgressCodec, process-owned GameStore and AtomicFile repository. Schema-1 saves receive three once; schema-2 zero remains zero. Board history and credit balance share the same serialized atomic snapshot. Completed/unlocked levels, accepted moves, seed and generator version are retained by existing migration. Restart/replay/next level/activity recreation/relaunch retain the wallet. Tests passed for codec/controller persistence; device persistence tests were compiled but not executed here.

E. Green-and-gold Zen Garden UI retained. Undo balance remains visible before matching, at zero, in deadlock and on victory. One refresh path uses packaged exact labels Undo (3), Undo (2), Undo (1), Undo (0). Deadlock no longer substitutes Undo Last Move. The Undo button has one line and shares footer width with Restart. No Watch Ad or privacy control remains.

F. All existing JVM tests retained. Initial full diagnostic run: OK (159 tests), 3.806 seconds. Added completed-level replay regression, then final JVM run: OK (160 tests), 3.767 seconds, zero failures. Instrumentation sources compiled successfully; tests requiring the removed ad UI were adapted/removed, with zero-credit recreation and relaunch coverage retained. UI assertions were NOT executed on a device.

G. Required first attempts, both exit 1:

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Both failed on `/home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck` (read-only filesystem).

Successful APK build, exit 0:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py
```

Final tests after adding the replay regression, exit 0:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py --jvm-only
```

H. APK: `app/build/outputs/apk/debug/app-debug.apk`, package `com.tilebloom.game`, min SDK 26, target SDK 36. Genuine diagnostic debug APK; not a successful standard Gradle build. Timestamp: 2026-10-28 18:18:07.772557 +01:00.

I. Size: 749971 bytes. SHA-256:

```
62f43621b554bad42cb8fa8d8fe871ab89f6e10a71a0f5c8a07c11191e832b6e
```

Previous APK SHA-256:

```
f246a09c203308e1e900f2e48a92ec00f061183f144c65133701d68895a7205b
```

J. `zipalign -c -v 4` passed. `apksigner verify --verbose --print-certs` passed, v2 and v3 signatures, one signer. AAPT2 confirmed the package and all four literal Undo labels in array/undo_balances. DEX inspection confirmed MainActivity, UndoCreditWallet and string-array lookup; Google Ads, UMP and RewardedAdManager descriptors absent. Evidence: `docs/codex12c/apk-*.txt`.

K. ADB was attempted. Its server cannot bind the local smart socket: Operation not permitted. Installation, launch, screen inspection, interactive Undo and device relaunch were NOT performed. Run in a host terminal with the existing emulator:

```
cd /home/walid/Documents/projects/androidGames/TileBloom
/home/walid/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/home/walid/Android/Sdk/platform-tools/adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

Do not uninstall or clear data. A new/legacy wallet shows Undo (3); an existing explicit zero correctly shows Undo (0). Match and Undo three times to check 2, 1, 0, then force-stop and reopen:

```
/home/walid/Android/Sdk/platform-tools/adb shell am force-stop com.tilebloom.game
/home/walid/Android/Sdk/platform-tools/adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

L. Used the existing `.tilebloom-git` repository because `.git` is a read-only placeholder in this environment. Parent commit: 00c4bb7. Delivery commit message: `fix: deliver three persistent undo credits without ads`. No reinitialization or publication.

M. Remaining problems: standard Gradle and ADB are restricted in this environment; emulator verification is pending on the host. No Android Lint run. The new signed, aligned APK was successfully generated and verified locally.
