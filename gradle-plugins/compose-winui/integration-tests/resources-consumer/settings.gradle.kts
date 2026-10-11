pluginManagement {
    repositories {
        providers.gradleProperty("composeWinUi.repository").orNull?.let { maven(it) }
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
            content { includeGroup("io.github.compose-fluent") }
        }
        gradlePluginPortal(); mavenCentral(); google()
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "compose-winui") {
            useModule("io.github.compose-fluent:compose-winui:${requested.version ?: "1.12-SNAPSHOT"}")
        }
    }
}
dependencyResolutionManagement {
    repositories {
        providers.gradleProperty("composeWinUi.repository").orNull?.let { maven(it) }
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
            content { includeGroup("io.github.compose-fluent.compose.components") }
        }
        mavenCentral(); google()
    }
}
rootProject.name = "compose-winui-resources-consumer"
include(":resource-producer")
