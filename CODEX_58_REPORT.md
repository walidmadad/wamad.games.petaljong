# CODEX 58 — Petaljong native green startup

A. Root cause and evidence: MainActivity previously inherited AppTheme without dedicated splash attributes or a launcher-specific startup theme. There is no AndroidX SplashScreen dependency. The current source already has a dark windowBackground (#102F27), so the reported white screenshot cannot conclusively be attributed to that current resource: the installed build/launcher behavior could not be inspected. Explicit framework splash resources now remove dependence on inferred system defaults. No claim is made that the old screenshot was reproduced.

B. Implementation: launcher Activity uses AppTheme.Starting. API 31+ configures android:windowSplashScreenBackground, android:windowSplashScreenAnimatedIcon and a matching icon background. These are framework attributes, not AndroidX attributes. The framework controls splash removal on first draw; installSplashScreen() and postSplashScreenTheme are AndroidX integration mechanisms and are not required here. MainActivity explicitly selects AppTheme.Running before super.onCreate. API 26–30 uses the same artwork and green in the starting-window drawable. No added Activity, dependency, splash hold, custom exit animation or second splash view.

C. Final color: #173F35, the exact permanent HomeTheme.palette.backgroundTop (rather than the suggested approximate #194B3E). Home's approved gradient is unchanged. Runtime window background and initial system bars use this green. Existing light system icons and normal system contrast/insets behavior are retained; existing content sets Home's bottom bar color as before. No night override can switch these new resources to white.

D. Official artwork: reuses mipmap/ic_launcher_foreground unchanged. No generated artwork, extra gold frame, white circle or changed launcher resource. The 232dp foreground bitmap sits centered on a transparent 288dp canvas. Across all five densities every nontransparent pixel lies within 93.47dp of center, inside the documented 96dp radius safe area. Static pixel geometry and source artwork were inspected; actual Android rendering and masking remain unverified. The original launcher assets were pre-existing untracked workspace files and remain untouched.

E. Transition: the starting window gives way to the existing first content draw. Runtime window uses matching green; Home retains title, artwork, level card, settings and gradient. No repeated in-app logo, artificial duration or animation is introduced. Flash-free rendering is an intended outcome pending Android verification, not a claimed observation.

F. Connectivity: CODEX 57's CHECKING/ONLINE/OFFLINE reducer, 1,500ms initial bound, 600ms recovery window, observer lifecycle and neutral green checking presentation are unchanged. The system splash never waits on connectivity. Existing offline handling remains responsible for genuine failures. Level-generation loading is unchanged.

G. Performance: no new startup work, network request, thread, synchronization, allocation in Activity code or startup delay. The system uses a bounded existing icon resource with a simple drawable wrapper. No measured cold-start timing is available.

H. Android capture: unavailable. ADB cannot bind its server socket (Operation not permitted); see docs/codex58/adb.log. No screenshots/recording or on-device visual verification is claimed. Tests A–F (online/offline cold starts, warm start, background return, small/large devices, dark mode and multiple API levels) remain unexecuted. No app was installed, uninstalled or cleared.

I. Requested Gradle testDebugUnitTest, assembleDebug and lintDebug were attempted together. Default wrapper fails on read-only cache; writable cache cannot download because sockets are forbidden; installed Gradle fails native service initialization. Logs: docs/codex58/gradle*.log. Supplementary cached diagnostics and resource checks are recorded in docs/codex58; these do not substitute for Gradle or Android runtime verification.

J. APK: no fresh verified APK could be generated. Existing app/build/outputs/apk/debug/app-debug.apk predates this change and is not this deliverable. Application ID com.tilebloom.game, signing configuration and private credentials are unchanged. Required certificate fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6 still needs verification on a fresh host-built APK. No fallback APK is offered as a verified build.

K. Commit: style: replace white startup splash with Petaljong green theme. Uses existing .tilebloom-git history; only this task's changes are staged, including only the launcher theme hunk in the already-modified manifest. Pre-existing edits and artwork remain untouched. No push or publication.

L. Remaining limitations: standard Gradle checks, fresh signed APK, actual startup visuals and timing require an Android-capable host with working build/ADB sockets. This implementation does not by itself prove the absence of flashes on every Android release or launcher.

Android platform guidance: https://developer.android.com/develop/ui/views/launch/splash-screen (framework splash attributes, default lifecycle and 288dp/192dp icon dimensions).

Final supplementary results: **342 JVM tests passed** in 126.28 seconds (docs/codex58/jvm-tests.log). Cached main + Android test Kotlin compilation passed (docs/codex58/compile.log). Final Android resource compile/link passed with API 36 / minSdk 26; its resource-only archive is diagnostic and is not an installable application APK. XML, exact Home color and safe-area geometry checks passed (docs/codex58/resource-check.txt). Git diff whitespace check passed. No executed Android instrumentation, standard Gradle success, visual verification or startup timing is claimed.
