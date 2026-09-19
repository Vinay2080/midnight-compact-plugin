# Path Rules: PSI & Parser Subsystems

**Scope:** `src/main/java/dev/verloren/midnight/parser/**`, `src/main/java/dev/verloren/midnight/lexer/**`, `src/main/java/dev/verloren/midnight/psi/**`

## Invariants & Guardrails

1. **Token Advancement Guarantee (Anti-Freeze Guard)**:
   - In all parser loops (`CompactParser`), the builder MUST advance tokens (`builder.advanceLexer()`) on every iteration.
   - If an unexpected token occurs, remap or wrap in `builder.error(...)` and advance. Never write a `while` loop that can spin on the same token without advancing.

2. **Tolerance for Incomplete Code**:
   - Guard every PSI node navigation against `null` and `PsiErrorElement`.
   - Never assume an expression has both left and right operands; during typing, the AST is frequently half-formed.

3. **Handwritten Architecture Invariant**:
   - The lexer (`CompactLexer`) and parser (`CompactParser`) are handwritten. Do NOT replace them with Grammar-Kit or JFlex generated files.
   - AST node wrappers must extend `CompactPsiElement` or appropriate typed interfaces in `dev.verloren.midnight.psi`.

4. **Namespace Separation**:
   - Strictly separate `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE`.
   - Resolving a type must never return a variable/constant; resolving an identifier in an expression must never return a struct/type.

5. **Testing Standard**:
   - Every syntax rule must have a golden tree test in `CompactParserTest` matching the golden `.txt` PSI dump.
   - Every operator addition must be verified in the operator precedence and associativity matrix.
