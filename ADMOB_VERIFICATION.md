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

Debug IDs are hardcoded Google TEST identifiers, separate from release. Release
builds intentionally fail at preReleaseBuild and have no production IDs. Before
removing that guard, configure an AdMob account, register the production app and
rewarded unit, set explicit release-only IDs, configure applicable UMP messages,
provide a real privacy policy, complete Google Play Data safety/ads disclosures,
review audience/age and advertising policies, test consent regions and revocation
on real devices, and perform signed production build verification. Do not use
production ads for development testing. No Firebase or mediation was added.

Official references checked for this iteration:
- https://developers.google.com/admob/android/rel-notes
- https://developers.google.com/admob/android/migration
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy
