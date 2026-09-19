# Test Suite Inventory & Execution Matrix

This document maps all automated test suites across the `dev.verloren.midnight` codebase, categorizing them by test tier, execution scope, and verification speed.

---

## 1. Test Execution Tiers

| Tier | Category | Target Packages | Execution Command | Avg Latency | Gate Level |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Tier 1** | **Lexer & Grammar Core** | `dev.verloren.midnight.lexer.*`<br>`dev.verloren.midnight.parser.*` | `.\gradlew.bat test --tests dev.verloren.midnight.parser.* --tests dev.verloren.midnight.lexer.*` | ~15s | Gate 2 (Pre-Commit) |
| **Tier 2** | **PSI & Resolution** | `dev.verloren.midnight.psi.*`<br>`dev.verloren.midnight.resolve.*`<br>`dev.verloren.midnight.type.*` | `.\gradlew.bat test --tests dev.verloren.midnight.resolve.* --tests dev.verloren.midnight.psi.*` | ~20s | Gate 2 (Pre-Commit) |
| **Tier 3** | **IDE Features & Editor** | `dev.verloren.midnight.completion.*`<br>`dev.verloren.midnight.formatter.*`<br>`dev.verloren.midnight.editor.*` | `.\gradlew.bat test --tests dev.verloren.midnight.completion.* --tests dev.verloren.midnight.formatter.*` | ~25s | Gate 3 (Pre-Push) |
| **Tier 4** | **Inspections & Annotators**| `dev.verloren.midnight.inspection.*`<br>`dev.verloren.midnight.annotator.*` | `.\gradlew.bat test --tests dev.verloren.midnight.inspection.* --tests dev.verloren.midnight.annotator.*` | ~30s | Gate 3 (Pre-Push) |
| **Tier 5** | **Toolchain & Integration** | `dev.verloren.midnight.run.*`<br>`dev.verloren.midnight.toolwindow.*` | `.\gradlew.bat test --tests dev.verloren.midnight.run.* --tests dev.verloren.midnight.toolwindow.*` | ~35s | Gate 4 (Full CI) |

---

## 2. Complete Test Suite Registry

### Core Language Subsystems
- **Lexer Tests:** `CompactLexerTest` — verifies tokenization against Compact language keywords, operators, comments, and string literals.
- **Parser Tests:** `CompactParserTest` — verifies AST structure and golden `.txt` tree conformance for all language declarations (circuits, witnesses, ledgers, contracts).
- **Resolver Tests:** `CompactResolveUtilTest`, `CompactMultiFileResolveTest` — asserts shadowing, cross-file includes, and VALUE vs TYPE namespace isolation.
- **Type Inference Tests:** `CompactTypeInferenceTest` — validates type checking for primitives, sized integers, structs, and return types.

### Editor & IDE Features
- **Completion Tests:** `CompactCompletionTest` — tests keyword, type, variable, and pragma completion in valid and suppressed scopes.
- **Formatter Tests:** `CompactFormatterTest` — validates indentation, spacing around binary operators, and brace alignment.
- **Inspection Tests:** `CompactInspectionsTest` — validates detection and quick-fixes for unused variables, missing types, and sealed ledger mutations.
- **Annotator Tests:** `CompactExternalAnnotatorTest` — tests 3-phase annotator pipeline, WSL path mapping, and diagnostic parsing.

### Toolchain & Runtime
- **Toolchain Discovery:** `CompactToolchainUtilTest` — validates discovery of `compact` compiler via WSL, filtering out Windows `compact.exe`.
- **Run Configurations:** `CompactRunConfigurationTest` — validates compiler arguments, working directory, and output console handling.

---

## 3. Fast Targeted Test Execution Guide

To quickly verify a patch without running the entire 500+ test suite:
```bash
# Verify a single test class
.\gradlew.bat test --tests dev.verloren.midnight.CompactBundleTest

# Verify all parser and lexer changes
.\gradlew.bat test --tests dev.verloren.midnight.parser.*

# Full suite pre-merge verification
.\gradlew.bat test
```
