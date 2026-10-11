package org.jetbrains.compose.winui

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

@CacheableTask
abstract class PrepareSkikoMingwRuntime : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeArchives: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val directory = outputDirectory.get().asFile
        directory.mkdirs()
        val entries = mapOf(
            "winui-mingw/windows-x64/skiko_winui_skia.dll" to "skiko_winui_skia.dll",
            "icudtl.dat" to "icudtl.dat",
        )
        val archives = runtimeArchives.files
        if (archives.isEmpty()) {
            entries.values.forEach { directory.resolve(it).delete() }
            return
        }
        val found = mutableSetOf<String>()
        archives.forEach { archive ->
            ZipFile(archive).use { zip ->
                entries.forEach entry@ { (entryName, outputName) ->
                    val entry = zip.getEntry(entryName) ?: return@entry
                    if (!found.add(outputName)) throw GradleException("Duplicate Skiko runtime file: $outputName")
                    zip.getInputStream(entry).use { input ->
                        directory.resolve(outputName).outputStream().use { input.copyTo(it) }
                    }
                }
            }
        }
        val missing = entries.values - found
        if (missing.isNotEmpty()) throw GradleException("Skiko WinUI runtime archives are missing: $missing")
    }
}
