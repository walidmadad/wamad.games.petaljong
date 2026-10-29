# CODEX 44 — Illustrated botanical Mahjong artwork

The 23-symbol illustrated art library and renderer update are implemented and desktop asset review is complete. Android runtime verification and a fresh APK remain blocked by the sandbox; the work is not visually approved on Android.

| Requested area | Result |
| --- | --- |
| A. Existing limitations | The former 23-symbol Canvas catalog used flat ovals, lines, basic paths and a few highlight strokes. It could not supply the modeled petals, botanical surfaces and illustration depth requested. |
| B. Art direction | A complete library of individually generated botanical enamel illustrations: rich natural colors, upper-left lighting, softly modeled forms and fine gold details. The three leading subjects are an emerald leaf, violet lavender and a pink flower with golden stamens. |
| C. Ceramic rendering | Warm ivory gradient with upper-left glossy falloff, fine gold rims, warm ceramic side walls and existing soft contact shadows. Dimensions, layering, hitboxes and selection/hint accents are preserved. |
| D. Illustrations | All 23 stable symbol IDs map explicitly to unique local transparent WebP resources (1,281,596 bytes total; 1.22 MiB). Board, tray and welcome artwork share this catalog. Old flat symbol drawing code is replaced. |
| E. Similar symbols | Pointed diagonal leaf, broad heart-shaped veined leaf and forked double leaf; six rounded daisy petals versus sunflower rays and dark seed disk; lavender sprigs versus lotus layers and iris falls; round pink flower petals versus notched cherry blossom petals. The complete asset contact sheet was inspected at 16/24/32/48px. Structural distinctions survive reduction, while microdetail collapses at 16px; actual dense-board readability still requires Android review. |
| F. Mystery tiles | Jade glaze and jade side walls, gold trim and an engraved generic laurel seal. Hidden backs never call the symbol renderer. Revealed fronts use ordinary ivory and the same symbol library. |
| G. Reference comparison | The referenced app icon/red annotations were not attached in this session and no matching icon image is bundled. Artwork follows the written description. Direct icon comparison and a claim of visual equivalence are unavailable. |
| H. Android screenshots | None captured: ADB cannot start its local socket listener (`Operation not permitted`). Deterministic production-Canvas fixtures cover the reference trio, requested showcase subjects, mystery/revealed/blocked states, all 23 identities, five themes, overlapping tiles and dense Level 33 at 320/360/400/800dp. These fixtures have not been executed here. Desktop asset contact sheets are explicitly not Android screenshots. |
| I. Performance | Local 256px lossless WebP assets; bounded 23-entry decoded bitmap cache, at most 5.75 MiB of ARGB pixels. No runtime downloads, per-frame bitmap allocation, generator work or new animation loop. Shared material shaders and original geometry remain. The cache is warmed at view creation before drawing; its startup cost and device frame times remain unmeasured. |
| J. Tests | Required Gradle commands attempted; task execution blocked by the read-only wrapper lock. A writable-cache installed-Gradle attempt also fails before task execution because its socket service cannot determine a usable wildcard IP. Diagnostic compilation of production Kotlin, all Android test sources and Android resources passed under JDK 17 / cached Kotlin 1.9.24 (existing deprecation warnings). JVM test sources also compiled. All 308 existing JVM regression tests passed in 219.75 seconds using compiled test classes; the fallback script's filename-based runner first failed to load a helper-only source as a test, so the corrected runner enumerated actual `*Test.class` files. These checks do not substitute for Gradle, lint or Android execution. |
| K. APK | No fresh verified APK can be delivered from this sandbox. The pre-existing APK is not this change's output. Its certificate was inspected and matches `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. Gradle/signing settings are unchanged. |
| L. Git | Changes committed locally with `feat: apply premium app icon botanical art style to Mahjong tiles` (see delivery message for commit hash). Existing `.tilebloom-git` history retained. Pre-existing locale changes and unrelated files are excluded. No push or publication. |
| M. Remaining limitations | Authoritative reference image, real Android screenshot review, dense-board/occlusion visual approval, first-decode/frame profiling, standard Gradle unit/lint/build success and fresh APK signature/alignment verification remain outstanding. |

No domain, persistence, procedural layout, generator version, mystery interaction, four-slot tray logic, credits, ads, audio, language resources, settings/privacy or network code is changed. No installation, uninstallation or player-data clearing was performed.

## Artwork and evidence

The built-in image generation tool was used through the imagegen skill. Final runtime artwork is stored in `app/src/main/res/drawable-nodpi/botanical_*.webp`. The first three illustrations and the complete catalog were inspected as desktop ivory-swatch composites; these are artwork reviews, not rendered Android tile faces. Prompt specifications and the stable-ID resource/size manifest are under `docs/codex44/`. `tools/prepare_botanical_art.py` only crops transparent padding and resamples generated originals for Android; it does not generate or repaint illustrations.

Run `tools/host_verify.sh` in a normal host terminal for the standard Gradle checks and fresh APK verification. Run `CeramicArtworkVisualTest` on Android to generate production Canvas fixtures under app-private `files/codex44-visual/`; these tests create isolated games and do not load or modify saved player progress. Fixtures are views rendered to Android Canvas, not full Activity screenshots. A real gameplay Activity screenshot and device performance assessment are still needed.

## Reviewed artifacts

- [Complete catalog, including reduced-size samples](docs/codex44/catalog-asset-review.png)
- [Leaf, lavender and pink flower](docs/codex44/reference-trio-asset-review.png)
- [All final generation prompts](docs/codex44/art-prompts.json)
- [Stable IDs, resource paths, sizes and SHA-256 hashes](docs/codex44/art-manifest.json)
- [Verification summary](docs/codex44/verification.txt)

APK contents could not be measured after a fresh build. The 1.22 MiB figure is the sum of bundled WebP files, not a measured APK size increase.
