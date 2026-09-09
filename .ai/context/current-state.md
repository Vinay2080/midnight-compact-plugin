# Current State

Last Updated: September 2026 (v1.2.4-dev / Phase 28 Complete)

---

## 1. Implementation Snapshot

### Implemented Subsystems
- **Lexer & Parser**: Handwritten in Java 25. Complete coverage of Compact grammar, declarations, ledger types, type expressions, statements, expressions, and error recovery.
- **PSI Infrastructure**: Element hierarchy (`CompactElement`, `CompactNamedElement`, declaration types, reference types, type nodes).
- **Name Resolution & Reference Contributor**: Lexical scoping, namespace separation (`VALUE` vs `TYPE`), multi-file resolution via `include` statements.
- **Code Completion**: Keyword completion, built-in type completion, scope-aware local variable and parameter completion, circuit and ledger field completion.
- **Semantic Inspections**:
  - Unresolved references (`CompactUnresolvedReferenceInspection`)
  - Duplicate declarations (`CompactDuplicateDeclarationInspection`)
  - Unused local variables (`CompactUnusedLocalVariableInspection`) with quick-fix
  - Type mismatch (`CompactTypeMismatchInspection`)
  - Pure circuit violation (`CompactPureCircuitInspection`)
  - Sealed ledger mutation (`CompactSealedFieldMutationInspection`)
  - Recursive circuit detection (`CompactRecursiveCircuitInspection`)
  - Constructor restrictions (`CompactConstructorRestrictionInspection`)
  - Undisclosed witness usage (`CompactUndisclosedWitnessInspection`)
  - Pragma compiler version mismatch (`CompactPragmaVersionInspection`) with bare version `>=` interpretation and two-part semver normalization
- **Code Style & Formatting**: Spacing rules, smart indentation, brace matching, quote handling, and commenter.
- **IDE Features**: Structure view, syntax color settings page, breadcrumbs, code folding, hover documentation provider, live templates, surround with, and file templates.
- **Run Configurations & Toolchain Integration**: Compiler run configuration, gutter run line markers, toolchain discovery for Linux, macOS, Windows native, and WSL.
- **Line Markers**: Bidirectional navigation between interface declarations, circuits, and contract implementations.
- **Compiler Version Management**:
  - Isolated multi-version directory structure under `~/.compact/versions/<version>/`.
  - Automated binary download and installation with progress indicators and platform archive unpacking.
  - Per-project compiler version persistence in `.idea/midnight.xml` (`MidnightProjectSettings`).
  - Active version resolution chain: per-project setting -> global settings -> system PATH / WSL.
  - Two-digit SemVer parsing (`0.23` -> `0.23.0` -> toolchain `0.31.1`, `0.26` -> `0.26.0` -> toolchain `0.34.0`).
- **Remix-Style Tool Window**:
  - Right-hand stripe tool window ("Compact Compiler") with SVG branding icon.
  - Interactive compiler version switcher dropdown.
  - Real-time pragma version compatibility indicator with color-coded status badges and dynamic file tracking.
  - Live updates via `CompactCompilerEventListener`, `FileEditorManagerListener`, and `DocumentListener`.
  - "Download More..." compiler version management dialog.
  - "Compile Current Contract" action triggering background compilation and problem reporting.
- **Phase 27: Status Bar Toolchain & Environment Widget (v1.2.2)**:
  - `CompactStatusBarWidgetFactory`: Registered in `plugin.xml` on editor status bar.
  - `CompactStatusBarWidget`: Lightweight, non-blocking widget displaying active Compact version with language version mapping.
  - `CompactStatusBarPopup`: Native speed-search popup menu to switch installed compiler versions, download new versions, jump to the Remix Compiler panel, or open Midnight settings.
- **Phase 28: Smart Enter, Doc Comments & Intentions Suite (v1.2.4)**:
  - `CompactSmartEnterProcessor`: Non-destructive `Ctrl+Shift+Enter` completion for circuits (`: Void` or preserve `:`), `const` declarations (requiring `=` before `;`), and parameterized types (`Bytes<>`, `Uint<>`).
  - `CompactCommenter` & `CompactDocCommentEnterHandler`: Clean `Enter` handling in `/* ... */` and `/** ... */` without duplicate asterisks or trailing `*/` syntax errors.
  - Full Intention Actions Suite (`Alt+Enter`):
    - `CompactTogglePureCircuitIntention`
    - `CompactToggleExportIntention`
    - `CompactSurroundWithDiscloseIntention`
    - `CompactInvertIfIntention`
    - `CompactSpecifyTypeExplicitlyIntention`
    - `CompactRemoveRedundantTypeIntention`
- **Architectural Decision Records (ADRs)**:
  - Fully maintained index in `.ai/decisions/README.md` covering all 25 major architectural subsystems (**ADR-001 through ADR-025**) with 100% coverage across all registered `plugin.xml` extension points, strict upstream compiler references, workspace reference plugin benchmarks, and anti-hardcoding evaluation.
- **Total Unit Test Count**: **472 passing tests** across 55 test classes with 0 failures and 0 warnings (`BUILD SUCCESSFUL`).

### Roadmap & Evolution (Phases 29–35)
- **Phase 29: New Project Wizard & Compact Templates**
- **Phase 30: Stub Indexing & Large Workspace Caching**
- **Phase 31: Advanced Refactorings (Rename, Extract Variable, Change Signature)**
- **Phase 32: Remix Blockchain Explorer & Local Node Sandbox**
- **Phase 33: Ledger Storage & ZK Constraint Profiler**
- **Phase 34: In-IDE Language Server / PSI-MCP Bridge**
- **Phase 35: Polyglot Compact/TypeScript Integration & Test Framework**
