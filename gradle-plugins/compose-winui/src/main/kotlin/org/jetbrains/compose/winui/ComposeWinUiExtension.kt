package org.jetbrains.compose.winui

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/** Exact coordinate mappings avoid conflating independently versioned CMP libraries. */
abstract class ComposeWinUiExtension @Inject constructor(objects: ObjectFactory) {
    val mingw: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val jvm: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /** Automatically stage Skiko DLLs and ICU data for MinGW binaries. */
    val mingwRuntime: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /** Keys and values are group:module:version. Explicit entries override group mappings. */
    val modules: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
        .convention(emptyMap())

    /** Keys are group:sourceVersion; values are destinationGroup:destinationVersion. */
    val groups: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
        .convention(emptyMap())

    /** Published release defaults; explicit module and group rules take precedence. */
    val releaseMappings: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
        .convention(ComposeWinUiRelease.mappings)

    fun redirect(from: String, to: String) {
        require(from.split(':').size == 3 && to.split(':').size == 3) {
            "redirect requires group:module:version coordinates"
        }
        modules.put(from, to)
    }

    /** Register navigation, material3 and other version families independently. */
    fun redirectGroup(group: String, fromVersion: String, toVersion: String, toGroup: String = group) {
        require(listOf(group, fromVersion, toVersion, toGroup).all { it.isNotBlank() && ':' !in it })
        groups.put("$group:$fromVersion", "$toGroup:$toVersion")
    }
}
