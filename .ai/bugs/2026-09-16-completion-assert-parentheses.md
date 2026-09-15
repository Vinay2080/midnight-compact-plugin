# Fix: `assert` Statement Completion Inserts Parentheses and Places Caret Inside

- **Date:** 2026-09-16
- **Feature / Component:** completion / live-templates
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

When invoking basic completion on `assert` (e.g. typing `ass` inside a circuit body and selecting `assert`), the completion contributor inserted bare `assert` without parentheses or arguments. In Compact syntax, an assertion statement is a keyword call expression requiring parentheses: `assert(condition, "failure message");` or `assert(condition);`. 

Furthermore, the live template abbreviation `ass` expanded to `assert $COND$, "$MSG$";` without enclosing parentheses, producing invalid Compact syntax that failed lexing and parsing.

## Context

The user specified the preferred Compact assertion structure:
```compact
assert(_x1 != _x2, "Cannot use the same number twice");
assert(_x1 > 0 && _x2 > 0, "No zero index, board starts at 1");
assert(_x1 <= 20 && _x2 <= 20, "Out of bounds, please keep ships on the board");
```
When completing `assert` in Compact files, developer ergonomics require that completion automatically inserts parentheses `()` and positions the caret inside `assert(<caret>)` with active tab-out scoping so the condition and failure message can be typed immediately.

## Root Cause

Two distinct completion mechanisms emitted bare `assert` without parentheses:

1. **`CompactCompletionContributor` raw keyword handling:**
   `"assert"` was enumerated in `STATEMENT_KEYWORDS` and `VALUE_KEYWORDS` as a raw `String`. Bare string items in `CompletionResultSet` are created with `LookupElementBuilder.create(keyword)` without any `InsertHandler`. When accepted by the user or auto-inserted, only the bare token `assert` was inserted.

2. **`Compact.xml` live template format:**
   In `src/main/resources/liveTemplates/Compact.xml`, the `ass` live template was configured with:
   ```xml
   <template name="ass" value="assert $COND$, &quot;$MSG$&quot;;" ...>
   ```
   This omitted the enclosing parentheses required by Compact's assertion grammar (`assert ( expr , STRING_LITERAL )`).

## Investigation

1. **Grammar & Reference Verification:**
   - Checked `compact/doc/compact-grammar.mdx`: `stmt` definition line 535 specifies `assert ( expr , str ) ;`.
   - Checked `docs/COMPACT_LANGUAGE_SRS.md`: line 517 specifies `| "assert" "(" expr "," STRING_LITERAL ")" ";"`.
   - Checked `CompactParser.java`: line 1106 parses `CompactTokenTypes.ASSERT` via `parseKeywordCallExpression(builder, CompactElementTypes.ASSERT_EXPR)`.
   - Checked existing examples in `compact/examples/assert/`: Modern Compact code uniformly uses `assert(cond, "msg");`.

2. **Prior Architecture Inspection:**
   - Reviewed `CompactParameterizedTypeInsertHandler.java` and prior KB entry `2026-09-16-sized-type-completion-angle-brackets.md` for delimiter insertion, lookahead inspection to prevent double parentheses, caret repositioning, tab-out scope registration via `TabOutScopesTracker`, and EDT template state handling.

3. **Reproduction Tests:**
   - Authored automated tests in `CompactLiveTemplateTest.java` and `CompactCompletionTest.java`. Both failed before production changes, confirming the exact reproduction.

## Solution

1. **Created `CompactAssertInsertHandler.java`:**
   - Implemented `InsertHandler<LookupElement>` following modern Java 25 idioms.
   - Inspects lookahead characters past whitespace for existing `(`.
   - If `(` is absent, inserts `"()"` and places the caret inside at `tailOffset + 1`.
   - If `(` is already present, avoids duplication and places caret directly inside the existing `(`.
   - Registers a tab-out scope with `TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor)` so pressing `Tab` exits past the closing parenthesis.
   - Triggers `AutoPopupController.getInstance(project).scheduleAutoPopup(editor)`.
   - Safely defers caret placement if an active template state is present.

2. **Updated `CompactCompletionContributor.java`:**
   - Removed `"assert"` from plain `STATEMENT_KEYWORDS` and `VALUE_KEYWORDS` arrays.
   - Introduced `createAssertLookupElement()` returning a prioritized lookup element (`10.0`) configured with `withPresentableText("assert")`, `withTailText("(condition, \"message\")", true)`, `withTypeText("statement")`, `.bold()`, and `CompactAssertInsertHandler.INSTANCE`.
   - Added `createAssertLookupElement()` to `addValueCompletions` default/unrestricted branch and `STATEMENT` context.

3. **Updated `src/main/resources/liveTemplates/Compact.xml`:**
   - Updated template `ass` to `value="assert($COND$, &quot;$MSG$&quot;);"`.

## Verification

1. **Reproduction & Targeted Tests:**
   - `CompactLiveTemplateTest.testAssertLiveTemplateFormat`: PASSED.
   - `CompactLiveTemplateTest.testAssertLiveTemplateExpansion`: PASSED.
   - `CompactCompletionTest.testAssertCompletionInsertsParenthesesAndPlacesCaretInside`: PASSED.
   - `CompactCompletionTest.testAssertCompletionWithExistingParenthesesDoesNotDuplicate`: PASSED.
   - `CompactCompletionTest.testAssertCompletionPrefixAutoInsert`: PASSED.

2. **IDE Inspection Loop:**
   - Executed `get_file_problems` and `lint_files` via `idea` MCP server on all modified/new files.
   - Resolved all warnings; 0 errors and 0 warnings reported.

3. **Full Regression Suite:**
   - Ran full project test suite `./gradlew test`.
   - 100% of all unit and integration tests passed with 0 regressions.

## Prevention / Lesson

Keyword call expressions in domain-specific languages (such as `assert`, `disclose`, `emit`) should not be treated as generic bare statement keywords in completion contributors. Whenever a keyword call expression requires parentheses and argument expressions by language grammar, its completion item should always attach a dedicated `InsertHandler` that inserts the call delimiter pair, places the caret inside, guards against delimiter duplication, and registers a tab-out scope.

## Related Files

- `src/main/java/dev/verloren/midnight/completion/CompactAssertInsertHandler.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/main/resources/liveTemplates/Compact.xml`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`
- `src/test/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateTest.java`

## Related ADRs / Context

- `.ai/bugs/2026-09-16-sized-type-completion-angle-brackets.md`
- `docs/COMPACT_LANGUAGE_SRS.md`
