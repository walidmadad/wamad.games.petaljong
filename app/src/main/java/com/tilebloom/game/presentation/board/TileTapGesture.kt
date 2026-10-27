package com.tilebloom.game.presentation.board

import kotlin.math.hypot

/** A single pointer must press and release the same visible tile without ever dragging. */
class TileTapGesture(private val slop: Float) {
    private var pointer: Int? = null
    private var tile: Int? = null
    private var origin = BoardPoint(0f, 0f)
    fun down(pointerId: Int, tileId: Int?, x: Float, y: Float) {
        pointer = pointerId; tile = tileId; origin = BoardPoint(x, y)
    }
    fun move(pointerId: Int, x: Float, y: Float) {
        if (pointerId != pointer || hypot(x - origin.x, y - origin.y) > slop) cancel()
    }
    fun up(pointerId: Int, tileId: Int?, x: Float, y: Float): Int? {
        move(pointerId, x, y)
        val result = tile.takeIf { pointer == pointerId && it == tileId }
        cancel()
        return result
    }
    fun cancel() { pointer = null; tile = null }
}
