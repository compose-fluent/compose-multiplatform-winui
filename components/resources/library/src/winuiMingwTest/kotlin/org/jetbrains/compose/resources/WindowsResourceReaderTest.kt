package org.jetbrains.compose.resources

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.toImageVector

class WindowsResourceReaderTest {
    @Test fun decodesBitmapWithAutomaticallyStagedSkikoRuntime() = runBlocking<Unit> {
        val image = WindowsResourceReader.read("2.png").toImageBitmap(160, 160)
        assertTrue(image.width > 0 && image.height > 0)
        val font = androidx.compose.ui.text.platform.Font(
            "native-test-font", WindowsResourceReader.read("Workbench-Regular.ttf"),
            androidx.compose.ui.text.font.FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Normal,
        )
        assertTrue(font.weight == androidx.compose.ui.text.font.FontWeight.Normal)
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="12" height="8"><rect width="12" height="8"/></svg>"""
        val painter = svg.encodeToByteArray().toSvgElement().toSvgPainter(Density(1f))
        assertEquals(12f, painter.intrinsicSize.width)
        assertEquals(8f, painter.intrinsicSize.height)
        val vector = """<vector xmlns:a="http://schemas.android.com/apk/res/android"
            a:width="24dp" a:height="16dp" a:viewportWidth="24" a:viewportHeight="16">
            <path a:fillColor="#ff0000" a:pathData="M0,0 L24,0 L24,16 Z"/></vector>"""
            .encodeToByteArray().toXmlElement().toImageVector(Density(1f))
        assertEquals(24f, vector.defaultWidth.value)
        assertEquals(16f, vector.defaultHeight.value)
        assertEquals(1, vector.root.size)
    }

    @Test fun readsStagedResources() = runBlocking<Unit> {
        val reader = WindowsResourceReader
        val bytes = reader.read("hello.txt")
        assertContentEquals(bytes.copyOfRange(1, 3), reader.readPart("hello.txt", 1, 2))
        assertContentEquals(byteArrayOf(), reader.readPart("hello.txt", 0, 0))
        assertTrue(reader.read("资源 空格.txt").decodeToString().contains("Unicode"))
        assertTrue(reader.getUri("资源 空格.txt").contains("%20"))
        assertFailsWith<MissingResourceException> { reader.read("absent") }
        assertFailsWith<MissingResourceException> { reader.getUri("absent") }
        assertFailsWith<IllegalArgumentException> { reader.readPart("hello.txt", -1, 1) }
        assertFailsWith<IllegalArgumentException> { reader.readPart("hello.txt", 0, Long.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { reader.read("../hello.txt") }
    }
}
