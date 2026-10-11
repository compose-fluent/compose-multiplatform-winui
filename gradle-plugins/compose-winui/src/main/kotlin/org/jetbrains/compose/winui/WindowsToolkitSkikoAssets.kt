package org.jetbrains.compose.winui

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty

/** Optional integration without forcing the Windows toolkit onto ordinary KMP projects. */
internal object WindowsToolkitSkikoAssets {
    fun configure(project: Project, compilationTask: String, files: Provider<List<RegularFile>>) {
        project.pluginManager.withPlugin("io.github.compose-fluent.windows-toolkit") {
            val noFiles = project.provider { emptyList<RegularFile>() }
            project.tasks.configureEach { task ->
                // Use the task's public Gradle properties across the optional plugin's classloader.
                val type = generateSequence(task.javaClass as Class<*>) { it.superclass }.firstOrNull {
                    it.name == "io.github.composefluent.windows.toolkit.gradle.StageWindowsPackageRuntimeAssetsTask"
                } ?: return@configureEach
                @Suppress("UNCHECKED_CAST")
                val compilations = type.getMethod("getApplicationCompilationTasks").invoke(task) as SetProperty<String>
                val assets = type.getMethod("getDependencyRuntimeAssetFiles").invoke(task) as ConfigurableFileCollection
                assets.from(compilations.flatMap { names ->
                    if (compilationTask in names) files else noFiles
                })
            }
        }
    }
}
