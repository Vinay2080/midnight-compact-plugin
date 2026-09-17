# ADR-032: Pragma Version Directives Code Completion and Quick Documentation

## Status
Accepted (v1.2.9 / Complete)

## Date
2026-09-17

## Subsystem
Completion & Documentation

## Related ADRs
- [ADR-001](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-001-handwritten-lexer-and-parser.md): Handwritten Lexer and Recursive-Descent Parser Engine
- [ADR-012](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-012-isolated-multi-version-compiler-management.md): Isolated Multi-Version Compiler Management & SemVer Normalization
- [ADR-019](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-019-context-aware-code-completion.md): Context-Aware Code Completion & Structural Scoping
- [ADR-023](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-023-quick-documentation-and-parameter-info.md): Quick Documentation Provider & Interactive Parameter Info

---

## 1. Context & Problem Statement
In Compact smart contracts (e.g. `ShieldedAccessControl.compact` and standard contracts), contracts begin with top-level compiler directives declaring version requirements:
```compact
pragma language_version >= 0.26.0;
pragma compiler_version >= 0.26.0;
```
Prior to this implementation:
1. When a developer typed `pragma ` at the top level of a file, IntelliJ did not classify the caret position as a pragma directive context. Instead, completion fell back to generic top-level declaration keywords (`circuit`, `struct`, `import`, `export`, etc.), polluting the completion popup with invalid syntax.
2. The canonical directives `language_version` and `compiler_version` were missing from auto-completion and snippet insertion.
3. Hovering over `language_version` or `compiler_version` in a contract, or inspecting completion lookup items (via `Ctrl+Q` / `F1`), displayed no documentation explaining SemVer constraint syntax (`>=`, `>`, `^`, `~`, `==`), the difference between the source language version specification and the compiler toolchain binary version, or the mandatory terminating semicolon.

---

## 2. Decision Drivers & Invariants
- **Compact Compiler Ground Truth**:
  - `compact/compiler/parser.ss` (lines 90–115): Defines `pragma-form` strictly as:
    ```scheme
    ("pragma" (or ("language_version" version-spec)
                  ("compiler_version" version-spec)) ";")
    ```
  - Upstream grammar permits only `language_version` and `compiler_version` directives.
- **Context Isolation & Negative Scoping**:
  - `CompactCompletionContext.classify` must return `AFTER_PRAGMA` only when the caret is preceded by a `pragma` keyword token at the top level of a file or within a `CompactPragmaForm`.
  - Must never offer pragma directives inside circuits, structs, enums, statements, expressions, or line/block/doc comments.
  - When typing inside an existing directive (e.g. `pragma langu<caret>`), the classification must resolve to `AFTER_PRAGMA`.
- **IntelliJ Platform Typing & Lookup Architecture**:
  - Implement `CompactDocumentationProvider.getDocumentationElementForLookupItem` to construct a synthetic `CompactPragmaForm` element via `CompactElementFactory.createPragmaForm` when a pragma lookup item is highlighted in the completion popup.
  - Implement `CompactDocumentationProvider.generateDoc` to output rich HTML quick documentation covering definition, description, valid version constraint operators, examples, and semantics.
  - Insertion handler `createPragmaInsertHandler` must ensure a trailing space separates the directive name and the version constraint without creating duplicate spaces.

---

## 3. Evaluated Options

### Option 1: Static Keyword List Addition
- **Description**: Add `"language_version"` and `"compiler_version"` to `DECLARATION_KEYWORDS`.
- **Drawback**: Suggests pragma directives indiscriminately at any declaration position (e.g., alongside `circuit`, `struct`), cluttering top-level completions and failing to provide contextual completion immediately following `pragma`.

### Option 2: Contextual Pragma Completion & Quick Documentation (Selected)
- **Description**:
  1. Add `Kind.AFTER_PRAGMA` to `CompactCompletionContext` with dedicated `isAfterPragma(...)` inspection.
  2. Register prioritized lookup elements for `language_version` (priority 100.0) and `compiler_version` (priority 90.0) with bold styling, `"pragma"` type text, and tail text `" >= <version>"`.
  3. Attach `createPragmaInsertHandler()` to insert a space and reposition the caret.
  4. Extend `CompactDocumentationProvider` to resolve both lookup items and declared `CompactPragmaForm` PSI nodes, rendering styled HTML documentation with version constraint syntax and examples.
- **Pros**: Perfectly mirrors the language grammar, conforms to JetBrains completion/documentation patterns, and provides rich inline developer assistance.

---

## 4. Decision Outcome
Adopted **Option 2**.

### Implementation Highlights:
- **`CompactCompletionContext.java`**:
  ```java
  public enum Kind {
    KEYWORD, AFTER_EXPORT, AFTER_SEALED, AFTER_PURE, AFTER_NEW, AFTER_PRAGMA,
    STATEMENT, TYPE, BYTES_SIZE, UINT_SIZE, MEMBER, VALUE, NONE
  }
  ```
  Inspects preceding visible leaf tokens and enclosing `CompactPragmaForm` PSI parents.
- **`CompactElementFactory.java`**:
  ```java
  public static @NotNull CompactPragmaForm createPragmaForm(@NotNull Project project, @NotNull String text) {
    CompactFile file = createFile(project, "pragma " + text + " >= 0.0.0;");
    return Objects.requireNonNull(PsiTreeUtil.findChildOfType(file, CompactPragmaForm.class));
  }
  ```
- **`CompactCompletionContributor.java`**:
  Handles `case AFTER_PRAGMA -> addAfterPragmaCompletions(result);`.
- **`CompactDocumentationProvider.java`**:
  Generates HTML documentation with definition, directive explanations, constraints (`>=`, `>`, `==`, etc.), and examples.

---

## 5. Feature Implementation Map
- PSI Factory: [`CompactElementFactory.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactElementFactory.java)
- Context Classifier: [`CompactCompletionContext.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContext.java)
- Completion Contributor: [`CompactCompletionContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java)
- Documentation Provider: [`CompactDocumentationProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/documentation/CompactDocumentationProvider.java)
- Completion Tests: [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java)
- Documentation Tests: [`CompactDocumentationTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/documentation/CompactDocumentationTest.java)

---

## 6. Scalability, Memory & Threading Impact
- **Time Complexity**: $O(1)$ PSI ancestor check and leaf scanning for completion; $O(1)$ HTML template rendering for documentation.
- **Memory Overhead**: Negligible; synthetic PSI elements for lookup items are created only on-demand when the documentation popup is triggered.
- **Threading Model**: Operates entirely within platform ReadAction boundaries on background pool / EDT.