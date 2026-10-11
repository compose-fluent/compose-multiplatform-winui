plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.compose-fluent"
version = "1.12-SNAPSHOT"

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
            name.set("Compose WinUI Gradle plugin")
            description.set("Compose 1.12 dependency redirection and Windows runtime staging")
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

// The short plugin ID's marker group is outside the Central namespace. Consumers
// resolve that ID to the implementation module in pluginManagement instead.
tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { repository.name != "CentralSnapshots" || publication.name == "pluginMaven" }
}

gradlePlugin {
    plugins {
        create("composeWinUi") {
            id = "compose-winui"
            implementationClass = "org.jetbrains.compose.winui.ComposeWinUiPlugin"
            displayName = "Compose WinUI"
            description = "Configurable Compose Multiplatform dependency redirection for Windows"
        }
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("native-utils"))
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test { useJUnitPlatform() }
