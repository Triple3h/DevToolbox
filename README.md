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
- Keyboard shortcut: <kbd>Ctrl</kbd> + <kbd>\\</kbd>
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

A right-hand tool window with three tabs:

- **REST Services** — list all request mappings in the project.
- **JSON Tools** — format / minify / escape / unescape JSON.
- **Text Diff** — compare two texts side by side.

<!-- Plugin description end -->

## Installation ⏳

Manually:

  Download the [latest release](https://github.com/Triple3h/RestfulHelper/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License 📄

This project is licensed under the MIT License.
