import io.github.composefluent.windows.toolkit.gradle.WindowsExtension
import io.github.composefluent.windows.toolkit.gradle.WindowsPackageType

plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.12.1"
    id("compose-winui") version "1.12-SNAPSHOT"
    id("io.github.compose-fluent.windows-toolkit") version "0.1.0" apply false
}

if (providers.gradleProperty("composeWinUi.testToolkit").map(String::toBoolean).getOrElse(false)) {
    apply(plugin = "io.github.compose-fluent.windows-toolkit")
    extensions.configure<WindowsExtension> {
        packageReferences {
            windowsSdk("10.0.26100.0", generateProjection = false)
            nugetPackage("Microsoft.WindowsAppSDK.WinUI", "2.2.1") { generateProjection = false }
            nugetPackage("Microsoft.WindowsAppSDK.Runtime", "2.2.0") { generateProjection = false }
            nugetPackage("Microsoft.WindowsAppSDK.DWrite", "2.1.0") { generateProjection = false }
        }
        application {
            mainClass.set("sample.toolkit.main")
            packageType.set(WindowsPackageType.None)
            selfContained()
            console.set(true)
        }
    }
}

composeWinUi {
    jvm.set(true)
}

kotlin {
    jvmToolchain(25)
    jvm("winuiJvm")
    mingwX64("winuiMingw") { binaries.executable() }
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.compose.runtime:runtime:1.12.1")
        implementation("org.jetbrains.compose.components:components-resources:1.12.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        implementation(project(":resource-producer"))
    }
}

compose.resources { packageOfResClass = "sample.consumer.resources" }

val launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.register<JavaExec>("runWinuiJvmResourceProbe") {
    javaLauncher.set(launcher)
    mainClass.set("ConsumerKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    classpath(kotlin.targets.getByName("winuiJvm").compilations.getByName("main").output.allOutputs,
        configurations.getByName("winuiJvmRuntimeClasspath"))
}
