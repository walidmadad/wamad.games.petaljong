package com.wamad.petaljong

import com.wamad.petaljong.domain.GardenTheme
import com.wamad.petaljong.presentation.board.HomeTheme
import org.junit.Assert.*
import org.junit.Test

class HomeThemeTest {
    @Test fun permanentHomePaletteIsIndependentOfVariedGameplay() {
        val home = HomeTheme.palette
        val levels = listOf(1L, 37L, 38L, 39L, 100L)
        val themes = levels.map { GardenTheme.forLevel(it).palette }
        assertTrue(themes.distinct().size >= 4)
        levels.forEach { level ->
            GardenTheme.forLevel(level)
            assertEquals(home, HomeTheme.palette)
            assertEquals(0xff173f35.toInt(), HomeTheme.palette.backgroundTop)
            assertEquals(0xff38745b.toInt(), HomeTheme.palette.backgroundBottom)
        }
        assertNotEquals(GardenTheme.forLevel(38).palette, home)
    }
}
