pluginManagement {
    includeBuild("../../..")
    repositories { gradlePluginPortal(); mavenCentral(); google() }
}

dependencyResolutionManagement {
    repositories { mavenCentral(); google() }
}
rootProject.name = "compose-winui-released-consumer"
