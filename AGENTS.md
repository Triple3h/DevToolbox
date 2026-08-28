# AGENTS.md

## Project

RestfulHelper — an IntelliJ IDEA plugin for quick navigation to HTTP URL mapping declarations
(Spring, JAX-RS, Micronaut, Helidon) in Java and Kotlin source code. Fork of request-mapper.
Published on JetBrains Marketplace (plugin id 17400).

Single-module Gradle Kotlin project. One source tree:

- `src/main/kotlin/com/github/nayacco/restfulhelper/`
  - `annotations/` — per-framework mapping-annotation models (`spring/`, `jaxrs/`, `micronaut/`) each with a `*MappingAnnotation` impl and a `UrlFormatter`; `extraction/` holds generic PSI visitors that read annotation values (string constants, references, concatenations, arrays).
  - `contributor/` — `ChooseByNameContributor`s: abstract `RequestMappingByNameContributor` plus `JavaRequestMappingContributor` and `KotlinRequestMappingContributor`.
  - `model/` — path/parameter model used for popup display.
  - Root classes: `GoToRequestMappingAction` (Navigate menu / Ctrl+\), `RequestMappingGoToContributor` (Search Everywhere factory), `RequestMappingModel` (a `FilteringGotoByModel`), `RequestMappingItem`.
- `src/main/resources/META-INF/plugin.xml` (+ `pluginKotlin.xml`, icons)

## Build & verification

JVM 21 toolchain; Kotlin 2.0.x; versions live in `gradle/libs.versions.toml`.
Gradle configuration cache and build cache are both enabled (`gradle.properties`).

```bash
./gradlew build                 # compile + checks, produces plugin zip
./gradlew test                  # JUnit 4 (note: src/test currently has no tests)
./gradlew runIde                # launch sandbox IDE with the plugin installed
./gradlew verifyPlugin          # plugin verifier vs recommended IDEs
./gradlew publishPlugin         # needs PUBLISH_TOKEN/CERTIFICATE_CHAIN/PRIVATE_KEY(+PASSWORD) env vars; runs patchChangelog first
```

IntelliJ Platform target comes from `gradle.properties`: IC 2024.3, `pluginSinceBuild=243`, **no untilBuild** — keep compatibility broad; don't pin features to APIs newer than build 243 or remove the open-ended range without intent.

## Critical gotchas

- **Extension-point namespace ≠ package name.** The custom extension point `requestMappingContributor` lives under namespace `com.github.goldsubmarine.restfulhelper` (legacy plugin id kept from upstream — Marketplace identity), while all Kotlin packages are `com.github.nayacco.restfulhelper`. Registering extensions under the wrong namespace silently drops them (see history: "fix Missing extension point" ×3).
- **Kotlin contributor is optional.** `plugin.xml` declares `<depends optional="true" config-file="pluginKotlin.xml">org.jetbrains.kotlin</depends>`. The Kotlin-side contributor only registers when the IDE has the Kotlin plugin. Both `com.intellij.java` and `org.jetbrains.kotlin` must stay listed in `platformBundledPlugins` in `gradle.properties`.
- **README markers are load-bearing.** The plugin description is extracted from README.md between `<!-- Plugin description -->` and `<!-- Plugin description end -->`; deleting/moving these fails the Gradle build. Same for change notes: CHANGELOG.md must contain an entry matching `pluginVersion` (or an `[Unreleased]` section) or packaging fails.

## Editing rules

- To add support for a new framework annotation: create a class in `annotations/<framework>/`, implement `MappingAnnotation` + `UrlFormatter`, register its FQN in that framework's `*Annotations.kt`, and ensure its simple name appears in `MappingAnnotation.Companion.supportedAnnotations` (matching is by simple name).
- URL parsing/formatting logic is per-framework; shared parsing helpers live in `extensions/Extensions.kt` and `utils/`.
- Releases: bump `pluginVersion` in `gradle.properties`, update CHANGELOG.md, tag/GitHub-release — `.github/workflows/release.yml` publishes automatically on the release event. Versions use the `<x.y.z>-stable[-suffix]` convention (channel derived from pre-release label).
- CI builds run Qodana (`qodana.yml`, JDK 21); keep it warning-clean.

## Docs worth reading before sensitive changes

- IntelliJ Platform docs: https://plugins.jetbrains.com/docs/intellij/
- Past bugfixes around extension registration: git log mentions of "extension point"
