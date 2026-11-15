package com.wamad.petaljong.presentation.board

import com.wamad.petaljong.domain.Tile

/** Point visibility follows the exact opaque rounded surfaces and painter order.
 * Availability is intentionally independent: side-blocked faces still carry artwork.
 * Shadows are translucent decoration and cannot conceal identity or become touch targets.
 */
object TileVisibility {
    fun faceVisibleAt(tile: Tile, tiles: List<Tile>, geometry: BoardGeometry, x: Float, y: Float): Boolean {
        if (tile.removed || !geometry.face(tile).containsRounded(x, y, geometry.radius)) return false
        val order = geometry.drawOrder(tiles)
        val index = order.indexOfFirst { it.id == tile.id }
        if (index < 0) return false
        return order.drop(index + 1).none {
            geometry.face(it).containsRounded(x, y, geometry.radius) ||
                geometry.edge(it).containsRounded(x, y, geometry.radius) ||
                geometry.body(it).containsRounded(x, y, geometry.radius)
        }
    }
}
