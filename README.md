# Midnight Language Plugin for IntelliJ IDEA

[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-Plugin-blue.svg)](https://plugins.jetbrains.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

An open-source JetBrains IntelliJ IDEA plugin providing first-class development support for **Compact**, the smart contract programming language of the **Midnight** privacy-preserving blockchain.

---

## 💡 What Problem Are We Solving?

### The Midnight & Compact Context
[Midnight](https://midnight.network) is a privacy-focused blockchain built around Zero-Knowledge (ZK) cryptography. Smart contracts on Midnight are written in **Compact**, a domain-specific language engineered specifically for ZK smart contract development. Compact allows developers to express private state logic (`witness`, `disclose`), public ledger state (`ledger`), zero-knowledge circuits (`circuit`), and cryptographic types (`Field`, `Uint`, `Bytes`) seamlessly.

### The Developer Experience Gap
Writing Compact smart contracts requires navigating complex ZK privacy primitives, strict syntax models, and custom type constructs. Without dedicated IDE support, developers face major hurdles:
- **Lack of Feedback**: Syntax errors, unclosed blocks, and missing type declarations are only caught during compiler execution.
- **Difficult Navigation**: Tracking variable bindings, circuit parameters, struct fields, and imported modules in complex contracts requires manual text searching.
- **Risky Refactoring**: Renaming circuits, state variables, or struct types across a contract is manual and error-prone.
- **Zero Discoverability**: Discovering keywords, primitive types, and enum members requires constantly referencing external documentation.

### Our Solution
The **Midnight Language Plugin** transforms IntelliJ IDEA into a full-featured IDE for Compact development. Built with a custom handwritten lexer, recursive-descent parser, Abstract Syntax Tree (PSI) model, and type inference engine, the plugin delivers intelligent editor features including syntax highlighting, symbol resolution, safe renaming, usage searching, and contextual code completion.

---

## ✨ Current Capabilities & Implemented Features

### 🎨 1. Lexer & Syntax Highlighting
- **Handwritten Lexer**: Custom state-machine lexer (`CompactLexer`) tailored for Compact syntax tokenization.
- **Full Token & Keyword Highlighting**:
  - Declaration & Control Keywords: `contract`, `circuit`, `witness`, `ledger`, `disclose`, `pragma`, `import`, `export`, `struct`, `enum`, `module`, `implements`, `pure`, `return`, `if`, `else`, `for`, `const`, `default`, `new`, `of`, `pad`, `sealed`, `slice`, `assert`, `as`.
  - Primitive Types: `Boolean`, `Field`, `Uint`, `Bytes`, `Opaque`, `Vector`, `JubjubScalar`, `Secp256k1Base`, `Secp256k1Scalar`.
  - Literals: Decimal, Hexadecimal (`0x`), Binary (`0b`), Octal (`0o`), strings, version strings, operators, and single/multi-line comments.

### 🧩 2. Handwritten Parser & PSI AST Architecture
- **Official Spec Alignment**: Built against official Compact compiler specifications (`Lparser` / `Lsrc`).
- **Complete Language Structure Parsing**: Parses top-level contracts, circuit definitions, module blocks, witness declarations, ledger constructors, type aliases, and control structures.
- **Pratt Expression Parser**: Precedence climbing for binary/unary operations, function calls, tuple expressions, member accesses, and ternary operations.
- **Resilient Error Recovery**: Smart error bounds prevent single-line syntax errors from corrupting highlighting or navigation in the rest of the file.

### 🔍 3. Symbol Navigation & Resolution (Go to Declaration)
- **Scoped Reference Resolution**: Jump directly to declarations (`Ctrl+Click` / `Cmd+Click` or `F12`):
  - Local variables, const bindings, parameters, and pattern destructuring.
  - Circuit and function definitions.
  - Struct, enum, and type alias declarations.
  - Struct field accesses (`object.field`) and enum member references (`Enum.Member`).
  - Module import bindings and import aliases (`import { a as b } from M`).

### 💡 4. Contextual Code Completion
- **Intelligent Autocomplete**: Context-aware completion suggestions via `Ctrl+Space`:
  - **Keywords**: Suggested at statement and declaration starting positions.
  - **Types**: Built-in primitives and user-defined types (structs, enums, type aliases) in type positions.
  - **Values**: In-scope variables, consts, circuit names, and parameters in expression positions.
  - **Enum Members**: Qualified member suggestions after typing `EnumName.`.

### ✏️ 5. Refactoring & Symbol Search
- **Safe Rename (`Shift+F6`)**: Rename circuits, variables, parameters, struct fields, and types. Automatically updates all reference sites with identifier validation to prevent invalid names or keyword collisions.
- **Find Usages (`Alt+F7`)**: Instantly search for all usages of any named element across the file using a custom word scanner.

### 🧮 6. Type Inference Engine
- **PSI Type System**: Dynamic type evaluation (`getType()`) for expressions, primitive literals, reference declarations, binary/unary operators, and struct field accesses.
- Powers downstream features such as field reference resolution and type-aware completion.

### 🛡️ 7. Semantic Inspections & Quick-Fixes
- **Unresolved Reference Inspection**: Live error detection for unresolved variables, constants, circuits, enum members, and struct fields.
- **Duplicate Declaration Inspection**: Real-time detection for duplicate variables, parameters, circuits, structs, enums, or type aliases within the same scope container, with full awareness of separate type vs. value namespaces.
- **Unused Local Variable Inspection & Quick-Fix**: Detects unused local `const` bindings within callable bodies (`circuit`, `witness`, `constructor`) with a one-click Quick-Fix (`Alt+Enter`) to safely delete the unused variable statement.
- **Type Mismatch Inspection**: Type compatibility validation for logical (`&&`, `||`), negation (`!`), and equality (`==`, `!=`) expressions.

### 🧪 8. Automated Test Suite
- **138/138 Automated Integration Tests Passing**: Verified coverage for lexer/parser edge cases, string escapes, numeric boundaries, reserved keywords, syntax error recovery, parameter scope shadowing, destructured pattern bindings, type alias resolution, generic parameter completion, enum/enum-member renaming, struct field & enum member find usages, validator identifier rules, cast expression typing, string literal typing, nested arithmetic & logical binary typing, struct literal type inference, unknown reference type fallbacks, and all semantic inspections with quick-fix verification.

---

## 🗺️ Roadmap & Future Goals

- [x] **Semantic Inspections & Quick-Fixes**: Inspections for unresolved references, duplicate declarations, unused local variables, and type mismatches.
- [ ] **Code Formatter & Indenter**: AST-based code formatting and automatic re-indentation (`Ctrl+Alt+L`) adhering to official Compact formatting standards.
- [ ] **Cross-File Resolution & Include Processing**: Workspace-wide resolution for `include` directives, multi-file module imports, and external contract definitions.
- [ ] **Standard Library & ZKIR Indexing**: Pre-indexed resolution and auto-completion for `standard-library.compact` and built-in ZKIR v3 functions.
- [ ] **Midnight Compiler Integration**: Embedded Midnight compiler execution with inline diagnostic overlays, compiler reporting, and circuit artifact verification.

---

## 🛠️ Project Structure

```text
.
├── src/main/java/dev/verloren/midnight/
│   ├── CompactFileType.java              # File type definition (.compact)
│   ├── lexer/                            # Handwritten Compact lexer & token definitions
│   ├── parser/                           # Handwritten recursive-descent parser & ParserDefinition
│   ├── psi/                              # PSI elements, interfaces, and resolve utilities
│   ├── highlighter/                      # Syntax highlighting implementation
│   ├── completion/                       # Code completion contributor
│   ├── refactoring/                      # Rename validator & refactoring support
│   ├── findUsages/                       # Find Usages provider & word scanner
│   ├── type/                             # Type inference engine & primitive types
│   ├── reference/                        # PSI reference resolution implementations
│   └── inspection/                       # Semantic inspections & quick-fixes
├── src/main/resources/
│   ├── META-INF/plugin.xml               # Plugin manifest
│   └── inspectionDescriptions/           # Inspection documentation descriptions
├── build.gradle.kts                      # Gradle build configuration
└── AGENTS.md                             # Architecture notebook & developer log
```

---

## 🚀 Development & Building

### Requirements
- **JDK 17** or higher
- **IntelliJ IDEA** (2023.2+ recommended)

### Build & Test Commands

- **Run Plugin Sandbox**:
  ```bash
  ./gradlew runIde
  ```

- **Run Automated Tests**:
  ```bash
  ./gradlew test
  ```

- **Build Plugin Distribution Package**:
  ```bash
  ./gradlew buildPlugin
  ```

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
