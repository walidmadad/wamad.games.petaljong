package com.wamad.petaljong.domain

/** Stable presentation tokens; never participate in puzzle identity or game rules. */
data class GardenTheme(val id: String, val background: Int, val glow: Int, val ceramic: Int, val accent: Int) {
    val palette = GardenPalette(
        background, glow, accent, blend(background, glow, .18f),
        blend(background, glow, .34f), blend(background, 0xff000000.toInt(), .18f),
        ivory, blend(ceramic, ivory, .24f), blend(ceramic, background, .4f),
        blend(background, 0xff000000.toInt(), .7f), blend(ceramic, background, .28f),
        gold, accent, ivory, blend(ivory, ceramic, .4f), blend(background, glow, .12f))
    companion object {
        private fun blend(a: Int, b: Int, amount: Float): Int {
            fun channel(shift: Int) = (((a ushr shift) and 255) * (1-amount) + ((b ushr shift) and 255) * amount).toInt()
            return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        private fun rgb(hex: Long) = (0xff000000L or hex).toInt()
        val ivory = rgb(0xfff5dd)
        val gold = rgb(0xd9b96f)
        val all = listOf(
            GardenTheme("spring", rgb(0x173f35), rgb(0x38745b), rgb(0xb9d5b9), rgb(0xe6b6c7)),
            GardenTheme("sunrise", rgb(0x343b24), rgb(0x756044), rgb(0xe2c28d), rgb(0xefb270)),
            GardenTheme("lotus", rgb(0x124b49), rgb(0x388d82), rgb(0x9dcfc6), rgb(0xe4aec4)),
            GardenTheme("twilight", rgb(0x25233e), rgb(0x665c85), rgb(0xc1b4d5), rgb(0xc9b1df)),
            GardenTheme("autumn", rgb(0x3c4430), rgb(0x735344), rgb(0xd6b590), rgb(0xe1a173)))
        fun forLevel(level: Long) = all[((level - 1).coerceAtLeast(0) % all.size).toInt()]
    }
}

/** Immutable screen-wide presentation; never serialized into a puzzle definition. */
data class GardenPalette(
    val backgroundTop: Int, val backgroundBottom: Int, val backgroundAccent: Int,
    val headerSurface: Int, val traySurface: Int, val emptyTraySlot: Int,
    val availableTileFace: Int, val blockedTileFace: Int, val tileSide: Int,
    val tileShadow: Int, val border: Int, val primaryAccent: Int, val secondaryAccent: Int,
    val textPrimary: Int, val textSecondary: Int, val buttonSurface: Int)
