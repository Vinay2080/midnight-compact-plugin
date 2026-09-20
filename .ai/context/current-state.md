# Current State

Last Updated: September 2026 (v1.3.5 / Generic Struct Type Resolution & Field Substitution, Typed Generic Either Helper Completions, Smart Struct Literal Completions, Architecture Guardrails & Modularity)

---

## 1. Implementation Snapshot

### Implemented Subsystems
- **Lexer & Parser**: Handwritten in Java 25. Complete coverage of Compact grammar, declarations, ledger types, type expressions, statements, expressions, and error recovery.
- **PSI Infrastructure**: Element hierarchy (`CompactElement`, `CompactNamedElement`, declaration types, reference types, type nodes).
- **Name Resolution & Reference Contributor**: Lexical scoping, namespace separation (`VALUE` vs `TYPE`), multi-file resolution via `include` statements.
- **Generic Struct Field Resolution & Type Substitution (v1.3.5)**:
  - `CompactTypeInferenceUtil`: Enhanced with structural type parameter substitution across generic and parameterized struct types (`CompactParameterizedType`).
  - `CompactMemberExprImpl` & `CompactStructFieldReference`: Resolves generic struct fields (such as `is_left`, `left`, `right` for `Either<Bytes<32>, ContractAddress>`) and substitutes generic type variables with concrete instantiated type arguments.
  - `CompactStructLiteralExprImpl` & `CompactCallExprImpl`: Type checks struct literal fields and generic circuit invocations against expected and substituted parameter types without false-positive type mismatches.
- **Member Autocompletion for Generic Struct Variables (v1.3.5)**:
  - `CompactCompletionContributor`: Automatically suggests struct members (`target.<caret>`) for parameterized struct instances, dynamically deriving field names and types from substituted generic types.
- **Smart Typed & Generic Struct Literal Completions (v1.3.5)**:
  - `CompactCompletionContributor`: Detects contextual expected types (assignments, circuit returns, call arguments) and offers smart typed struct literal completions (`Either<T1, T2> { is_left: true, left: ..., right: default<T2> }`).
  - `CompactEitherInsertHandler`: Interactive multi-tab live template across type arguments (`$L$`, `$R$`) and field values (`is_left: $IS_LEFT$`, `left: $LEFT$`, `right: $RIGHT$`).
  - Bracket-aware type argument parsing and automatic type inferencing for `default<Type>` expressions.
- **Typed Generic Constructor Helper Completions (v1.3.5)**:
  - `CompactEitherHelperInsertHandler`: Offers `right<L, R>(...)` and `left<L, R>(...)` constructor helper completions when returning or assigning to `Either<L, R>` types, automatically inferring and populating concrete type arguments, positioning the caret within parentheses, and registering tab-out scopes.
  - Contextual `default<Type>` completion with angle brackets, type parameter assistance, and smart type inference in expression contexts (`CompactParenthesesInsertHandler`).
  - Contextual prioritization for `true` and `false` boolean literals in condition contexts (`if (...)`, `assert(...)`, `is_left:` fields).
- **Architecture, Modularity & Generalization Guardrails (v1.3.5)**:
  - Codified architecture guardrails in `.agents/rules/architecture.rules.md` enforcing strict layer hierarchy (`[UI]` -> `[Resolution]` -> `[Type Engine]` -> `[PSI]`), the Rule of Generalization (anti-hardcoding invariant), mandatory User-Defined Mirror testing for standard library features, class size budget ($\le 400$ lines), and single source of truth for types.
  - `CompactArchitectureTest`: Automated architectural tests verifying package dependency rules, cycle prevention, and class budget limits.
- **Create File Action Formatting Guard (v1.3.5)**:
  - `CompactCreateFileAction`: Wrapped template post-creation formatting in `WriteCommandAction` to prevent `IncorrectOperationException`. Verified by `CompactCreateFileActionTest`.
- **Compiler Tool Window Selection Synchronization (v1.3.4)**:
  - `CompactCompilerPanel`: Synchronizes active compiler version card selection with `MidnightProjectSettings` and active editor files.
  - Updates selection highlights cleanly when project settings change or when switching files without triggering unwanted background compilation tasks or causing UI freezes.
- **Return Value Scope Completion (v1.3.4)**:
  - `CompactCompletionContributor`: Prioritizes and suggests in-scope circuit parameters and local bindings inside `return` statements and expressions, preventing unwarranted filtering when inferred types differ during active typing.
- **Prioritized Installed Compiler Suggestions for Pragma Constraints (v1.3.4 / ADR-034)**:
  - `CompactPragmaVersionInspection`: Evaluates whether locally installed compiler toolchains (`CompactVersionManager.getInstalledVersions()`) satisfy pragma constraints. Discovered satisfying toolchains are sorted descending (newest first via `CompactSemVerUtil.DESCENDING_COMPARATOR`) and offered as immediate, offline quick-fixes before external download options.
  - Locked vs Open Constraint Precision: Open constraints (`>= 0.20`, `0.20`) suggest higher compatible versions (such as toolchain `0.34.0` / language `0.26.0`), while locked/pinned constraints (`^0.20`, `~0.20`, `< 0.22`) strictly enforce SemVer upper bounds and suppress incompatible versions.
  - Redundancy Elimination: Eliminates duplicate download options when the exact required version is already installed locally, and de-duplicates multiple patch toolchains targeting the same language version.
  - `CompactSwitchCompilerVersionIntention`: Intention context action dynamically resolves whether the highest satisfying compiler is already installed, updating intention text and switching compilers directly without triggering background downloads.
  - `CompactSwitchCompilerQuickFix`: Enhanced with an explicit toolchain version parameter to cleanly configure selected compilers.
- **Dynamic File Template Pragma Version Resolution (v1.3.4 / ADR-033)**:
  - `CompactDefaultTemplatePropertiesProvider`: Registered under `<defaultTemplatePropertiesProvider>` in `plugin.xml` implementing IntelliJ platform `DefaultTemplatePropertiesProvider`. Dynamically extracts the active compiler toolchain version (`CompactToolchainUtil.getActiveCompilerVersion(project)`) and maps it to the source language version via `CompactVersionManager.getLanguageVersionForToolchain`.
  - Velocity Template Integration: Updated all 4 internal file templates (`Compact File`, `Compact Contract`, `Compact Module`, `Compact Interface`) with conditional Velocity directives: `#if ($COMPACT_LANGUAGE_VERSION && $COMPACT_LANGUAGE_VERSION != "")pragma language_version >= $COMPACT_LANGUAGE_VERSION;#else...#end`.
  - Resilient Fallback: Defaults gracefully to language version `0.26.0` (compiler `0.34.0`) when no custom toolchain is configured or when the project has not yet initialized.
  - `CompactCreateFileAction`: Injects `COMPACT_LANGUAGE_VERSION`, `LANGUAGE_VERSION`, `COMPACT_COMPILER_VERSION`, and `COMPILER_VERSION` into template creation parameters.
- **Pragma Version Directives Completion & Quick Documentation (v1.3.4 / ADR-032)**:
  - `CompactCompletionContext`: Classifies the caret as `Kind.AFTER_PRAGMA` when preceded by a top-level `pragma` keyword token or inside a `CompactPragmaForm`.
  - Directives completion: Prioritized completion suggestions for `language_version` (priority 100.0) and `compiler_version` (priority 90.0) with bold styling, `"pragma"` type text, and tail text `" >= <version>"`.
  - Non-destructive insertion: `createPragmaInsertHandler()` automatically appends a trailing space if not followed by whitespace, placing the cursor directly in position for the version constraint.
  - `CompactDocumentationProvider`: Implements `getDocumentationElementForLookupItem` to synthesize a documentation element for pragma items via `CompactElementFactory.createPragmaForm`, and renders rich HTML documentation (`generatePragmaDoc`) explaining semantic version constraints, supported comparison operators (`>=`, `>`, `^`, `~`, `==`), differences between source language specification and compiler binary versions, and concrete examples.
- **Standard Library Import Navigation & Documentation (v1.3.4 / ADR-017, ADR-023)**:
  - `CompactImportDeclarationImpl` & `CompactImportReference`: Automatically resolves `CompactStandardLibrary` module identifiers and import paths directly to the bundled `standard-library.compact` PSI file via `CompactStdlibService`.
  - `CompactReferenceContributor`: Provides module-level references on import identifiers to allow Ctrl+B / Ctrl+Click jump to declaration on `CompactStandardLibrary`.
  - `CompactGotoDeclarationHandler`: Direct fallback declaration navigation for `CompactStandardLibrary` identifier tokens to standard library PSI files.
  - `standard-library.compact`: Enriched with comprehensive JSDoc/CompactDoc documentation (`/** ... */`) covering all standard structs (`Maybe`, `Either`, `MerkleTreeDigest`, `ShieldedCoinInfo`, etc.), circuits (`some`, `none`, `left`, `right`, `receiveShielded`, `sendShielded`, `mintShieldedToken`, `blockTime`, etc.), and fields with `@param`, `@return`, and `@see` tags.
  - `CompactDocumentationProvider`: Full Quick Documentation (Ctrl+Q / F1) support for `CompactStandardLibrary` import statements and `standard-library.compact` file headers, rendering rich HTML documentation and architectural overviews.
- **Quote Auto-Completion & Pairing (v1.3.3 / ADR-031)**:
  - `CompactQuoteHandler`: `SimpleTokenSetQuoteHandler` registered in `plugin.xml` managing automatic quotation mark insertion, pairing, and cursor positioning between quotes (`"<caret>"`, `'<caret>'`).
  - Strict token categorization: `isOpeningQuote` returns `true` only for `CompactTokenTypes.UNTERMINATED_STRING` at `offset == iterator.getStart()`, triggering automatic closing quote insertion without corrupting typing in front of existing string literals.
  - Strict closing quote validation: `isClosingQuote` returns `true` only for closed `CompactTokenTypes.STRING_LITERAL` of length $\ge 2$ at `offset == iterator.getEnd() - 1`.
  - Context isolation: quote pairing is strictly suppressed inside line comments (`//`), block comments (`/* ... */`), and within existing closed strings. Respects user platform preferences (`CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE`).
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
  - Fully maintained index in `.ai/decisions/README.md` covering all 34 major architectural subsystems (**ADR-001 through ADR-034**) with 100% coverage across all registered `plugin.xml` extension points, strict upstream compiler references, workspace reference plugin benchmarks, and anti-hardcoding evaluation.

---

## 2. Test Suite & Verification Metrics

- **Total Tests**: **721 passing tests** (0 failures, 0 skipped, 100% success rate)
- **Active Test Suites**: **67 test classes**
- **Execution Time**: ~2m 40s via `./gradlew test`

### Test Suite Breakdown

| Subsystem / Test Class | Test Count | Status |
| :--- | :--- | :--- |
| `dev.verloren.midnight.inspection.CompactInspectionTest` | 100 | Passed |
| `dev.verloren.midnight.completion.CompactCompletionTest` | 95 | Passed |
| `dev.verloren.midnight.formatter.CompactFormatterTest` | 40 | Passed |
| `dev.verloren.midnight.editor.CompactDelimiterTypingTest` | 30 | Passed |
| `dev.verloren.midnight.editor.CompactAngleBraceTypingTest` | 24 | Passed |
| `dev.verloren.midnight.documentation.CompactDocumentationTest` | 22 | Passed |
| `dev.verloren.midnight.resolve.CompactResolveTest` | 21 | Passed |
| `dev.verloren.midnight.ide.templates.CompactLiveTemplateTest` | 19 | Passed |
| `dev.verloren.midnight.editor.CompactQuoteTypingTest` | 18 | Passed |
| `dev.verloren.midnight.resolve.CompactCrossFileResolveTest` | 17 | Passed |
| `dev.verloren.midnight.highlighter.CompactHighlightingTest` | 16 | Passed |
| `dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateTest` | 15 | Passed |
| `dev.verloren.midnight.type.CompactTypeInferenceTest` | 15 | Passed |
| `dev.verloren.midnight.editor.CompactSmartEnterTest` | 15 | Passed |
| `dev.verloren.midnight.navigation.CompactTypeDeclarationProviderTest` | 14 | Passed |
| `dev.verloren.midnight.intention.CompactPhase28IntentionsTest` | 12 | Passed |
| `dev.verloren.midnight.lexer.LexerTest` | 12 | Passed |
| `dev.verloren.midnight.parameterInfo.CompactParameterInfoHandlerTest` | 12 | Passed |
| `dev.verloren.midnight.annotator.CompactExternalAnnotatorTest` | 11 | Passed |
| `dev.verloren.midnight.findUsages.CompactFindUsagesTest` | 10 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationNameGeneratorTest` | 10 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTemplateTriggerTest` | 10 | Passed |
| `dev.verloren.midnight.structure.CompactStructureViewTest` | 9 | Passed |
| `dev.verloren.midnight.stdlib.CompactStandardLibraryTest` | 9 | Passed |
| `dev.verloren.midnight.reference.CompactReferenceTest` | 9 | Passed |
| `dev.verloren.midnight.editor.CompactDocCommentEnterTest` | 9 | Passed |
| `dev.verloren.midnight.refactoring.CompactRenameTest` | 9 | Passed |
| `dev.verloren.midnight.inspection.CompactPragmaVersionInspectionTest` | 8 | Passed |
| `dev.verloren.midnight.run.CompactToolchainUtilTest` | 7 | Passed |
| `dev.verloren.midnight.version.CompactVersionManagerTest` | 7 | Passed |
| `dev.verloren.midnight.editor.CompactLineMarkerTest` | 7 | Passed |
| `dev.verloren.midnight.parser.ErrorRecoveryParserTest` | 6 | Passed |
| `dev.verloren.midnight.completion.CompactInsertHandlersTest` | 6 | Passed |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolverTest` | 6 | Passed |
| `dev.verloren.midnight.run.CompactRunConfigurationTest` | 5 | Passed |
| `dev.verloren.midnight.statusbar.CompactStatusBarWidgetTest` | 5 | Passed |
| `dev.verloren.midnight.editor.CompactSurroundWithTest` | 5 | Passed |
| `dev.verloren.midnight.editor.CompactFoldingTest` | 4 | Passed |
| `dev.verloren.midnight.version.CompactSemVerUtilTest` | 4 | Passed |
| `dev.verloren.midnight.editor.CompactEditorFeaturesTest` | 4 | Passed |
| `dev.verloren.midnight.annotator.CompactQuickFixPreviewSideEffectTest` | 4 | Passed |
| `dev.verloren.midnight.toolwindow.CompactCompilerPanelTest` | 4 | Passed |
| `dev.verloren.midnight.toolwindow.CompactVersionCardTest` | 3 | Passed |
| `dev.verloren.midnight.editor.CompactInlayHintsTest` | 3 | Passed |
| `dev.verloren.midnight.lexer.PragmaTest` | 3 | Passed |
| `dev.verloren.midnight.settings.MidnightProjectSettingsTest` | 3 | Passed |
| `dev.verloren.midnight.symbol.CompactSymbolTest` | 3 | Passed |
| `dev.verloren.midnight.stdlib.CompactStdlibServiceTest` | 3 | Passed |
| `dev.verloren.midnight.settings.MidnightSettingsTest` | 3 | Passed |
| `dev.verloren.midnight.intention.CompactPragmaIntentionTest` | 3 | Passed |
| `dev.verloren.midnight.parser.PragmaParserTest` | 3 | Passed |
| `dev.verloren.midnight.psi.DeclarationPsiTest` | 3 | Passed |
| `dev.verloren.midnight.CompactTestUtilsTest` | 3 | Passed |
| `dev.verloren.midnight.parser.StatementParserTest` | 3 | Passed |
| `dev.verloren.midnight.CompactBundleTest` | 2 | Passed |
| `dev.verloren.midnight.run.CompactRunConfigurationProducerTest` | 2 | Passed |
| `dev.verloren.midnight.architecture.CompactArchitectureTest` | 2 | Passed |
| `dev.verloren.midnight.navigation.CompactChooseByNameTest` | 2 | Passed |
| `dev.verloren.midnight.parser.CompactParserDefinitionTest` | 2 | Passed |
| `dev.verloren.midnight.parser.EndToEndParserTest` | 2 | Passed |
| `dev.verloren.midnight.editor.CompactBreadcrumbsTest` | 2 | Passed |
| `dev.verloren.midnight.highlighter.CompactColorSettingsPageTest` | 1 | Passed |
| `dev.verloren.midnight.actions.CompactCreateFileActionTest` | 1 | Passed |
| `dev.verloren.midnight.psi.ElementFactoryConsistencyTest` | 1 | Passed |
| `dev.verloren.midnight.parser.TypePatternParserTest` | 1 | Passed |
| `dev.verloren.midnight.parser.ExpressionParserTest` | 1 | Passed |
| `dev.verloren.midnight.parser.DeclarationParserTest` | 1 | Passed |
| **Total Across 67 Suites** | **721** | **100% Passed** |

---

## 3. Known Limitations & Roadmap

### Known Limitations
1. **Multi-Module Project-Wide Indexing**: Currently, symbol resolution across multiple modules relies on explicit `include` and `import` AST navigation. Full workspace-wide stub indexing (`CompactStubIndex`) across unreferenced files is planned for Phase 31.
2. **Deep Flow Type Inference**: Type inference is structural and AST-driven (`CompactTypeInferenceUtil`). Full bidirectional flow analysis across complex higher-order expressions will be augmented alongside the language server PSI bridge (Phase 35).

### Roadmap & Evolution (Phases 31–36)
- **Phase 31: First-Class Type Hierarchy & Substitutor / Stub Indexing**
  - Project-wide stub index (`CompactStubIndex`) for cross-file declarations without parsing full AST.
  - Fast global Go to Symbol (`Ctrl+Alt+Shift+N`) across large Compact repositories.
- **Phase 32: Modular Completion Providers & Advanced Refactorings**
  - Safe Delete refactoring for unused circuits, witnesses, and types.
  - Extract Variable (`Ctrl+Alt+V`) and Extract Circuit/Function (`Ctrl+Alt+M`).
  - Introduce Parameter refactoring.
- **Phase 33: Remix-Style Blockchain & State Explorer**
  - Integrated contract simulation panel and ZK proving state inspector.
  - Contract deployment artifact viewer (`.compact.json` / ABI).
- **Phase 34: Ledger Storage & ZK Proving Profiler**
  - Real-time constraint counter for circuits (estimating R1CS / Plonk constraint count).
  - Ledger state layout visualization and storage slot cost analysis.
- **Phase 35: In-IDE Language Server / PSI Bridge**
  - Direct bridge between handwritten PSI AST and upstream Compact compiler AST representations.
- **Phase 36: Polyglot Compact / TypeScript Integration**
  - Midnight JS / Compact client bindings code generation from contracts.
  - Cross-language navigation between Compact contracts and TypeScript client test suites.
