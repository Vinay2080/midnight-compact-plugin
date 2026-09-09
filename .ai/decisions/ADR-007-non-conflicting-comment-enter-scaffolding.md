# ADR-007: Non-Conflicting Multi-Asterisk Comment & Doc Scaffolding

## Status
Accepted (Implemented in Phase 28 / v1.2.4)

## Context & Problem Statement
When pressing Enter after opening comments (`/**`, `/****`, `/*`) or within comments, two bugs occurred:
1. Typing `/****` followed by `Enter` caused duplicate asterisks (`*  *`) and duplicate closing tags (`*  */` followed by `*/`).
2. The extra trailing `*/` produced confusing syntax errors at the file level (`parse error: found "*" looking for a program element or end of file`), because outside a comment block `*/` is parsed as binary multiplication `*` followed by division `/`.
3. Typing `/*` on the same line after an existing closed comment `*/` (e.g. `/* first */ /*<caret>`) did not scaffold the new comment.

## Authoritative References
1. **Compact Upstream Compiler Lexer** ([`compact/compiler/lexer.ss:320-358`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/lexer.ss#L320-L358)):
   - Single line comments: `//` consumes until newline or EOF.
   - Block comments: `/*` consumes until `*/`.
   - Nested block comments are strictly prohibited:
     ```scheme
     (define-state-case maybe-nested-comment c
       [eof (unexpected c)]
       [#\* (nested-comment-error)]
       [else (lex-block-comment c)])
     ```
   - Any unclosed block comment triggers `unexpected end of file`.
   - Any dangling `*/` is parsed as two operators (`binop *` then `binop /`), causing top-level syntax errors.
2. **Production JetBrains Implementations**:
   - `intellij-scala`: [`ScalaCommenter.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/highlighter/ScalaCommenter.scala) implements `CodeDocumentationAwareCommenter`. In Scala, the IntelliJ platform's native `EnterHandler` automatically handles `/**` and multi-asterisk doc comments without needing custom regex string replacements.
   - `intellij-rust`: [`RsCommenter.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/commenter/RsCommenter.kt) & [`RsEnterInLineCommentHandler.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/typing/RsEnterInLineCommentHandler.kt).

## Decision
1. **Delegate Native Doc Comments to Platform Commenter**:
   `CompactCommenter` implements `CodeDocumentationAwareCommenter` declaring prefix `/**`, line prefix `*`, and suffix `*/`.
2. **Eliminate Duplicate Processing in `EnterHandlerDelegate`**:
   In `CompactDocCommentEnterHandler`, if the platform's commenter already handled the enter (i.e. if the line already starts with `*` or was an opened `/**`), the handler returns `Result.Continue` immediately without inserting anything.
3. **Dedicated Plain Block Comment Handling (`/* ... */`)**:
   Our custom handler activates only for plain block comments (`/*`) where the platform does not natively insert line prefixes.
4. **Multi-Comment Line Parsing**:
   Inspect only the last comment delimiter on the preceding line (`lastIndexOf("/*")` vs `lastIndexOf("*/")`) so that comments opened after closed comments on the same line (`/* first */ /*`) scaffold properly.
5. **Lookahead Closing Detection**:
   Inspect subsequent lines before inserting `*/`. If a closing `*/` already exists further down in the block, only insert the line prefix ` * ` and do not duplicate `*/`.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Instead of hardcoding checks like `if (text.equals("/**"))`, it relies on the platform commenter state machine and semantic delimiter positions (`lastOpen > lastClose`).
- **Does it work for multi-asterisk comments?**: Yes. `/**`, `/***`, `/****` are all recognized by the platform as doc comment variants and formatted cleanly with a single leading asterisk.
- **Does it prevent compiler syntax errors?**: Yes. Because closing `*/` is never duplicated, files remain syntactically valid and no orphan `*` / `/` operators reach the compiler parser.

## Feature Implementation Map
- Commenter: [`CompactCommenter.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactCommenter.java)
- Enter Handler: [`CompactDocCommentEnterHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactDocCommentEnterHandler.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.commenter>` and `<enterHandlerDelegate>`)
- Unit Tests: [`CompactDocCommentEnterTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactDocCommentEnterTest.java)

## Consequences & Future Maintenance
- Maintain decoupling between `CompactCommenter` (doc comments) and `CompactDocCommentEnterHandler` (plain block comments). Do not re-add doc comment string injection to the enter handler.
