package com.wamad.petaljong

import com.wamad.petaljong.presentation.board.TileTapGesture
import org.junit.Assert.*
import org.junit.Test

class TileTapGestureTest {
    @Test fun stableTapAndSmallJitter() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); g.move(0,12f,13f)
        assertEquals(16,g.up(0,16,11f,12f)); assertNull(g.up(0,16,11f,12f))
    }
    @Test fun leaveAndReturnIsNotATap() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); g.move(0,30f,10f); g.move(0,10f,10f)
        assertNull(g.up(0,16,10f,10f))
    }
    @Test fun upOnDifferentTileIsRejectedEvenWithinSlop() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); assertNull(g.up(0,17,11f,10f))
    }
    @Test fun downOnEmptyCannotSelectTileOnRelease() {
        val g = TileTapGesture(8f); g.down(0,null,10f,10f); assertNull(g.up(0,16,11f,10f))
    }
    @Test fun canceledGestureCannotResume() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); g.cancel(); assertNull(g.up(0,16,10f,10f))
        g.down(0,16,10f,10f); assertEquals(16,g.up(0,16,10f,10f))
    }
    @Test fun pointerChangeCannotCompleteTap() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); assertNull(g.up(1,16,10f,10f))
    }
    @Test fun removedTileCannotCompleteTap() {
        val g = TileTapGesture(8f); g.down(0,16,10f,10f); assertNull(g.up(0,null,10f,10f))
    }
}
