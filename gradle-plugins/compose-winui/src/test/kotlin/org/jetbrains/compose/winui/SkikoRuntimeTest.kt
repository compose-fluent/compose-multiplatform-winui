package org.jetbrains.compose.winui

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkikoRuntimeTest {
    @Test fun extractsOnlyNativeRuntimeAndKeepsExistingExecutable() {
        val directory = Files.createTempDirectory("compose-winui-runtime").toFile()
        try {
            fun jar(name: String, entries: Map<String, String>) {
                ZipOutputStream(directory.resolve(name).outputStream()).use { zip ->
                    entries.forEach { (path, value) ->
                        zip.putNextEntry(ZipEntry(path))
                        zip.write(value.toByteArray())
                        zip.closeEntry()
                    }
                }
            }
            jar("mingw.jar", mapOf(
                "winui-mingw/windows-x64/skiko_winui_skia.dll" to "native bridge",
                "winui-mingw/windows-x64/skiko_winui.dll" to "unused JVM bridge",
                "../escape.dll" to "must not be extracted",
            ))
            jar("windows.jar", mapOf("icudtl.dat" to "ICU data"))
            directory.resolve("settings.gradle").writeText("rootProject.name = 'runtime-test'")
            directory.resolve("build.gradle").writeText("""
                plugins { id 'compose-winui' }
                def prepare = tasks.register('prepareFixture', org.jetbrains.compose.winui.PrepareSkikoMingwRuntime) {
                    runtimeArchives.from('mingw.jar', 'windows.jar')
                    outputDirectory.set(layout.buildDirectory.dir('runtime'))
                }
                tasks.register('stageFixture', Copy) {
                    from(prepare.flatMap { it.outputDirectory })
                    into(layout.buildDirectory.dir('bin'))
                }
            """.trimIndent())
            val executable = directory.resolve("build/bin/app.exe")
            executable.parentFile.mkdirs()
            executable.writeText("existing executable")
            fun runner() = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("stageFixture", "--offline", "--configuration-cache")
            runner().build()
            assertEquals("native bridge", directory.resolve("build/bin/skiko_winui_skia.dll").readText())
            assertEquals("ICU data", directory.resolve("build/bin/icudtl.dat").readText())
            assertEquals("existing executable", executable.readText())
            assertFalse(directory.resolve("build/bin/skiko_winui.dll").exists())
            assertFalse(directory.resolve("build/escape.dll").exists())
            assertTrue(runner().build().output.contains("Reusing configuration cache"))
            jar("windows.jar", emptyMap())
            val missing = runner().buildAndFail()
            assertTrue(missing.output.contains("runtime archives are missing: [icudtl.dat]"), missing.output)
        } finally {
            directory.deleteRecursively()
        }
    }
}
