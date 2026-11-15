# CODEX 22 host verification and release configuration

No publish or upload step is authorized. Use Java 17 and a normal Linux shell
with network access, Android SDK 36/build tools 35.0.0, Google Maven and Maven Central.
The restricted environment could not start Gradle or ADB; see docs/codex22.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --console=plain :app:testDebugUnitTest
./gradlew --console=plain :app:assembleDebug
./gradlew --console=plain :app:lintDebug
# Or run tools/host_verify.sh to archive task logs and APK checks.
./tools/host_verify.sh
adb devices
./gradlew --console=plain :app:connectedDebugAndroidTest
```

The host verification script records fresh APK timestamp, bytes, SHA-256,
package, manifest test App ID, SDK DEX descriptors, alignment and signature.
Expected output: app/build/outputs/apk/debug/app-debug.apk. Existing APKs are
not evidence until these checks pass for this invocation.

## Deterministic Android UI tests

RewardInteractionTest replaces AdvertisingService.provider only in a debuggable
build. Its fake lives exclusively in androidTest. It uses a separate test-* save,
checks zero-credit prompts, cancel, unavailability, both rewards, duplicate
callbacks, persistence, restart and recreation. The production default is always
RewardedAdManager. These tests do not verify real Google delivery, UMP forms or
actual SDK initialization. Existing HintInteractionTest now expects the zero
Hint control to be accessible to the reward prompt.

## Real Google test-ad playtest (still required)

Use a Google Play/Google APIs emulator and the default production provider.
First back up private progress using an appropriate debug-only backup procedure;
install the freshly verified debug APK with `adb install -r` (never clear data).
Record existing level, tray, move history and both balances before and after.
Do not change the real save to force balances. Use an isolated emulator/profile
with a separate copied test fixture when zero-balance scenarios are needed.

Launch, allow UMP to update, and complete any required form. Check consent errors
in logcat; never override canRequestAds to force delivery. Consent messages for
the Google sample app may be unavailable; record this as a delivery blocker.
Play until Undo reaches zero; tap Undo, cancel once, then confirm Watch Ad.
Confirm Google's TEST label appears, complete the ad, dismiss, confirm exactly
one Undo, and press Undo separately. Repeat for Hint; press Hint separately and
confirm the CODEX 21 bright outline/bulb lasts four seconds. Capture screenshots,
logcat, balances and timestamps. Recreate Activity while showing an ad, then
restart the process and confirm no duplicated credits. Disable network, wait for
loaded ads to expire or use an isolated fresh process, request an ad, and verify
no reward and usable Restart. Test Home, victory/Next Level and privacy options.
No screen transition may show an ad without Watch Ad confirmation.

## Persistence and verification limits

The process-owned presentation captures an immutable reward purpose. The official
OnUserEarnedRewardListener is the only production path to grant a reward. A
one-shot ticket rejects duplicates, including after recreation. Fullscreen
failure cancels the ticket; dismissal retains an already-earned credit. There
is no server-side verification. A modified client can forge local state; process
death before a callback or before a successful atomic write may lose a reward.
If storage fails, the wallet stays unchanged and an explicit failure is shown;
there is no durable retry inbox or cross-process SDK callback recovery.

Rewards run on the main thread, capture the authoritative process controller,
queue an AtomicFile write behind earlier saves, wait for fsync, then change only
the selected wallet and refresh UI. No stale Activity wallet is used. This uses
the existing synchronous durable-transition mechanism, so slow storage can briefly
block UI; measure it on real devices before release. No schema migration occurs:
existing schema-5 zero balances remain zero and initial three credits stay intact.

## Before monetization

Debug uses Google's official demo App ID, rewarded units (both Hint and Undo),
and interstitial unit. Release reads ADMOB_APP_ID, ADMOB_REWARDED_HINT_ID,
ADMOB_REWARDED_UNDO_ID and ADMOB_INTERSTITIAL_ID from the ignored root file
`admob.local.properties`. There is no release fallback. `validateReleaseAdmob`
rejects missing/malformed values and Google's demo publisher IDs before release
tasks, including manifest and BuildConfig generation. Errors name keys only.
The existing rewarded manager selects a purpose-specific unit when the reward
dialog opens, invalidating stale loads when the purpose changes. UMP gating,
one-shot rewards and the interstitial frequency policy are preserved.

Run with Java 17 in a host terminal:

```bash
./gradlew :app:validateReleaseAdmob :app:verifyAdmobVariants :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
python3 tools/check_ads_apk.py app/build/outputs/apk/debug/app-debug.apk
```

`verifyAdmobVariants` generates both merged manifests and both BuildConfig files,
then compares the App ID and all three units to their variant's expected values.
It also checks `com.wamad.petaljong`, and prints no full production identifiers.
It does not assemble or publish release. A successful debug build outputs
`app/build/outputs/apk/debug/app-debug.apk` with demo ads.

CODEX 60 validation status: static checks confirm the local keys have valid
production ID syntax and are not Google's demo IDs. Gradle execution is blocked
in this restricted environment: the default wrapper cache is read-only; direct
Gradle with a writable temporary home fails to determine a usable wildcard IP.
Consequently neither generated variant verification nor a fresh APK build or
runtime test has completed for this change. Existing APKs are not evidence.
No application installation, uninstallation, data clearing or publishing occurred.

Configure applicable UMP messages, privacy policy and Play disclosures before
monetization; real SDK delivery and consent still require the device checks above.
No Firebase or mediation was added.

Official references checked for this iteration:
- https://developers.google.com/admob/android/rel-notes
- https://developers.google.com/admob/android/migration
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy

## CODEX 62 identity migration

Official application ID and namespace: `com.wamad.petaljong`. Check the AdMob account's Android package association matches this ID before monetization; Gradle does not update the account. No production identifiers or property keys were changed. Use only the Debug test ads for runtime verification.

The required command is:

```sh
./gradlew --no-daemon --console=plain :app:verifyAdmobVariants :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
```

The migration environment blocks Gradle startup (read-only wrapper cache; writable-home retry cannot determine a usable wildcard IP). Generated variants, fresh APKs and runtime ads therefore remain unverified. Previous APKs must not be used as migration evidence.
