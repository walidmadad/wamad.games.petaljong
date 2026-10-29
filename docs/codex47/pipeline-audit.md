# Actual artwork pipeline

Baseline: Git `0c07e2a` (preserving `.tilebloom-git` history).

`app/src/main/java/com/tilebloom/game/domain/Game.kt` defines append-only explicit
stable symbol IDs. `BotanicalSymbols` in
`app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt` maps the
IDs to the `resourceIds` array, calls `BitmapFactory.decodeResource(inScaled=false)`,
computes alpha bounds once, and calls `Canvas.drawBitmap` from `drawTile`.
The illustrations are **transparent raster WebP**, not vector drawables, SVG, or
Canvas botanical paths. The ceramic faces and generic mystery backs are Canvas drawings.

| Symbol | Stable ID | Actual pre-fix source (relative to repository root) |
| --- | ---: | --- |
| Green leaf | 0 | `app/src/main/res/drawable-nodpi/botanical_leaf.webp` |
| Lavender | 17 | `app/src/main/res/drawable-nodpi/botanical_lavender.webp` |
| Pink flower | 3 | `app/src/main/res/drawable-nodpi/botanical_flower.webp` |
| Sunflower | 5 | `app/src/main/res/drawable-nodpi/botanical_sunflower.webp` |
| Fern | 2 | `app/src/main/res/drawable-nodpi/botanical_fern.webp` |

APK resource filenames are `res/drawable-nodpi-v4/botanical_<name>.webp`.
The `v4` suffix is the compiled resource directory, not a competing source folder.
There is exactly one source resource per botanical name; no density/flavor override
was found. All 23 old APK asset payloads matched the pre-fix workspace assets.

Gameplay calls `BotanicalSymbols.drawTile` in `MahjongBoardView`; tray calls the
same function in `TileTrayView.kt`. `GardenWelcomeArt.kt` calls
`BotanicalSymbols.draw`, sharing the same bitmap loader and resource array.
There is no old-asset fallback: a decode failure throws. The only botanical cache is
a bounded process-local array of 23 decoded Bitmaps and their rectangles. There is
no botanical disk/download cache. Replacing the APK and restarting its process loads
the new bundled bytes. Concealed mystery tiles do not draw their hidden symbol.

## Why the previous changes could look unchanged

CODEX 44 introduced the detailed realistic/enamel-style raster illustrations now
seen on the device. CODEX 45 explicitly retained their original bytes (its report
sections D/H) while changing optical fit and ceramic appearance. It reported no
fresh APK or Android visual verification. The retained asset bytes are direct
evidence that a renderer-only refinement did not redesign those illustrations.
There is no CODEX 46 report or commit in the available checkout; its behavior is
not asserted. This audit does not infer a cache problem without evidence.

The running emulator package is `com.tilebloom.game`, version `0.1` / code `1`.
Its pulled APK matches the local pre-fix APK SHA-256 exactly:
`d2304ac6e85c981928880b45b6db40c00c78eb9e65ace46314b0503594755844`.
Consequently, version name alone cannot distinguish artwork iterations; compare
APK and resource hashes. The baseline screenshot really shows Level 37 in that
installed application, with the old detailed leaf in the tray and board.

## Reference availability

The icon and annotated reference described by the user are not attached in this
turn or present in the workspace. The application manifest also has no icon
resource declaration. The provided written style description is the reference for
generation. The comparison labels this missing image explicitly; it does not
fabricate an app-icon column or claim exact reference matching.
