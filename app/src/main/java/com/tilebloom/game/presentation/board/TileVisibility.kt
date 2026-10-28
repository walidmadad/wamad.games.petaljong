package com.tilebloom.game.presentation.board

import com.tilebloom.game.domain.Rules
import com.tilebloom.game.domain.Tile

/** Conservative privacy: overhead coverage shows ceramic only, even if an edge is exposed. */
object TileVisibility {
    fun symbolVisible(tile: Tile, tiles: List<Tile>) = !tile.removed &&
        tiles.none { !it.removed && it.layer > tile.layer && Rules.overlaps(tile, it) }
}
