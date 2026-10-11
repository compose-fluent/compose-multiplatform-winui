package org.jetbrains.compose.resources

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val parts = windowsLocaleTag().replace('_', '-').split('-')
    return ResourceEnvironment(
        LanguageQualifier(parts.first().lowercase()),
        ScriptQualifier(parts.drop(1).firstOrNull { it.length == 4 }.orEmpty()),
        RegionQualifier(parts.drop(1).firstOrNull { it.length == 2 || it.length == 3 }.orEmpty()),
        ThemeQualifier.selectByValue(windowsDarkTheme()),
        // Windows uses 96 DPI at 100%; resource qualifiers use the Compose density scale.
        DensityQualifier.selectByDensity(windowsDpi() / 96f)
    )
}

internal expect fun windowsLocaleTag(): String
internal expect fun windowsDarkTheme(): Boolean
internal expect fun windowsDpi(): Int

internal fun windowsFileUri(path: String): String = "file:///" + buildString {
    for (byte in path.replace('\\', '/').encodeToByteArray()) {
        val value = byte.toInt() and 255
        val char = value.toChar()
        if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-._~/:" ) append(char)
        else append('%').append(value.toString(16).uppercase().padStart(2, '0'))
    }
}
