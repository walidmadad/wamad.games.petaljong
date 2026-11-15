# Petaljong publishing checklist

Approved public names:

- Android launcher and in-app brand: **Petaljong**, unchanged spelling in all eight languages.
- Google Play listing title: **Petaljong: Mahjong Pair Match** (29 characters).
- Application ID and namespace: `com.wamad.petaljong`; all active Kotlin packages use this base namespace. Preserve storage identifiers and local signing configuration.

The Google Play title must later be entered manually in Google Play Console, including localized listings. Android resource changes do not update an existing Play listing. No listing submission or publication was performed for this rebrand.

Before distribution:

- Run the standard Gradle unit tests, debug assembly and Android Lint with JDK 17; inspect a freshly generated APK.
- Inspect the Debug signer; it is not a production upload key.
- Install the new package independently with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Never uninstall or clear the historical `com.tilebloom.game` app.
- The new identity starts with separate private data; verify its normal save/load and progression. No automatic cross-package transfer exists.
- Check launcher label/icon, centered Home title and tutorial in English, French, Spanish, Brazilian Portuguese, Arabic, Hindi, Japanese and German. Check Arabic RTL, Hindi/Japanese text, Settings and Privacy on device.
- Preserve the supplied botanical launcher artwork and existing ads/UMP configuration. Production release configuration remains subject to the existing release safeguards.

These are preparation and verification steps only; this checklist does not authorize publishing.

## First Play publication

The previous development application ID `com.tilebloom.game` is historical. Register only `com.wamad.petaljong` for this first publication; retain that identity afterward.

1. Check the AdMob account app association matches `com.wamad.petaljong`; Gradle does not update the account. Preserve configured production IDs unless an account-side correction is verified.
2. Create or select a dedicated upload key securely outside the repository, with the owner's approval. Never use the Debug key for production. Back up the key and credentials securely.
3. Configure a separate release signing config using ignored local credentials and explicitly attach it to the release build type. The existing optional local signing configuration applies only to Debug. No release signing was added by this migration.
4. Run `./gradlew --no-daemon --console=plain :app:verifyAdmobVariants :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease`.
5. Once release signing is configured, run `./gradlew --no-daemon --console=plain :app:bundleRelease`. Verify the resulting `app/build/outputs/bundle/release/app-release.aab` signature with `jarsigner -verify -verbose -certs`, package identity, version, and upload certificate. An unsigned Release APK is not ready for Play.
6. Enable Play App Signing, register the upload certificate, complete store listing, privacy policy, ads declaration, Data safety, content rating and testing requirements. Enter **Petaljong: Mahjong Pair Match** as the listing title. Upload and publication require a separate owner action.
