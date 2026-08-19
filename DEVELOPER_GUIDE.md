# Developer Guide — Midnight Compact Language Plugin

This guide is designed for developers who understand compiler design and Java, but want to learn the internal mechanics of this specific IntelliJ IDEA plugin for the Midnight Compact smart-contract language.

---

## 1. Project & Build Structure

The project uses Gradle with the JetBrains IntelliJ Platform Gradle Plugin (`org.jetbrains.intellij.platform`):

- **`build.gradle.kts`**: Defines target platform (`2026.2.0.1`), Kotlin JVM plugin, changelog, and test dependencies.
- **`src/main/resources/META-INF/plugin.xml`**: Declares language file types, syntax highlighters, parser definitions, completion contributors, inspections, formatting model builders, and structure view factories.
- **`src/main/java/dev/verloren/midnight/`**: All plugin production code.
- **`src/test/java/dev/verloren/midnight/`**: Unit test suites extending IntelliJ's test harness.

---

## 2. IntelliJ Platform Architecture & Concepts

To maintain this plugin, you must understand how IntelliJ models code in memory:

1. **AST (`ASTNode`)**: Low-level concrete syntax tree where every token (whitespace, punctuation, identifier) is preserved.
2. **PSI (`PsiElement`)**: Object-oriented semantic layer wrapping AST nodes. PSI provides methods like `getName()`, `getType()`, `resolve()`, and `delete()`.
3. **References (`PsiReference`)**: Created on usage sites (e.g. an identifier expression) pointing to declaration elements. When the user holds `Ctrl` or triggers rename, IntelliJ calls `reference.resolve()`.
4. **Resolve Cache (`ResolveCache`)**: IntelliJ caches `resolve()` results to avoid expensive re-traversal on every keystroke.
5. **Dumb Mode vs Smart Mode**: During project indexing, IntelliJ runs in "dumb mode". Our single-file resolver does not require stub indices, but cross-file includes respect project virtual file caches.
6. **Read / Write Actions**: PSI inspection happens in a Read Action. Modifying PSI (such as in rename or quick fixes) requires a Write Action command (`WriteCommandAction.runWriteCommandAction(...)`).

---

## 3. Subsystem Walkthrough (Bottom-Up)

```text
┌─────────────────────────────────────────────────────────────┐
│ 13. Inspections & Quick-Fixes                               │
├─────────────────────────────────────────────────────────────┤
│ 12. Structure View, Docs & Editor Features                  │
├─────────────────────────────────────────────────────────────┤
│ 11. Code Formatter & Smart Indentation                      │
├─────────────────────────────────────────────────────────────┤
│ 10. Refactoring & Find Usages (Inplace Rename, WordsScanner)│
├─────────────────────────────────────────────────────────────┤
│  9. Code Completion (Context Classifier, Contributors)      │
├─────────────────────────────────────────────────────────────┤
│  8. Type Inference (CompactType, Binary/Unary Evaluator)    │
├─────────────────────────────────────────────────────────────┤
│  7. Scope & Resolution (CompactResolveUtil, Namespaces)     │
├─────────────────────────────────────────────────────────────┤
│  6. References (CompactReferenceBase, Value/Type References)│
├─────────────────────────────────────────────────────────────┤
│  5. PSI Model (CompactPsiElement, CompactNamedElementImpl)  │
├─────────────────────────────────────────────────────────────┤
│  4. Element Factory (ASTNode -> PSI Wrapper Mapping)        │
├─────────────────────────────────────────────────────────────┤
│  3. Parser (CompactParser, Precedence Climbing, Recovery)   │
├─────────────────────────────────────────────────────────────┤
│  2. Token Types & Token Sets (CompactTokenTypes)            │
├─────────────────────────────────────────────────────────────┤
│  1. Lexer (CompactLexer extends LexerBase)                  │
└─────────────────────────────────────────────────────────────┘
```

---

### Step 1: Lexical Analysis

- **Files**:
  - `dev.verloren.midnight.lexer.CompactLexer`
  - `dev.verloren.midnight.lexer.CompactTokenTypes`
  - `dev.verloren.midnight.lexer.CompactTokenSets`
- **What they do**:
  - `CompactLexer` extends `LexerBase`. It maintains an offset pointer over the file buffer and categorizes characters into `IElementType` tokens.
- **Key implementation details**:
  - Stateless: `getState()` returns `0`.
  - Numeric literals: Checks `0x` (hex), `0b` (binary), `0o` (octal), and decimal.
  - Pragmas: Parses version numbers like `1.0.0` or `^0.1.0` while guarding against confusing version numbers with the range operator `..`.
  - Comments: Detects nested block comments and marks them as `UNTERMINATED_BLOCK_COMMENT` (since Compact forbids nested block comments).

---

### Step 2: Parsing & AST Construction

- **Files**:
  - `dev.verloren.midnight.parser.CompactParser`
  - `dev.verloren.midnight.parser.CompactParserDefinition`
  - `dev.verloren.midnight.parser.CompactElementTypes`
  - `dev.verloren.midnight.parser.CompactParserUtil`
- **What they do**:
  - `CompactParser` implements `PsiParser.parse(IElementType root, PsiBuilder builder)`.
- **How expressions are parsed**:
  - Top-level expressions use precedence climbing:
    ```java
    parseAssignmentExpression() -> parseTernaryExpression() -> parseBinaryExpression(0) -> parseUnaryExpression() -> parsePostfixExpression() -> parsePrimaryExpression()
    ```
- **Error Recovery Loop Safety**:
  - When encountering invalid syntax, `sync(builder, TOP_LEVEL_RECOVERY)` advances until finding a recovery token (e.g. `;`, `circuit`, `struct`).
  - To prevent infinite loops when a token is not consumed, the parser asserts `builder.getCurrentOffset() > startOffset` before looping; otherwise it forces `builder.advanceLexer()`.

---

### Step 3: PSI (Program Structure Interface) Hierarchy

- **Files**:
  - `dev.verloren.midnight.psi.CompactPsiElement`
  - `dev.verloren.midnight.psi.CompactNamedElement` / `CompactNamedElementImpl`
  - `dev.verloren.midnight.psi.CompactElementFactory`
  - Concrete classes in `dev.verloren.midnight.psi.*`
- **How PSI nodes work**:
  - When `CompactParserDefinition.createElement(ASTNode node)` is called by IntelliJ, `CompactElementFactory.createElement(node)` instantiates the specific Java wrapper.
  - Every named declaration extends `CompactNamedElementImpl`.
  - `getName()` searches AST children for `CompactTokenTypes.IDENTIFIER`.
  - `setName(String newName)` creates a new identifier leaf using `CompactElementFactory.createIdentifierLeaf(project, newName)` and replaces the old leaf in the AST.

---

### Step 4: Reference Resolution Mechanics

- **Files**:
  - `dev.verloren.midnight.reference.CompactReferenceBase`
  - `dev.verloren.midnight.reference.CompactValueReference`
  - `dev.verloren.midnight.reference.CompactTypeReference`
  - `dev.verloren.midnight.reference.CompactEnumMemberReference`
  - `dev.verloren.midnight.reference.CompactStructFieldReference`
  - `dev.verloren.midnight.resolve.CompactResolveUtil`

```text
Reference Resolution Execution Flow:

1. User hovers, presses Ctrl+B, or triggers Rename on an identifier.
2. PSI element (e.g. CompactReferenceExprImpl) returns a PsiReference.
3. IntelliJ invokes reference.resolve() -> delegates to reference.multiResolve().
4. CompactReferenceBase calls ResolveCache.resolveWithCaching(...).
5. PolyVariantResolver invokes reference.resolveInner().
6. CompactResolveUtil.resolveValue(name, place) or resolveType(name, place) runs.
7. CompactResolveUtil walks outward from innermost lexical AST scope to file/includes.
8. Matching CompactNamedElement is returned and cached by IntelliJ.
```

---

### Step 5: Lexical Scoping & Namespace Model

- **File**: `dev.verloren.midnight.resolve.CompactResolveUtil`
- **Namespaces**:
  - `Namespace.VALUE`: Variables, parameters, consts, circuits, witnesses, ledger declarations.
  - `Namespace.TYPE`: Structs, enums, type aliases, generic type parameters, builtin types.
- **Innermost Shadowing Algorithm**:
  1. `collectDeclarationLayers(place, namespace)` walks up the parent tree `place.getParent()`.
  2. For every `isLocalScope(scope)` (such as `CompactBlock`), it calls `collectNamedBefore(scope, place, namespace)`. Only declarations with `offset < place.offset` are added, preserving forward declaration rules in block scopes.
  3. Once a match is found in a closer scope layer, search stops immediately (shadowing outer variables).
  4. If no local declaration matches, file-level declarations, direct selection imports, included files, and prefixed module imports are searched.

---

### Step 6: Cross-File Resolution (`include` and `import`)

- **Files**:
  - `dev.verloren.midnight.psi.CompactIncludeDeclarationImpl`
  - `dev.verloren.midnight.reference.CompactIncludeReference`
  - `dev.verloren.midnight.reference.CompactImportReference`
  - `dev.verloren.midnight.resolve.CompactResolveUtil`
- **How includes work**:
  1. `include "helpers.compact";` contains a string literal.
  2. `CompactIncludeDeclarationImpl.resolveIncludedFile()` searches:
     - Local directory (`psiDirectory.findFile(path)`).
     - Parent directory of the virtual file.
     - Project content roots (`ProjectRootManager.getInstance(project).getContentRoots()`).
  3. `collectIncludedDeclarations()` collects declarations from the included file.
  4. Cycle detection uses a `Set<CompactFile> visited` to avoid infinite recursion.

---

### Step 7: Type Inference Engine

- **Files**:
  - `dev.verloren.midnight.type.CompactType`
  - `dev.verloren.midnight.type.CompactPrimitiveType`
  - `dev.verloren.midnight.type.CompactTypeInferenceUtil`
  - `dev.verloren.midnight.psi.CompactExpression`
- **How type evaluation works**:
  - Expressions implement `CompactExpression.getType()`:
    - `CompactLiteralExprImpl`: Returns `BOOLEAN` for `true`/`false`, `FIELD` for integer literals, `BYTES` for string literals.
    - `CompactBinaryExprImpl`: Calls `CompactTypeInferenceUtil.inferBinaryExprType(left, op, right)`. Comparisons and logical ops return `BOOLEAN`. Arithmetic ops return operand type.
    - `CompactReferenceExprImpl`: Resolves the reference to its declaration; if the declaration is a `CompactTypeElement`, queries its `getType()`. Uses a `RecursionGuard` (`TYPE_INFERENCE_GUARD`) to prevent infinite recursion during self-referential or cyclic type resolution.

---

### Step 8: Code Completion

- **Files**:
  - `dev.verloren.midnight.completion.CompactCompletionContributor`
  - `dev.verloren.midnight.completion.CompactCompletionContext`
- **How completion works**:
  1. IntelliJ calls `CompactCompletionContributor.addCompletions(...)`.
  2. `CompactCompletionContext.classify(position)` inspects preceding PSI tokens:
     - Preceded by `.` -> `Kind.MEMBER`.
     - Inside type reference or preceded by `:`, `as`, `<`, `#` -> `Kind.TYPE`.
     - Preceded by `;`, `{`, `}`, `else`, `export` -> `Kind.KEYWORD`.
     - Otherwise -> `Kind.VALUE`.
  3. Based on the classification, completions are fed into `CompletionResultSet`:
     - `KEYWORD`: Declaration and statement keywords.
     - `TYPE`: Builtin types (`Field`, `Uint`, `Boolean`), in-scope struct/enum/alias declarations, and prefixed imports.
     - `MEMBER`: Enum variants or struct fields.
     - `VALUE`: In-scope variables, parameters, circuits, ledger items, witnesses, and boolean literals.

---

### Step 9: Code Inspections & Quick-Fixes

- **Files**:
  - `dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection`
  - `dev.verloren.midnight.inspection.CompactDuplicateDeclarationInspection`
  - `dev.verloren.midnight.inspection.CompactUnusedLocalVariableInspection`
  - `dev.verloren.midnight.inspection.CompactTypeMismatchInspection`
  - `dev.verloren.midnight.inspection.fix.CompactRemoveUnusedVariableFix`
- **How inspections work**:
  - Extend `LocalInspectionTool` and override `buildVisitor(ProblemsHolder, boolean)`.
  - All inspections check `if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) return;` to avoid false positives while the user is typing incomplete code.
  - `CompactUnusedLocalVariableInspection` uses `ReferencesSearch.search(element, element.getUseScope()).findFirst()` to check whether local const bindings have usages. If unused, it attaches `CompactRemoveUnusedVariableFix`.

---

### Step 10: Formatter & Code Style

- **Files**:
  - `dev.verloren.midnight.formatter.CompactFormattingModelBuilder`
  - `dev.verloren.midnight.formatter.CompactBlock`
  - `dev.verloren.midnight.formatter.CompactLanguageCodeStyleSettingsProvider`
- **How formatting works**:
  - `CompactFormattingModelBuilder.createSpacingBuilder()` creates rule-based spacing (e.g. 1 space around binary operators, 0 before comma/semicolon, 1 after colon).
  - `CompactBlock.computeChildIndent()` calculates whether a child AST node gets `Indent.getNormalIndent()` (2 spaces) or `Indent.getNoneIndent()`.
  - `isIncomplete()` checks if braces/parentheses/brackets/angle brackets are closed, enabling smart indent when pressing Enter on unclosed constructs.

---

## 4. How to Run & Verify Tests

Run the full test suite using Gradle:

```bash
# Run all tests
./gradlew test

# Run a specific test suite
./gradlew test --tests "dev.verloren.midnight.resolve.*"
```

All 224 unit tests must pass before committing changes.
