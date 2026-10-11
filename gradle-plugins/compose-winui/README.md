# Compose WinUI Gradle plugin

The `compose-winui` project plugin redirects dependencies in MinGW x64 configurations
by default. JVM redirection requires `jvm.set(true)` and a Windows host. Platform
attributes, rather than target names, select the configurations. Android, Darwin,
Linux, JS, Wasm and common metadata configurations are not redirected.

The published WinUI release uses Kotlin/Compose compiler `2.4.0` and JDK 25.
Use that compiler baseline and configure `kotlin { jvmToolchain(25) }` for JVM
consumers. Dependency redirection does not change the project's toolchain.

The plugin release line `1.12` represents Compose 1.12 compatibility. The current
development version is `1.12-SNAPSHOT`, published to Central Portal Snapshots.
Resources follows the CMP WinUI core version rather than the plugin version:
the adapter for core `1.12.0-alpha01` is currently `1.12.0-alpha01-SNAPSHOT`.
Configure `settings.gradle.kts` to resolve the short plugin ID to its Maven module:

```kotlin
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
            content { includeGroup("io.github.compose-fluent") }
        }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "compose-winui") {
            useModule("io.github.compose-fluent:compose-winui:${requested.version ?: "1.12-SNAPSHOT"}")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
            content { includeGroup("io.github.compose-fluent.compose.components") }
        }
        mavenCentral()
        google()
    }
}
```

The short ID's plugin marker uses the group `compose-winui`, outside the verified
Central namespace. The snapshot therefore publishes the implementation module
`io.github.compose-fluent:compose-winui`; the resolution rule above keeps
`id("compose-winui")` available to consumers.

```kotlin
plugins {
    id("compose-winui") version "1.12-SNAPSHOT"
}

composeWinUi {
    // mingw.set(false) // disable the default MinGW mapping
    jvm.set(true)

    // The published release mappings below are already enabled.
    // Override a family when consuming a different release:
    // redirectGroup("org.jetbrains.androidx.navigation", "2.10.0-beta01",
    //     "2.10.0-alpha02", "io.github.compose-fluent.androidx.navigation")

    // An exact module rule takes precedence over a group rule, and can rename
    // the group or artifact (including a platform-specific artifact).
    redirect("example:library:1.0", "example.winui:library:1.0-alpha01")
}
```

Defaults include Resources as part of CMP, alongside the 36 modules published in
[Compose WinUI 1.12.0-alpha01](https://github.com/compose-fluent/compose-multiplatform-core/releases/tag/compose-winui/v1.12.0-alpha01).
`org.jetbrains.compose.*` maps to `io.github.compose-fluent.compose.*`, and
`org.jetbrains.androidx.*` maps to `io.github.compose-fluent.androidx.*`.

| Family | Exact CMP source version | WinUI version |
| --- | --- | --- |
| Animation, Foundation, Material, Runtime, UI | `1.12.1` | `1.12.0-alpha01` |
| Resources | `1.12.1` | `1.12.0-alpha01-SNAPSHOT` |
| Material3 | `1.12.0-alpha03` | `1.12.0-alpha01` |
| Material3 Adaptive | `1.3.0-rc01` | `1.3.0-alpha01` |
| Lifecycle | `2.11.0` | `2.11.0-alpha01` |
| Navigation 2 | `2.10.0-beta01` | `2.10.0-alpha01` |
| Navigation 3 | `1.1.2` | `1.1.0-alpha01` |
| Navigation Event | `1.1.0` | `1.1.0-alpha01` |
| Savedstate | `1.4.0` | `1.4.0-alpha01` |
| Window Core | `1.5.1` | `1.5.0-alpha01` |

Other versions and modules absent from the release are untouched. Concrete CMP
artifact requests with a WinUI implementation are mapped through the fork's root
metadata to select the correct published variant. CMP JVM leaves used by the
release's native-only UI modules remain available so their metadata forwarding
can finish without a substitution cycle. The release metadata also handles
modules whose Windows implementations are provided directly by Jetpack.

Skiko `0.150.1` (`skiko`, `skiko-jvm`, `skiko-awt`) maps to
`io.github.compose-fluent:skiko-winui:0.150.1-alpha02`, including transitive requests.
Explicit `redirect(...)` rules take precedence over `redirectGroup(...)`, which
take precedence over release defaults. `releaseMappings.set(emptyMap())` clears
all built-in mappings. No repository is added automatically: declare the snapshot
repository, `mavenCentral()` and `google()` as shown above. Original
`implementation(compose.components.resources)` declarations are redirected
automatically; no additional Resources rule is required.

For MinGW executables, tests, and shared libraries the plugin finds the **resolved**
Skiko WinUI version, downloads that version's `skiko-winui-mingw-runtime` and
`skiko-winui-windows` JARs, and extracts `skiko_winui_skia.dll` and `icudtl.dat`.
It copies those files beside each binary before linking, including binaries with
custom output directories. Import libraries are already embedded in the published
Skiko KLIB, so no manual import-library path or `linkerOpts` is needed. When the
Windows Toolkit plugin is present, its MinGW application runtime-assets layout
also receives these files. JVM application layouts receive no MinGW assets.
Distribute the two staged files with the native application. Set
`mingwRuntime.set(false)` to manage them yourself; `mingw.set(false)` also disables
automatic staging. Projects without a Skiko WinUI dependency download no runtime.

Alongside `org.jetbrains.compose`, the plugin also stages generated and transitive
Compose resources in `compose-resources` beside MinGW executables and tests. This
directory is also preserved in the Windows Toolkit's final application package,
and Toolkit source scanning waits for Compose resource accessors to be generated.
This uses the Compose plugin's existing KMP resource resolver; consumers need no fork
of the Compose Gradle plugin or manual resource-copy task. See the
[Resources build](../../components/resources/winui/README.md) for its publication
and platform implementation details.

Build and test from `gradle-plugins`:

```text
gradlew :compose-winui:test :compose-winui:validatePlugins
gradlew :compose-winui:publishToMavenLocal
```

The mapping tests use a fixture Maven repository to check transitive substitution,
platform isolation, independent versions, overrides and configuration-cache reuse.
Runtime archive tests verify staging without overwriting executables, ignoring
unrelated ZIP entries, and reporting missing payloads.

Validate real Maven Central dependencies from `gradle-plugins`:

```text
gradlew -p compose-winui/integration-tests/released-consumer compileKotlinWinuiJvm compileKotlinWinuiMingw verifyReleasedRedirection runDebugExecutableWinuiMingw
```

This consumer declares original CMP coordinates, compiles Nav2/Nav3 and Material3
for both Windows targets, verifies iOS stays on CMP, checks the JVM classpath for
AWT/Swing references, and executes a native Skia raster probe with automatically
staged DLL/data files. It uses only Maven Central and Google Maven for libraries;
the included build supplies just this Gradle plugin. The Resources WinUI build
(`components/resources/winui`) separately tests the published fork and Skiko.

The optional published Windows Toolkit packaging integration is checked with:

```text
gradlew -p compose-winui/integration-tests/windows-toolkit verifyToolkitSkikoAssets
```

For an end-to-end resource-generation check with the **official** Compose Gradle
plugin, first publish this plugin and the Resources adapter to an isolated local
repository. Run these PowerShell commands from `gradle-plugins`:

```powershell
$validationMaven = Join-Path (Get-Location) ".gradle/validation/maven"
./gradlew.bat :compose-winui:publishAllPublicationsToWinuiRepository "-PcomposeWinUi.publicationRepository=$validationMaven"
./gradlew.bat -p ../components/resources/winui publishAllPublicationsToWinuiRepository "-PcomposeWinUi.publicationRepository=$validationMaven"
./gradlew.bat -p compose-winui/integration-tests/resources-consumer runWinuiJvmResourceProbe runDebugExecutableWinuiMingw "-PcomposeWinUi.repository=$validationMaven" --configuration-cache
```

The consumer reads both its generated resources and another module's resources
on JVM and MinGW, without a manual copy task. Repeating the last command also
checks configuration-cache reuse. Core and Skiko dependencies still come from
their published repositories, while the new plugin/Resources publications stay
inside the workspace.

Omit `composeWinUi.repository` to run the same consumer against the published
snapshot. `--refresh-dependencies` fetches the latest timestamped snapshot.

The `publish-compose-winui-snapshot.yml` workflow tests both targets and publishes
the plugin plus the Resources root/JVM/MinGW publications. It runs when the
`codex/compose-winui-1.12-snapshot` branch is updated, and supports manual dispatch
once the workflow is available on the default branch.
It uses the existing organization secrets `MAVEN_CENTRAL_USERNAME` and
`MAVEN_CENTRAL_PASSWORD`. Local publishing can use the same environment variables:

```text
gradlew -p ../components/resources/winui publishAllPublicationsToCentralSnapshotsRepository
gradlew :compose-winui:publishPluginMavenPublicationToCentralSnapshotsRepository
```
