# Current State

Last Updated: September 2026 (v1.2.6-dev / Comprehensive Export Declarations, Type Completion & Live Template Macros)

---

## 1. Implementation Snapshot

### Implemented Subsystems
- **Lexer & Parser**: Handwritten in Java 25. Complete coverage of Compact grammar, declarations, ledger types, type expressions, statements, expressions, and error recovery.
- **PSI Infrastructure**: Element hierarchy (`CompactElement`, `CompactNamedElement`, declaration types, reference types, type nodes).
- **Name Resolution & Reference Contributor**: Lexical scoping, namespace separation (`VALUE` vs `TYPE`), multi-file resolution via `include` statements.
- **Code Completion & Comprehensive Export System (v1.2.5+)**:
  - Contextual classification in `CompactCompletionContext`:
    - `Kind.AFTER_EXPORT`: Disallows invalid file headers (`pragma`, `import`, `include`, `export`) and provides all exportable constructs (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and selection export (`{`).
    - `Kind.AFTER_SEALED`: Suggests `ledger`.
    - `Kind.AFTER_PURE`: Suggests `circuit`.
    - `Kind.AFTER_NEW`: Suggests `type`.
    - `Kind.NONE` for comment and docstring contexts (`isComment`).
  - Top-level declaration completion offering both bare and exported variants with `CompactDeclarationInsertHandler` interactive live template scaffolding.
  - `CompactLedgerInsertHandler`: Interactive live template tab-stops (`$NAME$`, `$TYPE$`), automatic `export ` prefix injection, and non-destructive lookahead preservation.
  - **Comment Completion Suppression (v1.2.6)**:
    - Defense-in-depth comment filtering in `CompactCompletionContributor` via `.andNot(PlatformPatterns.psiComment())` and early return guard.
    - `prevNonCommentLeaf` leaf traversal in `CompactCompletionContext` to prevent AST comment trivia from masquerading as statement or declaration starts.
    - Live template context filtering in `CompactLiveTemplateContextType` preventing expansion of declaration triggers (e.g. `ledg`, `led`, `cir`) within comments.
  - Parametric live templates in `Compact.xml` with bundle descriptions: `led`, `ledg`, `ledger`, `expled`, `cir`, `wit`, `expw`, `str`, `expstr`, `en`, `expen`, `type`, `expt`, `const`, `cct`.
- **Live Template Type Completion & Macro System (Phase 30 / v1.2.6)**:
  - `CompactTypeExpression`: Live template `Expression` providing interactive dropdown suggestions for all built-in types (`State`, `Counter`, `Void`, `Bytes`, `Field`, `Uint`, etc.), in-scope project types, and imported types when navigating through declaration templates.
  - `CompactTypeMacro`: Registered under `<liveTemplateMacro>` as `compactType(...)` for live template XML definitions.
  - Hardened `CompactCompletionContext`: Accurate classification of declaration type positions (after colons in ledger declarations, struct fields, const bindings, and after `=` in type aliases), preventing `isExportPreceding` keyword hijacking.
- **Generalized Declaration Name Auto-Numbering & Scope Analysis (Phase 29 / v1.2.6)**:
  - `CompactDeclarationNameGenerator`: Universal lowest-positive-integer gap filling (`circuit1`, `circuit2`, `witness1`, etc.) with explicit user-provided name preservation.
  - `CompactDeclarationType`: Extensible registry for standard Compact declarations (`circuit`, `witness`, `struct`, `enum`, `module`, `contract`, `type`, `ledger`, `const`) and custom runtime type extensions (`registerCustomType`).
  - Pluggable Live Template Macros registered under `<liveTemplateMacro>`: `compactDeclarationName`, `circuitName`, `witnessName`.
  - `CompactDeclarationInsertHandler`: Completion insert handler with auto-numbered declaration naming.
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
    - `CompactToggleExportIntention` (supports circuits, contracts, structs, enums, modules, types, ledgers, witnesses, and `const` statements)
    - `CompactSurroundWithDiscloseIntention`
    - `CompactInvertIfIntention`
    - `CompactSpecifyTypeExplicitlyIntention`
    - `CompactRemoveRedundantTypeIntention`
- **Architectural Decision Records (ADRs)**:
  - Fully maintained index in `.ai/decisions/README.md` covering all 27 major architectural subsystems (**ADR-001 through ADR-027**) with 100% coverage across all registered `plugin.xml` extension points, strict upstream compiler references, workspace reference plugin benchmarks, and anti-hardcoding evaluation.
- **Total Unit Test Count**: **566 passing tests** across 57 test classes with 0 failures and 0 warnings (`BUILD SUCCESSFUL`).

### Roadmap & Evolution (Phases 31–36)
- **Phase 31: Stub Indexing & Large Workspace Caching**
- **Phase 32: Advanced Refactorings (Rename, Extract Variable, Change Signature)**
- **Phase 33: Remix Blockchain Explorer & Local Node Sandbox**
- **Phase 34: Ledger Storage & ZK Constraint Profiler**
- **Phase 35: In-IDE Language Server / PSI-MCP Bridge**
- **Phase 36: Polyglot Compact/TypeScript Integration & Test Framework**
