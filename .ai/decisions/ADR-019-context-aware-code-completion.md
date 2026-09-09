# ADR-019: Context-Aware Code Completion & Structural Scoping

## Status
Accepted (Phases 5–6 / Complete)

## Context & Problem Statement
Autocompletion (`Ctrl+Space`) is one of the most critical developer productivity features in any IDE.
However, naive autocompletion engines that dump all known keywords, symbols, and types everywhere cause significant developer frustration:
1. Suggesting statement keywords (`if`, `return`, `for`) at the top level of a file or inside struct body definitions where they are illegal.
2. Suggesting types in value expression contexts (e.g. suggesting `struct Point` inside `const x = <caret>;`).
3. Suggesting value variables inside type annotations (e.g. suggesting local variable `let counter` inside `circuit foo(): <caret>`).
4. Suggesting member completions on non-struct/enum receivers or polluting member access after dot `.` with global keywords.

## Authoritative References
1. **Compact Grammar & Context Boundaries**:
   - [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): Top-level declarations vs contract body items vs block statements vs expression contexts.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsKeywordCompletionContributor.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/lang/core/completion/RsKeywordCompletionContributor.kt) — pattern-based contextual keyword filtering (`declarationPattern`, `statementPattern`).
   - `intellij-scala`: [`ScalaKeywordCompletionContributor.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/lang/completion/ScalaKeywordCompletionContributor.scala).

## Decision
1. **Context Classification (`CompactCompletionContext.classify(PsiElement)`)**:
   - Analyzes the AST parent hierarchy of the `completion-dummy-identifier` node at the caret.
   - Classifies context into:
     - `TOP_LEVEL`: Program and module header declarations (`pragma`, `include`, `import`, `export`, `contract`, `module`).
     - `CONTRACT_BODY`: Contract elements (`ledger`, `constructor`, `witness`, `circuit`, `pure circuit`).
     - `STATEMENT`: Inside circuit or constructor blocks (`const`, `if`, `for`, `return`, `assert`).
     - `TYPE_POSITION`: After `:`, `as`, in parameter types, or generic type arguments (`Field`, `Boolean`, `Bytes`, `Uint`, struct/enum types).
     - `MEMBER_ACCESS`: After `.` on an expression (struct fields, enum variants).
     - `EXPRESSION_VALUE`: Right-hand side of assignments, returns, call arguments (local variables, parameters, circuits, `true`, `false`, `disclose`).
2. **Namespace-Filtered Symbol Contribution**:
   - When in `TYPE_POSITION`: Only queries `CompactResolveUtil.collectTypeDeclarations(place)` and built-in type constants (`CompactTokenTypes.BUILTIN_TYPES`).
   - When in `EXPRESSION_VALUE`: Only queries `CompactResolveUtil.collectValueDeclarations(place)`.
3. **Dot Member Completion**:
   - Dispatches via `CompactStructFieldReference` or `CompactEnumMemberReference` to only suggest fields or variants belonging to the resolved receiver type, suppressing top-level keywords.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Context classification inspects PSI element types (`CompactElementTypes.CONTRACT_DEFINITION`, `CompactElementTypes.BLOCK`, etc.) rather than string matching.
- **Does it prevent clutter?**: Yes. Developers only see suggestions that are syntactically and semantically valid at that exact cursor location.

## Feature Implementation Map
- Contributor: [`CompactCompletionContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java)
- Context Classifier: `CompactCompletionContext.java`
- Resolver Hooks: `CompactResolveUtil.collectValueDeclarations`, `collectTypeDeclarations`
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<completion.contributor>`)
- Unit Tests: [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java)

## Consequences & Future Maintenance
- When adding new language constructs, register them under the appropriate context in `CompactCompletionContext` to maintain clean auto-completion lists.
