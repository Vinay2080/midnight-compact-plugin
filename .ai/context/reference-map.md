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

## 2. IntelliJ Platform Architecture References (`intellij-rust/` & `intellij-elixir/`)

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

---

## 3. Targeted Retrieval Guide for AI Agents

| Task | Target Reference File(s) | Do NOT Load |
| :--- | :--- | :--- |
| **New expression parsing** | `compact/compiler/parser.ss` (around `(define-grammar ...)`) | Entire `compact/` tree |
| **Type compatibility** | `references/infer-types.ss` | Entire Scheme compiler |
| **Stdlib builtins** | `compact/compiler/standard-library.compact` | Test suites or build scripts |
| **IntelliJ Stub Indexing** | `intellij-rust/.../stubs/` | Rust compiler or unrelated plugin files |
| **IntelliJ Doc Provider** | `intellij-rust/.../ide/docs/` | Entire `intellij-rust` repo |
