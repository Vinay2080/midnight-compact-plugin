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
[Semantic Layer] CompactResolveUtil (Single-file AST scope walker) + CompactTypeInferenceUtil
  ↓
[IDE Features]
  ├── References & Navigation (Go To Declaration, Find Usages)
  ├── Completion (CompactCompletionContributor)
  ├── Refactoring & Rename (CompactRefactoringSupportProvider, CompactNamesValidator)
  ├── Semantic Inspections (Unresolved Ref, Duplicate Decl, Unused Local, Type Mismatch)
  └── Code Style (CompactFormattingModelBuilder, CompactBlock, Indentation)
```

---

## 2. Subsystems

### 2.1 Lexer
- **Purpose**: Tokenize a Compact source into IntelliJ `IElementType` tokens.
- **Key Classes**:
  - `dev.verloren.midnight.lexer.CompactLexer`: Handwritten lexer extending `LexerBase`.
  - `dev.verloren.midnight.lexer.CompactTokenTypes`: All token types (keywords, primitives, operators, delimiters, literals, comments, whitespace).
- **Depends on**: IntelliJ Platform Lexer APIs.
- **Used by**: `CompactParserDefinition`, `CompactSyntaxHighlighter`, `CompactWordsScanner`.
- **Invariants**:
  - Keywords, primitives, and operators are categorized during lexing.
  - Must remain robust and never crash or hang on arbitrary/malformed character input.

### 2.2 Parser & AST
- **Purpose**: Parse token stream into an AST with rich error recovery.
- **Key Classes**:
  - `dev.verloren.midnight.parser.CompactParser`: Recursive-descent parser implementing `PsiParser`.
  - `dev.verloren.midnight.parser.CompactParserDefinition`: Plugin integration with IntelliJ PSI infrastructure.
  - `dev.verloren.midnight.psi.CompactElementTypes`: Node element types (`CIRCUIT_DEFINITION`, `WITNESS_DECLARATION`, `BLOCK`, `IF_STATEMENT`, `BINARY_EXPR`, etc.).
- **Depends on**: `CompactTokenTypes`, `CompactElementTypes`.
- **Used by**: `CompactParserDefinition`, IDE PSI build pass.
- **Invariants**:
  - Hand-crafted recursive descent with explicit error recovery markers.
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
  - `src/main/gen` is treated as an editable project source if present, but the current implementation resides in `src/main/java`.

### 2.4 Reference Resolution & Scoping
- **Purpose**: Resolve identifiers to their declaration elements.
- **Key Classes**:
  - `dev.verloren.midnight.resolve.CompactResolveUtil`: Scope tree walker with namespace separation.
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
  - Single-file resolution using `PsiTreeUtil` and AST traversal; no external index dependencies yet.
  - Soft-unresolved handling for external includes or builtins that are not defined in the local file.

### 2.5 Code Completion
- **Purpose**: Provide context-aware code completion.
- **Key Classes**:
  - `dev.verloren.midnight.completion.CompactCompletionContributor`: IntelliJ `CompletionContributor`.
- **Completion Modes**:
  - Keyword completion (declaration keywords at file level, statement keywords in blocks).
  - Expression values (in-scope value declarations, keywords like `true`, `false`, `self`).
  - Type completion (primitive types, in-scope structs, enums, type aliases, generic type parameters).
  - Enum member completion (triggered after `Enum.`).
- **Invariants**:
  - Fast, single-pass scope collection; must never block the UI thread.

### 2.6 Refactoring & Search
- **Purpose**: Safe symbol renaming and usage search.
- **Key Classes**:
  - `dev.verloren.midnight.refactoring.CompactRenameHandler` / `CompactRefactoringSupportProvider`.
  - `dev.verloren.midnight.refactoring.CompactNamesValidator`: Validates identifier tokens and rejects keywords.
  - `dev.verloren.midnight.findUsages.CompactFindUsagesProvider`: Integrates with IntelliJ Find Usages and words scanner.
- **Invariants**:
  - Reject Compact reserved keywords as identifiers during rename.
  - `setName` uses `CompactElementFactory` to replace leaf identifier tokens cleanly.

### 2.7 Type Inference System
- **Purpose**: Lightweight local type inference for editor features.
- **Key Classes**:
  - `dev.verloren.midnight.type.CompactType`: Common type interface.
  - `dev.verloren.midnight.type.CompactPrimitiveType`: Singleton constants for `BOOLEAN`, `FIELD`, `UINT`, `BYTES`, `OPAQUE`, `VOID`, `UNKNOWN`.
  - `dev.verloren.midnight.type.CompactNamedType`: Struct and enum nominal types.
  - `dev.verloren.midnight.type.CompactTypeInferenceUtil`: Evaluates types for expressions (literals, binary ops, calls, member access, casts).
  - `dev.verloren.midnight.psi.CompactExpression`: Base interface for typed PSI expression nodes.
  - `dev.verloren.midnight.psi.CompactTypeElement`: Interface for typed declarations (`getType()`).
- **Invariants**:
  - Lightweight and single-file; returns `CompactPrimitiveType.UNKNOWN` for complex uninferable or external constructs to avoid false positives.

### 2.8 Semantic Inspections & Quick-Fixes
- **Purpose**: Static analysis and quick fixes registered via `<localInspection>`.
- **Key Classes**:
  - `dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection`: Flags unresolved identifiers and fields (excluding soft-unresolved builtins).
  - `dev.verloren.midnight.inspection.CompactDuplicateDeclarationInspection`: Identifies duplicate sibling declarations in the same scope.
  - `dev.verloren.midnight.inspection.CompactUnusedLocalVariableInspection`: Flags unused local variables and provides `CompactRemoveUnusedVariableFix`.
  - `dev.verloren.midnight.inspection.CompactTypeMismatchInspection`: Type checking for booleans in conditionals, logical operators, and equality compatibility.
- **Invariants**:
  - Must guard against `PsiErrorElement` trees and incomplete code during editing.
  - Quick-fixes must use IntelliJ write commands and maintain formatting.

### 2.9 Formatter & Smart Indentation
- **Purpose**: Code formatting (`Ctrl + Alt + L`) and automatic indent on Enter.
- **Key Classes**:
  - `dev.verloren.midnight.formatter.CompactFormattingModelBuilder`: Creates `DocumentBasedFormattingModel`.
  - `dev.verloren.midnight.formatter.CompactBlock`: AST block computing spacing and child indentation.
  - `dev.verloren.midnight.formatter.CompactLanguageCodeStyleSettingsProvider`: Configures 2-space canonical indentation.
- **Invariants**:
  - Formatting must be idempotent: `format(format(x)) == format(x)`.
  - Handle malformed trees gracefully without throwing exceptions.

### 2.10 Structure View & Documentation Provider
- **Purpose**: File structure navigation and hover documentation (`Ctrl + Q`).
- **Key Classes**:
  - `dev.verloren.midnight.structure.CompactStructureViewFactory`, `CompactStructureViewModel`, `CompactStructureViewElement`.
  - `dev.verloren.midnight.documentation.CompactDocumentationProvider`, `CompactDocComment`.
- **Invariants**:
  - Render documentation in standard `DocumentationMarkup` sections with Markdown formatting.
  - Structure elements must guard against null declaration identifiers.

### 2.11 Toolchain, Run Configurations & External Linter
- **Purpose**: Local & WSL compiler execution, run configurations with gutter play buttons, and background diagnostics.
- **Key Classes**:
  - `dev.verloren.midnight.run.CompactToolchainUtil`: WSL and native compiler binary detection and path translation.
  - `dev.verloren.midnight.run.CompactConfigurationType`, `CompactRunConfiguration`, `CompactRunConfigurationProducer`.
  - `dev.verloren.midnight.annotator.CompactExternalAnnotator`, `CompactCompilerOutputParser`.
- **Invariants**:
  - Compiler execution must be asynchronous and never block EDT.
  - Deterministic per-contract output directories (`gen/<contract-path>`) to prevent artifact collision.

### 2.12 Semantic Gutter Markers & Bundled Standard Library
- **Purpose**: Privacy visualizer gutter icons and built-in standard library symbol resolution.
- **Key Classes**:
  - `dev.verloren.midnight.editor.CompactLineMarkerProvider`: Gutter icons for `witness`, `disclose`, `circuit`, `ledger`.
  - `dev.verloren.midnight.stdlib.CompactStdlibService`, `CompactStandardLibraryProvider`: Bundled `standard-library.compact` and `zkir-v3-library.compact` virtual files.
- **Invariants**:
  - Standard library initialization is deterministic with `0L` timestamp.
  - User-defined symbols lexically shadow standard library definitions.

---

## 3. Threading, Background Execution & Lifecycle Architecture

### 3.1 Operation vs. Thread Context Matrix

The following rules govern concurrency and execution across the plugin:

| Operation Category             | Required Thread / Context                              | Mechanism / API                                                                                | Existing Plugin Example                                                          | Critical Constraints & Pitfalls                                                                                              |
|:-------------------------------|:-------------------------------------------------------|:-----------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------|
| **PSI Read**                   | Background Thread or EDT with **ReadAction**           | Automatically active in inspections, completion, annotators; manual via `ReadAction.compute()` | `CompactResolveUtil.resolve()`, `CompactTypeInferenceUtil.inferType()`           | Reading PSI without ReadAction throws `ProcessCanceledException` or `AssertionError`. Never block waiting on external locks. |
| **PSI Mutation**               | **EDT only** with **WriteAction** & Command            | `WriteCommandAction.runWriteCommandAction(project, () -> ...)`                                 | `CompactRemoveUnusedVariableFix.applyFix()`, `CompactNamedElementImpl.setName()` | Modifying PSI on a background thread throws `IllegalStateException`. Mutations must be recorded in undo/redo stack.          |
| **VFS Read**                   | Any thread with **ReadAction**                         | `VirtualFile.findChild()`, `VirtualFile.getPath()`                                             | `CompactExternalAnnotator.collectInformation()`                                  | Avoid expensive disk operations or blocking I/O while holding a ReadAction.                                                  |
| **VFS Write**                  | **EDT only** with **WriteAction**                      | `WriteAction.run(...)` or `WriteCommandAction`                                                 | File creation actions via platform handlers                                      | Direct physical file writes (`new FileOutputStream`) bypass VFS unless followed by a refresh.                                |
| **VFS Refresh**                | Any thread (asynchronous strongly preferred)           | `VfsUtil.markDirtyAndRefresh(true, ...)`                                                       | Compiler output directory synchronization                                        | **NEVER** call synchronous refresh (`async = false`) on EDT; it freezes the UI while scanning the disk.                      |
| **UI Updates / Dialogs**       | **EDT only**                                           | `ApplicationManager.getApplication().invokeLater(...)`                                         | `CompactCreateFileAction.buildDialog()`, `MidnightSettingsComponent`             | Never touch Swing/JComponent hierarchies or open modal dialogs from a background worker thread.                              |
| **External Process Execution** | **Background Thread only**                             | `Task.Backgroundable`, `ExternalAnnotator.doAnnotate()`, `CommandLineState.startProcess()`     | `CompactExternalAnnotator.doAnnotate()`, `CompactRunProfileState.startProcess()` | **NEVER execute `process.waitFor()` or blocking CLI operations on EDT.** Doing so locks the entire IDE UI.                   |
| **Project / App Services**     | Any thread (service methods must ensure thread-safety) | `@Service` + `getInstance()`                                                                   | `CompactStdlibService` (Project), `MidnightSettingsState` (App)                  | Thread-safe retrieval. State mutation must use internal locks or volatile fields (e.g. double-checked locking).              |
| **Index Queries / Stubs**      | Any thread with ReadAction outside Dumb Mode           | `DumbService.isDumb(project)` guard                                                            | Future StubIndex cross-file symbol resolution                                    | Accessing indexes during indexing without `DumbAware` throws `IndexNotReadyException`.                                       |
| **Long Computations**          | Background thread with cancellation support            | `ProgressManager.getInstance().run(new Task.Backgroundable(...) { ... })`                      | External linting, bulk file resolution                                           | Must periodically invoke `ProgressManager.checkCanceled()` to respond to user keystrokes and cancellations.                  |

---

### 3.2 External Process Execution & WSL Boundaries

When invoking external Compact tools (`compact`, `compactc`):

1. **Toolchain Discovery (`CompactToolchainUtil`)**:
   - Searches settings, project `node_modules/.bin`, WSL distributions, and system PATH.
   - **Critical Trap**: On Windows, `compact.exe` exists in `C:\Windows\System32\compact.exe` as the native NTFS compression tool. `CompactToolchainUtil` specifically prioritizes WSL and filters out Windows system directories to prevent accidentally launching the NTFS compression utility.
2. **Command Line Construction**:
   - Always route through `CompactToolchainUtil.createCommandLine(project, args, workingDir)`.
   - WSL paths are automatically translated (`C:\path` -> `/mnt/c/path`).
3. **Execution & Handlers**:
   - **Run Configurations**: Managed via `CommandLineState` and `OSProcessHandler`. Output is streamed to the Run Console with `CompactConsoleFilter` parsing hyperlinked error locations (`file:line:col`).
   - **Background Linter (`CompactExternalAnnotator`)**: Executed via background thread with explicit timeouts (`process.waitFor(5, TimeUnit.SECONDS)`). If the timeout expires or execution is canceled, `process.destroyForcibly()` is called immediately to prevent orphan daemon processes.
4. **Cancellation**:
   - External processes must be aborted immediately if the enclosing `ProgressIndicator` is canceled or the parent `Disposable` is disposed of.

---

### 3.3 Caching & Lifecycle Invariants

1. **PSI-Derived Value Caching**:
   - Use `CachedValuesManager.getCachedValue(element, () -> Result.create(value, PsiModificationTracker.MODIFICATION_COUNT))`.
   - Concrete Example: [`CompactIncludeDeclarationImpl.resolveIncludedFile()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactIncludeDeclarationImpl.java).
2. **Reference Resolution Caching**:
   - Leverage `ResolveCache.getInstance(project).resolveWithCaching(this, RESOLVER, needToPreventRecursion, incompleteCode)`.
   - Set `needToPreventRecursion = true` to protect against circular symbol graphs.
   - Concrete Example: [`CompactReferenceBase`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactReferenceBase.java).
3. **Recursion Guards in Type Inference**:
   - Guard against recursive structural evaluation using `RecursionGuard<PsiElement>` created via `RecursionManager.createGuard(...)`.
   - Concrete Example: [`CompactPatternImpl.getType()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactPatternImpl.java) and [`CompactConstBindingImpl.getType()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactConstBindingImpl.java).
4. **Disposal & Leaks**:
   - Never retain strong references to `Project`, `PsiElement`, or `VirtualFile` in static caches, application-level services, or non-disposable listeners.
   - Use `Disposer.register(parentDisposable, childDisposable)` when subscribing to message buses or creating background task listeners.

---

### 3.4 Extension Points & Threading Catalog

Every extension point in `src/main/resources/META-INF/plugin.xml` runs in a specific thread context:

| Extension Point                    | Implementation Class                | Thread / Context               | DumbAware | Purpose                                            |
|:-----------------------------------|:------------------------------------|:-------------------------------|:----------|:---------------------------------------------------|
| `<fileType>`                       | `CompactFileType`                   | Registration (App startup)     | Yes       | Binds `.compact` extension to language             |
| `<lang.parserDefinition>`          | `CompactParserDefinition`           | Any thread (ReadAction)        | Yes       | Creates lexer, parser, and PSI AST nodes           |
| `<lang.syntaxHighlighterFactory>`  | `CompactSyntaxHighlighterFactory`   | Any thread                     | Yes       | Lexer-based token syntax coloring                  |
| `<colorSettingsPage>`              | `CompactColorSettingsPage`          | EDT (Settings dialog)          | Yes       | Color scheme customization page                    |
| `<annotator>`                      | `CompactHighlightingAnnotator`      | Background (ReadAction)        | No        | Fast semantic coloring (primitives, operators)     |
| `<externalAnnotator>`              | `CompactExternalAnnotator`          | Background (3-phase pipeline)  | No        | Authoritative `compactc` background linting        |
| `<completion.contributor>`         | `CompactCompletionContributor`      | Background (ReadAction)        | No        | Contextual autocomplete (types, keywords, values)  |
| `<lang.findUsagesProvider>`        | `CompactFindUsagesProvider`         | Background (ReadAction)        | No        | Find Usages and words scanner                      |
| `<lang.refactoringSupport>`        | `CompactRefactoringSupportProvider` | EDT (ReadAction)               | No        | In-place rename refactoring                        |
| `<lang.namesValidator>`            | `CompactNamesValidator`             | Pure string logic (any thread) | Yes       | Validates identifiers & rejects keywords           |
| `<lang.formatter>`                 | `CompactFormattingModelBuilder`     | Background (ReadAction)        | No        | Code formatting (`Ctrl + Alt + L`)                 |
| `<lang.foldingBuilder>`            | `CompactFoldingBuilder`             | Background (ReadAction)        | **Yes**   | Code folding for blocks, comments, imports         |
| `<lang.documentationProvider>`     | `CompactDocumentationProvider`      | Background (ReadAction)        | No        | Quick documentation hover (`Ctrl + Q`)             |
| `<breadcrumbsInfoProvider>`        | `CompactBreadcrumbsProvider`        | EDT (ReadAction)               | No        | Contextual scope breadcrumb bar                    |
| `<spellchecker.support>`           | `CompactSpellcheckingStrategy`      | Background (ReadAction)        | Yes       | Identifier token splitting & spellcheck            |
| `<lang.surroundWithRange>`         | `CompactSurroundDescriptor`         | EDT (ReadAction / WriteAction) | No        | Surround with `if` or block (`Ctrl + Alt + T`)     |
| `<configurationType>`              | `CompactConfigurationType`          | Any thread                     | **Yes**   | Run Configuration descriptor                       |
| `<runConfigurationProducer>`       | `CompactRunConfigurationProducer`   | Background (ReadAction)        | No        | Contextual run configuration producer              |
| `<applicationConfigurable>`        | `MidnightSettingsConfigurable`      | EDT                            | Yes       | Midnight settings UI under Languages               |
| `<applicationService>`             | `MidnightSettingsState`             | Thread-safe service            | Yes       | Persistent compiler and network settings           |
| `<projectService>`                 | `CompactStdlibService`              | Thread-safe service            | Yes       | Bundled standard library & ZKIR virtual files      |
| `<fileTemplateGroup>`              | `CompactFileTemplateGroupFactory`   | Any thread                     | Yes       | File templates descriptor                          |
| `<internalFileTemplate>`           | Four bundled `.ft` templates        | Any thread                     | Yes       | Registers `Compact Contract`, `Module`, etc.       |
| `<defaultLiveTemplates>`           | `/liveTemplates/Compact.xml`        | App startup                    | Yes       | Bundled live code snippets                         |
| `<psi.referenceContributor>`       | `CompactReferenceContributor`       | Background (ReadAction)        | No        | Injects direct `PsiReference` on identifier tokens |
| `<gotoDeclarationHandler>`         | `CompactGotoDeclarationHandler`     | Background (ReadAction)        | No        | `Ctrl + Click` navigation to declarations          |
| `<codeInsight.lineMarkerProvider>` | `CompactLineMarkerProvider`         | Background (ReadAction)        | No        | Gutter icons for `witness`, `disclose`, etc.       |
| `<localInspection>`                | 9 Semantic Inspection classes       | Background (ReadAction)        | No        | Static analysis checks & quick fixes               |
| `<action>` (`NewGroup`)            | `CompactCreateFileAction`           | **EDT** (Action execution)     | **Yes**   | New Compact File dialog & creation                 |

---

## 4. Test Structure & Strategy

All test suites extend IntelliJ test base classes (`ParsingTestCase` or `BasePlatformTestCase`):

| Test Class                                                                                                                                                                                          | Category                       | Base Class             | Test Count |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------|:-----------------------|:-----------|
| `LexerTest`, `PragmaTest`                                                                                                                                                                           | Tokenization                   | Standalone JUnit 4     | 15         |
| `DeclarationParserTest`, `StatementParserTest`, `ExpressionParserTest`, `PragmaParserTest`, `TypePatternParserTest`, `ErrorRecoveryParserTest`, `EndToEndParserTest`, `CompactParserDefinitionTest` | Parsing & AST                  | `ParsingTestCase`      | 19         |
| `DeclarationPsiTest`, `ElementFactoryConsistencyTest`                                                                                                                                               | PSI structure                  | `BasePlatformTestCase` | 3          |
| `CompactResolveTest`, `CompactCrossFileResolveTest`, `CompactReferenceTest`                                                                                                                         | Scope & Resolution             | `BasePlatformTestCase` | 44         |
| `CompactCompletionTest`                                                                                                                                                                             | Code Completion                | `BasePlatformTestCase` | 13         |
| `CompactRenameTest`, `CompactFindUsagesTest`, `CompactSymbolTest`                                                                                                                                   | Refactoring & Search           | `BasePlatformTestCase` | 22         |
| `CompactTypeInferenceTest`                                                                                                                                                                          | Type Inference                 | `BasePlatformTestCase` | 15         |
| `CompactInspectionTest`                                                                                                                                                                             | Inspections & Fixes            | `BasePlatformTestCase` | 91         |
| `CompactFormatterTest`                                                                                                                                                                              | Formatter & Indent             | `BasePlatformTestCase` | 39         |
| `CompactStructureViewTest`                                                                                                                                                                          | File Outline                   | `BasePlatformTestCase` | 9          |
| `CompactDocumentationTest`                                                                                                                                                                          | Quick Docs & Hover             | `BasePlatformTestCase` | 16         |
| `CompactHighlightingTest`, `CompactColorSettingsPageTest`                                                                                                                                           | Syntax & Semantic Highlighting | `BasePlatformTestCase` | 12         |
| `CompactFoldingTest`, `CompactBreadcrumbsTest`                                                                                                                                                      | Editor Structure               | `BasePlatformTestCase` | 6          |
| `CompactLiveTemplateTest`, `CompactFileTemplateTest`, `CompactSurroundWithTest`                                                                                                                     | Templates & Code Generation    | `BasePlatformTestCase` | 20         |
| `CompactRunConfigurationTest`, `CompactRunConfigurationProducerTest`, `CompactToolchainUtilTest`                                                                                                    | Run & Toolchain                | `BasePlatformTestCase` | 13         |
| `CompactExternalAnnotatorTest`, `CompactLineMarkerTest`                                                                                                                                             | External Diagnostics & Markers | `BasePlatformTestCase` | 7          |
| `CompactStandardLibraryTest`, `CompactStdlibServiceTest`, `MidnightSettingsTest`, `CompactTestUtilsTest`                                                                                            | Stdlib, Settings & DSL         | `BasePlatformTestCase` | 14         |
| **Total Passing Tests**                                                                                                                                                                             |                                |                        | **376**    |

---

## 5. Critical Invariants & Immutable Core

The following architectural components are mature, verified, and **MUST NOT be rewritten, replaced, or degraded**:

1. **Handwritten Recursive-Descent Lexer & Parser**:
   - `CompactLexer` and `CompactParser` are fully tested and handle incomplete code cleanly. Do not replace them with GrammarKit (`.bnf`), Antlr, or generated parsers.
2. **Lexical Scope Resolver (`CompactResolveUtil`)**:
   - All symbol lookups (in-file, includes, standard library) must route through or extend `CompactResolveUtil`. Do not introduce ad-hoc AST walkers that bypass namespace separation (`VALUE` vs `TYPE`) or innermost lexical shadowing.
3. **Tolerance for Incomplete Code**:
   - Every PSI wrapper, inspection visitor, formatter block, and structure view element must guard against `null` children, missing identifiers, and `PsiErrorElement` nodes.
4. **Threading Separation**:
   - Never execute external compiler processes or blocking disk operations on the EDT.
   - Never mutate PSI or VFS state outside a `WriteCommandAction` on the EDT.
5. **Zero Test Regressions**:
   - All 376 unit tests must pass (`./gradlew test`) before any task or feature is marked complete.
6. **Reference Code Discipline**:
   - Reference repositories (`compact/`, `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`) are read-only references. Never edit them or import them wholesale into the plugin build.
