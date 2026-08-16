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
- **Purpose**: Tokenize Compact source into IntelliJ `IElementType` tokens.
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
  - `src/main/gen` is treated as editable project source if present, but current implementation resides in `src/main/java`.

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
  - Lightweight and single-file; returns `CompactPrimitiveType.UNKNOWN` for complex un-inferable or external constructs to avoid false positives.

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

---

## 3. Test Structure & Strategy

All test suites extend IntelliJ test base classes (`ParsingTestCase` or `BasePlatformTestCase`):

| Test Class | Category | Base Class |
| :--- | :--- | :--- |
| `LexerTest`, `PragmaTest` | Tokenization | Standalone JUnit 4 |
| `DeclarationParserTest`, `StatementParserTest`, `ExpressionParserTest`, `PragmaParserTest`, `TypePatternParserTest`, `ErrorRecoveryParserTest`, `EndToEndParserTest` | Parsing & AST | `ParsingTestCase` |
| `DeclarationPsiTest`, `ElementFactoryConsistencyTest` | PSI structure | `BasePlatformTestCase` |
| `CompactResolveTest`, `CompactReferenceTest` | Scope & Resolution | `BasePlatformTestCase` |
| `CompactCompletionTest` | Code Completion | `BasePlatformTestCase` |
| `CompactRenameTest`, `CompactFindUsagesTest`, `CompactSymbolTest` | Refactoring & Search | `BasePlatformTestCase` |
| `CompactTypeInferenceTest` | Type Inference | `BasePlatformTestCase` |
| `CompactInspectionTest` | Inspections & Fixes | `BasePlatformTestCase` |
| `CompactFormatterTest` | Formatter & Indent | `BasePlatformTestCase` |

---

## 4. Architectural Invariants for Future Contributors

1. **Do NOT rebuild or replace existing subsystems**: Extend the current handwritten Lexer/Parser/PSI rather than introducing external generators.
2. **Reuse Resolution**: All symbol lookups must go through `CompactResolveUtil` or extend it.
3. **Respect Namespaces**: Preserve the separation between `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE`.
4. **Tolerance for Incomplete Code**: Every PSI method, inspection, and formatter block must check for null children and `PsiErrorElement`.
5. **No Regressions**: All 177 unit tests must pass before completing any task.
