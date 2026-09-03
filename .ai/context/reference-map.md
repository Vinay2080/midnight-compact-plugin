# Reference Map

This guide maps all external and reference materials present in the repository.

> **CRITICAL RULE**: Reference repositories are sources of **language semantics** and **IntelliJ architecture patterns**, NOT templates to blindly copy or bulk-load into the AI context window.
>
> Workflow:
> **Question → Reference Map → Targeted 1–3 Files → Extract Behavior / API → Implement in Plugin Architecture**

---

## 1. Compact Reference Implementation (`compact/` and `references/`)

### 1.1 Grammar & Parser Syntax
- **Locations**:
  - `compact/compiler/parser.ss` / `references/parser.ss`: Authoritative Chez Scheme recursive grammar.
  - `compact/doc/compact-grammar.mdx` / `references/compact-grammar.mdx`: EBNF specification of Compact grammar.
  - `compact/compiler/lexer.ss` / `references/lexer.ss`: Token categorization, strings, escapes, numeric representations.
  - `compact/compiler/export-keywords.ss`: Complete keyword and reserved keyword lists.
- **Use when**:
  - Verifying operator precedence and associativity.
  - Checking syntactic validity of new language constructs.
  - Clarifying tokenization rules (e.g. number bases, comment formats).

### 1.2 Type System & Semantic Analysis
- **Locations**:
  - `compact/compiler/infer-types.ss` / `references/infer-types.ss`: Type inference rules and typing judgments.
  - `compact/compiler/check-types-Lnodca.ss` / `references/check-types-Lnodca.ss`: Type checker and static analysis.
  - `compact/compiler/check-sealed-fields.ss` / `references/check-sealed-fields.ss`: Ledger field mutation and access rules.
  - `references/lsrc-typing.md`: Formal type rules document.
- **Use when**:
  - Determining type compatibility for operators, calls, and assignments.
  - Clarifying nominal vs structural type equivalence.
  - Implementing type inference extensions or type inspections.

### 1.3 Modules, Imports & Scoping
- **Locations**:
  - `compact/compiler/expand-modules-and-types.ss` / `references/expand-modules-and-types.ss`: Module expansion, alias resolution, export flattening.
  - `references/lsrc.md`: Canonical AST schema descriptions and identifier binding rules.
  - `references/renaming.agda`: Formal Agda specification for symbol renaming and alpha-equivalence.
- **Use when**:
  - Implementing cross-file imports (`import { ... } from ...`, `import ... prefix ...`).
  - Handling file inclusion (`include "..."`).
  - Designing StubIndex or global symbol table for multi-file projects.

### 1.4 Standard Library & Contracts
- **Locations**:
  - `compact/compiler/standard-library.compact`: Built-in Compact standard library.
  - `compact/compiler/zkir-v3-library.compact`: ZKIR primitive declarations.
  - `compact/examples/*.compact`: Canonical contract examples (`counter.compact`, `zerocash.compact`, etc.).
  - `compact/examples/errors/*.compact`: Malformed contract test cases for error diagnostic verification.
- **Use when**:
  - Indexing built-in standard library types and functions.
  - Creating realistic unit and integration test fixtures for parser and formatter.

---

## 2. IntelliJ Platform Architecture References (`intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`)

### 2.1 IntelliJ Rust (`intellij-rust/`)
- **Location**: `intellij-rust/src/main/kotlin/org/rust/`
- **Use when**:
  - **Stub Indexing & Multi-file Resolution**: `lang/core/stubs/`, `lang/core/resolve/` (high-performance IntelliJ index-based resolution).
  - **Type Inference Architecture**: `lang/core/types/infer/` (advanced typing pipelines in IntelliJ).
  - **Formatter & Code Style**: `ide/formatter/` (spacing builders, block hierarchies, wrap rules).
  - **Inspections & Quick Fixes**: `ide/inspections/` (best practices for `LocalInspectionTool` and `LocalQuickFix`).
  - **Structure View**: `ide/structure/` (`StructureViewModel`, `StructureViewTreeElement`).

### 2.2 IntelliJ Elixir (`intellij-elixir/`)
- **Location**: `intellij-elixir/src/org/elixir_lang/`
- **Use when**:
  - **Handwritten Lexer/Parser Integration**: `lexer/`, `parser/` (techniques for integrating custom non-GrammarKit parsers with IntelliJ PSI).
  - **Documentation Provider**: `templates/`, `reference/` (hover documentation rendering).
  - **Names Validator**: `refactoring/quote/NamesValidator.java`.

### 2.3 IntelliJ Scala (`intellij-scala/`)
- **Location**: `intellij-scala/scala/`
- **Sub-modules**:
  - `scala-impl/src/org/jetbrains/plugins/scala/`: Core language plugin implementation.
  - `compile-server/`, `compiler-integration/`, `compiler-jps/`: External compiler process integration.
  - `repl/`, `worksheet/`: Interactive console and worksheet scratchpad execution.
  - `structure-view/`: Structure view outline provider.
  - `debugger/`: Debugger configuration, breakpoints, and frame presentation.
- **Use when**:
  - **External Compiler Integration & Build Server**: `scala/compile-server/`, `scala/compiler-integration/`, `scala-impl/.../compiler/` (managing long-running external compiler daemons, background compilation pipelines, process exit monitoring, and process filters).
  - **Interactive REPL & Worksheet Execution**: `scala/repl/`, `scala/worksheet/`, `scala-impl/.../console/` (interactive scratchpad and evaluation loop with output folding and line execution).
  - **Type System & Recursive Resolution**: `scala-impl/.../lang/resolve/`, `scala-impl/.../lang/psi/types/` (advanced typing models, recursive type resolution, type bounds, and presentation).
  - **Auto-Import & Symbol Indexing**: `scala-impl/.../autoImport/`, `scala-impl/.../caches/` (smart import optimizers and PSI cache invalidation).
  - **Refactoring & Conversion**: `scala-impl/.../lang/refactoring/`, `scala-impl/.../conversion/` (complex AST refactorings and code transformations).

### 2.4 IntelliJ R (`Rplugin/`)
- **Location**: `Rplugin/`
- **Sub-modules**:
  - `psi/src/com/intellij/r/psi/`: PSI definitions, element generator, interpreter discovery, and execution profiles.
  - `src/org/jetbrains/r/`: UI actions, console, documentation, inspections, project generator, visualization.
  - `psi/grammars/`: `r.bnf`, `Roxygen.bnf`, and lexer skeletons.
- **Use when**:
  - **Interpreter & Toolchain Discovery**: `psi/.../interpreter/`, `src/.../interpreter/` (automatic detection of system, WSL, conda, and custom environment CLI binaries and paths).
  - **Run Configurations & Process Runners**: `psi/.../run/`, `src/.../run/` (command-line building, script runners, environment variable propagation, and console output parsers).
  - **Project Generators & DApp Wizards**: `src/.../projectGenerator/` (`DirectoryProjectGenerator`, `ProjectTemplatesFactory`, step UI for project scaffolds).
  - **Interactive Tool Windows & Visualizers**: `src/.../visualization/`, `src/.../console/` (custom tool windows, data visualizers, graphics window, interactive REPL console).
  - **Doc Comments & Help Rendering**: `psi/.../roxygen/`, `src/.../documentation/` (structured doc comment parsing, markdown doc rendering, and parameter tables).
  - **Programmatic PSI Generation**: `psi/.../RElementGenerator.java` (generating synthetic AST nodes via string snippets).

---

## 3. Local Blockchain Testnet Reference (`../midnight-local-dev/`)

- **Location**: `../midnight-local-dev/` (sibling directory)
- **Key Resources**:
  - `standalone.yml`: Docker Compose stack definition running local Midnight proof server, node, and indexer.
  - `accounts.json` / `accounts.example.json`: Pre-funded testnet dev accounts and seed configurations.
  - `private-identity-wallet/contracts/compile.ps1`: Reference compilation and deployment workflow for Midnight smart contracts.
- **Default Endpoints**:
  - Node RPC: `http://localhost:9944`
  - Proof Server: `http://localhost:6300`
  - Indexer: `http://localhost:8088`
- **Use when**:
  - Designing Level 5 Midnight Explorer Tool Window (connecting to localnet / testnet nodes).
  - Verifying contract compilation artifacts against real local proof-server and node requirements.
  - Testing run configurations against active local testnet services.

---

## 4. Targeted Retrieval Guide for AI Agents

| Task | Target Reference File(s) | Do NOT Load |
| :--- | :--- | :--- |
| **New expression parsing** | `compact/compiler/parser.ss` (around `(define-grammar ...)`) | Entire `compact/` tree |
| **Type compatibility** | `references/infer-types.ss` | Entire Scheme compiler |
| **Stdlib builtins** | `compact/compiler/standard-library.compact` | Test suites or build scripts |
| **IntelliJ Stub Indexing** | `intellij-rust/.../stubs/` | Rust compiler or unrelated plugin files |
| **IntelliJ Doc Provider** | `intellij-rust/.../ide/docs/` or `Rplugin/.../documentation/` | Entire `intellij-rust` / `Rplugin` repo |
| **Toolchain & WSL Discovery** | `Rplugin/psi/.../interpreter/` or `CompactToolchainUtil.java` | Entire R plugin tree |
| **Compiler Daemon / Runner** | `intellij-scala/scala/compile-server/` or `scala-impl/.../compiler/` | Entire Scala plugin tree |
| **Project Wizard / DApp Scaffold**| `Rplugin/.../projectGenerator/` | Unrelated R packages or UI code |
| **Interactive Console / REPL**| `intellij-scala/scala/repl/` or `Rplugin/.../console/` | Entire repl module |
| **Local Testnet & RPC Endpoints**| `../midnight-local-dev/standalone.yml` | `node_modules` or Docker layers |
