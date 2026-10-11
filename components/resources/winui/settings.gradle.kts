pluginManagement {
    includeBuild("../../../gradle-plugins")
    repositories { gradlePluginPortal(); mavenCentral(); google() }
}
dependencyResolutionManagement {
    repositories {
        providers.gradleProperty("composeWinUi.repository").orNull?.let { maven(it) }
        mavenCentral()
        google { content { includeGroupByRegex("androidx.*"); includeGroupByRegex("com\\.android.*") } }
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/") { mavenContent { snapshotsOnly() } }
    }
}
rootProject.name = "components-resources"
