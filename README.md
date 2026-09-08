# Midnight Compact Language Plugin for IntelliJ IDEA

[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-Plugin-blue.svg)](https://plugins.jetbrains.com)
[![Plugin Version](https://img.shields.io/badge/Version-v1.2.3-blue.svg)](CHANGELOG.md)
[![Java Toolchain](https://img.shields.io/badge/JDK-25-orange.svg)](build.gradle.kts)
[![Build & Tests](https://img.shields.io/badge/Tests-444%20Passing-brightgreen.svg)](test_output.txt)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Midnight Network](https://img.shields.io/badge/Midnight-Network-purple.svg)](https://midnight.network)

An open-source JetBrains IntelliJ Platform plugin delivering first-class, industrial-grade development support for **Compact**, the smart contract programming language of the **Midnight** privacy-preserving blockchain.

---

## 💡 What Problem Are We Solving?

### The Midnight & Compact Context
[Midnight](https://midnight.network) is a privacy-focused blockchain built around zero-knowledge (ZK) cryptography. Smart contracts on Midnight are authored in **Compact**, a domain-specific language engineered specifically for ZK-SNARK contract development. Compact lets developers seamlessly express private state logic (`witness`, `disclose`), public ledger state (`ledger`, `sealed`), zero-knowledge circuits (`circuit`, `pure`), module architectures, and domain cryptographic primitives (`Field`, `Uint`, `Bytes`, `Cell`, `Map`, `Vector`, `JubjubScalar`, `Secp256k1`).

### The Developer Experience Gap
Writing Compact smart contracts requires navigating complex ZK privacy primitives, strict syntax models, and custom type constructs. Without dedicated IDE tooling, developers face major challenges:
- **Zero Feedback Loop**: Syntax errors, unclosed blocks, and missing type declarations are only caught after manual terminal compiler invocations.
- **Difficult Navigation**: Tracing variable bindings, circuit parameters, struct fields, and imported modules across multi-file contracts requires manual text searches.
- **Risky Refactoring**: Renaming circuits, state variables, or struct types across contracts is error-prone and can break ZK verification circuits.
- **Toolchain Inconvenience**: Switching between multiple Compact compiler versions (e.g. `0.23` vs `0.26` / toolchains `0.31.1` vs `0.34.0`) requires manually modifying global system paths or terminal environments.
- **ZK Constraint Traps**: Accidentally modifying ledger state in `pure` circuits, attempting recursion inside circuit constraints, mutating `sealed` fields, or assigning undisclosed witness data directly to ledger state results in cryptic compiler failures or security bugs.

### Our Solution
The **Midnight Compact Language Plugin** transforms IntelliJ IDEA and IntelliJ-based IDEs into a comprehensive smart-contract development environment. Built with a custom handwritten lexer, recursive-descent Pratt parser, typed Abstract Syntax Tree (PSI) model, type inference engine, embedded Remix-style compiler tool window, multi-version compiler manager, non-blocking status bar monitor, and 10 static semantic inspections with one-click quick-fixes.

---

## ⚡ Feature Matrix at a Glance

| Feature | Keybinding (Win/Linux) | Keybinding (macOS) | Description |
| :--- | :--- | :--- | :--- |
| **Syntax Highlighting** | *Automatic* | *Automatic* | Lexer token coloring, keyword classification, literals, and comments |
| **Go to Declaration** | `Ctrl + B` / `F12` | `Cmd + B` | Scoped reference resolution for variables, circuits, structs, and modules |
| **Go to Type Declaration** | `Ctrl + Shift + B` | `Cmd + Shift + B` | Navigate directly to the underlying struct, enum, or type alias |
| **Contextual Code Completion** | `Ctrl + Space` | `Cmd + Space` | Context-aware completion for keywords, types, values, and members |
| **Parameter Info** | `Ctrl + P` | `Cmd + P` | Live tooltip with parameter names, types, and active argument tracking |
| **Inlay Parameter Hints** | *Automatic* | *Automatic* | Declarative inline hints for circuit and constructor arguments |
| **Find Usages** | `Alt + F7` | `Option + F7` | Fast, index-backed search for all element occurrences across the project |
| **Inplace Rename** | `Shift + F6` | `Shift + F6` | Safe refactoring with identifier validation and keyword collision guards |
| **Remix-Style Compiler Panel** | *Right Stripe* | *Right Stripe* | Dedicated tool window: version switcher, pragma badge, 1-click compile |
| **Status Bar Monitor** | *Status Bar* | *Status Bar* | Instant compiler version monitor with popup menu and quick actions |
| **Gutter Line Markers** | *Gutter Icons* | *Gutter Icons* | Bidirectional navigation between interface definitions and implementations |
| **Structure View** | `Alt + 7` | `Cmd + 7` | Outline tree of contracts, circuits, witnesses, structs, and enums |
| **Quick Documentation** | `Ctrl + Q` / `F1` | `Ctrl + J` | Inferred signatures, doc comments (`///`), and member definitions |
| **Code Formatting** | `Ctrl + Alt + L` | `Cmd + Option + L` | Canonical 2-space indentation and binary operator spacing |
| **Semantic Inspections** | `Alt + Enter` | `Option + Enter` | 10 real-time inspections with automated quick-fixes |
| **Compiler Run Action** | `Shift + F10` / `▶` | `Control + R` / `▶` | Gutter and toolbar execution for Compact compiler build targets |

---

## ✨ Features & Subsystems

### 🛠️ 1. Remix-Style "Compact Compiler" Tool Window
Located on the right-hand stripe of the IDE, the **Compact Compiler** panel provides a familiar, web-IDE-style experience directly inside IntelliJ:
- **Active Contract Tracking**: Automatically tracks the active `.compact` contract in the editor, updating contract metadata dynamically upon tab changes or document edits.
- **Live Compiler Version Switcher**: Instant dropdown to switch between installed compiler toolchains without touching terminal or environment variables.
- **Dynamic Pragma Compatibility Badges**: Real-time SemVer constraint evaluation displaying color-coded status badges:
  - `✔ pragma match` (green): Active compiler satisfies the file's `pragma language_version` constraint.
  - `⚠ version mismatch` (amber): Active compiler violates the file's pragma constraint.
- **1-Click Contract Compilation**: "Compile Current Contract" button builds the active contract in the background, rendering build artifacts, compiler diagnostics, and console output in the IDE.
- **Integrated Version Manager**: "Download More..." modal to discover, install, or remove compiler versions.

### 📦 2. Multi-Version Isolated Compiler Management
Manage multiple compiler toolchains simultaneously without system conflicts:
- **Isolated Directory Layout**: Compilers are cleanly isolated under `~/.compact/versions/<version>/` (e.g. `~/.compact/versions/0.34.0/compact`).
- **Automated Binary Downloader**: Automatically fetches, verifies, and unpacks release archives for the host OS and architecture with background progress indicators.
- **Per-Project Version Persistence**: Project-specific compiler choices are saved in `.idea/midnight.xml` (`MidnightProjectSettings`), allowing different repositories to target different Compact versions simultaneously.
- **Hierarchical Resolution Chain**:
  1. Per-project configured version (`.idea/midnight.xml`).
  2. Global IDE setting (Preferences → Languages & Frameworks → Midnight Compact).
  3. Auto-detected system binary (`PATH` or Windows Subsystem for Linux / WSL).
- **SemVer Constraint Engine**: Evaluates compound pragma clauses (`||`, `&&`), version ranges (`>=`, `>`, `<=`, `<`, `^`, `~`), bare version rules (e.g. `0.23` interpreted as `>= 0.23`), and two-digit normalization (`0.26` -> toolchain `0.34.0`).

### 📊 3. Status Bar Toolchain & Environment Monitor
A lightweight status bar widget (`CompactStatusBarWidget`) placed alongside native IDE controls:
- **Real-Time Display**: Shows the active Compact compiler and language version (e.g. `Compact: v0.34.0 (0.26.0)`).
- **Non-Blocking $O(1)$ Execution**: Guarantees zero UI-thread freezes by operating strictly on in-memory state.
- **Native Speed-Search Popup**:
  - View installed compiler versions with checkmark indicators.
  - 1-click switcher between installed compilers.
  - Reset to auto-detected system or WSL toolchain.
  - Launch background compiler downloader.
  - Jump directly to the Remix-style Compact Compiler tool window.
  - Open plugin configuration settings (`ShowSettingsUtil`).

### 🎨 4. Lexer, Parser & PSI Architecture
- **Handwritten State-Machine Lexer** (`CompactLexer`): Developed in Java 25 for maximum throughput and zero-allocation token streaming.
- **Pratt Precedence-Climbing Parser** (`CompactParser`): Complete coverage of the official Compact language grammar (`Lparser` / `Lsrc`), supporting assignment, ternary, logical, equality, relational, type casts (`as`), arithmetic, unary, member access, call, and tuple index expressions.
- **Resilient Syntax Recovery**: Boundary synchronization prevents isolated syntax errors from corrupting highlighting, navigation, or completion in the remainder of the file.
- **Full Token Classification**:
  - *Keywords*: `contract`, `circuit`, `witness`, `ledger`, `disclose`, `pragma`, `import`, `export`, `struct`, `enum`, `module`, `implements`, `pure`, `return`, `if`, `else`, `for`, `const`, `default`, `new`, `of`, `pad`, `sealed`, `slice`, `assert`, `as`.
  - *Primitive & Built-in Types*: `Boolean`, `Field`, `Uint`, `Bytes`, `Opaque`, `Vector`, `Cell`, `Map`, `Set`, `Counter`, `JubjubScalar`, `Secp256k1Base`, `Secp256k1Scalar`.
  - *Literals*: Hexadecimal (`0x`), Binary (`0b`), Octal (`0o`), Decimal, Strings (with escape sequence parsing), and SemVer version literals.

### 🧭 5. Scoped Symbol Resolution & Navigation
- **Go to Declaration (`Ctrl+B` / `Cmd+B` / `F12`)**:
  - Local `const` and pattern destructuring variables (strictly lexical, forward-declaration aware).
  - Circuit, witness, and constructor parameters.
  - Struct, enum, and type alias definitions.
  - Struct fields (`record.field`) and enum variants (`Color.Red`).
  - Qualified module imports and aliased imports (`import { a as b } from M`).
- **Go to Type Declaration (`Ctrl+Shift+B` / `Cmd+Shift+B`)**:
  - Navigate directly from any variable, parameter, expression, or ledger field to its underlying struct, enum, or type alias definition.
  - Unwraps generic type containers (e.g. `Cell<T>`, `Vector<T, N>`) down to nominal declarations.
- **Cross-File Resolution & Includes**:
  - Resolves `include "file.compact";` relative to the current file, directory, or project roots.
  - Recursion guard prevents infinite loops on circular file inclusions.
- **Bundled Standard Library & ZKIR Indexing**:
  - Bundles official `standard-library.compact` and `zkir-v3-library.compact` directly inside the plugin.
  - Pre-indexed on startup via `CompactStdlibService`, providing out-of-the-box navigation and completion for standard library circuits, types, and cryptographic functions.
- **Bidirectional Interface & Implementation Gutter Markers**:
  - Line markers in the editor gutter allow jumping seamlessly between interface circuit declarations and contract implementations.

### 💡 6. Code Insight, Completion & Documentation
- **Context-Aware Completion (`Ctrl+Space`)**:
  - **Keywords**: Filtered by declaration vs. statement context.
  - **Types**: Built-in primitives and user-defined structs/enums/aliases at type positions.
  - **Values**: In-scope variables, consts, circuit names, and parameters at expression positions.
  - **Members**: Qualified member suggestions after typing `Enum.` or `structInstance.`.
- **Parameter Info & Signature Help (`Ctrl+P` / `Cmd+P`)**:
  - In-editor tooltip showing parameter names and types during function, circuit, and constructor invocations.
  - Dynamically highlights active parameter based on caret location in argument lists.
- **Declarative Inlay Parameter Hints**:
  - Displays inline parameter name hints for circuit and constructor calls.
- **Quick Documentation (`Ctrl+Q` / `F1`)**:
  - Hover tooltips with inferred signatures, doc comments (`///` and `/** */`), struct field listings, and enum variant details formatted in HTML.
- **Structure View & Breadcrumbs (`Alt+7` / `Cmd+7`)**:
  - Visual hierarchical outline of contracts, modules, circuits, witnesses, structs, and enums.
  - Interactive editor breadcrumbs for rapid orientation in deeply nested contracts.

### 🛡️ 7. Semantic Inspections & Quick-Fixes
The plugin includes 10 dedicated static inspections with real-time error detection and quick-fixes (`Alt+Enter` / `Option+Enter`):

| Inspection | Severity | Description | Quick-Fix |
| :--- | :--- | :--- | :--- |
| **Unresolved Reference** | `Warning` | Flags unknown variables, circuits, enum members, and struct fields | — |
| **Duplicate Declaration** | `Warning` | Flags duplicate declarations in the same scope (aware of `VALUE` vs `TYPE` namespaces) | — |
| **Unused Local Variable** | `Warning` | Identifies local `const` bindings that have zero usages in their scope | **Delete unused variable** |
| **Type Mismatch** | `Error` | Validates boolean conditionals, negation (`!`), and operator compatibility | — |
| **Pure Circuit Constraint** | `Warning` | Flags ledger mutations or side effects inside circuits declared as `pure` | — |
| **Sealed Field Mutation** | `Warning` | Flags reassignments to ledger fields declared with the `sealed` modifier | — |
| **Recursive Circuit** | `Warning` | Detects direct or mutual circuit recursion violating ZK circuit limits | — |
| **Constructor Restriction** | `Warning` | Validates constructor rules, ledger state initialization, and initializers | — |
| **Undisclosed Witness** | `Warning` | Flags witness values assigned to ledger state without `disclose(...)` | — |
| **Pragma Version Mismatch** | `Error` | Flags contract pragma constraints incompatible with active compiler | **Switch compiler** / **Download compiler** / **Update pragma** |

### 🔍 8. Refactoring & Symbol Search
- **Safe Rename (`Shift+F6`)**: Inplace renaming for circuits, variables, parameters, struct fields, and types. Validates proposed names (`CompactNamesValidator`) to prevent syntax errors or keyword collisions.
- **Find Usages (`Alt+F7`)**: Project-wide usage search backed by a custom `WordsScanner`.

### 📐 9. Formatting & Editor Ergonomics
- **AST-Based Formatter (`Ctrl+Alt+L`)**: Automatically formats Compact source code with 2-space canonical indentation and operator spacing.
- **Smart Indent & Enter Handling**: Automatically indents new lines when pressing `Enter` inside blocks, contracts, or unclosed parentheses.
- **Paired Brace & Quote Matching**: Automatic insertion and balance verification for `{}`, `()`, `[]`, `<>`, `""`, and `''`.
- **Commenter**: Toggle line comments (`Ctrl+/`) and block comments (`Ctrl+Shift+/`).
- **Surround With (`Ctrl+Alt+T`)**: Surround statements or expressions with blocks or parentheses.
- **Spellchecking Strategy**: Compact-aware spellchecker skipping keywords and cryptographic primitives.
- **Live Templates**:
  - `cct`: Exported contract skeleton with circuit and ledger.
  - `ccti`: Contract implementation skeleton.
  - `mod`: Exported module definition.
  - `cir`: Exported circuit definition.
  - `wit`: Witness declaration.
  - `led`: Ledger block declaration.
  - `ledg`: Exported ledger field declaration.
  - `cons`: Contract constructor.
  - `str`: Struct definition.
  - `en`: Enum definition.
  - `ass`: `assert` condition statement.
  - `disc`: `disclose` witness expression.
  - `inc`: `include` relative file directive.
  - `imp`: `import` statement.
  - `type`: Type alias definition.
- **File Templates**: New file wizard supporting "Compact Contract", "Compact Module", "Compact Interface", and "Compact File".

### 🌐 10. External Annotator & Toolchain Runners
- **Background Diagnostic Pipeline**: Asynchronous background execution of the `compact` compiler with cancellation support on keystrokes, projecting compiler errors directly into the editor and Problems tool window.
- **Dedicated Run Configurations**: Run configurations for compiling contracts and circuits.
- **Gutter Run Icons (`▶`)**: Clickable gutter icons next to contract and circuit definitions.
- **Cross-Platform Auto-Discovery**: Automatic discovery for Linux, macOS (Intel & Apple Silicon), Windows native, and Windows Subsystem for Linux (WSL with automatic UNC path translation).

### 🛒 11. JetBrains Marketplace Project Recommendations
Configured with `dependencySupport` extension points. When opening projects with JavaScript or TypeScript manifests referencing:
- `@midnight-ntwrk/compact-runtime`
- `@midnight-ntwrk/compact-js`
- `@midnight-ntwrk/midnight-js-contracts`
- `@openzeppelin/compact-contracts`

IntelliJ Platform IDEs (including IntelliJ IDEA and WebStorm) will automatically recognize the project as a Midnight project and suggest installing this plugin.

---

## 💻 Compatible IDEs

The plugin is compatible with IntelliJ-based IDEs starting from version **2026.2** (Build `262+`):

| IDE | Edition | Status |
| :--- | :--- | :--- |
| **IntelliJ IDEA** | Community & Ultimate | **Full Support** |
| **WebStorm** | Commercial | **Full Support** (via dependency recommendation) |
| **CLion** | Commercial | **Full Support** |
| **GoLand** | Commercial | **Full Support** |
| **PyCharm** | Community & Professional | **Full Support** |
| **Rider** | Commercial | **Full Support** |
| **Android Studio** | Free | **Full Support** (version 2026.2+ base) |

---

## 🚀 Getting Started

### Installation
1. Open **Settings / Preferences** (`Ctrl+Alt+S` on Windows/Linux, `Cmd+,` on macOS).
2. Navigate to **Plugins** → **Marketplace**.
3. Search for **Midnight Compact Language**.
4. Click **Install** and restart the IDE if prompted.

*Alternatively, install from disk:*
Download the release `.zip` from [Releases](https://github.com/Vinay2080/midnight-plugin/releases), go to **Plugins** → ⚙️ → **Install Plugin from Disk...**, and select the archive.

### Toolchain Setup

#### Option A: Automatic Setup via Remix Tool Window (Recommended)
1. Open any `.compact` file.
2. Click the **Compact Compiler** tab on the right edge of the IDE.
3. If no compiler is found, click **Download More...** and choose your desired version (e.g. `0.34.0` for Compact `0.26`).
4. The plugin downloads, unpacks, and configures the compiler automatically.

#### Option B: Manual Toolchain Configuration
1. Open **Settings / Preferences** → **Languages & Frameworks** → **Midnight Compact**.
2. **Compact Compiler Path**:
   - **Linux / macOS**: Typically `/usr/local/bin/compact` or `~/.cargo/bin/compact`.
   - **Windows (WSL)**: Point to your WSL binary (e.g. `\\wsl$\Ubuntu\usr\bin\compact`).
   - **Windows (Native)**: Navigate to your installed `compact.exe` binary. *(Note: Do not select `C:\Windows\System32\compact.exe`, which is Windows' built-in NTFS compression tool).*
3. Click **Test / Validate** to verify communication, then click **Apply** and **OK**.

---

## ⌨️ Keyboard Shortcuts Reference

| Action | Windows / Linux | macOS |
| :--- | :--- | :--- |
| **Go to Declaration** | `Ctrl + B` / `F12` | `Cmd + B` |
| **Go to Type Declaration** | `Ctrl + Shift + B` | `Cmd + Shift + B` |
| **Basic Code Completion** | `Ctrl + Space` | `Cmd + Space` |
| **Parameter Info** | `Ctrl + P` | `Cmd + P` |
| **Quick Documentation** | `Ctrl + Q` | `F1` / `Ctrl + J` |
| **Show Context Actions / Quick-Fix** | `Alt + Enter` | `Option + Enter` |
| **Rename Element** | `Shift + F6` | `Shift + F6` |
| **Find Usages** | `Alt + F7` | `Option + F7` |
| **Reformat Code** | `Ctrl + Alt + L` | `Cmd + Option + L` |
| **Toggle Line Comment** | `Ctrl + /` | `Cmd + /` |
| **Toggle Block Comment** | `Ctrl + Shift + /` | `Cmd + Option + /` |
| **Surround With...** | `Ctrl + Alt + T` | `Cmd + Option + T` |
| **File Structure Popup** | `Ctrl + F12` | `Cmd + F12` |
| **Open Structure Tool Window** | `Alt + 7` | `Cmd + 7` |

---

## 🏗️ Project Architecture

```text
.
├── src/main/java/dev/verloren/midnight/
│   ├── CompactFileType.java               # File type registration & SVG icon binding
│   ├── annotator/                         # External annotator & compiler error diagnostics
│   ├── completion/                        # Context-aware completion contributor
│   ├── documentation/                     # Hover docs & Quick Documentation provider
│   ├── editor/                            # Commenter, brace matcher, quote handler, breadcrumbs, inlays
│   ├── findUsages/                        # Find Usages provider & custom WordsScanner
│   ├── formatter/                         # AST formatting model & code style settings
│   ├── highlighter/                       # Syntax highlighter factory & annotator
│   ├── ide/                               # File templates & live templates
│   ├── inspection/                        # 10 semantic inspections & quick-fixes
│   ├── intention/                         # In-editor pragma intention actions
│   ├── lexer/                             # Handwritten state-machine lexer & token types
│   ├── navigation/                        # Go to Declaration, Type Declaration, Class/Symbol
│   ├── parameterInfo/                     # Parameter Info & signature help tooltip handler
│   ├── parser/                            # Handwritten Pratt parser & element types
│   ├── psi/                               # Typed PSI hierarchy & element factory
│   ├── refactoring/                       # Rename validator & refactoring support
│   ├── reference/                         # PSI reference implementations
│   ├── resolve/                           # Scoped symbol resolution & include resolver
│   ├── run/                               # Run configurations & gutter line markers
│   ├── settings/                          # Application & project-level settings
│   ├── statusbar/                         # Status bar widget & speed-search popup
│   ├── stdlib/                            # Bundled standard library service
│   ├── structure/                         # Structure view model & tree elements
│   ├── toolwindow/                        # Remix-style Compact Compiler tool window
│   ├── type/                              # Type inference engine & primitives
│   └── version/                           # Multi-version compiler manager & SemVer engine
├── src/main/resources/
│   ├── META-INF/plugin.xml                # Plugin manifest & extension points
│   ├── icons/                             # Official Midnight SVG icons
│   ├── inspectionDescriptions/            # Inspection HTML documentation
│   ├── liveTemplates/                     # XML live template definitions
│   ├── messages/                          # Localized message bundles
│   └── stdlib/                            # Bundled standard-library.compact & zkir-v3
├── ARCHITECTURE.md                        # In-depth subsystem architecture guide
├── DEVELOPER_GUIDE.md                     # Developer onboarding & implementation manual
├── CHANGELOG.md                           # Version release history
└── build.gradle.kts                       # Gradle build & IntelliJ Platform configuration
```

---

## 🧪 Building & Testing

### Prerequisites
- **JDK 25** (Configured via Gradle Toolchain)
- **IntelliJ IDEA 2026.2+** (Build `262+`)

### Build Commands

```bash
# Launch a sandbox IDE with the Midnight plugin pre-installed
./gradlew runIde

# Run all 444+ automated unit & integration tests
./gradlew test

# Run a specific test class
./gradlew test --tests "dev.verloren.midnight.resolve.CompactResolveTest"

# Build the distributable plugin ZIP package (in build/distributions/)
./gradlew buildPlugin
```

### Testing Standard
The test suite consists of **444 automated tests across 52 test classes**, adhering to a 5-tier testing standard:
- **Tier 1 (Golden Tree Conformance)**: Full AST assertions verifying PSI trees against golden reference files.
- **Tier 2 (Partial Parsing & Error Recovery)**: Broken syntax recovery assertions ensuring uninterrupted editing.
- **Tier 3 (Precedence & Associativity Matrix)**: Exhaustive operator precedence verification.
- **Tier 4 (Stress & Stack Resilience)**: Deep nesting tests verifying recursion safety without stack overflows.
- **Tier 5 (Zero-Latency Concurrency)**: Guarded non-blocking read actions and cancellation responsiveness checks.

---

## 🗺️ Roadmap & Upcoming Goals

- [x] **Remix-Style Tool Window & Compiler Manager**: Interactive tool window with live contract tracking and multi-version downloads.
- [x] **Status Bar Monitor**: Real-time toolchain version indicator with quick switcher popup.
- [x] **Go to Type Declaration & Parameter Info**: Direct type jumps and interactive argument signature help.
- [x] **Standard Library & ZKIR Indexing**: Bundled, auto-indexed standard library and ZKIR primitives.
- [x] **Cross-File Resolution & Include Processing**: Multi-file resolution for `include` directives and module imports.
- [x] **AST Code Formatter**: 2-space canonical indentation and operator spacing.
- [x] **10 Static Inspections & Quick-Fixes**: Comprehensive semantic checks for ZK contracts.
- [ ] **Smart Enter & In-Editor Intentions**: Automated statement completion and enhanced quick-fixes (`CompactSmartEnterProcessor`).
- [ ] **ZKIR Disassembler & Bytecode Viewer**: Visual inspector for compiled zero-knowledge intermediate representation artifacts.
- [ ] **Test Runner Integration**: Direct test execution and gutter runners for Compact contract test suites.

---

## 🤝 Contributing

Contributions are welcome! If you encounter any bugs, have feature suggestions, or want to contribute improvements:
1. Check the [Issue Tracker](https://github.com/Vinay2080/midnight-plugin/issues) for open tasks or report a new issue.
2. Review [`ARCHITECTURE.md`](ARCHITECTURE.md) and [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) for technical guidelines.
3. Fork the repository, create your feature branch, ensure all tests pass (`./gradlew test`), and submit a Pull Request.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
