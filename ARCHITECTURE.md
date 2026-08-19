# Architecture Guide — Midnight Compact Language Plugin

This document provides a comprehensive technical breakdown of the architecture, subsystems, and IntelliJ Platform integration of the **Midnight Compact Language Plugin** (`dev.verloren.midnight`).

---

## 1. Overview & Purpose

The plugin delivers first-class development support in JetBrains IntelliJ IDEA for **Compact**, the domain-specific smart-contract language of the **Midnight blockchain network**. Compact is designed for writing privacy-preserving zero-knowledge contracts where circuits, witnesses, ledger state, and exportable modules require rigorous static checking and syntax awareness.

The plugin provides:
- **Syntax Highlighting & Token Classification** (keywords, builtin types, operators, literals, comments).
- **Resilient Recursive-Descent Parsing** with grammar error recovery and AST construction.
- **Typed PSI (Program Structure Interface)** reflecting the Compact AST.
- **Lexical & Cross-File Symbol Resolution** supporting distinct `VALUE` and `TYPE` namespaces, module imports, prefixes, and file inclusions (`include`).
- **Code Insight Features**: Smart completion, inplace rename refactoring, Find Usages, Structure View, Quick Documentation, and Code Formatting.
- **Real-Time Inspections**: Unresolved references, duplicate declarations, unused local variables (with quick-fixes), and basic type mismatch checking.

---

## 2. End-to-End Architectural Pipeline

The following diagram illustrates how source text flows through the plugin subsystems to power IntelliJ IDE features:

```text
Compact Source (.compact file buffer)
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. LEXICAL ANALYSIS                                         │
│    dev.verloren.midnight.lexer.CompactLexer                 │
│    dev.verloren.midnight.lexer.CompactTokenTypes            │
│    dev.verloren.midnight.lexer.CompactTokenSets             │
└─────────────────────────────┬───────────────────────────────┘
                              │ Token Stream (IElementType)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SYNTACTIC PARSING & AST GENERATION                       │
│    dev.verloren.midnight.parser.CompactParser (PsiParser)   │
│    dev.verloren.midnight.parser.CompactParserDefinition     │
│    dev.verloren.midnight.parser.CompactElementTypes         │
└─────────────────────────────┬───────────────────────────────┘
                              │ AST Hierarchy (ASTNode)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. PROGRAM STRUCTURE INTERFACE (PSI MODEL)                  │
│    CompactElementFactory (maps ASTNode -> CompactPsiElement)│
│    CompactFile / CompactNamedElementImpl / CompactBlock     │
│    Declarations: Circuit, Witness, Ledger, Struct, Enum...  │
│    Expressions: Binary, Unary, Member, Call, Cast, Ref...   │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌───────────────────────────────┐ ┌───────────────────────────┐
│ 4. RESOLUTION & SCOPE LAYER   │ │ 5. TYPE INFERENCE ENGINE  │
│    CompactResolveUtil         │ │    CompactType            │
│    - Innermost lexical scope  │ │    CompactPrimitiveType   │
│    - Namespace.VALUE vs TYPE  │ │    CompactTypeInferenceUtil│
│    - Module exports & prefixes│ └─────────────┬─────────────┘
│    - Relative & Root includes │               │
│    CompactScopes / Symbols    │               │
└──────────────┬────────────────┘               │
               │                                │
               ├────────────────────────────────┘
               ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. IDE FEATURES & EXTENSIONS (plugin.xml)                   │
│  ├── Navigation & References (PsiReference / CompactRefBase)│
│  ├── Code Completion (CompactCompletionContributor)         │
│  ├── Find Usages (CompactFindUsagesProvider)                │
│  ├── Rename Refactoring (CompactNamesValidator, Inplace)    │
│  ├── Static Inspections (Unresolved, Duplicates, Types)     │
│  ├── Quick Fixes (CompactRemoveUnusedVariableFix)           │
│  ├── Formatting & Smart Indent (CompactFormattingModel)     │
│  ├── Structure View (CompactStructureViewModel)             │
│  └── Quick Documentation (CompactDocumentationProvider)     │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. IntelliJ Platform Integration (`plugin.xml`)

IntelliJ uses [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) to bind extension points to implementation classes for the `Compact` language.

### Core Language Extensions
| Extension Point | Implementation Class | Purpose |
| :--- | :--- | :--- |
| `fileType` | [`CompactFileType`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/CompactFileType.java) | Registers `.compact` extension and file icon. |
| `lang.parserDefinition` | [`CompactParserDefinition`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactParserDefinition.java) | Ties lexer, parser, file node type, and PSI factory. |
| `lang.syntaxHighlighterFactory` | [`CompactSyntaxHighlighterFactory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactSyntaxHighlighterFactory.java) | Provides syntax highlighting lexer and token text attribute mappings. |

### Editing & Refactoring Extensions
| Extension Point | Implementation Class | Purpose |
| :--- | :--- | :--- |
| `completion.contributor` | [`CompactCompletionContributor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java) | Suggests keywords, in-scope values, types, and enum members. |
| `lang.findUsagesProvider` | [`CompactFindUsagesProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/findUsages/CompactFindUsagesProvider.java) | Provides word scanner and descriptive names for Find Usages search. |
| `lang.namesValidator` | [`CompactNamesValidator`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/refactoring/CompactNamesValidator.java) | Validates identifier tokens and rejects keywords during rename. |
| `lang.refactoringSupport` | [`CompactRefactoringSupportProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/refactoring/CompactRefactoringSupportProvider.java) | Enables inplace renaming of `CompactNamedElement` declarations. |
| `lang.formatter` | [`CompactFormattingModelBuilder`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/formatter/CompactFormattingModelBuilder.java) | Builds abstract block tree for code reformatting (`Ctrl+Alt+L`). |
| `langCodeStyleSettingsProvider` | [`CompactLanguageCodeStyleSettingsProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/formatter/CompactLanguageCodeStyleSettingsProvider.java) | Configures 2-space canonical indentation and code style defaults. |
| `lang.psiStructureViewFactory` | [`CompactStructureViewFactory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/structure/CompactStructureViewFactory.java) | Builds file hierarchy tree for Structure View tool window. |
| `lang.documentationProvider` | [`CompactDocumentationProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/documentation/CompactDocumentationProvider.java) | Generates HTML Quick Documentation on hover (`Ctrl+Q`). |
| `lang.commenter` | [`CompactCommenter`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactCommenter.java) | Line (`//`) and block (`/* */`) comment actions. |
| `lang.braceMatcher` | [`CompactPairedBraceMatcher`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactPairedBraceMatcher.java) | Auto-closing and matching for `{}`, `()`, `[]`, `<>`. |
| `lang.quoteHandler` | [`CompactQuoteHandler`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactQuoteHandler.java) | Auto-closing double/single quotes for string literals. |

### Code Inspections
| Short Name | Implementation Class | Description |
| :--- | :--- | :--- |
| `CompactUnresolvedReference` | [`CompactUnresolvedReferenceInspection`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactUnresolvedReferenceInspection.java) | Flags unresolved identifier and member references. |
| `CompactDuplicateDeclaration` | [`CompactDuplicateDeclarationInspection`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactDuplicateDeclarationInspection.java) | Flags colliding duplicate declarations in identical scope. |
| `CompactUnusedLocalVariable` | [`CompactUnusedLocalVariableInspection`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactUnusedLocalVariableInspection.java) | Flags local `const` bindings that have zero usages. |
| `CompactTypeMismatch` | [`CompactTypeMismatchInspection`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactTypeMismatchInspection.java) | Checks boolean conditionals and type compatibility. |

---

## 4. Parsing Subsystem

### 4.1 Lexer (`CompactLexer`)
- **Base Class**: `com.intellij.lexer.LexerBase`.
- **Implementation Style**: Handwritten, stateless, character-by-character scanner.
- **Key Capabilities**:
  - Distinguishes keywords from identifiers via `Map<String, IElementType>`.
  - Recognizes numeric literals: Hexadecimal (`0x...`), Binary (`0b...`), Octal (`0o...`), Decimal.
  - Recognizes semantic version literals in `pragma` statements (`1`, `1.0`, `1.2.3`).
  - Resolves multi-character operators (`==`, `!=`, `<=`, `>=`, `=>`, `+=`, `-=`, `&&`, `||`, `..`, `...`).
  - Detects unterminated strings, unterminated block comments, and illegal characters (`BAD_CHARACTER`).

### 4.2 Parser (`CompactParser`)
- **Base Interface**: `com.intellij.lang.PsiParser`.
- **Implementation Style**: Handwritten recursive-descent parser with precedence climbing for binary expressions.
- **Error Recovery**:
  - Uses `TOP_LEVEL_RECOVERY` token set (`pragma`, `include`, `import`, `export`, `circuit`, `struct`, `enum`, `type`, `ledger`, `witness`, `constructor`, `;`) to resynchronize after syntax errors without aborting file parsing.
  - Guarantees loop progression (checks `builder.getCurrentOffset()`) to prevent IntelliJ Event Dispatch Thread (EDT) freezes during incomplete user input.
- **Expression Parsing**:
  - Precedence climbing algorithm in `parseBinaryExpression(builder, minPrecedence)` handling logical OR (0), logical AND (1), equality (2), relational (3), type cast `as` (4), additive (5), multiplicative (6).
  - Postfix expressions handle tuple indexing `[i]`, member access `.field`, and function/circuit invocation `(...)`.

### 4.3 PSI Creation (`CompactParserDefinition` & `CompactElementFactory`)
- When IntelliJ builds the PSI tree from AST nodes, `CompactParserDefinition.createElement(ASTNode)` delegates directly to `CompactElementFactory.createElement(ASTNode)`.
- `CompactElementFactory` maps every `CompactElementTypes.*` constant to its corresponding concrete typed Java wrapper in `dev.verloren.midnight.psi.*`.

---

## 5. PSI (Program Structure Interface) Model

```text
PsiElement (IntelliJ)
  └── CompactPsiElement
        ├── CompactBlock
        ├── CompactExpression (Interface)
        │     ├── CompactBinaryExprImpl
        │     ├── CompactUnaryExprImpl
        │     ├── CompactCallExprImpl
        │     ├── CompactCastExprImpl
        │     ├── CompactMemberExprImpl
        │     ├── CompactReferenceExprImpl
        │     ├── CompactLiteralExprImpl
        │     ├── CompactStructLiteralExprImpl
        │     └── CompactParenExprImpl
        ├── CompactTypeElement (Interface)
        │     ├── CompactBuiltinTypeImpl
        │     ├── CompactTypeReferenceImpl
        │     └── CompactTypedPatternImpl
        └── CompactNamedElementImpl (implements CompactNamedElement / PsiNameIdentifierOwner)
              ├── CompactCircuitDefinitionImpl
              ├── CompactWitnessDeclarationImpl
              ├── CompactLedgerDeclarationImpl
              ├── CompactStructDefinitionImpl
              ├── CompactStructFieldImpl
              ├── CompactEnumDefinitionImpl
              ├── CompactEnumMemberImpl
              ├── CompactTypeDefinitionImpl
              ├── CompactConstBindingImpl
              ├── CompactParameterImpl
              ├── CompactPatternImpl
              ├── CompactGenericParameterImpl
              ├── CompactImportElementImpl
              └── CompactModuleDefinitionImpl
```

### Key PSI Invariants:
1. **`CompactNamedElement`**: Implemented by all declarations capable of navigation, renaming, or search.
2. **`getNameIdentifier()`**: Extracts the leaf `CompactTokenTypes.IDENTIFIER` token from the node's AST children.
3. **`setName(String)`**: Replaces the leaf identifier node using `CompactElementFactory.createIdentifierLeaf(project, newName)`.
4. **`getUseScope()`**: Restricts local variables, parameters, and generic parameters to `LocalSearchScope(containingFile)` while promoting top-level declarations to `GlobalSearchScope.projectScope(project)`.

---

## 6. Symbol Resolution & Scoping

Symbol resolution is encapsulated in [`CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java).

### 6.1 Namespaces
Resolution strictly isolates two distinct namespaces:
1. **`Namespace.VALUE`**:
   - Local `const` bindings and pattern variables.
   - Circuit, constructor, and witness parameters.
   - Ledger variables, witness declarations, and circuit definitions.
   - Enum members (when qualified).
2. **`Namespace.TYPE`**:
   - Struct definitions (`struct Point { ... }`).
   - Enum definitions (`enum Color { ... }`).
   - Type aliases (`type Amount = Uint<64>;`).
   - External contract declarations (`contract Oracle { ... }`).
   - Generic type parameters (`<#T>`).
   - Builtin primitive types (`Boolean`, `Field`, `Uint`, `Bytes`, etc.).

### 6.2 Scope Traversal Order
When `resolve(name, place, namespace)` is invoked, it evaluates layers from the innermost AST scope outward:

```text
1. Local Block Scopes:
   Walks preceding statements in the current block and parent blocks
   (only declarations located BEFORE the reference offset are visible).
       │
       ▼
2. Callable Parameter Scopes:
   Circuit / Witness / Constructor / Lambda parameter lists.
       │
       ▼
3. Enclosing Module Declarations:
   Declarations defined directly inside the containing module.
       │
       ▼
4. File-Level Declarations:
   Top-level declarations in the current .compact file.
       │
       ▼
5. Direct Import Elements:
   Selective imports: `import { X, Y as Z } from MyModule;`.
       │
       ▼
6. Cross-File Inclusions (`include`):
   Declarations recursively collected from included files (`include "lib.compact";`).
       │
       ▼
7. Prefixed Module Imports:
   Prefixed imports: `import MyModule prefix my_;` -> matches `my_foo`.
```

### 6.3 Cross-File Resolution
- [`CompactIncludeDeclarationImpl`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactIncludeDeclarationImpl.java) resolves file paths via:
  1. Directory of the containing file.
  2. Relative path from the containing file's virtual parent folder.
  3. Content root relative paths across the project.
- Cycle Detection: Recursive includes use a `Set<CompactFile> visited` guard to prevent infinite recursion on circular inclusions.
- Caching: Results are cached via `CachedValuesManager` tied to `PsiModificationTracker.MODIFICATION_COUNT`.

---

## 7. IDE Features

### 7.1 Code Completion (`CompactCompletionContributor`)
Classifies the cursor location using `CompactCompletionContext.classify(position)` into:
- **`KEYWORD`**: Top-level keywords (`circuit`, `struct`, `contract`, etc.) or statement keywords (`if`, `for`, `return`, `const`).
- **`TYPE`**: Builtin types, in-scope struct/enum/alias names, generic parameters, and prefixed type imports.
- **`MEMBER`**: Enum variants after `Enum.` or struct fields after `structInstance.`.
- **`VALUE`**: In-scope local variables, parameters, circuits, ledger items, witnesses, and boolean literals.

### 7.2 Find Usages & Rename
- **Find Usages**: `CompactFindUsagesProvider` integrates with IntelliJ's indexer via `DefaultWordsScanner` tokenizing words on `CompactTokenTypes.IDENTIFIER`.
- **Inplace Rename**: `CompactRefactoringSupportProvider` permits inline renaming on any `CompactNamedElement`.
- **Validation**: `CompactNamesValidator` ensures proposed names are valid Compact identifiers and not reserved keywords.

### 7.3 Code Formatting & Indentation
- `CompactFormattingModelBuilder` constructs a `CompactBlock` tree with `SpacingBuilder`.
- 2-space canonical indentation for block bodies, struct declarations, enum declarations, contract bodies, and module declarations.
- Enforces strict spacing around binary operators (`+`, `-`, `*`, `/`, `==`, `!=`, `<`, `>`, `&&`, `||`, `=>`, `as`), commas, and colons.

### 7.4 Structure View & Documentation
- **Structure View**: `CompactStructureViewModel` exposes top-level definitions, modules, circuits, structs (with nested fields), enums (with variants), and const bindings.
- **Quick Documentation**: `CompactDocumentationProvider` formats syntax headers, doc comments (`///` and `/** */`), field listings, and enum variant lists into rich HTML on mouse hover or quick doc shortcut.

---

## 8. Compiler & Toolchain Integration Status

- Currently, toolchain execution (invoking `compactc` or Docker containers) is planned for future phases.
- The plugin currently focuses on pure AST/PSI analysis, editor features, and in-IDE static inspections without external CLI dependencies.

---

## 9. Test Suite Verification

The test suite consists of 224 automated unit tests running on the IntelliJ Platform Test Framework (`BasePlatformTestCase` and `ParsingTestCase`):

```text
Suite Breakdown:
├── Lexer & Pragmas (15 tests): Token correctness, versions, operators, comments.
├── Parser (17 tests): Top-level forms, statements, expressions, error recovery.
├── PSI & Factory (3 tests): Node wrapping, element replacement consistency.
├── Resolution (28 tests): Lexical shadowing, namespaces, module exports, cross-file includes.
├── References (6 tests): Reference binding, target offsets, navigation.
├── Completion (5 tests): Keyword, type, value, and member auto-completion.
├── Rename & Search (18 tests): Inplace rename, keyword protection, Find Usages.
├── Type Inference (12 tests): Binary, unary, literal, and reference type evaluation.
├── Inspections (57 tests): Unresolved references, duplicates, unused vars, type mismatches.
├── Formatter (39 tests): Spacing, line wrapping, block indentation, smart enter.
├── Structure View (9 tests): Tree elements, icons, navigation presentation.
└── Documentation (9 tests): Signature rendering, doc comment extraction, HTML output.
```
