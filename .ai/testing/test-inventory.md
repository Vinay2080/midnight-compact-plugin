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

To quickly verify a patch without running the entire test suite:
```powershell
# Verify a single test class
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern dev.verloren.midnight.CompactBundleTest

# Verify all parser and lexer changes
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "dev.verloren.midnight.parser.*"

# Full suite pre-merge verification
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -AllTests
```

---

## 4. Static Analysis & Qodana SARIF Integration

Qodana static code inspection is configured via [`qodana.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/qodana.yaml) targeting `projectJDK: "25"` with `profile: qodana.starter`.

### SARIF Ingestion Protocol
When Qodana analysis runs (locally via Qodana CLI or in periodic static analysis workflows):
1. **SARIF Output**: Results are written to `build/reports/qodana/qodana.sarif.json`.
2. **Knowledge Mapping**:
   - High-severity security issues (taint analysis, path traversal) $\to$ Escalate directly to [`.ai/security/threat-boundaries.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/security/threat-boundaries.md) or [`.ai/security/vulnerability-rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/security/vulnerability-rules.md).
   - Deprecated IntelliJ platform API usages $\to$ Log into [`.ai/meta/deprecation-register.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/meta/deprecation-register.md).
   - Threading / `ReadAction` warnings $\to$ Add to [`.agents/rules/threading.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/threading.rules.md).
3. **Single Authoritative Record**: Do not duplicate individual SARIF findings across multiple `.ai/` files. Update only the single authoritative file as defined by the mapping above.
