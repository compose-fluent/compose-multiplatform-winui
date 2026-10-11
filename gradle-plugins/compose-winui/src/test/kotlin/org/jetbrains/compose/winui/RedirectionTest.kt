package org.jetbrains.compose.winui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedirectionTest {
    @Test fun publishedReleaseCoordinates() {
        fun resolve(group: String, module: String, version: String) =
            mappedCoordinates(group, module, version, emptyMap(), emptyMap(), ComposeWinUiRelease.mappings)
        assertEquals("io.github.compose-fluent.compose.ui:ui:1.12.0-alpha01",
            resolve("org.jetbrains.compose.ui", "ui", "1.12.1"))
        assertEquals("io.github.compose-fluent.compose.ui:ui:1.12.0-alpha01",
            resolve("org.jetbrains.compose.ui", "ui-desktop", "1.12.1"))
        assertEquals("io.github.compose-fluent.androidx.navigation:navigation-compose:2.10.0-alpha01",
            resolve("org.jetbrains.androidx.navigation", "navigation-compose", "2.10.0-beta01"))
        assertEquals("io.github.compose-fluent.androidx.navigation3:navigation3-ui:1.1.0-alpha01",
            resolve("org.jetbrains.androidx.navigation3", "navigation3-ui", "1.1.2"))
        assertEquals("io.github.compose-fluent.compose.material3:material3:1.12.0-alpha01",
            resolve("org.jetbrains.compose.material3", "material3", "1.12.0-alpha03"))
        assertEquals("io.github.compose-fluent.compose.material3.adaptive:adaptive:1.3.0-alpha01",
            resolve("org.jetbrains.compose.material3.adaptive", "adaptive", "1.3.0-rc01"))
        assertEquals("io.github.compose-fluent:skiko-winui:0.150.1-alpha02",
            resolve("org.jetbrains.skiko", "skiko-awt", "0.150.1"))
        for (module in listOf("components-resources", "components-resources-desktop", "components-resources-jvm", "components-resources-mingwx64")) {
            assertEquals("io.github.compose-fluent.compose.components:components-resources:1.12.0-alpha01-SNAPSHOT",
                resolve("org.jetbrains.compose.components", module, "1.12.1"))
        }
        assertNull(resolve("org.jetbrains.androidx.navigation3", "navigation3-ui", "1.1.7"))
        assertNull(resolve("org.jetbrains.compose.ui", "ui", "1.12.2"))
        // Modules absent from the release must not redirect to unpublished coordinates.
        assertNull(resolve("org.jetbrains.compose.ui", "ui-tooling", "1.12.1"))
        assertNull(resolve("org.jetbrains.compose.components", "components-resources", "1.12.2"))
        // Native-only modules must finish their JVM forwarding at CMP's leaf.
        for (module in listOf("ui-geometry", "ui-unit", "ui-util", "ui-backhandler")) {
            assertNull(resolve("org.jetbrains.compose.ui", "$module-desktop", "1.12.1"))
            assertNull(resolve("org.jetbrains.compose.ui", "$module-jvm", "1.12.1"))
            assertEquals("io.github.compose-fluent.compose.ui:$module:1.12.0-alpha01",
                resolve("org.jetbrains.compose.ui", "$module-mingwx64", "1.12.1"))
        }
    }

    @Test fun platformSelection() {
        assertTrue(shouldRedirect("native", "mingw_x64", false, true, false))
        for (target in listOf("linux_x64", "linux_arm64", "ios_arm64", "ios_simulator_arm64", "macos_arm64", "android_arm64", null)) {
            assertFalse(shouldRedirect("native", target, true, true, true), "Unexpected redirection for $target")
        }
        assertFalse(shouldRedirect("native", "mingw_x64", true, false, true))
        assertFalse(shouldRedirect("jvm", null, true, true, false))
        assertTrue(shouldRedirect("jvm", null, true, true, true))
        assertFalse(shouldRedirect("jvm", null, false, true, true))
        for (platform in listOf("androidJvm", "js", "wasm", "common", null)) {
            assertFalse(shouldRedirect(platform, null, true, true, true))
        }
    }

    @Test fun independentVersionsAndExplicitOverrides() {
        val groups = mapOf(
            "org.jetbrains.compose.ui:1.12.1" to "org.jetbrains.compose.ui:1.12.0-alpha01",
            "org.jetbrains.androidx.navigation:2.9.0" to "org.jetbrains.androidx.navigation:2.9.0-winui",
            "org.jetbrains.androidx.navigation3:1.0.0" to "org.jetbrains.androidx.navigation3:1.0.0-winui"
        )
        assertEquals("org.jetbrains.compose.ui:ui:1.12.0-alpha01",
            mappedCoordinates("org.jetbrains.compose.ui", "ui", "1.12.1", emptyMap(), groups))
        assertNull(mappedCoordinates("org.jetbrains.compose.ui", "ui", "1.12.2", emptyMap(), groups))
        assertEquals("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-winui",
            mappedCoordinates("org.jetbrains.androidx.navigation", "navigation-compose", "2.9.0", emptyMap(), groups))
        assertEquals("org.jetbrains.androidx.navigation3:navigation3-ui:1.0.0-winui",
            mappedCoordinates("org.jetbrains.androidx.navigation3", "navigation3-ui", "1.0.0", emptyMap(), groups))
        assertEquals("custom:ui-winuijvm:42", mappedCoordinates("org.jetbrains.compose.ui", "ui", "1.12.1",
            mapOf("org.jetbrains.compose.ui:ui:1.12.1" to "custom:ui-winuijvm:42"), groups))
        assertNull(mappedCoordinates("unrelated", "ui", "1.12.1", emptyMap(), groups))
    }
}
