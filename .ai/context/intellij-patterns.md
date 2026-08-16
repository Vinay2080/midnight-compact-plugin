# IntelliJ Platform Patterns & Conventions

This guide documents the specific IntelliJ Platform APIs and design patterns utilized in `midnight-plugin`.

---

## 1. PSI & AST Construction

### Typed AST Wrappers (`dev.verloren.midnight.psi.impl.*`)
- **Purpose**: Map raw AST nodes to strongly-typed Java PSI elements.
- **IntelliJ APIs**: `ASTWrapperPsiElement`, `PsiElement`, `PsiNamedElement`.
- **Existing Examples**: `CompactCircuitDefinitionImpl`, `CompactWitnessDeclarationImpl`, `CompactReferenceExprImpl`.
- **When to use**: Whenever adding or modifying AST node representations.
- **When NOT to use**: Do not implement custom PSI trees outside `CompactElementTypes` and `CompactParserDefinition`.

### Element Factory (`CompactElementFactory`)
- **Purpose**: Programmatically generate PSI subtrees from source snippets (used during refactoring, rename, and quick-fixes).
- **IntelliJ APIs**: `PsiFileFactory.getInstance(project).createFileFromText(...)`.
- **Existing Example**: `CompactElementFactory.createIdentifier(project, newName)`.
- **When to use**: Symbol renaming (`setName`), creating replacement nodes in quick-fixes.
- **When NOT to use**: Do not manually construct AST nodes without using the parser/factory.

---

## 2. Symbol Resolution & Scoping

### Single-File AST Walking (`CompactResolveUtil`)
- **Purpose**: Fast lexical scope resolution for single-file editing.
- **IntelliJ APIs**: `PsiTreeUtil.findFirstParent`, `PsiTreeUtil.findChildrenOfType`, `PsiElement.getParent()`.
- **Existing Example**: `CompactResolveUtil.resolveReference(element, namespace)`.
- **When to use**: Resolving local variables, parameters, module declarations, and file-level definitions.
- **When NOT to use**: Do not perform whole-project disk scans inside resolution; defer cross-file search to StubIndex when implemented.

### Polymorphic PsiReference (`CompactReferenceExprImpl`, `CompactStructFieldReference`, `CompactEnumMemberReference`)
- **Purpose**: Connect usages to declarations for navigation (`Ctrl + Click`, `Cmd + B`), hover, and rename.
- **IntelliJ APIs**: `PsiReference`, `PsiReferenceBase`, `PsiElement.getReference()`, `PsiElement.getReferences()`.
- **Existing Example**: `CompactReferenceExprImpl.getReference()`, `CompactStructFieldReference`.
- **When to use**: Expression nodes referencing declarations.
- **When NOT to use**: Do not use central `PsiReferenceContributor` when PSI elements directly own their reference.

---

## 3. Code Completion

### Contextual Completion (`CompactCompletionContributor`)
- **Purpose**: Provide intelligent auto-complete for keywords, types, variables, and enum members.
- **IntelliJ APIs**: `CompletionContributor`, `CompletionParameters`, `CompletionResultSet`, `LookupElementBuilder`, `PlatformPatterns`.
- **Existing Example**: `CompactCompletionContributor.fillCompletionVariants(...)`.
- **When to use**: Adding completion support for new syntax or language constructs.
- **When NOT to use**: Avoid long-running calculations or disk I/O in the completion thread.

---

## 4. Refactoring & Find Usages

### Renaming & Identifier Validation (`CompactNamesValidator`, `CompactRefactoringSupportProvider`)
- **Purpose**: Validate symbol names and reject keywords during inline rename (`Shift + F6`).
- **IntelliJ APIs**: `NamesValidator`, `RefactoringSupportProvider`.
- **Existing Example**: `CompactNamesValidator.isIdentifier(...)`, `CompactRefactoringSupportProvider`.
- **When to use**: Enforcing lexical identifier rules and enabling in-place rename.

### Words Scanner & Find Usages (`CompactFindUsagesProvider`)
- **Purpose**: Enable symbol usage searching and text occurrences.
- **IntelliJ APIs**: `FindUsagesProvider`, `DefaultWordsScanner`.
- **Existing Example**: `CompactFindUsagesProvider`.
- **When to use**: Highlighting usages and project-wide symbol search.

---

## 5. Inspections & Quick-Fixes

### Local Inspection (`LocalInspectionTool`)
- **Purpose**: Perform fast, on-the-fly static analysis and report warnings/errors in the editor.
- **IntelliJ APIs**: `LocalInspectionTool`, `PsiElementVisitor`, `ProblemsHolder`, `ProblemHighlightType`.
- **Existing Examples**:
  - `CompactUnresolvedReferenceInspection`
  - `CompactDuplicateDeclarationInspection`
  - `CompactUnusedLocalVariableInspection`
  - `CompactTypeMismatchInspection`
- **When to use**: Flagging syntax/semantic errors and unused code.
- **When NOT to use**: Do not run inspections on trees containing `PsiErrorElement` if the syntax is incomplete, to prevent cascading false positives.

### Local Quick-Fix (`LocalQuickFix`)
- **Purpose**: Automated code fixes with one click (`Alt + Enter`).
- **IntelliJ APIs**: `LocalQuickFix`, `ProblemDescriptor`, `WriteCommandAction`.
- **Existing Example**: `CompactRemoveUnusedVariableFix`.
- **When to use**: Providing automatic code cleanups for detected inspection issues.

---

## 6. Formatter & Smart Indent

### Formatter Model & Block Hierarchy (`CompactFormattingModelBuilder`, `CompactBlock`)
- **Purpose**: Code formatting (`Ctrl + Alt + L`) and automatic indent on Enter.
- **IntelliJ APIs**: `FormattingModelBuilder`, `AbstractBlock`, `SpacingBuilder`, `Indent`, `Spacing`.
- **Existing Example**: `CompactFormattingModelBuilder`, `CompactBlock`.
- **When to use**: Defining whitespace rules, token spacing, and block indentation.
- **When NOT to use**: Never hardcode character-level text manipulation for formatting; always use `SpacingBuilder` and `Indent` blocks.

---

## 7. Testing Infrastructure

### Platform Test Cases
- **Lexer Tests**: Standalone JUnit 4 (`CompactLexer.start(...)`, `assertTokens(...)`).
- **Parser Tests**: `ParsingTestCase` (`parseFile(...)`, `DebugUtil.psiToString(...)`).
- **Feature Tests**: `BasePlatformTestCase` (`myFixture.configureByText(CompactFileType.INSTANCE, text)`).
- **When to use**: Every new semantic rule, inspection, quick-fix, or parser extension MUST have companion tests in `src/test/java/`.
