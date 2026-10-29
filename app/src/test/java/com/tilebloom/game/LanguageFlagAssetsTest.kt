package com.tilebloom.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LanguageFlagAssetsTest {
    @Test fun bundledFlagsHaveConsistentHighResolutionWithoutMissingAssets() {
        val root = if (File("src/main/res").isDirectory) File("src/main/res") else File("app/src/main/res")
        val files = File(root, "drawable-nodpi").listFiles()!!.filter { it.name.startsWith("flag_") }
        assertEquals(setOf("en", "fr", "es", "pt_br", "ar", "hi", "ja", "de", "automatic"),
            files.map { it.name.removePrefix("flag_").removeSuffix(".png") }.toSet())
        files.forEach { file ->
            val image = ImageIO.read(file)
            assertNotNull(file.name, image)
            assertEquals(file.name, 504, image.width)
            assertEquals(file.name, 336, image.height)
        }
    }
}
