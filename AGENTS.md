# AGENTS.md

## Project

DevToolbox — a personal IntelliJ IDEA dev toolbox. A fork of RestfulHelper (itself a
fork of request-mapper), extended with dev utilities beyond REST mapping navigation.

Two feature areas:

1. **REST mapping navigation** (inherited from RestfulHelper): quick navigation to HTTP
   URL mapping declarations (Spring, JAX-RS, Micronaut, Helidon) in Java and Kotlin source.
2. **Dev Toolbox** (self-use additions): a single right-hand tool window with three tabs —
   REST Services (endpoint list), JSON tools (format / minify / escape / unescape) and a
   side-by-side text diff tool.

Plugin id / namespace: `com.tripleh.devtoolbox`. **Not published on Marketplace** —
installed from disk (self-use). Package root: `com.tripleh.devtoolbox`.

Single-module Gradle Kotlin project. One source tree:

- `src/main/kotlin/com/tripleh/devtoolbox/`
  - `annotations/` — per-framework mapping-annotation models (`spring/`, `jaxrs/`, `micronaut/`) each with a `*MappingAnnotation` impl and a `UrlFormatter`; `extraction/` holds generic PSI visitors that read annotation values (string constants, references, concatenations, arrays).
  - `contributor/` — `ChooseByNameContributor`s: abstract `RequestMappingByNameContributor` plus `JavaRequestMappingContributor` and `KotlinRequestMappingContributor`.
  - `model/` — path/parameter model used for popup display.
    - `tools/` — the Dev Toolbox feature:
      - `json/` — `JsonToolsTabsPanel` (multi-document sub-tabs: "+" opens, per-tab close button) hosting one `JsonToolsPanel` per tab (split editor + tree view; Format/Minify/Escape/Unescape, indent selector, debounced live parse). `JsonFormat` (dependency-free recursive-descent JSON validator that builds a `JsonValue` tree with source offsets; minify/indent are tree serializers), `JsonEscape` (string escaping/unescaping), `JsonSyntaxHighlighter.kt` (self-contained JSON lexer + `JsonEditorField` line-numbered editor — deliberately independent of the optional IDE JSON plugin). No external JSON library on purpose (keeps Qodana clean).
      - `diff/` — `TextDiffPanel` (side-by-side `JTextPane` with line highlighting, click-to-sync, Alt+Up/Down navigation), `TextDiff` (Myers O(ND) line diff).
      - `ui/` — `StatusPanel` shared status bar.
      - `DevToolboxToolWindowFactory` — registers the single "Dev Toolbox" tool window with three tabs (REST Services / JSON Tools / Text Diff); `Memory` remembers the last active tab per IDE session. The REST tab is `RestServicesPanel` (root package), refreshed via `DumbService.runWhenSmart`.
  - Root classes: `GoToRequestMappingAction` (Navigate menu / Ctrl+\), `RequestMappingGoToContributor` (Search Everywhere factory), `RequestMappingModel` (a `FilteringGotoByModel`), `RequestMappingItem`, `RestServicesPanel` (the REST Services tab content, consumed by `DevToolboxToolWindowFactory`).
- `src/main/resources/META-INF/plugin.xml` (+ `pluginKotlin.xml`, icons)

## Build & verification

JVM 21 toolchain; Kotlin 2.0.x; versions live in `gradle/libs.versions.toml`.
Gradle configuration cache and build cache are both enabled (`gradle.properties`).

```bash
./gradlew build                 # compile + checks (produces build/libs jars, not the zip)
./gradlew buildPlugin           # produces the installable zip in build/distributions/
./gradlew test                  # JUnit 4 (note: src/test currently has no tests)
./gradlew runIde                # launch sandbox IDE with the plugin installed
./gradlew verifyPlugin          # plugin verifier vs recommended IDEs
```

Publishing is not set up for this fork (self-use; install from disk).

IntelliJ Platform target comes from `gradle.properties`: IC 2024.3, `pluginSinceBuild=243`, **no untilBuild** — keep compatibility broad; don't pin features to APIs newer than build 243 or remove the open-ended range without intent.

## Critical gotchas

- **Extension-point namespace == plugin id.** The custom extension point `requestMappingContributor` lives under the same `com.tripleh.devtoolbox` namespace as the plugin id (they were unified during the rename away from the upstream ids). Registering extensions under the wrong namespace silently drops them — if you ever change the plugin id, update `plugin.xml`/`pluginKotlin.xml` `defaultExtensionNs` AND the hardcoded name string in `extensions/Extensions.kt` together (this bug burned the upstream project three times).
- **Kotlin contributor is optional.** `plugin.xml` declares `<depends optional="true" config-file="pluginKotlin.xml">org.jetbrains.kotlin</depends>`. The Kotlin-side contributor only registers when the IDE has the Kotlin plugin. Both `com.intellij.java` and `org.jetbrains.kotlin` must stay listed in `platformBundledPlugins` in `gradle.properties`.
- **README markers are load-bearing.** The plugin description is extracted from README.md between `<!-- Plugin description -->` and `<!-- Plugin description end -->`; deleting/moving these fails the Gradle build. Same for change notes: CHANGELOG.md must contain an entry matching `pluginVersion` (or an `[Unreleased]` section) or packaging fails.
- **`./gradlew build` does not produce the zip.** The installable plugin zip is created by `./gradlew buildPlugin` (older IntelliJ Gradle templates ran `buildPlugin` as part of `build`; this one does not). Look in `build/distributions/`.

## Editing rules

- To add support for a new framework annotation: create a class in `annotations/<framework>/`, implement `MappingAnnotation` + `UrlFormatter`, register its FQN in that framework's `*Annotations.kt`, and ensure its simple name appears in `MappingAnnotation.Companion.supportedAnnotations` (matching is by simple name).
- URL parsing/formatting logic is per-framework; shared parsing helpers live in `extensions/Extensions.kt` and `utils/`.
- To add a new tool: create a panel class in `tools/<tool>/`, wire it as a tab in `DevToolboxToolWindowFactory`, and register it in `tools/` section of this file.
- JSON utilities must stay dependency-free (no external JSON lib) to keep Qodana warning-clean; `JsonFormat`/`JsonEscape` are the single source of truth for JSON validation/escaping.
- Releases: bump `pluginVersion` in `gradle.properties`, update CHANGELOG.md (the `[Unreleased]` section becomes the release notes), then push a version tag — `git tag v0.6.0-stable && git push origin v0.6.0-stable`. The tag name (minus the `v`) must equal `pluginVersion` or the workflow fails. Versions use the `<x.y.z>-stable[-suffix]` convention. CI builds run Qodana (`qodana.yml`, JDK 21); keep it warning-clean.

## Publishing — this fork is self-use, NOT published

This fork is for personal use and installed from disk (`./gradlew buildPlugin` →
`build/distributions/*.zip` → Install Plugin from Disk). Do **not** run `publishPlugin`.

Releases are handled by `.github/workflows/release-to-github.yml` (self-use): pushing a
`v*` tag builds the plugin and creates/overwrites a GitHub Release with the zip attached
(no Marketplace, no secrets beyond the default `GITHUB_TOKEN`). Pushing the same tag
again overwrites the release (via `gh release create --force`); note that overwriting an
existing *tag* is best-effort on GitHub — for a clean rerun, delete the remote tag and
push it again.

The upstream `release.yml` (which bound to the Marketplace `PUBLISH_TOKEN`/account and
ran `publishPlugin`) and the `releaseDraft` job in `build.yml` have been removed from
this fork.

## Why this fork was renamed (and the extension-point trap)

Renamed from RestfulHelper to **DevToolbox** (plugin id `com.tripleh.devtoolbox`, package root
`com.tripleh.devtoolbox`) for personal use. This is a fork of a fork (RestfulHelper ← request-mapper).

The upstream project kept **two** identities: a legacy plugin id `com.github.goldsubmarine.restfulhelper`
(used as the extension-point namespace) and a different package `com.github.nayacco.restfulhelper`.
Registering extensions under the wrong namespace silently dropped them (history: "fix Missing
extension point" ×3). During this fork's rename the two were unified — plugin id, extension-point
namespace (`plugin.xml` / `pluginKotlin.xml` `defaultExtensionNs`) and the package all equal
`com.tripleh.devtoolbox`. **If you ever change the plugin id again, update all three places
together, including the hardcoded extension-point name string in `extensions/Extensions.kt`.**

## Docs worth reading before sensitive changes

- IntelliJ Platform docs: https://plugins.jetbrains.com/docs/intellij/
- Past bugfixes around extension registration: git log mentions of "extension point"
