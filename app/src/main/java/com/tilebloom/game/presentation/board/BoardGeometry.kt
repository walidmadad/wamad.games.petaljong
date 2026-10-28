package com.tilebloom.game.presentation.board

import com.tilebloom.game.domain.Tile
import kotlin.math.hypot
import kotlin.math.min

data class BoardPoint(val x: Float, val y: Float)
data class BoardPadding(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f)
data class BoardRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center get() = BoardPoint((left + right) / 2, (top + bottom) / 2)
    fun inset(amount: Float) = BoardRect(left + amount, top + amount, right - amount, bottom - amount)
    fun offset(y: Float) = BoardRect(left, top + y, right, bottom + y)
    fun containsRounded(x: Float, y: Float, radius: Float): Boolean {
        if (right <= left || bottom <= top || x < left || x >= right || y < top || y >= bottom) return false
        val r = min(radius, min((right - left) / 2, (bottom - top) / 2))
        return hypot(x - x.coerceIn(left + r, right - r), y - y.coerceIn(top + r, bottom - r)) <= r
    }
}

/** Active initial layout in view-local pixels (legacy Level 1 bounds when omitted). Logical footprints remain 1 × 1 in the engine.
 * Face, solid edge and shadow use the same transform. Shadows are intentionally not targets.
 * Hit testing sees all remaining tiles, regardless of their legal availability.
 */
class BoardGeometry(width: Float, height: Float, padding: BoardPadding = BoardPadding(), tiles: List<Tile>? = null) {
    private val minX = tiles?.minOf { it.x.toFloat() } ?: 0f
    private val minY = tiles?.minOf { it.y.toFloat() * 1.3f - it.layer * .09f } ?: 0f
    private val spanX = tiles?.maxOf { it.x.toFloat() + 1f }?.minus(minX) ?: 4.5f
    private val spanY = tiles?.maxOf { (it.y.toFloat() + 1f) * 1.3f - it.layer * .09f }?.minus(minY) ?: 5.4f
    val scale = min((width - padding.left - padding.right).coerceAtLeast(0f) / (spanX + .35f),
        (height - padding.top - padding.bottom).coerceAtLeast(0f) / (spanY + .45f))
    private val origin = BoardPoint(
        padding.left + (width - padding.left - padding.right - spanX * scale) / 2 - minX * scale,
        padding.top + (height - padding.top - padding.bottom - spanY * scale) / 2 - minY * scale + .12f * scale)
    val radius get() = scale * .1f
    fun toScreen(x: Double, y: Double, layer: Int = 0) = BoardPoint(
        origin.x + x.toFloat() * scale, origin.y + y.toFloat() * scale * 1.3f - layer * scale * .09f)
    fun toBoard(x: Float, y: Float, layer: Int = 0): BoardPoint? = if (scale <= 0f) null else
        BoardPoint((x - origin.x) / scale, (y - origin.y + layer * scale * .09f) / (scale * 1.3f))
    fun footprint(tile: Tile): BoardRect {
        val a = toScreen(tile.x, tile.y, tile.layer)
        val b = toScreen(tile.x + 1, tile.y + 1, tile.layer)
        return BoardRect(a.x, a.y, b.x, b.y)
    }
    fun face(tile: Tile) = footprint(tile).inset(scale * .035f)
    fun edge(tile: Tile) = face(tile).offset(scale * .045f)
    fun body(tile: Tile) = face(tile).offset(scale * .085f)
    fun shadow(tile: Tile) = face(tile).offset(scale * .14f)
    fun drawOrder(tiles: List<Tile>) = tiles.filterNot { it.removed }
        .sortedWith(compareBy<Tile> { it.layer }.thenBy { it.y }.thenBy { it.x }.thenBy { it.id })
    fun hitTest(tiles: List<Tile>, x: Float, y: Float): Tile? {
        if (scale <= 0f) return null
        return drawOrder(tiles).asReversed().firstOrNull {
            face(it).containsRounded(x, y, radius) || edge(it).containsRounded(x, y, radius)
        }
    }
}
