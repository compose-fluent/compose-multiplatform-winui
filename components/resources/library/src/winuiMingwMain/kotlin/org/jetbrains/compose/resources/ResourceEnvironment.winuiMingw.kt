package org.jetbrains.compose.resources

import kotlinx.cinterop.*
import platform.windows.*

internal actual fun windowsLocaleTag(): String = memScoped {
    val buffer = allocArray<UShortVar>(85)
    if (GetUserDefaultLocaleName(buffer, 85) > 0) buffer.toKStringFromUtf16() else "en-US"
}

internal actual fun windowsDpi(): Int {
    // Kotlin/Native's bundled Windows headers do not declare this Windows 10 API.
    val library = LoadLibraryW("user32.dll") ?: return 96
    return try {
        val query = GetProcAddress(library, "GetDpiForSystem")?.reinterpret<CFunction<() -> UInt>>()
        (query?.invoke()?.toInt() ?: 96).let { if (it > 0) it else 96 }
    } finally {
        FreeLibrary(library)
    }
}

internal actual fun windowsDarkTheme(): Boolean = memScoped {
    val value = alloc<UIntVar>()
    val size = alloc<UIntVar> { this.value = 4u }
    val result = RegGetValueW(HKEY_CURRENT_USER,
        "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        "AppsUseLightTheme", RRF_RT_REG_DWORD.toUInt(), null, value.ptr, size.ptr)
    result == 0 && value.value == 0u
}
