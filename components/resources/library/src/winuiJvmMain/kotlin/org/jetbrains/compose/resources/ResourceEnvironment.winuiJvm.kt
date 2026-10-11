package org.jetbrains.compose.resources

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.util.Locale

internal actual fun windowsLocaleTag(): String = Locale.getDefault().toLanguageTag()

internal actual fun windowsDpi(): Int = runCatching {
    Arena.ofConfined().use { arena ->
        val symbol = SymbolLookup.libraryLookup("user32", arena).find("GetDpiForSystem").orElseThrow()
        Linker.nativeLinker().downcallHandle(symbol, FunctionDescriptor.of(ValueLayout.JAVA_INT))
            .invokeWithArguments() as Int
    }
}.getOrDefault(96).let { if (it > 0) it else 96 }

internal actual fun windowsDarkTheme(): Boolean = runCatching {
    Arena.ofConfined().use { arena ->
        fun wide(value: String): MemorySegment = arena.allocate((value.length + 1L) * 2, 2).also { memory ->
            value.forEachIndexed { index, char -> memory.setAtIndex(ValueLayout.JAVA_CHAR, index.toLong(), char) }
        }
        val symbol = SymbolLookup.libraryLookup("advapi32", arena).find("RegGetValueW").orElseThrow()
        val call = Linker.nativeLinker().downcallHandle(symbol, FunctionDescriptor.of(ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS))
        val value = arena.allocate(ValueLayout.JAVA_INT)
        val size = arena.allocate(ValueLayout.JAVA_INT).also { it.set(ValueLayout.JAVA_INT, 0, 4) }
        val status = call.invokeWithArguments(MemorySegment.ofAddress(-2147483647L),
            wide("Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"), wide("AppsUseLightTheme"),
            0x10, MemorySegment.NULL, value, size) as Int
        status == 0 && value.get(ValueLayout.JAVA_INT, 0) == 0
    }
}.getOrDefault(false)
