# CODEX 45 — Larger botanical artwork and ceramic depth

Implemented the visual refinement; Android visual approval and a fresh APK are blocked by this environment. No claim of device screenshot verification is made.

| Area | Result |
| --- | --- |
| A. Root causes | Every 256px asset previously used the same 2.24-unit square, including transparent margins. Ink occupied substantially less than that square, especially narrow lavender. The board's .34 transform reduced this further. |
| B. Optical scaling | Decode-time alpha bounds include every nontransparent pixel. A per-family width/height envelope fits those bounds uniformly and centers the resulting ink rectangle. Lavender/fern/bamboo get portrait space, wings get width, round flowers get balanced limits, and sunflower gets a smaller envelope. No stretching. |
| C. Readability | All 23 symbols grow by 13–50% linearly; lavender +50%, leaf +25%, pink flower +19%, sunflower +13%. Larger rendering gives existing stems, veins, petal separation and centers more pixels. Illustrations are unchanged; no new microdetail or claimed manual stem thickening. Geometric ink centering is implemented; perceptual centering still needs device review. |
| D. Ceramic | Slightly softer ivory face gradient, lighter lower edge, directional warm side-wall gradient, original rounded corners and upper-left glaze. Tray ivory follows the revised palette. Home uses its original composition and sizing. |
| E. Gold | Restrained diagonal metallic gradient, a 1.6%-of-tile-width rim and fine pale inner bevel. Blocked rims stay muted. |
| F. Depth | Original body/edge offsets and tile geometry are untouched. Directional side-wall shading and slightly stronger two-band contact shadows move light toward upper-left and shadows toward lower-right. No runtime blur. Real opaque draw-order occlusion remains. |
| G. Mystery | Jade glaze and identity-independent botanical seal retained. Added the same outer metallic rim after the jade surface so it remains visible. Hidden backs never call symbol drawing. |
| H. Similar symbols | All 23 stable IDs, original asset bytes, silhouettes and resource mappings retained. Different leaves, purple plants and yellow flowers remain their original illustrations. |
| I. Before/after | Numerical before/after ink extents are in `docs/codex45/fit-audit.json`. Baseline renderer is retained in Git commit `d6a476c8ce8b851c8cb4ea33ab20834b54bc9bc4`. No before/after Android images were captured. |
| J. Android visuals | Deterministic isolated Level 37, full catalog and overlap fixtures target 320/360/400/800dp across all themes. Showcase adds ladybug, similar variants, concealed/revealed mystery, blocked ordinary and partial coverage. Fixtures compile but have not run. ADB cannot open its local socket. The supplied screenshots/app icon are not attached or bundled here; direct icon comparison is unavailable. The existing desktop reference-trio sheet was reviewed only to confirm retained artwork and its small-size limitations. |
| K. Performance | 23-entry bitmap cache remains bounded at 5.75 MiB. Alpha scan occurs once on load (about 1.5 million pixels total); source/destination rectangles and shaders are cached. Draws reuse Paint and shader matrix, with no per-frame image decoding or bitmap allocation. Two extra rim passes and side shading have not been profiled on Android. |
| L. Tests | All 308 fresh JVM regressions passed (304.624s); final diagnostic resource/production/Android-test compilation passed. Standard Gradle and Android execution remain blocked. Added instrumentation checks for nonempty normalized artwork and portrait safe margins. Existing identity, concealment, theme, deterministic rendering and Undo pixel regressions remain. Numerical audit passes for all 23 assets: proportional fit, safe extents and increased size. |
| M. APK | No fresh APK produced. Standard Gradle tasks fail before execution; installed Gradle with writable cache also fails its socket/IP service. Existing APK is stale and must not be treated as this change. Signing configuration is unchanged; no credentials added. Fresh timestamp/size/hash/package/alignment/signature/content verification remains pending. |
| N. Git | Local verified-source commit requested; see delivery message for hash. Existing `.tilebloom-git` history retained. Pre-existing locale edits and untracked unrelated reports excluded. No push or publication. |
| O. Limitations | Actual-size Android image review, optical tuning, tray recognition, dense-board contrast, frame profiling, standard Gradle/lint success and a fresh signed APK remain outstanding. Numerical fit checks and diagnostic compilation are not substitutes for those checks. |

Geometry, hitboxes, generator/version, progression, saves, domain rules, mystery interactions, tray capacity, Hint/Undo, settings, ads/UMP, audio, localization and loading behavior are not edited. No installation, uninstall, clear-data or progression write was performed.

## Verification

- Required wrapper commands were attempted separately: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, all with `--no-daemon --console=plain`. Wrapper lock fails on the read-only Gradle home.
- Installed Gradle 8.13 under Java 17 with a writable `/tmp` cache fails before task execution: `Could not determine a usable wildcard IP for this machine`.
- `python3 tools/check_botanical_fit.py` passes for all assets; its output is committed.
- Diagnostic resource compilation/linking and production + Android-test Kotlin compilation use cached tooling under JDK 17. Final results are recorded in `docs/codex45/verification.txt`.
- Fresh JVM regression sources are compiled and executed through JUnitCore with cached dependencies; final count is recorded in the verification file.
- The first diagnostic attempt used the environment's Java 25 and failed on that compiler's Java version parsing. It was rerun with Java 17.

On a normal host, run `tools/host_verify.sh` for the supported build and APK checks, then the Android instrumentation suite. `CeramicArtworkVisualTest` writes Canvas fixtures under app-private `files/codex45-visual/`; it uses isolated games and never accesses progression. Retrieve with `adb exec-out run-as com.tilebloom.game cat files/codex45-visual/<filename>` after executing the tests. These are view-level Canvas fixtures; a full Activity screenshot and visual review remain necessary. For a before/after comparison, run the same Level 37 fixture against the baseline renderer in a separate checkout, preserving the same dimensions, density, theme and seed.
