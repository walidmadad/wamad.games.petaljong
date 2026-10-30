# CODEX 59 — mystery back opacity correction

The production renderer reused a Paint whose color was the ordinary ceramic upper-edge glint: alpha 0xCF for available tiles and 0x55 for blocked tiles. Setting a shader does not reset Paint alpha. Consequently the opaque green shader was drawn with only 207/255 or 85/255 opacity over the already-drawn ivory ceramic face. This explains why blocked mystery backs were particularly pale. There is no ColorMatrix or disabled-state filter in this path, and the theme shadow overlay already excludes hidden backs. Physical occlusion uses ordinary opaque draw ordering; it does not intentionally desaturate a lower back. This diagnosis is from production source; Android reproduction is pending.

Changes in MahjongBoardView:

- Hidden backs skip the ordinary ivory face, broad white glaze, ordinary rim and ordinary glint. Revealed mysteries retain that ordinary ceramic path and assigned botanical artwork.
- drawMystery explicitly resets Paint to opaque white before assigning the green shader (white here is Paint modulation, not a drawn white layer). Thus shader opacity cannot inherit glint or feedback alpha.
- Existing emblem leaf and petal shaders explicitly reset opacity after their translucent shadows. Their geometry and artwork are unchanged; gold no longer inherits the shadow's low alpha.
- Available face remains #409A65 → #2F8B57 → #2F8B57 → #267347, with restrained green glaze. Blocked face is #328151 → #28784B → #28784B → #246C43 and has no glaze. Main green occupies gradient positions .18–.78. Gold trim uses #E3C981 → #D9BD70 → #D9BD70 → #C6A252. Existing flower shader remains #FFF3D5 → #E6C875.
- No rule, generator version, dimensions, hitboxes, saves, signing, branding, language, audio, tutorial or advertising changes. Pre-existing unrelated workspace edits were left out of this commit.

The Android production-Canvas fixture now samples unobscured face pixels on available, laterally blocked and partially covered hidden backs for every theme. Its range rejects the old pale blend. Existing reveal, blocking-touch and Level 45 fixtures remain. Captures now go to files/codex59-visual. These instrumentation checks are not executed in this environment.

Screenshots: the user's Level 45 reference image was not attached in this turn or found locally. No before/after Android screenshots were obtained. ADB cannot bind its server socket (Operation not permitted). No app was installed, uninstalled or cleared; saved progress is untouched. Do not treat this implementation as visually approved.

Build/APK: standard testDebugUnitTest, assembleDebug, assembleDebugAndroidTest and lintDebug were attempted. The wrapper cannot write its default cache; invoking installed Gradle with a writable cache fails to create its socket-dependent FileLockContentionHandler. Logs are in docs/codex59. No fresh APK was built or installed and no claim is made that the emulator contains this renderer. Application ID and signing configuration are unchanged. Required certificate fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6 still requires verification on a fresh APK. Existing APKs are not this deliverable.

Host completion steps: with JDK 17 and working Android access, run the four Gradle tasks above; verify application ID and signer using SDK tools; install using adb install -r only; run MysteryTileVisualTest; pull files/codex59-visual; capture actual Level 45 gameplay and compare the same tiles with the user's original image on brown, green, turquoise and purple backgrounds. The previous renderer is preserved in docs/codex59/MahjongBoardView.before.kt.txt for a controlled comparison. Do not clear progression to stage screenshots.

Supplementary cached main + androidTest Kotlin compilation and full JVM regression results are recorded below when finished. These diagnostics do not build an APK or execute Android rendering. No push or publication.

Final supplementary results: **342 JVM regression tests passed**, 160.11 seconds, using cached Kotlin/JUnit with JDK 17 (`docs/codex59/jvm-tests.log`). Final main + Android test Kotlin compilation and Android resource compile/link passed (`compile.log`). Diagnostic bytecode inspection confirms the final face and emblem opacity resets (`compiled-renderer-check.txt`). Git diff whitespace check passed. None of these results establishes Android visual acceptance, successful Gradle checks, or a fresh signed APK.

Commit: `fix: prevent washed-out rendering on green mystery tiles`, recorded in the existing `.tilebloom-git` history. Only this task's renderer, instrumentation and evidence files are included.
