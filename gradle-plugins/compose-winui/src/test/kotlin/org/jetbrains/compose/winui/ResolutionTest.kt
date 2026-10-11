package org.jetbrains.compose.winui

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class ResolutionTest {
    @Test fun resolvesTransitiveMappingsWithoutTouchingOtherPlatforms() {
        val directory = Files.createTempDirectory("compose-winui-resolution").toFile()
        try {
            fun publish(group: String, module: String, version: String, dependency: String = "") {
                val artifact = directory.resolve("repo/${group.replace('.', '/')}/$module/$version")
                artifact.mkdirs()
                artifact.resolve("$module-$version.pom").writeText("""
                    <project><modelVersion>4.0.0</modelVersion><groupId>$group</groupId>
                    <artifactId>$module</artifactId><version>$version</version><packaging>pom</packaging>
                    $dependency</project>
                """.trimIndent())
            }
            publish("sample", "app", "1", """<dependencies><dependency>
                <groupId>org.jetbrains.compose.ui</groupId><artifactId>ui</artifactId><version>1.12.1</version>
                </dependency></dependencies>""")
            val skikoDependency = """<dependencies><dependency><groupId>org.jetbrains.skiko</groupId>
                <artifactId>skiko</artifactId><version>0.150.1</version></dependency>
                <dependency><groupId>org.jetbrains.compose.components</groupId>
                <artifactId>components-resources</artifactId><version>1.12.1</version></dependency></dependencies>"""
            publish("org.jetbrains.compose.ui", "ui", "1.12.1", skikoDependency)
            publish("io.github.compose-fluent.compose.ui", "ui", "1.12.0-alpha01", skikoDependency)
            publish("custom.ui", "ui", "42", skikoDependency)
            publish("org.jetbrains.skiko", "skiko", "0.150.1")
            publish("io.github.compose-fluent", "skiko-winui", "0.150.1-alpha02")
            publish("org.jetbrains.compose.components", "components-resources", "1.12.1")
            publish("io.github.compose-fluent.compose.components", "components-resources", "1.12.0-alpha01-SNAPSHOT")
            directory.resolve("settings.gradle").writeText("rootProject.name = 'resolution-test'")
            directory.resolve("build.gradle").writeText("""
                plugins { id 'compose-winui' }
                composeWinUi {
                    jvm.set(providers.gradleProperty('enableJvm').map { it.toBoolean() }.orElse(false))
                    mingw.set(providers.gradleProperty('enableMingw').map { it.toBoolean() }.orElse(true))
                    if (providers.gradleProperty('overrideGroup').isPresent()) {
                        redirectGroup('org.jetbrains.compose.ui', '1.12.1', '42', 'custom.ui')
                    }
                    if (providers.gradleProperty('disableDefaults').isPresent()) releaseMappings.set([:])
                }
                repositories { maven { url = uri('repo') } }
                def platform = Attribute.of('org.jetbrains.kotlin.platform.type', String)
                def target = Attribute.of('org.jetbrains.kotlin.native.target', String)
                [native: ['native', 'mingw_x64'], jvm: ['jvm', null],
                    androidJvm: ['androidJvm', null], common: ['common', null],
                    ios: ['native', 'ios_arm64'], macos: ['native', 'macos_arm64'],
                    linux: ['native', 'linux_x64'], js: ['js', null], wasm: ['wasm', null]
                ].each { name, values ->
                    def c = configurations.create(name + 'Probe') {
                        canBeResolved = true
                        canBeConsumed = false
                        attributes.attribute(platform, values[0])
                        if (values[1] != null) attributes.attribute(target, values[1])
                    }
                    dependencies.add(c.name, 'sample:app:1')
                    tasks.register(name + 'Probe') {
                        def result = c.incoming.resolutionResult.rootComponent
                        doLast {
                            def app = result.get().dependencies.first().selected
                            println('SELECTED=' + app.dependencies.first().selected.id.displayName)
                            println('SKIKO=' + app.dependencies.first().selected.dependencies.first().selected.id.displayName)
                            println('RESOURCES=' + app.dependencies.first().selected.dependencies.find {
                                it.requested.module == 'components-resources'
                            }.selected.id.displayName)
                        }
                    }
                }
            """.trimIndent())
            val result = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("nativeProbe", "jvmProbe", "androidJvmProbe", "commonProbe", "--offline", "--stacktrace")
                .build()
            assertTrue(result.output.contains("SELECTED=io.github.compose-fluent.compose.ui:ui:1.12.0-alpha01"), result.output)
            assertTrue(result.output.split("SELECTED=org.jetbrains.compose.ui:ui:1.12.1").size == 4, result.output)
            assertTrue(result.output.contains("SKIKO=io.github.compose-fluent:skiko-winui:0.150.1-alpha02"), result.output)
            assertTrue(result.output.split("SKIKO=org.jetbrains.skiko:skiko:0.150.1").size == 4, result.output)
            assertTrue(result.output.contains("RESOURCES=io.github.compose-fluent.compose.components:components-resources:1.12.0-alpha01-SNAPSHOT"), result.output)
            assertTrue(result.output.split("RESOURCES=org.jetbrains.compose.components:components-resources:1.12.1").size == 4, result.output)
            val jvmResult = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("jvmProbe", "-PenableJvm=true", "--offline", "--configuration-cache")
                .build()
            val expected = if (System.getProperty("os.name").startsWith("Windows"))
                "io.github.compose-fluent.compose.ui:ui:1.12.0-alpha01" else "org.jetbrains.compose.ui:ui:1.12.1"
            assertTrue(jvmResult.output.contains("SELECTED=$expected"), jvmResult.output)
            val expectedResources = if (System.getProperty("os.name").startsWith("Windows"))
                "io.github.compose-fluent.compose.components:components-resources:1.12.0-alpha01-SNAPSHOT"
            else "org.jetbrains.compose.components:components-resources:1.12.1"
            assertTrue(jvmResult.output.contains("RESOURCES=$expectedResources"), jvmResult.output)
            val cached = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("jvmProbe", "-PenableJvm=true", "--offline", "--configuration-cache")
                .build()
            assertTrue(cached.output.contains("Reusing configuration cache"), cached.output)
            // Even explicit JVM opt-in must leave every non-Windows target untouched.
            val otherTargets = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("androidJvmProbe", "commonProbe", "iosProbe", "macosProbe", "linuxProbe",
                    "jsProbe", "wasmProbe", "-PenableJvm=true", "--offline")
                .build()
            assertTrue(otherTargets.output.split("SELECTED=org.jetbrains.compose.ui:ui:1.12.1").size == 8,
                otherTargets.output)
            assertTrue(otherTargets.output.split("SKIKO=org.jetbrains.skiko:skiko:0.150.1").size == 8,
                otherTargets.output)
            assertTrue(otherTargets.output.split("RESOURCES=org.jetbrains.compose.components:components-resources:1.12.1").size == 8,
                otherTargets.output)
            val overridden = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                .withArguments("nativeProbe", "-PoverrideGroup=true", "--offline").build()
            assertTrue(overridden.output.contains("SELECTED=custom.ui:ui:42"), overridden.output)
            for (argument in listOf("-PenableMingw=false", "-PdisableDefaults=true")) {
                val disabled = GradleRunner.create().withProjectDir(directory).withPluginClasspath()
                    .withArguments("nativeProbe", argument, "--offline").build()
                assertTrue(disabled.output.contains("SELECTED=org.jetbrains.compose.ui:ui:1.12.1"), disabled.output)
                assertTrue(disabled.output.contains("SKIKO=org.jetbrains.skiko:skiko:0.150.1"), disabled.output)
                assertTrue(disabled.output.contains("RESOURCES=org.jetbrains.compose.components:components-resources:1.12.1"), disabled.output)
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
