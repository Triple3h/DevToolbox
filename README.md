<div align="center">
    <img src="./src/main/resources/META-INF/pluginIcon.svg" width="220" height="220" alt="logo"/>
</div>
<h1 align="center">DevToolbox</h1>
<p align="center">DevToolbox is an IntelliJ IDEA personal dev toolbox, forked from <a href="https://github.com/Nayacco/RestfulHelper">RestfulHelper</a>.</p>

<!-- Plugin description -->

DevToolbox is a personal IntelliJ IDEA toolbox forked from [RestfulHelper](https://github.com/Nayacco/RestfulHelper) (which itself is a fork of [request-mapper](https://plugins.jetbrains.com/plugin/9567-request-mapper)), with additional dev utilities.

## Features ✨

#### 📗 REST mapping navigation (from RestfulHelper)

Quick navigation to HTTP URL mapping declarations (Spring, JAX-RS, Micronaut, Helidon) in Java and Kotlin source code.

- Search everywhere (<kbd>Shift</kbd> twice)
- Keyboard shortcut: <kbd>Cmd</kbd> + <kbd>\\</kbd> (macOS) / <kbd>Ctrl</kbd> + <kbd>\\</kbd> (Windows/Linux)
- Navigate (menu bar) -> Request Mapping

Supported annotations:

| Spring  | JAX-RS  | Micronaut  | Helidon (JAX-RS) |
|:-:|:-:|:-:|:-:|
| ```@RequestMapping``` | | | |
| ```@GetMapping``` | ```@GET``` | ```@Get``` | ```@GET``` |
| ```@PostMapping```  | ```@POST``` | ```@Post``` | ```@POST``` |
| ```@PutMapping``` | ```@PUT``` | ```@Put``` | ```@PUT``` |
| ```@DeleteMapping``` | ```@DELETE``` | ```@Delete``` | ```@DELETE``` |
| ```@PatchMapping``` | ```@PATCH``` |  ```@Patch``` | ```@PATCH``` |
| | ```@OPTIONS``` |  ```@Options``` | ```@OPTIONS``` |
| | ```@HEAD``` | ```@Head``` | ```@HEAD``` |

#### 🧰 Dev Toolbox (new)

A right-hand tool window — open it via **View → Tool Windows → Dev Toolbox**. It groups three tabs:

- **REST Services** — list all request mappings in the project (the REST mapping navigation above, now a tab in the window).
- **JSON Tools** — format / minify / escape / unescape JSON.
- **Text Diff** — compare two texts side by side.

<!-- Plugin description end -->

## Installation ⏳

This fork is self-use and not published to JetBrains Marketplace. Build the plugin zip locally and
install from disk:

```bash
./gradlew buildPlugin   # produces build/distributions/DevToolbox-<version>.zip
```

Then <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License 📄

This project is licensed under the MIT License.
