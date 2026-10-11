package org.jetbrains.compose.winui

/** Compose 1.12 mappings, including the Resources adapter published with this plugin. */
internal object ComposeWinUiRelease {
    val mappings: Map<String, String> = buildMap {
        // These native-only modules forward JVM variants to CMP itself. Mapping
        // their CMP JVM leaf back to this root would create a forwarding cycle.
        val cmpJvmForwards = setOf("ui-geometry", "ui-unit", "ui-util", "ui-backhandler")
        fun family(group: String, cmpVersion: String, winuiVersion: String, vararg modules: String) {
            val winuiGroup = group.replaceFirst("org.jetbrains.", "io.github.compose-fluent.")
            modules.forEach { module ->
                val destination = "$winuiGroup:$module:$winuiVersion"
                put("$group:$module:$cmpVersion", destination)
                // Resolve concrete CMP platform coordinates through the fork's KMP root:
                // its published metadata selects WinUI or the release's upstream redirect.
                for (suffix in listOf("desktop", "jvm", "mingwx64")) {
                    if (group == "org.jetbrains.compose.ui" && module in cmpJvmForwards && suffix != "mingwx64") continue
                    put("$group:$module-$suffix:$cmpVersion", destination)
                }
            }
        }
        family("org.jetbrains.compose.animation", "1.12.1", "1.12.0-alpha01", "animation", "animation-core")
        family("org.jetbrains.compose.foundation", "1.12.1", "1.12.0-alpha01", "foundation", "foundation-layout")
        family("org.jetbrains.compose.material", "1.12.1", "1.12.0-alpha01", "material", "material-ripple")
        family("org.jetbrains.compose.runtime", "1.12.1", "1.12.0-alpha01", "runtime", "runtime-saveable")
        family("org.jetbrains.compose.components", "1.12.1", "1.12-SNAPSHOT", "components-resources")
        family("org.jetbrains.compose.ui", "1.12.1", "1.12.0-alpha01",
            "ui", "ui-geometry", "ui-graphics", "ui-text", "ui-unit", "ui-util", "ui-backhandler")
        family("org.jetbrains.compose.material3", "1.12.0-alpha03", "1.12.0-alpha01",
            "material3", "material3-window-size-class", "material3-adaptive-navigation-suite")
        family("org.jetbrains.compose.material3.adaptive", "1.3.0-rc01", "1.3.0-alpha01",
            "adaptive", "adaptive-layout", "adaptive-navigation")
        family("org.jetbrains.androidx.lifecycle", "2.11.0", "2.11.0-alpha01",
            "lifecycle-common", "lifecycle-runtime", "lifecycle-viewmodel", "lifecycle-viewmodel-savedstate",
            "lifecycle-runtime-compose", "lifecycle-viewmodel-compose", "lifecycle-viewmodel-navigation3")
        family("org.jetbrains.androidx.navigation", "2.10.0-beta01", "2.10.0-alpha01",
            "navigation-common", "navigation-runtime", "navigation-compose")
        family("org.jetbrains.androidx.navigation3", "1.1.2", "1.1.0-alpha01", "navigation3-ui")
        family("org.jetbrains.androidx.navigationevent", "1.1.0", "1.1.0-alpha01", "navigationevent-compose")
        family("org.jetbrains.androidx.savedstate", "1.4.0", "1.4.0-alpha01", "savedstate", "savedstate-compose")
        family("org.jetbrains.androidx.window", "1.5.1", "1.5.0-alpha01", "window-core")
        for (module in listOf("skiko", "skiko-jvm", "skiko-awt")) {
            put("org.jetbrains.skiko:$module:0.150.1", "io.github.compose-fluent:skiko-winui:0.150.1-alpha02")
        }
    }
}
