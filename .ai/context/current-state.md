# Current State Snapshot

Last Updated: August 2026

---

## 1. Implementation Status

### Completed (Phases 1–9)
- **Phase 1: Lexer & Parser**
  - Handwritten `CompactLexer` supporting numbers (hex, octal, binary, decimal), strings, escapes, comments, delimiters, active keywords, and reserved keywords.
  - Handwritten `CompactParser` implementing recursive-descent parsing for pragmas, contracts, modules, imports, exports, ledgers, constructors, circuits, witnesses, structs, enums, type aliases, statements, and expressions.
  - Resilient error recovery on missing tokens, invalid modifier combinations, and incomplete statements.
- **Phase 2: Handwritten PSI Layer**
  - Concrete PSI elements for declarations, expressions, statements, and types under `dev.verloren.midnight.psi.impl.*`.
  - `CompactNamedElement` wrapper for all declared symbols.
  - `CompactElementFactory` for PSI snippet creation.
- **Phase 3: References, Scope Resolution & Completion**
  - **Build & Distribution Compatibility**:
    - Open-ended IntelliJ IDEA build compatibility: `sinceBuild = "242"`, `untilBuild = null` (compatible with 2024.2, 2024.3, 2025.1, 2025.2, etc.).
    - Java 21 LTS bytecode targeting (`sourceCompatibility = 21`, `targetCompatibility = 21`).
    - Explicit `extensions="compact"` and `fieldName="INSTANCE"` added to `<fileType>` in `plugin.xml` ensuring `.compact` files bind to `CompactFileType`, `CompactParserDefinition`, and syntax highlighters.
    - Windows & WSL Toolchain: prioritized WSL discovery on Windows (`/home/<user>/.local/bin/compact`, `.cargo/bin/compact`, etc.), filtered out Windows NTFS `C:\Windows\System32\compact.exe`, and auto-associated distro in `parseConfiguredPath`.
    - Package distribution verified at `build/distributions/midnight-plugin-1.0.0-SNAPSHOT.zip`.
  - Scope walker in `CompactResolveUtil` with split `VALUE` and `TYPE` namespaces.
  - Local lexical shadowing (innermost first).
  - Navigation for identifiers, struct literals, enum members, and struct fields.
  - Rename and Find Usages support.
  - Contextual code completion in `CompactCompletionContributor`.
- **Phase 4: Type Inference & Numeric Types**
  - Concrete type system with `CompactType`, `CompactPrimitiveType`, `CompactUintType`, and `CompactNumericLiteralType`.
  - Type-aware integer literal resolution preserving numeric values for boundary checking and assignability against `Uint<N>` / `Field`.
  - Lightweight type evaluation in `CompactTypeInferenceUtil` (literals, binary operations, member access, casts).
  - Type-aware struct field resolution.
- **Phase 5: Semantic Inspections & Quick-Fixes**
  - `CompactUnresolvedReferenceInspection` (with soft-unresolved skip for builtins).
  - `CompactDuplicateDeclarationInspection` (respecting separate namespaces and scopes).
  - `CompactUnusedLocalVariableInspection` with `CompactRemoveUnusedVariableFix`.
  - `CompactTypeMismatchInspection`:
    - Validates `&&` and `||` logical operands (`Boolean` expected).
    - Validates `!` negation operand (`Boolean` expected).
    - Validates `==` and `!=` type compatibility (supporting numeric literals and all `Uint` bit widths).
    - Validates `if (condition)` control-flow predicates (`Boolean` expected).
    - Validates relational `<, <=, >, >=` operand compatibility (permitting `Uint` and numeric literal comparisons and rejecting `Boolean`/`Field` relational operands).
    - Validates arithmetic `+, -, *, /, %` operations and rejects `Boolean` operands.
    - Validates `const x: Type = expr;` declaration initializers against declared types with bounds checking on numeric literals.
- **Phase 6: Formatter & Smart Indentation**
  - `CompactFormattingModelBuilder` and `CompactBlock`.
  - 2-space canonical indentation and context-aware spacing rules.
- **Phase 7: Structure View & File Outline**
  - `CompactStructureViewFactory`, `CompactStructureViewModel`, and `CompactStructureViewElement`.
  - Hierarchy representation for contracts, modules, circuits, witnesses, ledgers, constructors, structs, struct fields, enums, enum members, type aliases, const bindings, pragmas, includes, imports, and exports.
- **Phase 8: Documentation Provider & Quick Docs**
  - `CompactDocumentationProvider` rendering quick docs (`Ctrl + Q` / hover tooltips) with native `DocumentationMarkup` (definitions, markdown description paragraphs, two-column sections table for `Params:`, `Returns:`, `Throws:`, `See also:`, `Since:`, `Deprecated:`, `Notice:`, `Dev:`, `Fields:`, and `Variants:`).
  - Javadoc block doc comments (`/** ... */`) and line doc comments (`///`) normalization, stripping asterisk line prefixes, and full doc tag parsing.
  - Inline markdown parsing (inline code `` `code` ``, bold `**text**`, italic `*text*`, links `[text](url)`).
  - Parameter doc inheritance (hovering on a parameter resolves the `@param` tag from the enclosing circuit/witness/constructor doc comment).
  - Struct field doc inheritance (hovering on a struct field resolves the `@param` tag from the parent struct doc comment).
  - Direct doc comment resolution (`getCustomDocumentationElement` on `/** ... */` resolves to the documented declaration).
  - Rendered documentation for Reader Mode (`generateRenderedDoc`).
  - `CodeDocumentationAwareCommenter` in `CompactCommenter` for auto-formatting doc comment asterisks and typing ergonomics.
- **Phase 9: Cross-File Resolution & Import Awareness**
  - Direct and transitive file inclusion resolution via `include "relative/path.compact";`.
  - Selective and module imports via `import { Symbol } from './path';` and `import { square } from Module;`.
  - Cross-file enum definition and enum-variant member navigation (`GameState.PLAYING`).
  - Go To Declaration navigation on `include "..."` and `import ... from '...'` path strings.
  - Cycle-safe recursive include traversal.
  - Strict preservation of local-over-external shadowing and `VALUE` vs. `TYPE` namespaces across files.

- **Phase 10: PSI Refactoring & Architectural Alignment**
  - Expanded `CompactNamedElementImpl.getUseScope()` to project search scope for top-level/exported declarations and local search scope for parameters and locals, enabling cross-file Find Usages.
  - Added strongly typed accessors to PSI interfaces (`CompactCircuitDefinition.getParameters()`, `getBody()`, `getReturnTypeElement()`, `CompactStructDefinition.getFields()`, `CompactEnumDefinition.getMembers()`, `CompactTypeDefinition.getTargetTypeElement()`, `CompactConstructorDeclaration.getParameters()`, `getBody()`, etc.).
  - Hardened `CompactIncludeDeclarationImpl.resolveIncludedFile()` and `CompactImportDeclarationImpl.resolveImportedFile()` with deterministic directory / content-root relative resolution and `CachedValuesManager` caching.
  - Implemented `equals()` and `hashCode()` on `CompactReferenceBase` for optimal `ResolveCache` hit rates.
  - Replaced thread-unsafe UserData recursion flags with thread-safe `RecursionManager` in `CompactReferenceExprImpl.getType()`.
  - Registered essential IDE typing ergonomics extensions: `CompactCommenter` (`//` and `/* */`), `CompactPairedBraceMatcher` (`{}`, `()`, `[]`, `<>`), and `CompactQuoteHandler` (`"`, `'`).

- **Phase 11: Syntax & Semantic Highlighting Overhaul**
  - Designed and implemented complete semantic and syntactic color registry (`CompactHighlighterColors`) with 42+ dedicated `TextAttributesKey`s mapped to standard `DefaultLanguageHighlighterColors`.
  - Fine-grained semantic distinction for declarations vs. call sites vs. read/write usages: circuits, witnesses, constructors, contracts, modules, structs, enums, enum members, fields, type aliases, type parameters, constants, parameters, local variables, write reassignments (`LOCAL_VARIABLE_WRITE`), ledger states, ledger writes (`LEDGER_WRITE`), and imported symbols.
  - Modifiers (`export`, `pure`, `sealed`, `new`, `implements`, `external`) distinguished with `CompactHighlighterColors.MODIFIER`.
  - Built-in primitive types (`Field`, `Boolean`, `Uint<N>`, `Bytes<N>`, `Vector`, `Opaque`, `Cell`, `Void`, `JubjubScalar`, etc.) and standard library types (`Counter`, `Set`, `Map`, `List`, `HistoricMerkleTree`, `MerkleTree`, `Kernel`, `ContractAddress`, `ShieldedCoinInfo`, `Maybe`, `Either`, etc.) distinguished from custom nominal struct/enum/alias type references.
  - Built-in functions (`assert`, `disclose`, `fold`, `slice`, `pad`, `emit`, `map`, `transientHash`, `persistentHash`, `transcribe`, `publicKey`, `degradeToTransient`, `default`, etc.) distinctively highlighted from user-defined circuit/witness calls.
  - Struct literal field names (`Point { x: 1, y: 2 }`) and destructuring patterns highlighted appropriately.
  - String escape sequence highlighting distinguishing valid escapes (`\n`, `\t`, `\xHH`, `\u{...}`) from invalid escape sequences.
  - Doc comments (`///`, `/**`) visually distinguished from standard line and block comments.
  - Pragma directives and semantic version literals (`^0.20.0`) highlighted.
  - Full IDE Settings → Editor → Color Scheme → Compact configuration page (`CompactColorSettingsPage`) with comprehensive demo code and interactive sample highlighting.
  - Semantic annotator (`CompactHighlightingAnnotator`) registered in `plugin.xml`.

- **Phase 12: Level 1 IntelliJ Platform Integration**
    - **File Templates & "New Compact File" Action**:
        - Created `CompactCreateFileAction` in `NewGroup` context menu with templates for Empty File, Compact Contract,
          Compact Module, and Compact Interface.
        - Registered `CompactFileTemplateGroupFactory` under `fileTemplateGroup`.
    - **Live Templates (Snippets)**:
        - Created `CompactLiveTemplateProvider` and `CompactLiveTemplateContextType`.
        - Bundled standard live templates in `liveTemplates/Compact.xml` (`cct`, `cir`, `wit`, `led`, `str`, `en`,
          `ass`, `disc`, `inc`, `imp`, `type`).
    - **Code Folding Builder**:
        - Implemented `CompactFoldingBuilder` with intelligent folding of `{ ... }` blocks (contracts, modules,
          circuits, ledgers, constructors, structs, enums, blocks), multiline block comments (`/* ... */`), doc comments
          (`/// ...`), and contiguous `include`/`import` statement groups.
    - **Editor Breadcrumbs**:
        - Implemented `CompactBreadcrumbsProvider` displaying live contextual scope breadcrumbs at the bottom of the
          editor (`contract Name > circuit name > if block`).
    - **Spellchecking Strategy**:
        - Implemented `CompactSpellcheckingStrategy` with smart identifier sub-token splitting and comment/string
          literal spellchecking.
    - **Surround With**:
        - Implemented `CompactSurroundDescriptor`, `CompactIfSurrounder`, and `CompactBlockSurrounder` enabling
          `Ctrl + Alt + T` statement wrapping into `if (expr) { ... }` or `{ ... }` blocks.

- **Phase 13: Compiler-Derived Smart Contract Inspections & Quick-Fixes**
  - `CompactPureCircuitInspection`: Enforces `pure circuit` invariants (rejects ledger reads/writes, witness invocations, event emissions, and impure circuit calls) with `CompactRemovePureModifierFix`.
  - `CompactSealedFieldMutationInspection`: Prevents mutation of `sealed ledger` fields outside `constructor`.
  - `CompactRecursiveCircuitInspection`: Rejects direct self-recursion and mutual recursion in ZK circuits.
  - `CompactConstructorRestrictionInspection`: Rejects event emissions (`emit`) and cross-contract calls inside `constructor`.
  - `CompactUndisclosedWitnessInspection`: Enforces Witness Protection Program (WPP) rules requiring `disclose(...)` when assigning private witness data to public ledger state with `CompactWrapWithDiscloseFix`.

- **Phase 15: Compact Compiler Run Configuration & Gutter "Play" Buttons**
  - `CompactConfigurationType`, `CompactConfigurationFactory`, `CompactRunConfiguration`, `CompactRunConfigurationEditor`, `CompactRunProfileState`, `CompactConsoleFilter`, and `CompactRunLineMarkerContributor`.
  - Enables 1-click execution from the editor gutter, automatic `compactc` CLI execution, fast dev build flag (`--skip-zk`), and clickable console error hyperlinks.

- **Phase 16: Midnight Settings Page (`Languages & Frameworks -> Midnight Compact`)**
  - `MidnightSettingsState`, `MidnightSettingsComponent`, and `MidnightSettingsConfigurable`.
  - Application-level persistent settings managing compiler executable path, default output directory, default ZK skip behavior, and Devnet/Node RPC endpoint.

- **Phase 17: External Linter & Background Diagnostics (`ExternalAnnotator`)**
  - `CompactExternalAnnotator`, `CompactCompilerDiagnostic`, and `CompactCompilerOutputParser`.
  - Background asynchronous execution of `compactc --vscode --skip-zk` with 100% official compiler diagnostic parsing into editor annotations.

- **Phase 18: Semantic Gutter Line Markers (Privacy & Circuit Visualizer)**
  - `CompactLineMarkerProvider`: Renders gutter icons for private off-chain queries (`witness`), Zero-Knowledge boundary transitions (`disclose`), public on-chain circuits, and ledger storage fields.

- **Phase 19: Architectural Hardening & Concurrency Safety**
  - Configured `CompactParserDefinition.getStringLiteralElements()` returning `CompactTokenSets.STRING_LITERALS` enabling native IntelliJ string literal language injection and quote handlers.
  - Implemented `CompactStdlibService` (`@Service(Service.Level.PROJECT)`) providing thread-safe, race-free bundled standard library & ZKIR initialization with deterministic `0L` timestamp.
  - Removed dead `BOOLEAN_LITERALS` lookup map in `CompactLexer`.
  - Added non-annotated leaf punctuation and whitespace fast-exit check in `CompactHighlightingAnnotator`.
  - Added `CompactFile.getTopLevelDeclarations()` avoiding deep AST recursive tree walks in `CompactResolveUtil`.
  - Created `CompactTestUtils` DSL test helpers (`doCheckResolve` / `doCheckNoResolve`) with marker DSL.

- **Phase 20: Return-Type Verification, Compiler Exception Diagnostics & Completion Prioritization**
  - Implemented return statement type-checking in `CompactTypeMismatchInspection` verifying `return <expr>` against enclosing `circuit`, `witness`, or constructor return types using `CompactType.isAssignableTo()` and `CompactPsiUtil.getCallableReturnType()`.
  - Added multi-line compiler exception diagnostic parsing in `CompactCompilerOutputParser` to parse `Exception: <file> line <line> char <col>:\n<details>` emitted by `compactc`.
  - Prioritized `true` and `false` literals (`PrioritizedLookupElement.withPriority(..., 100.0)`) in `CompactCompletionContributor` for Boolean return contexts.

- **Phase 21: File Templates & CompactCreateFileAction Optimization**
- **Phase 21: File Templates & Creation Action Modernization**
  - Re-authored all 4 file templates in `src/main/resources/fileTemplates/internal/` aligning with official Midnight Compact specifications (`Compact Contract`, `Compact Module`, `Compact Interface`, `Compact File`).
  - Registered templates under `<internalFileTemplate>` in `plugin.xml`.

- **Phase 22: Hardened File Creation Action (`CompactCreateFileAction`)**
  - Implemented nested subdirectory creation via `CreateFileAction.MkDirs` (e.g. `contracts/tokens/MyContract`).
  - Pure identifier extraction (`extractSimpleName`) preventing Velocity `${NAME}` pollution.
  - Contextual `InputValidatorEx` using `CompactNamesValidator` to reject invalid filesystem characters, path errors, and language keywords (`circuit`, `witness`, `ledger`, `contract`).
  - Integrated platform lifecycle (`createFileFromTemplate`) with automated editor opening, FUS logging, code reformatting via `CodeStyleManager`, and caret positioning after identifiers.
  - Persisted user's last chosen template via `getDefaultTemplateProperty()`.
  - Expanded `CompactFileTemplateTest` to 12 tests verifying nested directories, suffix stripping, name extraction, and validation logic.

### Planned (Future Roadmap)

- **Level 5 Integration**: New Project/DApp Wizard (`DirectoryProjectGenerator`), Interactive Debugger (`XDebugger`, breakpoints, simulator stack frame inspector), Midnight Explorer Tool Window, TypeScript Polyglot Cross-Navigation.

---

## 2. Test Verification Status

- **Total Unit Tests**: **376 passing** (0 failures, 0 skipped, 100% success rate across forty-four test classes).
- **Execution Command**: `./gradlew test`
- **Breakdown**:
  - `CompactHighlightingTest`: 11 tests
  - `CompactColorSettingsPageTest`: 1 test
  - `CompactResolveTest` + `CompactCrossFileResolveTest`: 38 tests (including forward references, top-level ledger resolution, and local parameter shadowing precedence)
  - `CompactInspectionTest`: 91 tests (including return-statement type mismatch validation, condition checking, and relational/arithmetic operators)
  - `CompactStandardLibraryTest` + `CompactStdlibServiceTest`: 8 tests (verifying direct element reference resolution and Ctrl+B / Ctrl+Click GotoDeclaration navigation for bundled standard library and ZKIR symbols)
  - `CompactChooseByNameTest`: 2 tests
  - `CompactInlayHintsTest`: 3 tests
  - `CompactRunConfigurationTest` + `CompactRunConfigurationProducerTest` + `CompactToolchainUtilTest`: 13 tests (verifying per-contract deterministic output directory calculation and compiler output compatibility)
  - `MidnightSettingsTest`: 3 tests
  - `CompactExternalAnnotatorTest`: 3 tests (verifying standard colon-separated and multi-line compiler exception diagnostic parsing)
  - `CompactLineMarkerTest`: 4 tests
  - `CompactDocumentationTest`: 16 tests
  - `CompactStructureViewTest`: 9 tests
  - `CompactFormatterTest`: 39 tests
  - `CompactTypeInferenceTest`: 15 tests
  - `CompactFoldingTest`: 4 tests
  - `CompactBreadcrumbsTest`: 2 tests
  - `CompactLiveTemplateTest`: 3 tests
  - `CompactFileTemplateTest`: 12 tests
  - `CompactSurroundWithTest`: 5 tests
  - `CompactEditorFeaturesTest`: 4 tests
  - `LexerTest` + `PragmaTest`: 15 tests
  - Parser Tests (`DeclarationParserTest`, `StatementParserTest`, `ExpressionParserTest`, `PragmaParserTest`, `TypePatternParserTest`, `ErrorRecoveryParserTest`, `EndToEndParserTest`, `CompactParserDefinitionTest`): 19 tests
  - `CompactRenameTest`: 9 tests
  - `CompactFindUsagesTest`: 10 tests
  - `CompactReferenceTest`: 6 tests
  - `CompactCompletionTest`: 13 tests (including prioritized Boolean return value completion)
  - `CompactSymbolTest`: 3 tests
  - `CompactTestUtilsTest`: 3 tests
  - PSI Tests (`DeclarationPsiTest`, `ElementFactoryConsistencyTest`): 3 tests

---

## 3. Important Production Classes

| Domain | Primary Classes |
| :--- | :--- |
| **Lexer** | `dev.verloren.midnight.lexer.CompactLexer`, `CompactTokenTypes` |
| **Parser** | `dev.verloren.midnight.parser.CompactParser`, `CompactParserDefinition`, `CompactElementTypes` |
| **PSI** | `dev.verloren.midnight.psi.impl.CompactPsiElement`, `CompactNamedElementImpl`, `CompactElementFactory`, `CompactFile`, `CompactIncludeDeclarationImpl`, `CompactImportDeclarationImpl` |
| **Resolve & Scope** | `dev.verloren.midnight.resolve.CompactResolveUtil`, `CompactScopeProcessor`, `CompactReferenceExprImpl`, `CompactStructFieldReference`, `CompactEnumMemberReference`, `CompactIncludeReference`, `CompactImportReference`, `CompactReferenceContributor`, `CompactGotoDeclarationHandler` |
| **Completion** | `dev.verloren.midnight.completion.CompactCompletionContributor` |
| **Refactoring** | `dev.verloren.midnight.refactoring.CompactRefactoringSupportProvider`, `CompactNamesValidator` |
| **Find Usages** | `dev.verloren.midnight.findUsages.CompactFindUsagesProvider` |
| **Type System** | `dev.verloren.midnight.type.CompactType`, `CompactPrimitiveType`, `CompactUintType`, `CompactNumericLiteralType`, `CompactNamedType`, `CompactTypeInferenceUtil` |
| **Inspections** | `dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection`, `CompactDuplicateDeclarationInspection`, `CompactUnusedLocalVariableInspection`, `CompactTypeMismatchInspection`, `CompactPureCircuitInspection`, `CompactSealedFieldMutationInspection`, `CompactRecursiveCircuitInspection`, `CompactConstructorRestrictionInspection`, `CompactUndisclosedWitnessInspection` |
| **Formatter** | `dev.verloren.midnight.formatter.CompactFormattingModelBuilder`, `CompactBlock`, `CompactLanguageCodeStyleSettingsProvider` |
| **Structure View** | `dev.verloren.midnight.structure.CompactStructureViewFactory`, `CompactStructureViewModel`, `CompactStructureViewElement` |
| **Documentation** | `dev.verloren.midnight.documentation.CompactDocumentationProvider`, `CompactDocComment` |
| **Toolchain & Run** | `dev.verloren.midnight.run.CompactToolchainUtil`, `CompactConfigurationType`, `CompactRunConfiguration`, `CompactRunConfigurationProducer` |
| **Diagnostics & Markers**| `dev.verloren.midnight.annotator.CompactExternalAnnotator`, `CompactCompilerOutputParser`, `dev.verloren.midnight.editor.CompactLineMarkerProvider` |
| **Standard Library** | `dev.verloren.midnight.stdlib.CompactStdlibService`, `CompactStandardLibraryProvider` |

---

## 4. Known Limitations

1. **StubIndex Optimization**: Cross-file symbol resolution currently operates by traversing included ASTs; IntelliJ `StubIndex` optimization is planned for large-scale enterprise monorepos.
2. **Standard Library Resolution**: Bundled standard library symbols (`standard-library.compact`, `zkir-v3-library.compact`) are fully resolvable in-memory via `CompactStdlibService` and navigable with `CompactGotoDeclarationHandler`.

---

## 5. Reference Repositories Map

| Reference Repository | Purpose | Key Subsystems & Locations |
| :--- | :--- | :--- |
| **`compact/` & `references/`** | Official Midnight Compact compiler | Grammar (`parser.ss`), Type Checker (`check-types-Lnodca.ss`), Type Inference (`infer-types.ss`), Stdlib (`standard-library.compact`, `zkir-v3-library.compact`), Examples (`examples/`) |
| **`intellij-rust/`** | IntelliJ Platform Kotlin architecture | Stub indices (`lang/core/stubs/`), Type inference pipeline (`lang/core/types/infer/`), Formatter spacing rules (`ide/formatter/`), Inspections (`ide/inspections/`) |
| **`intellij-elixir/`** | Custom handwritten parsing & quick docs | Handwritten lexer/parser integration (`lexer/`, `parser/`), Quick doc provider (`templates/`, `reference/`), Names validator (`refactoring/quote/NamesValidator.java`) |
| **`intellij-scala/`** | Advanced compilation, REPL & build tools | External compile server / daemons (`compile-server/`, `compiler-integration/`), Interactive REPL console (`repl/`, `worksheet/`), Complex type systems (`scala-impl/.../lang/psi/types/`), Structure view (`structure-view/`) |
| **`Rplugin/`** | Toolchain discovery, run configs & visualizers | Interpreter & WSL auto-detection (`psi/.../interpreter/`), Run configurations & execution profile (`src/.../run/`), New Project Wizard (`src/.../projectGenerator/`), Interactive console & tool windows (`src/.../console/`, `src/.../visualization/`) |
| **`../midnight-local-dev/`** | Local blockchain testnet environment | Docker compose stack (`standalone.yml`), Dev accounts (`accounts.json`), Smart contract compilation and deployment scripts (`private-identity-wallet/contracts/compile.ps1`), Endpoints (`http://localhost:9944`, `http://localhost:6300`) |


