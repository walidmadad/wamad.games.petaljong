package com.wamad.petaljong

import org.junit.Assert.*
import org.junit.Test
import java.io.DataInputStream
import java.io.File

class LanguageFlagAssetsTest {
    @Test fun bundledFlagsHaveConsistentHighResolutionWithoutMissingAssets() {
        val root = if (File("src/main/res").isDirectory) File("src/main/res") else File("app/src/main/res")
        val files = File(root, "drawable-nodpi").listFiles()!!.filter { it.name.startsWith("flag_") }
        assertEquals(setOf("en", "fr", "es", "pt_br", "ar", "hi", "ja", "de", "automatic"),
            files.map { it.name.removePrefix("flag_").removeSuffix(".png") }.toSet())
        files.forEach { file ->
            DataInputStream(file.inputStream().buffered()).use { input ->
                val signature = ByteArray(8)
                input.readFully(signature)
                assertArrayEquals(file.name, byteArrayOf(
                    0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                ), signature)
                // PNG starts with a 13-byte IHDR chunk; integers are big-endian.
                assertEquals(file.name, 13, input.readInt())
                assertEquals(file.name, 0x49484452, input.readInt()) // IHDR
                assertEquals(file.name, 504, input.readInt())
                assertEquals(file.name, 336, input.readInt())
                // Require the rest of IHDR and its CRC, including for truncated files.
                input.readFully(ByteArray(9))
            }
        }
    }
}
