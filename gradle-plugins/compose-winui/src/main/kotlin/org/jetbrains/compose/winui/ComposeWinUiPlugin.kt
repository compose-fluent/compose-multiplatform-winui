package org.jetbrains.compose.winui

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.attributes.Attribute
import java.util.Locale

class ComposeWinUiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("composeWinUi", ComposeWinUiExtension::class.java)
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            MingwSkikoRuntime.configure(project, extension)
            project.pluginManager.withPlugin("org.jetbrains.compose") {
                MingwComposeResources.configure(project)
            }
        }
        val windows = project.providers.systemProperty("os.name")
            .map { it.lowercase(Locale.ROOT).startsWith("windows") }
        project.configurations.configureEach { configuration ->
            configuration.resolutionStrategy.dependencySubstitution { substitutions ->
                substitutions.all { dependency ->
                    val platform = configuration.attributes.keySet()
                        .firstOrNull { it.name == "org.jetbrains.kotlin.platform.type" }
                        ?.let { key ->
                            @Suppress("UNCHECKED_CAST")
                            configuration.attributes.getAttribute(key as Attribute<Any>)?.toString()
                        }
                    val nativeTarget = configuration.attributes
                        .getAttribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java))
                    if (!shouldRedirect(platform, nativeTarget, windows.get(), extension.mingw.get(), extension.jvm.get())) {
                        return@all
                    }
                    val requested = dependency.requested as? ModuleComponentSelector ?: return@all
                    val target = mappedCoordinates(requested.group, requested.module, requested.version,
                        extension.modules.get(), extension.groups.get(), extension.releaseMappings.get()) ?: return@all
                    dependency.useTarget(target, "Compose WinUI platform dependency mapping")
                }
            }
        }
    }
}

internal fun shouldRedirect(platform: String?, nativeTarget: String?, windows: Boolean, mingw: Boolean, jvm: Boolean): Boolean =
    (mingw && platform == "native" && nativeTarget == "mingw_x64") ||
        (jvm && windows && platform == "jvm")

internal fun mappedCoordinates(group: String, module: String, version: String,
    modules: Map<String, String>, groups: Map<String, String>, releaseMappings: Map<String, String> = emptyMap()): String? {
    modules["$group:$module:$version"]?.let { return it }
    val destination = groups["$group:$version"] ?: return releaseMappings["$group:$module:$version"]
    val (targetGroup, targetVersion) = destination.split(':').also {
        require(it.size == 2 && it.all(String::isNotBlank)) { "Invalid composeWinUi group destination: $destination" }
    }
    return "$targetGroup:$module:$targetVersion"
}
