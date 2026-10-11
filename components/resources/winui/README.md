# Compose Resources for WinUI

This build publishes `io.github.compose-fluent.compose.components:components-resources:1.12-SNAPSHOT`
with `winuiJvm` and `winuiMingw` variants. It reuses the Resources common API,
blocking resource state, Skia image/SVG decoding and byte-backed fonts. The regular
Resources build remains in `../library`.

Publication is limited to the JVM and MinGW binaries plus the required KMP root
metadata. No Android, Apple, Linux, JS or Wasm target or forwarding publication is
created by this build. The `compose-winui` plugin leaves those consumer targets on
their original CMP dependencies, including when JVM redirection is enabled.

The JVM variant loads classpath resources and uses Windows APIs through JDK FFM for
system DPI and the light/dark preference. Run with JDK 25 and
`--enable-native-access=ALL-UNNAMED`, as required by the WinUI host. The MinGW
variant uses Unicode Win32 file APIs, supports ranged reads, and loads from
`compose-resources` beside the executable, independent of the working directory.
When used alongside the Compose Gradle plugin, `compose-winui` stages generated
and transitive resources there when linking MinGW executables and tests. Copy
this directory alongside the executable when distributing it.

Use the repository's Gradle 9.5 wrapper, from this directory:

```powershell
../../../gradle-plugins/gradlew.bat -p . winuiJvmTest winuiMingwTest --no-configuration-cache
```

Dependencies default to the published Compose WinUI release on Maven Central,
without Maven Local or a core composite build. MinGW DLLs and ICU data are staged
automatically by `compose-winui`. Optionally provide
`-PcomposeWinUi.repository=<local Maven repository>` to validate other publications.
Other version properties:

| Property | Default |
| --- | --- |
| `composeWinUi.version` | `1.12-SNAPSHOT` (Resources publication) |
| `composeWinUi.coreVersion` | `1.12.0-alpha01` (published core release) |
| `composeWinUi.skikoVersion` | `0.150.1-alpha02` |

`composeWinUi.publicationRepository` optionally enables a `Winui` Maven repository
for `publishAllPublicationsToWinuiRepository`; it accepts a directory or repository
URL. `CentralSnapshots` publishes to Central Portal Snapshots using the
`MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` environment variables.

Resources is part of the plugin's default CMP mapping: original CMP Resources
`1.12.1` dependencies automatically select this adapter on the enabled Windows
targets. Consumers use `compose-winui` version `1.12-SNAPSHOT` and declare the
snapshot repository as shown in the [plugin setup](../../../gradle-plugins/compose-winui/README.md).
JVM consumers explicitly opt in:

```kotlin
composeWinUi {
    jvm.set(true)
}
```

The native vector XML parser supports namespaces, quoted attributes, standard and
numeric entities, comments, processing instructions and CDATA. DTDs/external
entities are rejected. Composable resource selection uses Compose's current
window density/theme/locale; non-composable selection uses the system defaults.

## Validation

The test suites cover JVM PNG/SVG/font loading, classpath/ranged resource reads,
Unicode paths, Windows environment lookup, and native PNG/SVG/vector/font loading using
automatically staged Skiko runtime files. To also validate publication metadata,
replace the test task names in the command above with:

```text
generateMetadataFileForKotlinMultiplatformPublication
generateMetadataFileForWinuiJvmPublication
generateMetadataFileForWinuiMingwPublication
```

This checks the root, JVM and MinGW publications against the published dependencies.
The [plugin's consumer checks](../../../gradle-plugins/compose-winui/README.md)
also exercise generated resources, transitive library resources and configuration
cache reuse with the official Compose Gradle plugin.
