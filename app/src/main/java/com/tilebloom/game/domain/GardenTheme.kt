package com.tilebloom.game.domain

/** Stable presentation tokens; never participate in puzzle identity or game rules. */
data class GardenTheme(val id: String, val background: Int, val glow: Int, val ceramic: Int, val accent: Int) {
    companion object {
        private fun rgb(hex: Long) = (0xff000000L or hex).toInt()
        val all = listOf(
            GardenTheme("spring", rgb(0x173f35), rgb(0x38745b), rgb(0xb9d5b9), rgb(0xe6b6c7)),
            GardenTheme("sunrise", rgb(0x173e46), rgb(0x62734c), rgb(0xe2c28d), rgb(0xefb270)),
            GardenTheme("lotus", rgb(0x124b49), rgb(0x388d82), rgb(0x9dcfc6), rgb(0xe4aec4)),
            GardenTheme("twilight", rgb(0x26374f), rgb(0x665c85), rgb(0xc1b4d5), rgb(0xc9b1df)),
            GardenTheme("autumn", rgb(0x3c4430), rgb(0x827044), rgb(0xd6b590), rgb(0xe1a173)))
        fun forLevel(level: Long) = all[((level - 1).coerceAtLeast(0) % all.size).toInt()]
        val ivory = rgb(0xfff5dd)
        val gold = rgb(0xd9b96f)
    }
}
