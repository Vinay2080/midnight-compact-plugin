# Double Advancement and Character Swallowing on Unrecognized Tokens

- **Date:** 2026-10-01
- **Feature / Component:** lexer / tokenization
- **Severity:** High
- **Status:** Resolved

## Symptoms

When `CompactLexer` encountered any unrecognized character (such as `@`, `~`, or `` ` ``), it consumed two characters from the input buffer instead of one. Consequently, whatever valid token directly followed the unrecognized character (e.g. the variable name in `@a` or the integer literal in `~1`) was silently swallowed or corrupted into a syntax error.

## Context

During a full engineering verification of `dev.verloren.midnight.lexer.CompactLexer` against the architectural guidelines and `review-modify-optimize.md` framework, the `advance()` character dispatch loop was inspected for edge-case correctness and boundary safety.

## Root Cause

In `CompactLexer.java`, the main `switch (ch)` block in `advance()` handled unrecognized characters via:

```java
      default:
        finishHelper(TokenType.BAD_CHARACTER);
    }

    finishHelper(CompactTokenTypes.BAD_CHARACTER);
```

Because the `default:` branch lacked a `return;` statement, execution fell through the bottom of the `switch` statement and called `finishHelper` a second time. Each `finishHelper` call executes `position++; finish(...)`. As a result, `position` was incremented twice for a single bad character, effectively swallowing the adjacent character.

Additionally, `CompactLexer.java` had an upward dependency import on `dev.verloren.midnight.parser.CompactParserDefinition` (solely referenced in Javadoc), violating the strict downward dependency hierarchy.

## Investigation

A targeted unit test in `LexerTest.java` was authored to assert single-character `BAD_CHARACTER` isolation:
```java
assertTokens("@a", CompactTokenTypes.BAD_CHARACTER, CompactTokenTypes.IDENTIFIER);
```
Running this test immediately failed with an assertion mismatch: the lexer produced `BAD_CHARACTER` spanning 2 characters (offset 0 to 2) and failed to emit the subsequent `IDENTIFIER` (`a`).

## Solution

1. Added `return;` to the `default:` branch of the `switch` block in `advance()` and eliminated the trailing redundant `finishHelper(...)` invocation.
2. Standardized token emission to `CompactTokenTypes.BAD_CHARACTER`.
3. Removed the upward import `dev.verloren.midnight.parser.CompactParserDefinition` to guarantee strict package isolation.
4. Added reproduction and mirror test assertions in `LexerTest.java` covering unrecognized characters and user-defined struct tokens.

## Verification

1. Unit tests:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "dev.verloren.midnight.lexer.*"
   ```
   All lexer unit tests passed with 0 failures.
2. Architecture verification:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "CompactArchitectureTest"
   ```
   ArchUnit layer isolation verified with 0 violations.

## Prevention / Lesson

Every branch in a state machine or lexical scanner dispatch switch must explicitly terminate with a `return` or `break`. Any code positioned outside the switch body must either be unreachable or intentionally part of a shared exit path. Switch expressions or compiler linter rules on fallthrough prevent silent double-advancement errors.

## Related Files

- `src/main/java/dev/verloren/midnight/lexer/CompactLexer.java`
- `src/test/java/dev/verloren/midnight/lexer/LexerTest.java`
- `src/test/java/dev/verloren/midnight/architecture/CompactArchitectureTest.java`

## Related ADRs / Context

- `ADR-001-handwritten-lexer-and-parser.md`
