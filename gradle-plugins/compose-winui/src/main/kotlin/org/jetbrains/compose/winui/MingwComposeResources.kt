package org.jetbrains.compose.winui

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.KonanTarget

/** Compose's KMP resource resolver adds generated/transitive resources to these source sets. */
internal object MingwComposeResources {
    fun configure(project: Project) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
            if (target.konanTarget != KonanTarget.MINGW_X64) return@configureEach
            target.binaries.configureEach binaries@ { binary ->
                if (binary !is Executable && binary !is TestExecutable) return@binaries
                val resources = project.files(project.provider {
                    (binary.compilation.associatedCompilations + binary.compilation).flatMap { compilation ->
                        compilation.allKotlinSourceSets.map { it.resources.sourceDirectories }
                    }
                })
                val name = "sync${target.name.uppercaseFirst()}${binary.name.uppercaseFirst()}ComposeResources"
                // Reuse the task if a future Compose plugin already supplies MinGW staging.
                val sync = if (name in project.tasks.names) project.tasks.named(name) else {
                    project.tasks.register(name, Sync::class.java) {
                        it.group = "compose winui"
                        it.description = "Stages Compose resources beside ${binary.name}."
                        it.from(resources)
                        it.into(project.provider { binary.outputDirectory.resolve("compose-resources") })
                    }
                }
                binary.linkTaskProvider.configure { it.dependsOn(sync) }
                if (binary is Executable) {
                    val directory = project.provider { binary.outputDirectory.resolve("compose-resources") }
                    val staged = project.files(project.fileTree(directory)).builtBy(sync)
                    WindowsToolkitComposeResources.configure(
                        project, "${target.name}:${binary.compilation.name}:${binary.name}", directory, staged,
                    )
                }
            }
        }
    }

    private fun String.uppercaseFirst(): String = replaceFirstChar { it.uppercaseChar() }
}
