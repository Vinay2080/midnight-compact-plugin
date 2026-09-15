# Code and Declaration Completions Triggering Inside Comments and Documentation Blocks

- **Date:** 2026-09-16
- **Feature / Component:** completion / live-templates
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

Typing prefixes like `ledg`, `led`, `cir`, or other keywords followed by `Enter` or `Tab` inside single-line comments (`// ...`), block comments (`/* ... */`), or documentation comments (`/** ... */`, `/// ...`) in Compact files (such as `src/main/resources/stdlib/zkir-v3-library.compact`) erroneously offered and expanded code completion lookup items and live templates into full declaration structures (e.g., `ledger <name>: <type>;`).

## Context

When editing comments or writing docstrings in Compact source files or stdlib files, users typing ordinary prose (e.g. referencing `ledger` or starting an abbreviation) were interrupted by IDE keyword suggestions, template expansions, and auto-insertion of smart contract syntax directly inside documentation text.

## Root Cause

Two interrelated mechanisms were triggering expansions inside comment contexts:

1. **Unfiltered Completion Contributor Pattern**:
   `CompactCompletionContributor` registered its provider via `PlatformPatterns.psiElement().withLanguage(CompactLanguage.INSTANCE)` without excluding `PsiComment`. Because comments belong to `CompactLanguage`, the contributor was executed for carets positioned inside comments.
2. **False Declaration Classification by Context Classifier**:
   `CompactCompletionContext.classify(@NotNull PsiElement position)` called `PsiTreeUtil.prevVisibleLeaf(position)`. For a caret positioned inside a `PsiComment`, `prevVisibleLeaf` looked backwards *past* the entire comment token, retrieving the preceding semantic code leaf (such as a semicolon `;` or open brace `{`). As a result, `isDeclarationOrStatementStart(previous)` evaluated to `true`, mistakenly categorizing the position inside the comment as `Kind.KEYWORD`.
3. **Overly Broad Live Template Context**:
   `CompactLiveTemplateContextType.isInContext` only verified that the target file was a `CompactFile`. It did not check whether the caret offset resided within a comment or documentation element, allowing live template expansion triggers (like `ledg\t`) to fire within comments.

## Investigation

Inspection of IntelliJ platform best practices and reference language plugins (such as `intellij-rust`'s `RsKeywordCompletionContributor` and `RsContextType`) confirmed that code completion and live template contexts must explicitly filter out comments. Specifically:
- `RsPsiPattern.kt` restricts keywords to non-comment elements and skips comments and whitespace when resolving preceding code tokens.
- `RsContextType.kt` explicitly returns `false` from `isInContext` when `element is PsiComment`.

In `midnight-compact-plugin`:
- `CompactTokenSets.COMMENTS` contains `LINE_COMMENT`, `BLOCK_COMMENT`, and `UNTERMINATED_BLOCK_COMMENT`.
- Compact doc comments wrap delegate `PsiComment` instances via `CompactDocComment` (implementing `PsiDocCommentBase`).
- Checking `position instanceof PsiComment`, `PsiTreeUtil.getParentOfType(position, PsiComment.class, false) != null`, and `CompactTokenSets.COMMENTS.contains(position.getNode().getElementType())` reliably identifies any comment or doc comment context.

## Solution

A multi-layered defense-in-depth approach was implemented:

1. **`CompactCompletionContributor.java`**:
   - Updated the completion contributor registration pattern with `.andNot(PlatformPatterns.psiComment()).andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiComment()))`.
   - Added an immediate early return in `addCompactCompletions` if `CompactCompletionContext.isComment(position)` is true.
   - Refactored preceding leaf searches (`isReturnContext`, `getExpectedType`, `addMemberCompletions`) to use `prevNonCommentLeaf(position)`.

2. **`CompactCompletionContext.java`**:
   - Added `isComment(@NotNull PsiElement position)` checking `PsiComment` inheritance, comment parentage, and `CompactTokenSets.COMMENTS` token types.
   - Added `prevNonCommentLeaf(@NotNull PsiElement position)` to reliably locate preceding semantic code leaves while skipping non-semantic comment trivia.
   - In `classify(@NotNull PsiElement position)`: immediately return `Kind.NONE` if `isComment(position)` is true.
   - In `isExportPreceding(@NotNull PsiElement position)`: immediately return `false` if `isComment(position)` is true.

3. **`CompactLiveTemplateContextType.java`**:
   - Updated `isInContext(@NotNull TemplateActionContext templateActionContext)` to inspect the PSI element at `templateActionContext.getStartOffset()` and return `false` if `CompactCompletionContext.isComment(element)` is true.

## Verification

The fix was validated through unit tests in `CompactCompletionTest.java` and `CompactLiveTemplateTest.java`:
- `CompactCompletionTest.testNoCompletionInsideLineComment`: Verified zero completion lookups inside `// led<caret>`.
- `CompactCompletionTest.testNoCompletionInsideBlockComment`: Verified zero completion lookups inside `/* led<caret> */`.
- `CompactCompletionTest.testNoCompletionInsideDocComment`: Verified zero completion lookups inside `/** led<caret> */`.
- `CompactCompletionTest.testCommentContextClassificationReturnsNone`: Verified `classify` returns `Kind.NONE` across line, block, and doc comments.
- `CompactCompletionTest.testTopLevelCompletionAfterCommentWorks`: Verified top-level declaration completions (`circuit`, `export circuit`) remain fully functional when preceded by comments.
- `CompactLiveTemplateTest.testLiveTemplateContextNotInComment`: Verified `isInContext` returns `false` inside line comments.
- `CompactLiveTemplateTest.testLiveTemplateContextNotInDocComment`: Verified `isInContext` returns `false` inside doc comments.
- `CompactLiveTemplateTest.testLiveTemplateExpansionSuppressedInsideComment`: Verified typing `ledg\t` inside a line comment does not expand to `export ledger`.
- Full test suite verified with `./gradlew test --rerun`: 100% pass (0 failures, 0 warnings).

## Prevention / Lesson

Completion contributors and live template contexts must always guard against comment and documentation nodes. AST leaf traversals designed to determine syntactic context (like statement starts or preceding modifiers) must never conflate the absence of a code token at the caret with trivia elements like comments; they should explicitly traverse semantic non-comment leaves.

## Related Files

- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContext.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/main/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateContextType.java`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`
- `src/test/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateTest.java`

## Related ADRs / Context

- None
