package com.tilebloom.game.presentation

import com.tilebloom.game.R

/** Bundled representative country decorations; never locale or region authorities. */
internal object LanguageFlags {
    fun drawable(tag: String): Int = when (tag) {
        "en" -> R.drawable.flag_en
        "fr" -> R.drawable.flag_fr
        "es" -> R.drawable.flag_es
        "pt-BR" -> R.drawable.flag_pt_br
        "ar" -> R.drawable.flag_ar
        "hi" -> R.drawable.flag_hi
        "ja" -> R.drawable.flag_ja
        "de" -> R.drawable.flag_de
        else -> R.drawable.flag_automatic
    }
}
