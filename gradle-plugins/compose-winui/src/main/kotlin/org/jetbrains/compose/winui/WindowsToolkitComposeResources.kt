package org.jetbrains.compose.winui

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File

/** Preserve resource paths when the optional Toolkit stages its application package. */
internal object WindowsToolkitComposeResources {
    fun configure(project: Project, variantId: String, root: Provider<File>, resources: ConfigurableFileCollection) {
        project.pluginManager.withPlugin("io.github.compose-fluent.windows-toolkit") {
            val generators = project.tasks.matching {
                it.name == "generateComposeResClass" || it.name.startsWith("generateResourceAccessorsFor") ||
                    it.name.startsWith("generateExpectResourceCollectorsFor") ||
                    it.name.startsWith("generateActualResourceCollectorsFor")
            }
            // Toolkit 0.1 reads Kotlin source roots, including Compose-generated sources.
            project.tasks.matching {
                it.name == "generateWinRTAuthoringCandidates" || it.name == "generateWinRTProjections"
            }.configureEach { it.dependsOn(generators) }
            // Register after Toolkit's task conventions, which set their target-path map.
            project.afterEvaluate {
                project.tasks.configureEach { task ->
                    val type = generateSequence(task.javaClass as Class<*>) { it.superclass }.firstOrNull {
                        it.name == "io.github.composefluent.windows.toolkit.gradle.StageWinAppPackageTask"
                    } ?: return@configureEach
                    @Suppress("UNCHECKED_CAST")
                    val variant = type.getMethod("getApplicationVariant").invoke(task) as Property<String>
                    val payload = type.getMethod("getPackagePayloadFiles").invoke(task) as ConfigurableFileCollection
                    @Suppress("UNCHECKED_CAST")
                    val paths = type.getMethod("getProjectPriTargetPaths").invoke(task) as MapProperty<String, String>
                    val files = variant.zip(resources.elements) { selected, elements ->
                        if (selected == variantId) elements.map { it.asFile } else emptyList()
                    }
                    payload.from(files)
                    paths.putAll(files.zip(root) { selected, directory ->
                        selected.associate { file ->
                            file.toPath().toAbsolutePath().normalize().toString() to
                                "compose-resources/${file.relativeTo(directory).invariantSeparatorsPath}"
                        }
                    })
                }
            }
        }
    }
}
