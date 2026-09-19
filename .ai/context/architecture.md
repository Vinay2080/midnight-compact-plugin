# Architecture & Subsystem Guide

This document describes the concrete, verified architecture of the Midnight Compact IntelliJ IDEA plugin.

---

## 1. Pipeline Overview

```text
Compact Source Text (.compact)
  ↓
[Lexer] CompactLexer (extends LexerBase) + CompactTokenTypes
  ↓
[Parser] CompactParser (implements PsiParser) + CompactElementTypes
  ↓
[PSI] CompactPsiElement / CompactFile / Typed AST Wrappers (dev.verloren.midnight.psi.impl.*)
  ↓
[Semantic Layer] CompactResolveUtil (AST scope walker) + CompactTypeInferenceUtil
  ↓
[IDE Features]
  ├── References & Navigation (Go To Declaration, Go To Type, Find Usages, Standard Library)
  ├── Completion (CompactCompletionContributor, CompactParameterizedTypeInsertHandler, CompactLedgerInsertHandler)
  ├── Live Templates & Macros (CompactTypeMacro, CompactDeclarationNameGenerator)
  ├── File Templates & Properties (CompactDefaultTemplatePropertiesProvider, CompactCreateFileAction)
  ├── Editor Typing (CompactAngleBraceTypedHandler, CompactDelimiterTypedHandler, CompactQuoteHandler)
  ├── Smart Enter & Intentions (CompactSmartEnterProcessor, CompactToggleExportIntention)
  ├── Refactoring & Rename (CompactRefactoringSupportProvider, CompactNamesValidator)
  ├── Semantic Inspections (10 local inspections & quick fixes)
  ├── Code Style (CompactFormattingModelBuilder, CompactBlock, Indentation)
  ├── Toolchain & Annotator (CompactExternalAnnotator, CompactToolchainUtil, WSL Mapping)
  └── Compiler Tool Window & Status Bar (CompactCompilerToolWindowFactory, CompactStatusBarWidget)
```

---

## 2. Subsystems

### 2.1 Lexer
- **Purpose**: Tokenise Compact source code into IntelliJ `IElementType` tokens.
- **Key Classes**:
  - `dev.verloren.midnight.lexer.CompactLexer`: Handwritten lexer extending `LexerBase`.
  - `dev.verloren.midnight.lexer.CompactTokenTypes`: All token types (keywords, primitives, operators, delimiters, literals, comments, whitespace).
- **Depends on**: IntelliJ Platform Lexer APIs.
- **Used by**: `CompactParserDefinition`, `CompactSyntaxHighlighter`, `CompactWordsScanner`.
- **Invariants**:
  - Keywords, primitives, and operators are categorised during lexing.
  - Must remain robust and never crash or hang on arbitrary or malformed character input.
  - Aligns with upstream compiler lexer (`compact/compiler/lexer.ss`) and ADR-001.

### 2.2 Parser & AST
- **Purpose**: Parse token stream into an AST with rich error recovery.
- **Key Classes**:
  - `dev.verloren.midnight.parser.CompactParser`: Handwritten recursive-descent parser implementing `PsiParser`.
  - `dev.verloren.midnight.parser.CompactParserDefinition`: Plugin integration with IntelliJ PSI infrastructure.
  - `dev.verloren.midnight.psi.CompactElementTypes`: Node element types (`CIRCUIT_DEFINITION`, `WITNESS_DECLARATION`, `BLOCK`, `IF_STATEMENT`, `BINARY_EXPR`, etc.).
- **Depends on**: `CompactTokenTypes`, `CompactElementTypes`.
- **Used by**: `CompactParserDefinition`, IDE PSI build pass.
- **Invariants**:
  - Hand-crafted recursive descent with explicit error recovery markers (ADR-001).
  - Loop safety: every parser loop MUST guarantee token advancement to avoid EDT freezes.
  - Never regenerate parser from grammar files without explicit project migration.

### 2.3 PSI (Program Structure Interface)
- **Purpose**: Provide high-level, typed object-oriented representations of AST nodes.
- **Key Classes**:
  - `dev.verloren.midnight.psi.impl.CompactPsiElement`: Base PSI class.
  - `dev.verloren.midnight.psi.impl.CompactNamedElementImpl`: Implements `CompactNamedElement` / `PsiNamedElement` for declarations.
  - `dev.verloren.midnight.psi.impl.CompactElementFactory`: Factory for generating and replacing PSI snippets during rename and quick-fixes.
  - AST node wrappers in `dev.verloren.midnight.psi.impl.*` (declarations, expressions, statements, types).
- **Depends on**: `CompactElementTypes`, `CompactTokenTypes`.
- **Used by**: All semantic layers and IDE features.
- **Invariants**:
  - All declared named symbols (circuits, witnesses, structs, enums, parameters, consts, import aliases) must implement `CompactNamedElement`.
  - Tolerant of incomplete code and `PsiErrorElement` nodes without throwing exceptions.

### 2.4 Reference Resolution & Scoping
- **Purpose**: Resolve identifiers to declaration elements.
- **Key Classes**:
  - `dev.verloren.midnight.resolve.CompactResolveUtil`: Scope tree walker with namespace separation (ADR-002).
  - `dev.verloren.midnight.resolve.CompactScopeProcessor`: Callback processor collecting declarations.
  - `dev.verloren.midnight.psi.impl.CompactReferenceExprImpl`: Value reference (`PsiReference`).
  - `dev.verloren.midnight.psi.impl.CompactTypeReferenceImpl`: Type reference (`PsiReference`).
  - `dev.verloren.midnight.psi.impl.CompactStructFieldReference`: Field reference via base expression type inference.
  - `dev.verloren.midnight.psi.impl.CompactEnumMemberReference`: Enum member reference (`Enum.Member`).
- **Namespace Model**:
  - `CompactResolveUtil.Namespace.VALUE`: Variables, parameters, consts, circuits, witnesses, constructor, modules.
  - `CompactResolveUtil.Namespace.TYPE`: Structs, enums, type aliases, primitive types, generic type parameters.
- **Invariants**:
  - Innermost-first lexical shadowing: local bindings shadow outer/file-level bindings of the same namespace.
  - Single-file resolution using `PsiTreeUtil` and AST traversal; soft-unresolved handling for external includes or builtins not defined in the local file.

### 2.5 Code Completion & Export Scoping
- **Purpose**: Context-aware code completion with export filtering, pragma directives, and comment suppression (ADR-019, ADR-026, ADR-028, ADR-032).
- **Key Classes**:
  - `dev.verloren.midnight.completion.CompactCompletionContributor`: IntelliJ `CompletionContributor`.
  - `dev.verloren.midnight.completion.CompactCompletionContext`: Structural classifier determining cursor context (`TOP_LEVEL`, `STATEMENT`, `EXPRESSION`, `TYPE`, `AFTER_EXPORT`, `AFTER_SEALED`, `AFTER_PURE`, `AFTER_NEW`, `AFTER_PRAGMA`, `NONE`).
  - `dev.verloren.midnight.completion.CompactDeclarationInsertHandler`: Insert handler for declaration completions with auto-numbering.
  - `dev.verloren.midnight.completion.CompactLedgerInsertHandler`: Insert handler for ledger declarations with live template scaffolding.
- **Invariants**:
  - Keyword and completion suggestions are strictly suppressed inside comments and docstrings.
  - `export const` strictly prohibited per upstream grammar (ADR-028).
  - Pragma directive completions (`language_version`, `compiler_version`) suggested with trailing space insertion (ADR-032).
  - In-scope bindings and parameters are prioritised in return value expressions.
  - Fast, non-blocking single-pass scope collection on background threads.

### 2.6 Editor Typing: Angle Brackets, Delimiter Skipping & Quotes
- **Purpose**: Intelligent angle bracket auto-closing, overtyping, backspacing, delimiter skipping, and quote pairing (ADR-029, ADR-030, ADR-031).
- **Key Classes**:
  - `dev.verloren.midnight.editor.CompactAngleBraceTypedHandler`: `TypedHandlerDelegate` pairing `<|>` on generic types, expressions, and headers; stepping over on `>`.
  - `dev.verloren.midnight.editor.CompactAngleBraceBackspaceHandler`: `BackspaceHandlerDelegate` deleting matching `>` when backspacing `<` in `<|>`.
  - `dev.verloren.midnight.editor.CompactDelimiterTypedHandler`: `TypedHandlerDelegate` skipping existing closing delimiters (`)`, `]`, `}`, `>`), structural punctuation (`:`, `;`, `,`), and closing quotes (`"`, `'`) without duplicate symbol insertion.
  - `dev.verloren.midnight.editor.CompactQuoteHandler`: `SimpleTokenSetQuoteHandler` providing double and single quote pairing, caret placement, and step-over.
  - `dev.verloren.midnight.completion.CompactParameterizedTypeInsertHandler`: Appends `<>`, positions caret inside and opens auto-popup for size options (`Uint`, `Bytes`, etc.).
- **Invariants**:
  - Zero false positives on comparison operators (`a < b`).
  - No pairing or skipping inside comments or existing string bodies.
  - Retains the cursor position inside angle brackets when live templates are active.

### 2.7 Live Template Macros & Auto-Numbering
- **Purpose**: Dynamic variable calculation, gap-filling declaration naming, and interactive type dropdowns (ADR-025, ADR-027, ADR-029).
- **Key Classes**:
  - `dev.verloren.midnight.ide.templates.CompactDeclarationNameGenerator`: Scope-aware auto-numbering generator (`circuit1`, `witness1`, etc.) with self-collision evasion.
  - `dev.verloren.midnight.ide.templates.CompactDeclarationType`: Declaration construct registry (`CIRCUIT`, `WITNESS`, `STRUCT`, `ENUM`, `MODULE`, `CONTRACT`, `TYPE`, `LEDGER`, `CONST`).
  - `dev.verloren.midnight.ide.templates.CompactDeclarationNameMacro`, `CompactCircuitNameMacro`, `CompactWitnessNameMacro`: Macros registered in `plugin.xml`.
  - `dev.verloren.midnight.ide.templates.CompactTypeMacro` & `CompactTypeExpression`: `compactType()` macro providing interactive type dropdown in live templates.
- **Invariants**:
  - Never mutates explicit user-provided identifiers.
  - Sibling scopes remain cleanly isolated.

### 2.8 Smart Enter & Intentions Suite
- **Purpose**: Non-destructive statement completion and contextual editor intentions (ADR-006, ADR-007, ADR-008).
- **Key Classes**:
  - `dev.verloren.midnight.editor.smartEnter.CompactSmartEnterProcessor`: `SmartEnterProcessor` for circuits, consts, types, and statement blocks.
  - `dev.verloren.midnight.editor.CompactDocCommentEnterHandler`: Clean `Enter` handling in block and doc comments.
  - `dev.verloren.midnight.editor.CompactDeclarationEnterHandler`: Automatic continuation in multi-line declarations.
  - Intention Actions: `CompactSwitchCompilerVersionIntention`, `CompactUpdatePragmaVersionIntention`, `CompactTogglePureCircuitIntention`, `CompactToggleExportIntention`, `CompactSurroundWithDiscloseIntention`, `CompactInvertIfIntention`, `CompactSpecifyTypeExplicitlyIntention`, `CompactRemoveRedundantTypeIntention`.
- **Invariants**:
  - Intention previews must never execute external compiler processes or mutate disk state (verified by `CompactQuickFixPreviewSideEffectTest`).

### 2.9 Semantic Inspections & Quick-Fixes
- **Purpose**: Real-time static analysis, offline toolchain prioritization, and automated quick fixes (ADR-004, ADR-034).
- **Key Classes**:
  - Ten local inspections: `CompactUnresolvedReferenceInspection`, `CompactDuplicateDeclarationInspection`, `CompactUnusedLocalVariableInspection`, `CompactTypeMismatchInspection`, `CompactPureCircuitInspection`, `CompactSealedFieldMutationInspection`, `CompactRecursiveCircuitInspection`, `CompactConstructorRestrictionInspection`, `CompactUndisclosedWitnessInspection`, `CompactPragmaVersionInspection`.
- **Invariants**:
  - Must guard against `PsiErrorElement` trees during typing.
  - Quick-fixes run in WriteCommandActions with undo support.
  - `CompactPragmaVersionInspection` prioritizes installed satisfying compilers descending before suggesting remote downloads (ADR-034).

### 2.10 Formatter & Indentation Model
- **Purpose**: Code style formatting (`Ctrl + Alt + L`) and automatic indent on Enter (ADR-005).
- **Key Classes**:
  - `dev.verloren.midnight.formatter.CompactFormattingModelBuilder`, `CompactBlock`, `CompactLanguageCodeStyleSettingsProvider`.
- **Invariants**:
  - Idempotent formatting: `format(format(code)) == format(code)`. Canonical 2-space indentation.

### 2.11 Toolchain, Run Configurations & External Linter
- **Purpose**: Compiler execution, gutter play markers, and background diagnostics (ADR-011, ADR-015, ADR-016).
- **Key Classes**:
  - `dev.verloren.midnight.run.CompactToolchainUtil`: Toolchain discovery across Linux, macOS, native Windows, and WSL; evades `C:\Windows\System32\compact.exe`.
  - `dev.verloren.midnight.run.CompactConfigurationType`, `CompactRunConfiguration`, `CompactRunConfigurationProducer`, `CompactRunLineMarkerContributor`.
  - `dev.verloren.midnight.annotator.CompactExternalAnnotator`: Asynchronous 3-phase annotator with on-demand shadow buffer compilation and WSL path translation.
- **Invariants**:
  - External compiler execution must be asynchronous and never block EDT.
  - Active editor documents are compiled from in-memory buffers to prevent caret jumps and trailing whitespace loss.

### 2.12 Status Bar Widget & Remix Compiler Tool Window
- **Purpose**: Persistent compiler state monitoring, rapid switching, synchronised card selection, and contract compilation (ADR-012, ADR-013, ADR-014).
- **Key Classes**:
  - `dev.verloren.midnight.statusbar.CompactStatusBarWidgetFactory`, `CompactStatusBarWidget`, `CompactStatusBarPopup`.
  - `dev.verloren.midnight.toolwindow.CompactCompilerToolWindowFactory`, `CompactCompilerPanel`.
  - `dev.verloren.midnight.version.CompactVersionManager`, `CompactSemVerUtil`.
- **Invariants**:
  - Multi-version compilers isolated under `~/.compact/versions/<version>/`.
  - Per-project compiler persistence in `.idea/midnight.xml`.
  - Compiler tool window card selection synchronised with `MidnightProjectSettings` and active editor files.

### 2.13 File Templates & Dynamic Properties Provider
- **Purpose**: File generation with dynamic pragma version resolution (ADR-010, ADR-033).
- **Key Classes**:
  - `dev.verloren.midnight.ide.fileTemplates.CompactDefaultTemplatePropertiesProvider`: Registered in `plugin.xml` implementing `DefaultTemplatePropertiesProvider` to inject active compiler/language version variables.
  - `dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory`: Factory managing bundled internal file templates (`Compact File`, `Compact Contract`, `Compact Module`, `Compact Interface`).
  - `dev.verloren.midnight.actions.CompactCreateFileAction`: Action creating new Compact files with directory sanitisation.
- **Invariants**:
  - Dynamic fallback to language `0.26.0` (compiler `0.34.0`) when unconfigured.
  - Template generation avoids hardcoded pragma directives in template bodies.

### 2.14 Standard Library Virtual VFS & Documentation
- **Purpose**: Standard library declaration navigation, virtual indexing, and rich hover documentation (ADR-017, ADR-023).
- **Key Classes**:
  - `dev.verloren.midnight.stdlib.CompactStdlibService`: Application service providing virtual access to bundled `standard-library.compact`.
  - `dev.verloren.midnight.psi.impl.CompactImportDeclarationImpl` & `CompactImportReference`: Resolves `import CompactStandardLibrary;` to virtual stdlib PSI.
  - `dev.verloren.midnight.documentation.CompactDocumentationProvider`: Quick Documentation (Ctrl+Q / F1) for stdlib circuits, types, and directives.
- **Invariants**:
  - Stdlib files are read-only and bundled directly in plugin resources.

---

## 3. Threading, Background Execution & Lifecycle Architecture

### 3.1 Operation vs. Thread Context Matrix

| Operation Category    | Required Thread / Context                | Mechanism / API                                         | Critical Constraints                                               |
|:----------------------|:-----------------------------------------|:--------------------------------------------------------|:-------------------------------------------------------------------|
| **PSI Read**          | Background Thread or EDT with ReadAction | `ReadAction.compute()`                                  | Never block waiting on external locks.                             |
| **PSI Mutation**      | **EDT only** with WriteAction & Command  | `WriteCommandAction.runWriteCommandAction()`            | Modifying PSI off-EDT throws `IllegalStateException`.              |
| **VFS Read**          | Any thread with ReadAction               | `VirtualFile.findChild()`                               | Avoid expensive disk operations under ReadAction.                  |
| **VFS Write**         | **EDT only** with WriteAction            | `WriteAction.run()`                                     | Direct disk writes bypass VFS unless followed by refresh.          |
| **VFS Refresh**       | Any thread (asynchronous preferred)      | `VfsUtil.markDirtyAndRefresh(true, ...)`                | **NEVER** call synchronous refresh on EDT.                         |
| **UI Updates**        | **EDT only**                             | `ApplicationManager.getApplication().invokeLater()`     | Never manipulate Swing components from background threads.         |
| **Process Execution** | **Background Thread only**               | `Task.Backgroundable`, `ExternalAnnotator.doAnnotate()` | **NEVER execute blocking process calls on EDT.**                   |
| **Services**          | Any thread (thread-safe)                 | `@Service` + `getInstance()`                            | Double-checked locking / volatile fields.                          |
| **Index Queries**     | Any thread with ReadAction               | `DumbService.isDumb(project)` guard                     | Accessing indexes during indexing throws `IndexNotReadyException`. |

### 3.2 Extension Points & Threading Catalogue

| Extension Point                          | Implementation Class                       | Thread / Context                | DumbAware | Purpose                                          |
|:-----------------------------------------|:-------------------------------------------|:--------------------------------|:----------|:-------------------------------------------------|
| `<fileType>`                             | `CompactFileType`                          | App Startup                     | Yes       | Binds `.compact` extension to language           |
| `<lang.parserDefinition>`                | `CompactParserDefinition`                  | Any thread (ReadAction)         | Yes       | Lexer, parser, and PSI AST nodes                 |
| `<lang.syntaxHighlighterFactory>`        | `CompactSyntaxHighlighterFactory`          | Any thread                      | Yes       | Lexer-based token syntax coloring                |
| `<colorSettingsPage>`                    | `CompactColorSettingsPage`                 | EDT (Settings dialog)           | Yes       | Color scheme customization page                  |
| `<annotator>`                            | `CompactHighlightingAnnotator`             | Background (ReadAction)         | No        | Fast semantic syntax highlighting                |
| `<externalAnnotator>`                    | `CompactExternalAnnotator`                 | Background (3-phase pipeline)   | No        | Upstream `compactc` background linting           |
| `<completion.contributor>`               | `CompactCompletionContributor`             | Background (ReadAction)         | No        | Contextual code autocompletion                   |
| `<lang.findUsagesProvider>`              | `CompactFindUsagesProvider`                | Background (ReadAction)         | No        | Find Usages and words scanner                    |
| `<lang.namesValidator>`                  | `CompactNamesValidator`                    | Pure string logic (any thread)  | Yes       | Validates identifiers & rejects keywords         |
| `<lang.refactoringSupport>`              | `CompactRefactoringSupportProvider`        | EDT (ReadAction)                | No        | In-place rename refactoring                      |
| `<lang.formatter>`                       | `CompactFormattingModelBuilder`            | Background (ReadAction)         | No        | Code formatting (`Ctrl + Alt + L`)               |
| `<langCodeStyleSettingsProvider>`        | `CompactLanguageCodeStyleSettingsProvider` | EDT (Settings dialog)           | Yes       | Code style settings definitions                  |
| `<lang.psiStructureViewFactory>`         | `CompactStructureViewFactory`              | EDT (ReadAction)                | No        | Structure view visual tree                       |
| `<lang.documentationProvider>`           | `CompactDocumentationProvider`             | Background (ReadAction)         | No        | Quick documentation hover (`Ctrl + Q`)           |
| `<lang.commenter>`                       | `CompactCommenter`                         | Any thread                      | Yes       | Line (`//`) and block (`/* */`) comments         |
| `<lang.braceMatcher>`                    | `CompactPairedBraceMatcher`                | Any thread                      | Yes       | Bracket pairing for `{}`, `[]`, `()`             |
| `<lang.quoteHandler>`                    | `CompactQuoteHandler`                      | EDT                             | Yes       | Double and single quote auto-pairing & step-over |
| `<typedHandler>`                         | `CompactAngleBraceTypedHandler`            | EDT                             | Yes       | Angle bracket auto-closing & overtyping          |
| `<typedHandler>`                         | `CompactDelimiterTypedHandler`             | EDT                             | Yes       | Delimiter, colon, semicolon, & quote skipping    |
| `<backspaceHandlerDelegate>`             | `CompactAngleBraceBackspaceHandler`        | EDT                             | Yes       | Balanced angle bracket backspace deletion        |
| `<lang.smartEnterProcessor>`             | `CompactSmartEnterProcessor`               | EDT (WriteCommandAction)        | No        | Non-destructive `Ctrl+Shift+Enter`               |
| `<enterHandlerDelegate>`                 | `CompactDocCommentEnterHandler`            | EDT (WriteCommandAction)        | No        | Multi-asterisk comment scaffolding               |
| `<enterHandlerDelegate>`                 | `CompactDeclarationEnterHandler`           | EDT (WriteCommandAction)        | No        | Declaration line continuation                    |
| `<gotoClassContributor>`                 | `CompactGotoClassContributor`              | Background (ReadAction)         | Yes       | `Ctrl + N` navigation to contracts/structs       |
| `<gotoSymbolContributor>`                | `CompactGotoSymbolContributor`             | Background (ReadAction)         | Yes       | `Ctrl + Alt + Shift + N` navigation              |
| `<gotoDeclarationHandler>`               | `CompactGotoDeclarationHandler`            | Background (ReadAction)         | No        | `Ctrl + Click` navigation to declarations        |
| `<typeDeclarationProvider>`              | `CompactTypeDeclarationProvider`           | Background (ReadAction)         | No        | `Ctrl + Shift + B` navigation to types           |
| `<psi.referenceContributor>`             | `CompactReferenceContributor`              | Background (ReadAction)         | No        | Direct `PsiReference` injection                  |
| `<configurationType>`                    | `CompactConfigurationType`                 | Any thread                      | Yes       | Run Configuration descriptor                     |
| `<runConfigurationProducer>`             | `CompactRunConfigurationProducer`          | Background (ReadAction)         | No        | Contextual run configuration producer            |
| `<runLineMarkerContributor>`             | `CompactRunLineMarkerContributor`          | Background (ReadAction)         | No        | Gutter play buttons on contracts                 |
| `<applicationService>`                   | `MidnightSettingsState`                    | Thread-safe service             | Yes       | Persistent compiler settings                     |
| `<applicationConfigurable>`              | `MidnightSettingsConfigurable`             | EDT                             | Yes       | Settings UI under Languages                      |
| `<toolWindow>`                           | `CompactCompilerToolWindowFactory`         | EDT                             | Yes       | Remix-style compiler panel on right stripe       |
| `<statusBarWidgetFactory>`               | `CompactStatusBarWidgetFactory`            | EDT                             | Yes       | Status bar toolchain & version widget            |
| `<notificationGroup>`                    | Midnight Notifications                     | App Startup                     | Yes       | Balloon notification group                       |
| `<intentionAction>` (x8)                 | 2 pragma + 6 editor intentions             | EDT (ReadAction / WriteCommand) | No        | `Alt + Enter` contextual editor actions          |
| `<defaultTemplatePropertiesProvider>`    | `CompactDefaultTemplatePropertiesProvider` | Any thread                      | Yes       | Dynamic file template properties provider        |
| `<fileTemplateGroup>`                    | `CompactFileTemplateGroupFactory`          | Any thread                      | Yes       | File templates descriptor                        |
| `<internalFileTemplate>` (x4)            | Contract, Module, Interface, File          | Any thread                      | Yes       | Bundled file templates                           |
| `<defaultLiveTemplates>`                 | `/liveTemplates/Compact.xml`               | App Startup                     | Yes       | Bundled live code snippets                       |
| `<liveTemplateContext>`                  | `CompactLiveTemplateContextType`           | Any thread                      | Yes       | Scopes live templates to Compact code            |
| `<liveTemplateMacro>` (x4)               | DeclarationName, Circuit, Witness, Type    | Any thread                      | Yes       | Live template dynamic macro expressions          |
| `<lang.foldingBuilder>`                  | `CompactFoldingBuilder`                    | Background (ReadAction)         | Yes       | Code folding for blocks, comments                |
| `<breadcrumbsInfoProvider>`              | `CompactBreadcrumbsProvider`               | EDT (ReadAction)                | No        | Scope breadcrumb navigation bar                  |
| `<spellchecker.support>`                 | `CompactSpellcheckingStrategy`             | Background (ReadAction)         | Yes       | Spellchecking for identifier tokens              |
| `<lang.surroundDescriptor>`              | `CompactSurroundDescriptor`                | EDT (WriteCommandAction)        | No        | Surround with block or `if` (`Ctrl+Alt+T`)       |
| `<codeInsight.declarativeInlayProvider>` | `CompactInlayHintsProvider`                | Background (ReadAction)         | No        | Declarative inline parameter hints               |
| `<codeInsight.lineMarkerProvider>`       | `CompactLineMarkerProvider`                | Background (ReadAction)         | No        | Gutter icons for `witness`, `disclose`           |
| `<codeInsight.parameterInfo>`            | `CompactParameterInfoHandler`              | Background (ReadAction) / EDT   | No        | Parameter info tooltip (`Ctrl + P`)              |
| `<errorHandler>`                         | `JetBrainsMarketplaceErrorReportSubmitter` | EDT                             | Yes       | Marketplace exception reporter                   |
| `<localInspection>` (x10)                | 10 Semantic Inspection classes             | Background (ReadAction)         | No        | Static analysis checks & quick fixes             |
| `<action>` (`NewGroup`)                  | `CompactCreateFileAction`                  | EDT (Action execution)          | Yes       | New Compact File dialog & creation               |

---

## 4. Test Structure & Strategy

All 65 test suites extend IntelliJ test base classes (`ParsingTestCase` or `BasePlatformTestCase`):

| Test Class                                                                  | Category                | Base Class             | Test Count |
|:----------------------------------------------------------------------------|:------------------------|:-----------------------|:-----------|
| `dev.verloren.midnight.inspection.CompactInspectionTest`                    | Inspections & Fixes     | `BasePlatformTestCase` | 99         |
| `dev.verloren.midnight.completion.CompactCompletionTest`                    | Code Completion         | `BasePlatformTestCase` | 80         |
| `dev.verloren.midnight.formatter.CompactFormatterTest`                      | Formatter & Indent      | `BasePlatformTestCase` | 39         |
| `dev.verloren.midnight.editor.CompactDelimiterTypingTest`                   | Delimiter Skipping      | `BasePlatformTestCase` | 30         |
| `dev.verloren.midnight.editor.CompactAngleBraceTypingTest`                  | Angle Bracket Typing    | `BasePlatformTestCase` | 24         |
| `dev.verloren.midnight.documentation.CompactDocumentationTest`              | Hover Documentation     | `BasePlatformTestCase` | 22         |
| `dev.verloren.midnight.resolve.CompactResolveTest`                          | Scope & Resolution      | `BasePlatformTestCase` | 21         |
| `dev.verloren.midnight.ide.templates.CompactLiveTemplateTest`               | Live Templates          | `BasePlatformTestCase` | 19         |
| `dev.verloren.midnight.editor.CompactQuoteTypingTest`                       | Quote Typing            | `BasePlatformTestCase` | 18         |
| `dev.verloren.midnight.resolve.CompactCrossFileResolveTest`                 | Scope & Resolution      | `BasePlatformTestCase` | 17         |
| `dev.verloren.midnight.highlighter.CompactHighlightingTest`                 | Syntax Highlighting     | `BasePlatformTestCase` | 16         |
| `dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateTest`           | File Templates          | `BasePlatformTestCase` | 15         |
| `dev.verloren.midnight.type.CompactTypeInferenceTest`                       | Type Inference          | `BasePlatformTestCase` | 15         |
| `dev.verloren.midnight.editor.CompactSmartEnterTest`                        | Smart Enter             | `BasePlatformTestCase` | 15         |
| `dev.verloren.midnight.navigation.CompactTypeDeclarationProviderTest`       | Type Navigation         | `BasePlatformTestCase` | 14         |
| `dev.verloren.midnight.intention.CompactPhase28IntentionsTest`              | Editor Intentions       | `BasePlatformTestCase` | 12         |
| `dev.verloren.midnight.lexer.LexerTest`                                     | Tokenization            | Standalone JUnit 4     | 12         |
| `dev.verloren.midnight.parameterInfo.CompactParameterInfoHandlerTest`       | Parameter Info          | `BasePlatformTestCase` | 12         |
| `dev.verloren.midnight.annotator.CompactExternalAnnotatorTest`              | External Linter         | `BasePlatformTestCase` | 11         |
| `dev.verloren.midnight.findUsages.CompactFindUsagesTest`                    | Find Usages             | `BasePlatformTestCase` | 10         |
| `dev.verloren.midnight.ide.templates.CompactDeclarationNameGeneratorTest`   | Template Name Generator | `BasePlatformTestCase` | 10         |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTemplateTriggerTest` | Template Triggers       | `BasePlatformTestCase` | 10         |
| `dev.verloren.midnight.reference.CompactReferenceTest`                      | Scope & References      | `BasePlatformTestCase` | 9          |
| `dev.verloren.midnight.editor.CompactDocCommentEnterTest`                   | Doc Comment Enter       | `BasePlatformTestCase` | 9          |
| `dev.verloren.midnight.stdlib.CompactStandardLibraryTest`                   | Standard Library        | `BasePlatformTestCase` | 9          |
| `dev.verloren.midnight.structure.CompactStructureViewTest`                  | Structure View          | `BasePlatformTestCase` | 9          |
| `dev.verloren.midnight.refactoring.CompactRenameTest`                       | Refactoring & Rename    | `BasePlatformTestCase` | 9          |
| `dev.verloren.midnight.inspection.CompactPragmaVersionInspectionTest`       | Inspections & Fixes     | `BasePlatformTestCase` | 8          |
| `dev.verloren.midnight.version.CompactVersionManagerTest`                   | Version Manager         | `BasePlatformTestCase` | 7          |
| `dev.verloren.midnight.editor.CompactLineMarkerTest`                        | Gutter Line Markers     | `BasePlatformTestCase` | 7          |
| `dev.verloren.midnight.run.CompactToolchainUtilTest`                        | Toolchain Discovery     | `BasePlatformTestCase` | 6          |
| `dev.verloren.midnight.parser.ErrorRecoveryParserTest`                      | Parsing & AST           | `ParsingTestCase`      | 6          |
| `dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolverTest` | Trigger Resolver        | `BasePlatformTestCase` | 6          |
| `dev.verloren.midnight.editor.CompactSurroundWithTest`                      | Surround With           | `BasePlatformTestCase` | 5          |
| `dev.verloren.midnight.run.CompactRunConfigurationTest`                     | Run Configurations      | `BasePlatformTestCase` | 5          |
| `dev.verloren.midnight.statusbar.CompactStatusBarWidgetTest`                | Status Bar Widget       | `BasePlatformTestCase` | 5          |
| `dev.verloren.midnight.editor.CompactFoldingTest`                           | Code Folding            | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.completion.CompactInsertHandlersTest`                | Completion Handlers     | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.editor.CompactEditorFeaturesTest`                    | Quote & Brace Matching  | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.toolwindow.CompactCompilerPanelTest`                 | Remix Compiler UI       | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.version.CompactSemVerUtilTest`                       | SemVer Evaluator        | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.annotator.CompactQuickFixPreviewSideEffectTest`      | Annotator Previews      | `BasePlatformTestCase` | 4          |
| `dev.verloren.midnight.symbol.CompactSymbolTest`                            | Symbol Navigation       | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.settings.MidnightSettingsTest`                       | Settings & State        | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.settings.MidnightProjectSettingsTest`                | Per-Project Settings    | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.toolwindow.CompactVersionCardTest`                   | Remix Compiler UI       | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.stdlib.CompactStdlibServiceTest`                     | Stdlib Virtual VFS      | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.CompactTestUtilsTest`                                | Test Utilities          | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.lexer.PragmaTest`                                    | Tokenization            | Standalone JUnit 4     | 3          |
| `dev.verloren.midnight.parser.StatementParserTest`                          | Parsing & AST           | `ParsingTestCase`      | 3          |
| `dev.verloren.midnight.parser.PragmaParserTest`                             | Parsing & AST           | `ParsingTestCase`      | 3          |
| `dev.verloren.midnight.editor.CompactInlayHintsTest`                        | Inlay Parameter Hints   | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.intention.CompactPragmaIntentionTest`                | Pragma Intentions       | `BasePlatformTestCase` | 3          |
| `dev.verloren.midnight.run.CompactRunConfigurationProducerTest`             | Run Configurations      | `BasePlatformTestCase` | 2          |
| `dev.verloren.midnight.psi.DeclarationPsiTest`                              | PSI Structure           | `BasePlatformTestCase` | 2          |
| `dev.verloren.midnight.parser.EndToEndParserTest`                           | Parsing & AST           | `ParsingTestCase`      | 2          |
| `dev.verloren.midnight.CompactBundleTest`                                   | Message Bundle          | `BasePlatformTestCase` | 2          |
| `dev.verloren.midnight.parser.CompactParserDefinitionTest`                  | Parsing & AST           | `ParsingTestCase`      | 2          |
| `dev.verloren.midnight.navigation.CompactChooseByNameTest`                  | Symbol Navigation       | `BasePlatformTestCase` | 2          |
| `dev.verloren.midnight.editor.CompactBreadcrumbsTest`                       | Scope Breadcrumbs       | `BasePlatformTestCase` | 2          |
| `dev.verloren.midnight.psi.ElementFactoryConsistencyTest`                   | PSI Structure           | `BasePlatformTestCase` | 1          |
| `dev.verloren.midnight.parser.TypePatternParserTest`                        | Parsing & AST           | `ParsingTestCase`      | 1          |
| `dev.verloren.midnight.parser.ExpressionParserTest`                         | Parsing & AST           | `ParsingTestCase`      | 1          |
| `dev.verloren.midnight.parser.DeclarationParserTest`                        | Parsing & AST           | `ParsingTestCase`      | 1          |
| `dev.verloren.midnight.highlighter.CompactColorSettingsPageTest`            | Color Settings          | `BasePlatformTestCase` | 1          |
| **Total Across 65 Suites**                                                  |                         |                        | **697**    |

---

## 5. Critical Invariants & Immutable Core

The following architectural components are mature, verified, and **MUST NOT be rewritten, replaced, or degraded**:

1. **Handwritten Recursive-Descent Lexer & Parser**:
   - `CompactLexer` and `CompactParser` are fully tested and handle incomplete code cleanly (ADR-001). Do not replace them with GrammarKit (`.bnf`), Antlr, or generated parsers.
2. **Lexical Scope Resolver (`CompactResolveUtil`)**:
   - All symbol lookups (in-file, includes, standard library) must route through or extend `CompactResolveUtil` (ADR-002). Do not introduce ad-hoc AST walkers that bypass namespace separation (`VALUE` vs `TYPE`) or innermost lexical shadowing.
3. **Tolerance for Incomplete Code**:
   - Every PSI wrapper, inspection visitor, formatter block, and structure view element must guard against `null` children, missing identifiers, and `PsiErrorElement` nodes.
4. **Threading Separation**:
   - Never execute external compiler processes or blocking disk operations on the EDT.
   - Never mutate PSI or VFS state outside a `WriteCommandAction` on the EDT.
5. **Zero Test Regressions**:
   - All **697 unit tests** across all **65 test suites** must pass (`./gradlew test`) before any task or feature is marked complete.
6. **Reference Code Discipline**:
   - Reference repositories (`compact/`, `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`) are read-only references. Never edit them or import them wholesale into the plugin build.
7. **Architectural Decision Rigour**:
   - Every major subsystem design or semantic change must be accompanied by an ADR in `.ai/decisions/` citing upstream compiler sources and workspace reference implementations (ADR-001 through ADR-034).
