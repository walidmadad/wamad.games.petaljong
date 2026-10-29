# CODEX 47 — Actual botanical artwork replacement

The 23 production illustrations are replaced with new generated illustrations. Real installed Level 37 gameplay visibly changes. The supplied APK is a **resource-only verification APK**, not a successful Gradle source build; standard Gradle and lint remain blocked by this environment.

## A. Why the previous changes were not visible

CODEX 45 retained all original illustration bytes and changed fit/ceramics. Its report explicitly records no fresh APK. The installed baseline APK contained the same old artwork as the checkout. No botanical disk cache or competing resource directory was found. CODEX 44's detailed enamel-style artwork was still the actual raster art being displayed. No CODEX 46 report/commit is present here, so its implementation is not asserted. See [the complete trace](docs/codex47/pipeline-audit.md).

## B. Exact sources before the fix

All source paths below are relative to `app/src/main/res/drawable-nodpi/`:

| Symbol | Stable ID | Actual source |
| --- | ---: | --- |
| Green leaf | 0 | `botanical_leaf.webp` |
| Lavender | 17 | `botanical_lavender.webp` |
| Pink flower | 3 | `botanical_flower.webp` |
| Sunflower | 5 | `botanical_sunflower.webp` |
| Fern | 2 | `botanical_fern.webp` |

`domain/Game.kt` defines IDs. `BotanicalSymbols` in `presentation/MahjongBoardView.kt` maps IDs to drawable resources, decodes transparent WebP with `BitmapFactory`, and renders them using `Canvas.drawBitmap`. The compiled APK paths are `res/drawable-nodpi-v4/botanical_<name>.webp`. There is no SVG/vector botanical renderer or old-art fallback. Ceramic and generic mystery-back paths are separate Canvas drawings.

## C–D. Newly illustrated and replaced files

These are 23 newly created illustrations replacing the corresponding existing files in `app/src/main/res/drawable-nodpi/`. Resource names and stable IDs are retained. No old illustration was filtered, repainted, or reused. All images were generated with the built-in `image_gen.imagegen` tool, then proportionally normalized to transparent 256×256 lossless WebP. Their combined packaged size is 1,226,476 bytes.

| Stable ID | Replacement file | Bytes |
| --- | --- | ---: |
| 0 | `botanical_leaf.webp` | 46,682 |
| 1 | `botanical_lotus.webp` | 58,116 |
| 2 | `botanical_fern.webp` | 55,402 |
| 3 | `botanical_flower.webp` | 52,036 |
| 4 | `botanical_berries.webp` | 65,218 |
| 5 | `botanical_sunflower.webp` | 67,324 |
| 6 | `botanical_mushroom.webp` | 51,652 |
| 7 | `botanical_butterfly.webp` | 63,290 |
| 8 | `botanical_bamboo.webp` | 53,380 |
| 9 | `botanical_acorn.webp` | 54,498 |
| 10 | `botanical_maple_leaf.webp` | 46,908 |
| 11 | `botanical_pinecone.webp` | 58,584 |
| 12 | `botanical_iris.webp` | 57,816 |
| 13 | `botanical_ginkgo_leaf.webp` | 35,114 |
| 14 | `botanical_cherry_blossom.webp` | 62,916 |
| 15 | `botanical_dragonfly.webp` | 45,160 |
| 16 | `botanical_cactus.webp` | 56,412 |
| 17 | `botanical_lavender.webp` | 55,198 |
| 18 | `botanical_ladybug.webp` | 47,388 |
| 19 | `botanical_snail.webp` | 51,188 |
| 20 | `botanical_veined_leaf.webp` | 60,876 |
| 21 | `botanical_double_leaf.webp` | 38,428 |
| 22 | `botanical_yellow_daisy.webp` | 42,890 |

[Manifest](docs/codex47/art-manifest.json) records every old/new SHA-256, generated source path/hash, resource name and stable ID. [Prompts](docs/codex47/art-prompts.json) record the generation specifications. The dragonfly received an additional transparent-cutout cleanup. Alpha 254 is accepted as near-opaque ink; alpha-zero background is required, and original alpha is retained.

## E. Production integration

The existing 23-entry production resource array already points at these files. Replacing their bytes updates board, tray and Home through the shared `BotanicalSymbols` loader. The integration audit checks actual enum IDs, resource order, uniqueness and absence of alternate resource overrides. No production Kotlin, tile frame, domain, generator/version, matching, advertisements, languages, audio, settings or persistence code changed.

The only cache is process-local decoded Bitmaps. APK update/relaunch replaced that process; no old disk cache exists. All 23 old botanical payloads are absent from the new runtime resource slots. Historical documentation images are not Android resources.

## F. Old/new reference trio

[Old/new leaf, lavender and flower, including 24/32/48px samples](docs/codex47/reference-trio-before-after.png). [Complete new catalog](docs/codex47/catalog-comparison-new.png).

The icon reference is not attached in this turn or bundled in the checkout. Its column is labelled unavailable rather than fabricated. Generation follows the user's written art direction; exact icon matching is NOT VERIFIED.

[Same Android renderer fixture before/after at gameplay sizes](docs/codex47/android-fixture-before-after.png).

## G. Actual Android gameplay

[Before: installed Level 37](docs/codex47/android-before.png).
[After: updated installed Level 37](docs/codex47/android-after.png).
[Full screenshot comparison](docs/codex47/android-level37-before-after.png).
[Updated Home](docs/codex47/android-home-after.png).

The real 1080×2400 Activity screenshot preserves the same Level 37 board, one leaf in the four-slot tray, 22 pairs remaining, ordinary tiles and concealed mystery tiles. Broad gold leaf veins, fuller violet clusters, new rounded pink petals, stemmed daisy/sunflower and changed fern/cactus/greenery are visibly different at gameplay size. These are actual ADB screenshots, not asset mockups. The user's original screenshot attachment is unavailable; the comparison uses the old installed board captured before updating it.

## H. Installed APK and saved data

Installed package remains `com.tilebloom.game`, version `0.1`, code `1`. The baseline pulled from Android matched the original local APK (`d2304ac6e85c981928880b45b6db40c00c78eb9e65ace46314b0503594755844`). The update used `adb install -r`, with no uninstall or data clearing.

The installed APK was pulled back after the update: its whole-file hash equals the delivered APK, and every one of its 23 botanical resource hashes equals the new manifest. The signing certificate remains `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. See [installed audit](docs/codex47/installed-artwork-audit.json), installed package dumps, public certificate evidence and install transcript under `docs/codex47/`.

The actual save `files/player-progress-v1`, application settings and audio preferences retain exactly their original SHA-256 hashes. Level 37, board state and tray are preserved. No private key is included in Git.

## I. Tests and build checks

- PASS: all 308 existing JVM regressions, fresh diagnostic compilation and JUnitCore, 260.654 seconds.
- The bounds test exposed faint generated alpha fringe around cactus artwork. Packaging now crops padding using the existing alpha >16 convention; renderer fit limits and test assertions remain unchanged.
- PASS: final illustration mapping, 23 distinct changed hashes, transparency, proportional bounds and safe extents.
- PASS: Android resources compile/link using SDK aapt2; diagnostic production + Android-test Kotlin compilation.
- PASS: all 6 Android artwork instrumentation tests, 56.994 seconds, including all 23 identities, bounds, mystery concealment, blocked/Undo pixels, showcase and dense Levels 33/37 across five themes and 320/360/400/800dp. See `docs/codex47/android-artwork-tests-final.txt`.
- The first full visual-test run exposed a pre-existing invalid fixture: Undo was attempted after victory, where the existing rules disable it. The test now keeps another pair on the board and checks Undo while PLAYING. Production rules are unchanged. The initial failure is retained in the evidence.
- PASS: resource-only APK signature, alignment, all 23 replacement payloads and 227 unchanged non-artwork ZIP entries, including DEX, manifest, resource table, audio and language resources.
- BLOCKED: standard `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`, before task execution, by the read-only wrapper lock. Installed Gradle with a writable temporary cache also cannot create its wildcard-IP/socket service. These are not reported as successful Gradle/lint runs.

The auxiliary visual-test APK is a separately packaged unchanged test class (apart from the corrected Undo fixture), not the standard Gradle test artifact. Diagnostic scripts and logs are under `docs/codex47/`.

## J. Delivered APK

- Path: `app/build/outputs/apk/artworkVerification/tilebloom-codex47-artwork.apk`
- Size: 8,005,913 bytes
- SHA-256: `c4d7a5680b040fdd8fd11eb55dc5844daf28b6fcd0d48f4940091a41dc86e313`
- Build timestamp UTC: `2026-10-29T16:11:00.310945+00:00`
- Type: **resource-only verification APK**, rebuilt from the exact installed baseline with 23 WebP payload substitutions and compatible re-signing. This preserves baseline executable code and configuration. It is not represented as a new Gradle/source compilation.

The existing `app/build/outputs/apk/debug/app-debug.apk` is the old baseline and is NOT the deliverable for this iteration. Use the artworkVerification path above. A normal host Gradle build will consume the same new checked-in resources.

## K. Git

Changes are committed locally with `fix: replace realistic tile artwork with stylized botanical illustrations`. The delivery message supplies the commit hash. Existing `.tilebloom-git` history is preserved. Pre-existing locale edits and unrelated reports are excluded. No push or publication.

## L. Remaining limitations

Standard Gradle build, Gradle JVM task and lint cannot run in this environment. Exact app-icon comparison and direct comparison against the user's unattached screenshot are unavailable. Runtime frame profiling and physical-device review are not performed. The supplied APK is explicitly a resource-only verification build. These limits do not substitute for the installed screenshot and resource-hash evidence above.
