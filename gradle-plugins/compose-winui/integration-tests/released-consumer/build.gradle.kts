import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import java.util.zip.ZipFile

plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.compose") version "2.4.0"
    id("compose-winui")
}

composeWinUi { jvm.set(true) }

kotlin {
    jvmToolchain(25)
    jvm("winuiJvm")
    mingwX64("winuiMingw") {
        binaries.executable { baseName = "skiko-runtime-probe" }
    }
    iosArm64()
    sourceSets.commonMain.dependencies {
        // Original CMP coordinates: the plugin selects the published Windows fork.
        implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
        implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.3.0-rc01")
        implementation("org.jetbrains.androidx.navigation:navigation-compose:2.10.0-beta01")
        implementation("org.jetbrains.androidx.navigation3:navigation3-ui:1.1.2")
        implementation("org.jetbrains.androidx.window:window-core:1.5.1")
    }
    sourceSets.getByName("winuiJvmMain").dependencies {
        // Also exercise an explicit CMP JVM artifact rather than only KMP roots.
        implementation("org.jetbrains.compose.ui:ui-desktop:1.12.1")
    }
}

tasks.register("verifyReleasedRedirection") {
    doLast {
        for (configurationName in listOf("winuiJvmRuntimeClasspath", "winuiMingwCompileKlibraries")) {
            val configuration = configurations.getByName(configurationName)
            val files = configuration.resolve()
            check(files.isNotEmpty())
            val modules = configuration.incoming.resolutionResult.allComponents.mapNotNull {
                it.id as? ModuleComponentIdentifier
            }
            val coordinates = modules.map { "${it.group}:${it.module}:${it.version}" }.toSet()
            val required = listOf(
                "io.github.compose-fluent.compose.material3:material3:1.12.0-alpha01",
                "io.github.compose-fluent.compose.material3.adaptive:adaptive-navigation:1.3.0-alpha01",
                "io.github.compose-fluent.androidx.navigation:navigation-compose:2.10.0-alpha01",
                "io.github.compose-fluent.androidx.navigation3:navigation3-ui:1.1.0-alpha01",
                "io.github.compose-fluent.androidx.window:window-core:1.5.0-alpha01",
                "io.github.compose-fluent:skiko-winui:0.150.1-alpha02",
            )
            required.forEach { check(it in coordinates) { "$configurationName did not select $it: $coordinates" } }
            check(modules.none { it.group == "org.jetbrains.compose.ui" && it.module == "ui-desktop" })
            check(modules.none { it.group == "org.jetbrains.skiko" && it.module.startsWith("skiko-awt") })
            println("Verified $configurationName: ${modules.size} published modules")
        }
        val ios = configurations.getByName("iosArm64CompileKlibraries")
        check(ios.resolve().isNotEmpty())
        val iosModules = ios.incoming.resolutionResult.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
        check(iosModules.none { it.group.startsWith("io.github.compose-fluent") })
        check(iosModules.any {
            it.group == "org.jetbrains.androidx.navigation3" && it.module == "navigation3-ui" && it.version == "1.1.2"
        })
        println("Verified iOS remains on CMP: ${iosModules.size} modules")

        val forbidden = listOf("java/awt/", "javax/swing/", "java.awt.", "javax.swing.", "org/jetbrains/skiko/awt/")
        val jars = configurations.getByName("winuiJvmRuntimeClasspath").resolve().filter { it.extension == "jar" }
        val violations = mutableListOf<String>()
        jars.forEach { jar ->
            ZipFile(jar).use { zip ->
                zip.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                    val constants = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.ISO_8859_1) }
                    if (forbidden.any(constants::contains)) violations += "${jar.name}: ${entry.name}"
                }
            }
        }
        check(violations.isEmpty()) { "AWT/Swing references in WinUI classpath:\n${violations.joinToString("\n")}" }
        println("Verified ${jars.size} JVM jars without AWT/Swing references")
    }
}
