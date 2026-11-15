package com.wamad.petaljong

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class LocalizationResourcesTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private fun xml(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    private fun strings(file: File): Map<String, String> {
        val nodes = xml(file).getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .filter { it.getAttribute("translatable") != "false" }
            .associate { it.getAttribute("name") to it.textContent }
    }
    @Test fun exactlyEightLocalesAndNoChinese() {
        val nodes = xml(File(res, "xml/locales_config.xml")).getElementsByTagName("locale")
        assertEquals(listOf("en", "fr", "es", "pt-BR", "ar", "hi", "ja", "de"),
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") })
    }
    @Test fun everyLocaleHasAllStringsAndMatchingFormattingArguments() {
        val english = strings(File(res, "values/strings.xml"))
        val format = Regex("%[0-9]+[${'$'}][ds]")
        for (locale in listOf("fr", "es", "pt-rBR", "ar", "hi", "ja", "de")) {
            val translated = strings(File(res, "values-$locale/strings.xml"))
            assertEquals(locale, english.keys, translated.keys)
            for ((key, text) in translated) {
                assertTrue("$locale/$key", text.isNotBlank())
                assertEquals("$locale/$key", format.findAll(english.getValue(key)).map { it.value }.toList(),
                    format.findAll(text).map { it.value }.toList())
            }
        }
    }
    @Test fun pairPluralsCoverArabicCategoriesAndAllLocales() {
        for (locale in listOf("", "-fr", "-es", "-pt-rBR", "-ar", "-hi", "-ja", "-de")) {
            val document = xml(File(res, "values$locale/strings.xml"))
            val plurals = document.getElementsByTagName("plurals")
            val pairs = (0 until plurals.length).map { plurals.item(it) as Element }.single { it.getAttribute("name") == "pairs_remaining" }
            val items = pairs.getElementsByTagName("item")
            val quantities = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }
            assertTrue(quantities.containsAll(listOf("one", "other")))
            if (locale == "-ar") assertEquals(setOf("zero", "one", "two", "few", "many", "other"), quantities.toSet())
        }
    }
}
