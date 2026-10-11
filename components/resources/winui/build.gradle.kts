plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("compose-winui")
    `maven-publish`
}

tasks.withType<Test>().configureEach { jvmArgs("--enable-native-access=ALL-UNNAMED") }

group = "io.github.compose-fluent.compose.components"
val coreVersion = providers.gradleProperty("composeWinUi.coreVersion").getOrElse("1.12.0-alpha01")
version = providers.gradleProperty("composeWinUi.version").getOrElse("$coreVersion-SNAPSHOT")
val skikoVersion = providers.gradleProperty("composeWinUi.skikoVersion").getOrElse("0.150.1-alpha02")

providers.gradleProperty("composeWinUi.publicationRepository").orNull?.let { repository ->
    publishing.repositories.maven {
        name = "Winui"
        url = uri(repository)
    }
}

publishing {
    repositories.maven {
        name = "CentralSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        credentials {
            username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
            password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Compose Resources for WinUI")
            description.set("Compose 1.12 Resources for Windows JVM and MinGW")
            url.set("https://github.com/compose-fluent/compose-multiplatform-winui")
            licenses { license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            } }
            developers { developer {
                id.set("compose-fluent")
                name.set("Compose Fluent contributors")
            } }
            scm {
                url.set("https://github.com/compose-fluent/compose-multiplatform-winui")
                connection.set("scm:git:https://github.com/compose-fluent/compose-multiplatform-winui.git")
            }
        }
    }
}

composeWinUi {
    jvm.set(true)
    redirect("org.jetbrains.skiko:skiko:0.150.1", "io.github.compose-fluent:skiko-winui:$skikoVersion")
}

kotlin {
    jvm("winuiJvm") { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
    mingwX64("winuiMingw")
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("../library/src/commonMain/kotlin")
            dependencies {
                implementation("io.github.compose-fluent.compose.runtime:runtime:$coreVersion")
                implementation("io.github.compose-fluent.compose.foundation:foundation:$coreVersion")
                // These primitives are forwarded upstream on JVM by the release.
                // Declare the APIs used directly by Resources on the compile classpath.
                implementation("io.github.compose-fluent.compose.ui:ui-geometry:$coreVersion")
                implementation("io.github.compose-fluent.compose.ui:ui-unit:$coreVersion")
                implementation("io.github.compose-fluent:skiko-winui:$skikoVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
        val winuiMain by creating {
            dependsOn(commonMain)
            kotlin.srcDirs("../library/src/blockingMain/kotlin", "../library/src/skikoMain/kotlin",
                "../library/src/winuiMain/kotlin")
        }
        val winuiJvmMain by getting {
            dependsOn(winuiMain)
            kotlin.srcDirs("../library/src/jvmAndAndroidMain/kotlin", "../library/src/winuiJvmMain/kotlin")
            // Reuse classloader loading and cache helpers, without AWT desktop environment code.
            kotlin.srcDir("../library/src/desktopMain/kotlin")
            kotlin.exclude("**/ResourceEnvironment.desktop.kt")
        }
        val winuiMingwMain by getting {
            dependsOn(winuiMain)
            kotlin.srcDir("../library/src/winuiMingwMain/kotlin")
        }
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val winuiJvmTest by getting {
            kotlin.srcDir("../library/src/winuiJvmTest/kotlin")
            resources.srcDir("../library/src/desktopTest/resources")
            resources.srcDir("../library/src/commonTest/resources")
        }
        val winuiMingwTest by getting { kotlin.srcDir("../library/src/winuiMingwTest/kotlin") }
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
            languageSettings.optIn("org.jetbrains.compose.resources.InternalResourceApi")
            languageSettings.optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
        }
    }
}

kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
    binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
        val testBinary = this
        val stage = tasks.register<Sync>("stage${name.replaceFirstChar { it.uppercaseChar() }}Resources") {
            from("../library/src/desktopTest/resources")
            from("../library/src/commonTest/resources")
            from("../library/src/winuiMingwTest/resources")
            into(testBinary.outputDirectory.resolve("compose-resources"))
        }
        linkTaskProvider.configure { dependsOn(stage) }
    }
}
