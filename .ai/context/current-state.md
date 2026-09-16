# Current State

Last Updated: September 2026 (v1.3.2 / In-Memory Shadow Buffer Compilation, Caret Jump & Whitespace Stripping Fix, Universal Delimiter Skipping)

---

## 1. Implementation Snapshot

### Implemented Subsystems
- **Lexer & Parser**: Handwritten in Java 25. Complete coverage of Compact grammar, declarations, ledger types, type expressions, statements, expressions, and error recovery.
- **PSI Infrastructure**: Element hierarchy (`CompactElement`, `CompactNamedElement`, declaration types, reference types, type nodes).
- **Name Resolution & Reference Contributor**: Lexical scoping, namespace separation (`VALUE` vs `TYPE`), multi-file resolution via `include` statements.
- **In-Memory Shadow Buffer Compilation & Caret Jump Prevention (v1.3.2)**:
  - Eliminated forced document saves (`FileDocumentManager.saveDocument`) and EDT `invokeLater` dispatch from `CompactExternalAnnotator.collectInformation()`.
  - Captures unsaved editor text dynamically in `InitialInfo.unsavedContent()` on the read thread.
  - Spawns background external compiler execution against temporary shadow files preserving relative directory includes via `--compact-path`.
  - Eliminates editor caret jumps to column 0 and prevents premature stripping of trailing spaces or indentation whitespace during active typing.
- **Delimiter & Structural Symbol Skipping (v1.3.0 / ADR-030)**:
  - `CompactDelimiterTypedHandler`: `TypedHandlerDelegate` registered in `plugin.xml` intercepting typed closing delimiters and structural punctuation when the matching character is present immediately ahead of the caret.
  - Supported tokens: Closing delimiters (`)` `RPAREN`, `]` `RBRACKET`, `}` `RBRACE`, `>` `GT`), structural punctuation (`:` `COLON`, `;` `SEMICOLON`, `,` `COMMA`), and closing string quotes (`"` and `'` `STRING_LITERAL`).
  - Cleanly handles multi-character typing workflows (e.g. `witness localSk(<caret>): Bytes<32>;` -> typing `)` then `:` cleanly skips both to `witness localSk():<caret> Bytes<32>;` without duplicate tokens).
  - Context isolation: Suppresses skipping inside comments (`LINE_COMMENT`, `BLOCK_COMMENT`), non-quote symbols inside string literals, before string opening quotes, when active selection exists, and respects user platform preferences (`AUTOINSERT_PAIR_BRACKET`, `AUTOINSERT_PAIR_QUOTE`).
- **Angle Bracket Pairing, Overtyping & Backspace Deletion (v1.3.0 / ADR-029)**:
  - `CompactAngleBraceTypedHandler`: `TypedHandlerDelegate` registered in `plugin.xml` providing intelligent angle bracket auto-closing (`<|>`) after parameterized types (`Vector`, `Uint`, `Bytes`, `Opaque`, `Field`, `Boolean`), parameterized expressions (`default`, `slice`), type identifiers (`Map`, `Set`, `Cell`, `T`), and generic declaration headers (`circuit foo<`, `witness bar<`, `struct Box<`, `type Alias<`, `module Mod<`, `contract Cont<`).
  - Overtyping step-over: typing `>` immediately before an existing closing `>` advances the caret without inserting redundant angle brackets when balanced.
  - `CompactAngleBraceBackspaceHandler`: `BackspaceHandlerDelegate` automatically deleting the matching closing `>` when backspacing `<` in `<|>`.
  - Negative context suppression: prevents pairing after comparison operators (`<`), inside comments, inside string literals, and in the middle of identifiers.
- **Parameterized Type Completion & Sizing Options (v1.3.0 / ADR-029)**:
  - `CompactParameterizedTypeInsertHandler`: `InsertHandler<LookupElement>` automatically appending `<>`, placing the caret inside `<|>`, registering empty tab-out scope with `TabOutScopesTracker`, and scheduling auto-popup lookup for size options.
  - Built-in type sizing completions: `Uint` suggests `8`, `16`, `32`, `64`, `128`, `256`; `Bytes` suggests `32`; `Opaque` inserts `<\"\">`.
  - Concurrency & live template coordination: schedules caret repositioning via `ApplicationManager.getApplication().invokeLater(...)` when an active `TemplateState` is present, preventing premature live template completion from ejecting the caret.
- **Code Completion & Comprehensive Export System (v1.3.0 / ADR-019, ADR-026, ADR-028)**:
  - Contextual classification in `CompactCompletionContext`:
    - `Kind.AFTER_EXPORT`: Disallows invalid file headers (`pragma`, `import`, `include`, `export`) and provides all exportable constructs (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and selection export (`{`). Prohibits invalid top-level `export const` per upstream compiler specification (ADR-028).
    - `Kind.AFTER_SEALED`: Suggests `ledger`.
    - `Kind.AFTER_PURE`: Suggests `circuit`.
    - `Kind.AFTER_NEW`: Suggests `type`.
    - `Kind.NONE` for comment and docstring contexts (`isComment`).
  - Top-level declaration completion offering both bare and exported variants with `CompactDeclarationInsertHandler` interactive live template scaffolding.
  - `CompactLedgerInsertHandler`: Interactive live template tab-stops (`$NAME$`, `$TYPE$`), automatic `export ` prefix injection, and non-destructive lookahead preservation.
  - **Comment Completion Suppression (v1.3.0)**:
    - Defense-in-depth comment filtering in `CompactCompletionContributor` via `.andNot(PlatformPatterns.psiComment())` and early return guard.
    - `prevNonCommentLeaf` leaf traversal in `CompactCompletionContext` to prevent AST comment trivia from masquerading as statement or declaration starts.
    - Live template context filtering in `CompactLiveTemplateContextType` preventing expansion of declaration triggers (e.g. `ledg`, `led`, `cir`) within comments.
  - Parametric live templates in `Compact.xml` with bundle descriptions: `led`, `ledg`, `ledger`, `expled`, `cir`, `wit`, `expw`, `str`, `expstr`, `en`, `expen`, `type`, `expt`, `const`, `cct`.
- **Tool Window Responsiveness & Typing Stability (v1.3.1)**:
  - Optimized editor change notifications in `CompactCompilerPanel` to prevent redundant card re-renders and EDT queue stalls when typing.
  - Suppressed background compiler check alarms and index reads on rapid text typing events, preventing caret freezes and dropped keystrokes (e.g. initial keywords and spacebar).
  - Hardened live template write action boundary in `CompactParameterizedTypeInsertHandler` and `CompactAssertInsertHandler` to prevent template state corruptions and threading exceptions.
  - Modernized `CompactBundle` constructor using explicit `CompactBundle.class` reference to eliminate platform deprecation warnings.
- **Live Template Type Completion & Macro System (Phase 30 / v1.3.0 / ADR-027, ADR-029)**:
  - `CompactTypeExpression`: Live template `Expression` providing interactive dropdown suggestions for all built-in types (`State`, `Counter`, `Void`, `Bytes`, `Field`, `Uint`, etc.), in-scope project types, and imported types when navigating through declaration templates.
  - `CompactTypeMacro`: Registered under `<liveTemplateMacro>` as `compactType(...)` for live template XML definitions.
  - Hardened `CompactCompletionContext`: Accurate classification of declaration type positions (after colons in ledger declarations, struct fields, const bindings, and after `=` in type aliases), preventing `isExportPreceding` keyword hijacking.
- **Generalized Declaration Name Auto-Numbering & Scope Analysis (Phase 29 / v1.3.0 / ADR-027)**:
  - `CompactDeclarationNameGenerator`: Universal lowest-positive-integer gap filling (`circuit1`, `circuit2`, `witness1`, etc.) with explicit user-provided name preservation.
  - `CompactDeclarationType`: Extensible registry for standard Compact declarations (`circuit`, `witness`, `struct`, `enum`, `module`, `contract`, `type`, `ledger`, `const`) and custom runtime type extensions (`registerCustomType`).
  - Pluggable Live Template Macros registered under `<liveTemplateMacro>`: `compactDeclarationName`, `circuitName`, `witnessName`.
  - `CompactDeclarationInsertHandler`: Completion insert handler with auto-numbered declaration naming.
- **Asynchronous External Annotator & Quick-Fix Preview Guard (v1.3.0 / ADR-016)**:
  - High-performance asynchronous external linter pipeline executing upstream compiler in background threads.
  - Eliminated editor lag by synchronizing dirty documents on demand rather than blocking EDT.
  - Robust WSL path translation between Windows host files and Linux WSL toolchain paths.
  - `CompactQuickFixPreviewSideEffectTest`: Safe intention previews that never mutate workspace disk state or trigger external process execution during preview painting.
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
  - "Compile Current Contract" action triggering background compilation and problem reporting.
- **Phase 27: Status Bar Toolchain & Environment Widget (v1.2.2 / ADR-014)**:
  - `CompactStatusBarWidgetFactory`: Registered in `plugin.xml` on editor status bar.
  - `CompactStatusBarWidget`: Lightweight, non-blocking widget displaying active Compact version with language version mapping.
  - `CompactStatusBarPopup`: Native speed-search popup menu to switch installed compiler versions, download new versions, jump to the Remix Compiler panel, or open Midnight settings.
- **Phase 28: Smart Enter, Doc Comments & Intentions Suite (v1.2.4 / ADR-006, ADR-007, ADR-008)**:
  - `CompactSmartEnterProcessor`: Non-destructive `Ctrl+Shift+Enter` completion for circuits (`: Void` or preserve `:`), `const` declarations (requiring `=` before `;`), and parameterized types (`Bytes<>`, `Uint<>`).
  - `CompactCommenter` & `CompactDocCommentEnterHandler`: Clean `Enter` handling in `/* ... */` and `/** ... */` without duplicate asterisks or trailing `*/` syntax errors.
  - Full Intention Actions Suite (`Alt+Enter`):
    - `CompactTogglePureCircuitIntention`
    - `CompactToggleExportIntention` (supports circuits, contracts, structs, enums, modules, types, ledgers, witnesses; excludes block-scoped `const` per ADR-028)
    - `CompactSurroundWithDiscloseIntention`
    - `CompactInvertIfIntention`
    - `CompactSpecifyTypeExplicitlyIntention`
    - `CompactRemoveRedundantTypeIntention`
- **Architectural Decision Records (ADRs)**:
  - Fully maintained index in `.ai/decisions/README.md` covering all 30 major architectural subsystems (**ADR-001 through ADR-030**) with 100% coverage across all registered `plugin.xml` extension points, strict upstream compiler references, workspace reference plugin benchmarks, and anti-hardcoding evaluation.

---

## 2. Test Suite & Verification Metrics

- **Total Tests**: **646 passing tests** (0 failures, 0 skipped, 100% success rate)
- **Active Test Suites**: **64 test classes**
- **Execution Time**: ~2m 30s via `./gradlew test`

### Test Suite Breakdown

| Subsystem / Test Class | Test Count | Status |
| :--- | :--- | :--- |
| `dev.verloren.midnight.inspection.CompactInspectionTest` | 93 | Passed |
| `dev.verloren.midnight.completion.CompactCompletionTest` | 75 | Passed |
| `dev.verloren.midnight.formatter.CompactFormatterTest` | 39 | Passed |
| `dev.verloren.midnight.editor.CompactDelimiterTypingTest` | 30 | Passed |
| `dev.verloren.midnight.editor.CompactAngleBraceTypingTest` | 24 | Passed |
| `dev.verloren.midnight.resolve.CompactResolveTest` | 21 | Passed |
| `dev.verloren.midnight.resolve.CompactCrossFileResolveTest` | 17 | Passed |
| `dev.verloren.midnight.ide.templates.CompactLiveTemplateTest` | 17 | Passed |
| `dev.verloren.midnight.documentation.CompactDocumentationTest` | 16 | Passed |
| `dev.verloren.midnight.highlighter.CompactHighlightingTest` | 16 | Passed |
| `dev.verloren.midnight.editor.CompactSmartEnterTest` | 15 | Passed |
| `dev.verloren.midnight.type.CompactTypeInferenceTest` | 15 | Passed |
| `dev.verloren.midnight.navigation.CompactTypeDeclarationProviderTest` | 14 | Passed |
| `dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateTest` | 12 | Passed |
| `dev.verloren.midnight.intention.CompactPhase28IntentionsTest` | 12 | Passed |
| `dev.verloren.midnight.lexer.LexerTest` | 12 | Passed |
| `dev.verloren.midnight.parameterInfo.CompactParameterInfoHandlerTest` | 12 | Passed |
| `dev.verloren.midnight.annotator.CompactExternalAnnotatorTest` | 11 | Passed |
| `dev.verloren.midnight.findUsages.CompactFindUsagesTest` | 10 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationNameGeneratorTest` | 10 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTemplateTriggerTest` | 10 | Passed |
| `dev.verloren.midnight.editor.CompactDocCommentEnterTest` | 9 | Passed |
| `dev.verloren.midnight.refactoring.CompactRenameTest` | 9 | Passed |
| `dev.verloren.midnight.reference.CompactReferenceTest` | 9 | Passed |
| `dev.verloren.midnight.structure.CompactStructureViewTest` | 9 | Passed |
| `dev.verloren.midnight.editor.CompactLineMarkerTest` | 7 | Passed |
| `dev.verloren.midnight.version.CompactVersionManagerTest` | 7 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolverTest` | 6 | Passed |
| `dev.verloren.midnight.parser.ErrorRecoveryParserTest` | 6 | Passed |
| `dev.verloren.midnight.run.CompactToolchainUtilTest` | 6 | Passed |
| `dev.verloren.midnight.editor.CompactSurroundWithTest` | 5 | Passed |
| `dev.verloren.midnight.run.CompactRunConfigurationTest` | 5 | Passed |
| `dev.verloren.midnight.statusbar.CompactStatusBarWidgetTest` | 5 | Passed |
| `dev.verloren.midnight.stdlib.CompactStandardLibraryTest` | 5 | Passed |
| `dev.verloren.midnight.annotator.CompactQuickFixPreviewSideEffectTest` | 4 | Passed |
| `dev.verloren.midnight.editor.CompactEditorFeaturesTest` | 4 | Passed |
| `dev.verloren.midnight.editor.CompactFoldingTest` | 4 | Passed |
| `dev.verloren.midnight.toolwindow.CompactCompilerPanelTest` | 4 | Passed |
| `dev.verloren.midnight.version.CompactSemVerUtilTest` | 4 | Passed |
| `dev.verloren.midnight.CompactBundleTest` | 3 | Passed |
| `dev.verloren.midnight.CompactTestUtilsTest` | 3 | Passed |
| `dev.verloren.midnight.editor.CompactInlayHintsTest` | 3 | Passed |
| `dev.verloren.midnight.inspection.CompactPragmaVersionInspectionTest` | 3 | Passed |
| `dev.verloren.midnight.lexer.PragmaTest` | 3 | Passed |
| `dev.verloren.midnight.parser.PragmaParserTest` | 3 | Passed |
| `dev.verloren.midnight.parser.StatementParserTest` | 3 | Passed |
| `dev.verloren.midnight.settings.MidnightProjectSettingsTest` | 3 | Passed |
| `dev.verloren.midnight.settings.MidnightSettingsTest` | 3 | Passed |
| `dev.verloren.midnight.stdlib.CompactStdlibServiceTest` | 3 | Passed |
| `dev.verloren.midnight.symbol.CompactSymbolTest` | 3 | Passed |
| `dev.verloren.midnight.toolwindow.CompactVersionCardTest` | 3 | Passed |
| `dev.verloren.midnight.completion.CompactInsertHandlersTest` | 2 | Passed |
| `dev.verloren.midnight.editor.CompactBreadcrumbsTest` | 2 | Passed |
| `dev.verloren.midnight.intention.CompactPragmaIntentionTest` | 2 | Passed |
| `dev.verloren.midnight.navigation.CompactChooseByNameTest` | 2 | Passed |
| `dev.verloren.midnight.parser.CompactParserDefinitionTest` | 2 | Passed |
| `dev.verloren.midnight.parser.EndToEndParserTest` | 2 | Passed |
| `dev.verloren.midnight.psi.DeclarationPsiTest` | 2 | Passed |
| `dev.verloren.midnight.run.CompactRunConfigurationProducerTest` | 2 | Passed |
| `dev.verloren.midnight.highlighter.CompactColorSettingsPageTest` | 1 | Passed |
| `dev.verloren.midnight.parser.DeclarationParserTest` | 1 | Passed |
| `dev.verloren.midnight.parser.ExpressionParserTest` | 1 | Passed |
| `dev.verloren.midnight.parser.TypePatternParserTest` | 1 | Passed |
| `dev.verloren.midnight.psi.ElementFactoryConsistencyTest` | 1 | Passed |
| **Total Across 64 Suites** | **646** | **100% Passed** |

---

## 3. Roadmap & Evolution (Phases 31–36)

- **Phase 31: Stub Indexing & Large Workspace Caching**
- **Phase 32: Advanced Refactorings (Rename, Extract Variable, Change Signature)**
- **Phase 33: Remix Blockchain Explorer & Local Node Sandbox**
- **Phase 34: Ledger Storage & ZK Constraint Profiler**
- **Phase 35: In-IDE Language Server / PSI-MCP Bridge**
- **Phase 36: Polyglot Compact/TypeScript Integration & Test Framework**
