package com.wamad.petaljong
import com.wamad.petaljong.domain.Status
import com.wamad.petaljong.presentation.board.ScreenPresentation
import org.junit.Assert.*
import org.junit.Test
class ScreenPresentationTest {
    @Test fun mutuallyExclusive() { Status.entries.forEach { status ->
        val p=ScreenPresentation.forStatus(status,true)
        assertFalse(p.victory && p.footer); assertEquals(status==Status.PLAYING,p.footer)
        assertEquals(status!=Status.WON,p.restart)
        assertEquals(status==Status.WON,p.advance)
    } }
    @Test fun transitionsReset() { listOf(Status.WON,Status.PLAYING,Status.NO_MOVES,Status.PLAYING,Status.WON).forEach {
        val p=ScreenPresentation.forStatus(it,false); assertFalse(p.advance); assertEquals(it==Status.WON,p.victory)
    } }
}
