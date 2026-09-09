# ADR-021: Find Usages Engine & In-Place Rename Refactoring

## Status
Accepted (Phases 7–8 / Complete)

## Context & Problem Statement
Refactoring smart contract code (renaming parameters, variables, circuits, or ledger state fields) requires:
1. **Find Usages (`Alt+F7`)**: Accurately locating every reference across the codebase without false positives from comments or string literals.
2. **In-Place Rename (`Shift+F6`)**: Renaming a symbol directly in the editor and instantly updating all referencing sites across files.
3. **Identifier Validation**: Preventing users from renaming an identifier to an illegal string (e.g. `123bad` or a reserved keyword like `contract` or `witness`).

## Authoritative References
1. **IntelliJ Indexing & Refactoring Platform**:
   - `FindUsagesProvider`, `WordsScanner`, `DefaultWordsScanner`
   - `RefactoringSupportProvider`
   - `NamesValidator`
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsFindUsagesProvider.kt`, `RsRefactoringSupportProvider.kt`, `RsNamesValidator.kt`.

## Decision
1. **High-Performance Lexer-Backed Word Scanner (`CompactFindUsagesProvider`)**:
   - Uses `DefaultWordsScanner(new CompactLexer(), TokenSet.create(IDENTIFIER), COMMENTS, LITERALS)`.
   - Indexes identifier tokens into IntelliJ's global words index, allowing instant project-wide search without parsing every file on disk.
   - Formats user-friendly type labels ("circuit", "witness", "ledger field", "struct", "parameter") in the Find Usages results pane.
2. **In-Place Rename (`CompactRefactoringSupportProvider`)**:
   - Enables `isInplaceRenameAvailable(element, context)` for all `CompactNamedElement` nodes (variables, circuits, fields, types).
   - Allows inline editing with real-time preview of changed usages.
3. **Strict Name Validation (`CompactNamesValidator`)**:
   - Implements `NamesValidator.isIdentifier(name, project)` and `isKeyword(name, project)`.
   - Checks candidates against the official Compact lexer grammar and keyword tables, blocking invalid renames with immediate UI feedback.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Words scanning and name validation reuse the core `CompactLexer` and `CompactTokenSets`, ensuring identical rules between editor parsing and refactoring.
- **Does it support cross-file refactoring?**: Yes. The words scanner feeds IntelliJ's Project-wide index, allowing renaming across included files.

## Feature Implementation Map
- Find Usages: [`CompactFindUsagesProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/findUsages/CompactFindUsagesProvider.java)
- Refactoring Support: [`CompactRefactoringSupportProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/refactoring/CompactRefactoringSupportProvider.java)
- Names Validator: [`CompactNamesValidator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/refactoring/CompactNamesValidator.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.findUsagesProvider>`, `<lang.refactoringSupport>`, `<lang.namesValidator>`)
- Unit Tests: [`CompactFindUsagesTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/findUsages/CompactFindUsagesTest.java)

## Consequences & Future Maintenance
- Future refactorings (Extract Variable, Change Signature, Move Contract) will leverage `CompactRefactoringSupportProvider` and `CompactNamesValidator`.
