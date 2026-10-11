import io.github.composefluent.windows.toolkit.gradle.StageWindowsPackageRuntimeAssetsTask

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("compose-winui")
    id("io.github.compose-fluent.windows-toolkit") version "0.1.0"
}

composeWinUi { jvm.set(true) }

kotlin {
    jvmToolchain(25)
    jvm("winuiJvm")
    mingwX64("winuiMingw") { binaries.executable() }
    sourceSets.commonMain.dependencies { implementation("org.jetbrains.skiko:skiko:0.150.1") }
}

windows.application { mainClass.set("MainKt") }

// Check the published toolkit's packaging model without executing SDK restore,
// application compilation, signing or deployment tasks.
tasks.register("verifyToolkitSkikoAssets") {
    dependsOn("prepareWinuiMingwMainSkikoRuntime")
    doLast {
        val stages = tasks.withType<StageWindowsPackageRuntimeAssetsTask>().toList()
        val native = stages.filter { "compileKotlinWinuiMingw" in it.applicationCompilationTasks.get() }
        val jvm = stages.filter { "compileKotlinWinuiJvm" in it.applicationCompilationTasks.get() }
        check(native.size == 2) { "Expected debug and release native layouts: ${stages.map { it.name }}" }
        check(jvm.size == 1) { "Expected a JVM application layout" }
        native.forEach { stage ->
            val files = stage.dependencyRuntimeAssetFiles.files
            check(files.map { it.name }.toSet().containsAll(setOf("skiko_winui_skia.dll", "icudtl.dat")))
            check(files.all { it.isFile })
            check(stage.taskDependencies.getDependencies(stage).any { it.name == "prepareWinuiMingwMainSkikoRuntime" })
            println("Verified ${stage.name}: ${files.map { it.name }}")
        }
        jvm.forEach { stage ->
            check(stage.dependencyRuntimeAssetFiles.files.none { it.name in setOf("skiko_winui_skia.dll", "icudtl.dat") })
            check(stage.taskDependencies.getDependencies(stage).none { it.name.startsWith("prepareWinuiMingw") })
            println("Verified ${stage.name}: no MinGW runtime files or preparation tasks")
        }
    }
}
