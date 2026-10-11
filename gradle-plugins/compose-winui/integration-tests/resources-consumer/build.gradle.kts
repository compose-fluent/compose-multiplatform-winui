plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.12.1"
    id("compose-winui") version "1.12-SNAPSHOT"
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
