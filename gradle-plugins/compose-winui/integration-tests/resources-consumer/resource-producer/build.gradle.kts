plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("compose-winui")
}

composeWinUi {
    jvm.set(true)
}

kotlin {
    jvmToolchain(25)
    jvm("winuiJvm")
    mingwX64("winuiMingw")
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.compose.runtime:runtime:1.12.1")
        implementation("org.jetbrains.compose.components:components-resources:1.12.1")
    }
}

compose.resources { packageOfResClass = "sample.producer.resources" }
