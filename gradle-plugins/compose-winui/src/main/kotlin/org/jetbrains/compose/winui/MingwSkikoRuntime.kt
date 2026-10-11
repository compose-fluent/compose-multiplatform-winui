package org.jetbrains.compose.winui

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object MingwSkikoRuntime {
    fun configure(project: Project, extension: ComposeWinUiExtension) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
            if (target.konanTarget != KonanTarget.MINGW_X64) return@configureEach
            val prepared = mutableMapOf<String, org.gradle.api.tasks.TaskProvider<PrepareSkikoMingwRuntime>>()
            target.binaries.configureEach binaries@ { binary ->
                if (binary !is Executable && binary !is TestExecutable && binary !is SharedLibrary) return@binaries
                val compilation = binary.compilation
                val prepare = prepared.getOrPut(compilation.name) {
                    val name = target.name.uppercaseFirst() + compilation.name.uppercaseFirst()
                    val compile = project.configurations.getByName(compilation.compileDependencyConfigurationName)
                    val selectedVersion = compile.incoming.resolutionResult.rootComponent.map(::selectedSkikoVersion)
                    val runtime = project.configurations.create("composeWinUi${name}SkikoRuntime") {
                        it.isCanBeConsumed = false
                        it.isCanBeResolved = true
                        it.isTransitive = false
                        it.defaultDependencies { dependencies ->
                            if (extension.mingw.get() && extension.mingwRuntime.get()) {
                                selectedVersion.get().takeIf(String::isNotEmpty)?.let { version ->
                                    for (module in listOf("skiko-winui-mingw-runtime", "skiko-winui-windows")) {
                                        dependencies.add(project.dependencies.create("io.github.compose-fluent:$module:$version"))
                                    }
                                }
                            }
                        }
                    }
                    project.tasks.register("prepare${name}SkikoRuntime", PrepareSkikoMingwRuntime::class.java) {
                        it.group = "compose winui"
                        it.description = "Extracts the resolved Skiko WinUI MinGW DLL and ICU data."
                        it.runtimeArchives.from(runtime)
                        it.outputDirectory.set(project.layout.buildDirectory.dir("compose-winui/skiko-runtime/${target.name}/${compilation.name}"))
                    }.also { prepare ->
                        val files = runtime.elements.zip(prepare.flatMap { it.outputDirectory }) { archives, directory ->
                            if (archives.isEmpty()) emptyList() else listOf(
                                directory.file("skiko_winui_skia.dll"), directory.file("icudtl.dat"),
                            )
                        }
                        WindowsToolkitSkikoAssets.configure(project, compilation.compileTaskProvider.name, files)
                    }
                }
                val stageName = target.name.uppercaseFirst() + binary.name.uppercaseFirst()
                val stage = project.tasks.register("stage${stageName}SkikoRuntime", Copy::class.java) {
                    it.group = "compose winui"
                    it.description = "Stages Skiko runtime files beside ${binary.name}."
                    it.from(prepare.flatMap { task -> task.outputDirectory })
                    // Copy only the prepared runtime files; never synchronize/delete the binary directory.
                    it.into(project.provider { binary.outputDirectory })
                }
                binary.linkTaskProvider.configure { it.dependsOn(stage) }
            }
        }
    }

    private fun selectedSkikoVersion(root: ResolvedComponentResult): String {
        val visited = mutableSetOf<org.gradle.api.artifacts.component.ComponentIdentifier>()
        val versions = mutableSetOf<String>()
        fun visit(component: ResolvedComponentResult) {
            if (!visited.add(component.id)) return
            val id = component.id as? ModuleComponentIdentifier
            if (id?.group == "io.github.compose-fluent" && id.module in setOf("skiko-winui", "skiko-winui-mingw")) {
                versions.add(id.version)
            }
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { visit(it.selected) }
        }
        visit(root)
        check(versions.size <= 1) { "Multiple Skiko WinUI versions in MinGW dependencies: $versions" }
        return versions.singleOrNull().orEmpty()
    }

    private fun String.uppercaseFirst(): String = replaceFirstChar { it.uppercaseChar() }
}
