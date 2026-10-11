package org.jetbrains.compose.resources

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.vector.xmldom.parseWindowsXml
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.MalformedXMLException

class WinuiResourcesTest {
    @Test fun decodesBitmapAndSvgWithWinuiSkiko() = runBlocking<Unit> {
        val reader = JvmResourceReader(javaClass.classLoader)
        val bitmap = reader.read("2.png").toImageBitmap(160, 160)
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="12" height="8"><rect width="12" height="8"/></svg>"""
        val painter = svg.encodeToByteArray().toSvgElement().toSvgPainter(androidx.compose.ui.unit.Density(1f))
        assertEquals(12f, painter.intrinsicSize.width)
        assertEquals(8f, painter.intrinsicSize.height)
        val font = androidx.compose.ui.text.platform.Font("test-font", reader.read("Workbench-Regular.ttf"),
            androidx.compose.ui.text.font.FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Normal)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, font.weight)
    }
    @Test fun classpathResourcesAndSlices() = runBlocking<Unit> {
        val reader = JvmResourceReader(javaClass.classLoader)
        val bytes = reader.read("hello.txt")
        assertTrue(bytes.isNotEmpty())
        assertContentEquals(bytes.copyOfRange(1, 3), reader.readPart("hello.txt", 1, 2))
        assertTrue(reader.getUri("hello.txt").isNotEmpty())
        assertFailsWith<MissingResourceException> { reader.read("does-not-exist") }
    }

    @Test fun unicodeUri() {
        assertEquals("file:///C:/%E8%B5%84%E6%BA%90/a%20b%23.png", windowsFileUri("C:\\资源\\a b#.png"))
    }

    @Test fun windowsEnvironmentWithoutAwt() {
        assertTrue(windowsLocaleTag().isNotBlank())
        assertTrue(windowsDpi() > 0)
        windowsDarkTheme()
        getSystemEnvironment()
    }

    @Test fun nativeVectorXmlNamespacesAndEntities() {
        val root = parseWindowsXml("""<?xml version="1.0"?><vector xmlns:a="http://schemas.android.com/apk/res/android"
            a:width="24dp"><!-- comment --><path a:name="a&amp;b&#x1F600;" a:pathData="M0,0 L1,1"/></vector>""")
        assertEquals("vector", root.localName)
        assertEquals("24dp", root.getAttributeNS("http://schemas.android.com/apk/res/android", "width"))
        val path = root.childNodes.item(0) as Element
        assertEquals("a&b😀", path.getAttributeNS("http://schemas.android.com/apk/res/android", "name"))
        for (bad in listOf("<vector><path></vector>", "<!DOCTYPE vector><vector/>", "<vector a='&unknown;'/>", "<vector/><other/>")) {
            assertFailsWith<MalformedXMLException> { parseWindowsXml(bad) }
        }
    }
}
