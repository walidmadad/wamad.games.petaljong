# CODEX 43 — Premium Botanical Ceramic

Implementation complete; Android visual review and a fresh verified APK are blocked by this environment. No runtime screenshot comparison or device performance result is claimed.

| Requested area | Implementation / evidence |
| --- | --- |
| A. Level 33 problems | The referenced screenshot was not attached to this session. Source inspection found a dark inset rim plus outer border, a theme accent band between face and body, strongly tinted blocked faces, blocking corner bars, and relatively heavy contact shadows. These are plausible sources of clutter in dense stacks, not observations from an inspected Level 33 image. |
| B. Art direction | Original Canvas vector artwork and warm, quiet ceramic surfaces; restrained natural botanical colors and gold details. No photographic, emoji, external, or copied artwork. |
| C. Faces | Theme-independent warm ivory vertical lighting with a small lower-edge falloff. Blocked faces use a separate opaque muted ceramic gradient. |
| D. Borders | One fine warm rim and a short top-edge highlight replace the repeated dark outlines. Removed blocking corner bars and the extra selected inset outline. Existing selection and hint accents remain. |
| E. Depth and shadows | Existing body/edge offsets and layer transforms retained exactly. Muted stone lower side and cream upper side replace the saturated accent band. Reduced shadow-band opacity while keeping elevated layers stronger. Upper bodies and faces remain opaque. |
| F. Botanical catalog | All 23 existing IDs and accessibility labels retained. Added veins, petal shading, seed details, cap highlights, wing details, berry highlights, bamboo lighting, acorn texture, maple veins, pinecone lighting, lavender highlights, cactus ribs, ladybug highlights, snail shell lighting and dragonfly wing veins. Refined paired-leaf silhouettes, veined-leaf silhouette and mushroom stem color. Related families remain separate illustrations. Original optical scale and safe margins retained. |
| G. Mystery backs | Deep jade diagonal ceramic lighting, fine gold rim and original circular floral seal with paired botanical leaves. Removed the bright folded-corner motif. No assigned identity is consulted when drawing a back. |
| H. Blocking and coverage | Recognizable illustrations retain a restrained tint on muted blocked ceramic. Existing geometry, draw order and opaque upper surfaces still determine physical occlusion; no random visibility rule. |
| I. Dense stacks | Quieter borders, neutral side walls and reduced shadow opacity target clutter without moving any tile or changing its geometry. Actual Level 33 readability still requires visual inspection. |
| J. Themes | Ivory and botanical colors stay consistent across all five current themes. Theme shadows and selection/hint accents remain. Runtime fixtures cover every theme. |
| K. Performance | Three shared gradients with one reusable transform replace per-tile gradient construction. Botanical vector commands are recorded once per identity in a bounded 23-entry Picture cache; replay avoids path construction and trigonometry. No symbol bitmaps, blur filters, generator work or new animation loops. Initial recording remains lazy on first use. Device frame timing is unmeasured. |
| L. Screenshots | None captured or inspected: ADB cannot start its local socket listener (`Operation not permitted`). New isolated fixtures write production Canvas PNGs under app-private `files/codex43-visual/`. Baseline renderer is available in Git parent history for a future genuine before/after run. No simulated image is labeled an Android screenshot. |
| M. Tests | New Android tests cover unique/distinct supported catalog artwork, deterministic pixels, all-identity mystery concealment, visible blocked symbols across themes, match/undo restoration, full catalog, Level 33 and mixed/overlapping fixtures at 320/360/400/800dp. Existing coverage for occlusion, mystery interactions, hints, gestures, saves and reduced motion is retained. Android tests are source-compiled only, not executed here. See verification results below. |
| N. Android verification | All three required Gradle commands were attempted and failed before task execution on the read-only wrapper lock. Installed Gradle with a writable temporary cache also failed before task execution because it could not determine a usable wildcard IP for its coordination socket. ADB failed before device enumeration. No installation, uninstall, data clearing, or launch occurred. |
| O. APK | No fresh APK could be built; timestamp, size, SHA-256, alignment and signature verification for a new APK are unavailable. Package remains `com.tilebloom.game`; signing configuration was not edited. Required certificate remains `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`; no new artifact is claimed to have been checked against it. |
| P. Git | Changes committed locally using the existing `.tilebloom-git` history. No push, publication, history rewrite or Git reinitialization. Pre-existing eight-language resource edits and unrelated untracked files are excluded. |
| Q. Limitations | Needs a normal host Gradle build, lint, real-device/emulator instrumentation, screenshot inspection and device performance checks before visual approval. No claim of visually verified premium quality, responsiveness, dense-stack readability or final APK delivery. |

Gameplay, symbol identities, board and touch geometry, generator versions, witness logic, difficulty, mystery rules, tray capacity, saved progress, audio/animation policy, ads, locale selection, RTL policy and privacy/settings code were not edited. Symbols are also shared by the existing tray renderer; its layout and behavior are unchanged.

## Verification results

Diagnostic compilation of all production and Android test sources plus Android resource compilation/linking: **PASS** under JDK 17 (existing deprecation warnings). JVM regressions: **PASS**, 308 tests across 34 compiled test classes (corrected diagnostic runner; see below). Logs are in `docs/codex43/` (local ignored log files).

Commands attempted:

```sh
./gradlew --no-daemon --console=plain :app:testDebugUnitTest
./gradlew --no-daemon --console=plain :app:assembleDebug
./gradlew --no-daemon --console=plain :app:lintDebug
```

Diagnostic compilation uses cached Kotlin 1.9.24 and Android 36 APIs under JDK 17, with cached dependency jars. It checks sources and resources; it does not replace Gradle, lint or Android execution. The JVM fallback's filename-based runner incorrectly tries to load `WitnessReplayTest` even though that source only defines a helper function. The corrected diagnostic invocation selects the actual compiled `*Test.class` classes, excluding helper-file classes.
