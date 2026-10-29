# CODEX 40 — Eight-language support

A. Localization architecture: Android string resources, with English in `res/values` and seven complete locale directories. The existing framework Activity is retained. Android 13+ uses `LocaleManager.applicationLocales` as the authority and declares an explicit eight-entry locale configuration for system Settings. Android 8–12 uses a single legacy preference and a localized Activity base context. No dependency was added. See [Android per-app language documentation](https://developer.android.com/guide/topics/resources/app-languages).

B. Supported languages: English (`en`), French (`fr`), Spanish (`es`), Brazilian Portuguese (`pt-BR`), Arabic (`ar`), Hindi (`hi`), Japanese (`ja`), German (`de`). Gradle resource filtering also limits dependency translations to these languages. Chinese was not added.

C. Language selection: a Language/current-language row joins the existing Settings panel. The scrollable picker contains Automatic plus all eight native names, radio indicators, 52 dp minimum row heights, ivory text, gold indicators, rounded green surfaces and a Close button. No flags.

D. Automatic behavior: the empty override is the default. Android resource resolution follows the system locale list, using supported matches and the English default resources when none match. Selecting Automatic clears the override. No default locale is hardcoded into Activity startup.

E. Runtime switching: Android 13+ performs its normal locale configuration change; older devices recreate the Activity after persisting the selected tag. Every view is rebuilt with localized resources. The process-owned GameStore retains the same puzzle, seed, tray, history and wallets. Recreation now distinguishes a saved Activity from a fresh Home entry, so changing language cannot finalize a completed level merely because Home was visible. Revealed mystery tiles are not hidden by configuration-change stopping.

F. Translation coverage: 111 translatable string resources are present in every language, plus the undo balance array and pair-count plurals. TileBloom remains unchanged. This includes Settings, Home, gameplay feedback, terminal outcomes, loading, connectivity, rewards, privacy feedback, mystery instructions, tile names, content descriptions and accessibility announcements. Dynamic messages retain positional Android format arguments. Pair counts use `getQuantityString`; Arabic includes all six plural categories. Historical generator messages and developer diagnostics are not displayed by the current presentation and are left unchanged.

G. Arabic RTL: the existing `supportsRtl` declaration remains enabled; Activity/dialog layouts inherit Android directionality and existing start/end positioning. Next and Undo vectors support directionality, including explicitly setting the direction on manually drawn icon instances. The board and tray Views explicitly retain LTR coordinate geometry, so neither puzzle coordinates nor slot order are mirrored.

H. Hindi/Japanese typography: localized interface text uses Android's sans font fallback; no font files were added. Only the unchanged TileBloom brand uses the decorative serif face. Scroll containers and wrap-content text accommodate multiple lines. Device shaping, glyph coverage, line breaking and clipping checks are **NOT VERIFIED**. Instrumentation includes narrow-width complex-script TextView layout checks but could not execute here.

I. Persistence: framework locale storage is authoritative on Android 13+, including changes made through system Settings. Older Android versions persist a single language tag. On OS upgrade, a legacy explicit tag is handed to the framework once if no framework override already exists, then removed from legacy storage. Gameplay save formats are unchanged. Sound, credits and ad preferences use their existing storage.

J. Settings integration: Home and gameplay use the same existing Settings entry. Dialogs consume background touches and pause active gameplay ad-time accounting. Sound Effects and UMP Privacy remain available. Closing returns to the launching screen. The Home LEVEL card and its direct click behavior are retained; no Play button, Home arrow, separate online Home privacy/sound button or background music was introduced.

K. Ads/privacy: AdMob/UMP provider implementations, reward grants, interstitial frequency, consent and cooldown persistence are unchanged. Locale selection does not invoke an ad show, consume credits or navigate levels. Activity attachment follows the existing lifecycle. Google-managed consent forms are not manually translated. Real SDK behavior after locale changes remains **NOT VERIFIED** on a device.

L. Automated verification: see `docs/codex40`. Resource compilation/linking and main plus instrumentation Kotlin diagnostic compilation passed using cached tools and Java 17. Final changed icon code and interaction tests are also compiled separately. All **307 JVM tests passed** in 187.466 seconds (304 existing plus three localization resource tests). New host tests check exact locale configuration, complete resource-key coverage, matching formatting arguments and plural categories. Device tests cover all eight resources, English fallback, locale priority, dynamic formatting, Arabic direction, Hindi/Japanese layout, locale switching, saved state/credits/tray, relaunch, Automatic mode, sound, Home clickability and Privacy with fake ads. Existing gameplay/ad/audio/navigation tests remain in place. Instrumentation is compiled, **NOT EXECUTED**.

All three requested standard commands were attempted:

```
./gradlew --no-daemon --console=plain :app:testDebugUnitTest
./gradlew --no-daemon --console=plain :app:assembleDebug
./gradlew --no-daemon --console=plain :app:lintDebug
```

They fail before task execution because the wrapper lock is read-only. The installed Gradle distribution with Java 17 and a writable temporary Gradle home also fails before execution: no usable wildcard IP is available in this sandbox. Diagnostic compilation/JUnit execution is separate evidence, not a Gradle or Android Lint pass. No genuine missing-translation warning was suppressed.

M. Android screenshots: **NOT VERIFIED / NOT CAPTURED**. ADB cannot start its smartsocket listener (`Operation not permitted`). Required host capture matrix: Home + Settings for all eight languages; Arabic gameplay; Hindi victory; Japanese offline; German reward prompt; Portuguese loading; Spanish defeat. Narrow portrait, larger text, TalkBack, clipping/overlap, glyph shaping, RTL and real UMP interaction still require device review. No screenshots are fabricated.

N. APK: **NO FRESH APK GENERATED** because the supported Gradle build is blocked. The existing APK predates this localization change and must not be treated as CODEX 40. Existing artifact evidence only: package `com.tilebloom.game`, timestamp `2026-10-29 12:27:50 +0100`, 7,631,361 bytes, SHA-256 `b4eb139771623f4b4b8ade3beb34c5b75b6868a2cdb65b9d06f94d2db83a4dd9`. Its signer certificate matches `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. Fresh artifact timestamp/size/hash/package/alignment/signature/locales/picker verification remains outstanding. Signing files were not modified. No uninstall or player-data clearing occurred.

O. Git: local commit uses the existing `.tilebloom-git` history because the environment's `.git` directory is an empty read-only mount. No history rewrite, initialization, push or publication. Unrelated pre-existing `CODEX_18_REPORT.md` and `docs/codex26/` are excluded. No private signing material is staged.

P. Remaining limitations: a host with working Gradle and ADB is required for Android Lint, the fresh compatible signed APK, executed instrumentation and the complete screenshot/typography review. Translation resources are complete by key/format checks; native-speaker review and visual QA remain advisable before release. Nothing was published.
