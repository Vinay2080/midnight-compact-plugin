# Project Goal

Build an IntelliJ IDEA plugin for the Midnight Compact language used for Midnight smart contracts.

Core technologies and feature areas:

- IntelliJ Platform plugin in Kotlin
- Compact language support
- Grammar-Kit parser
- JFlex lexer
- Generated PSI
- ParserDefinition
- Syntax highlighting
- References
- Completion
- Inspections
- Formatter

## Progress

- [x] Lexer
- [x] Syntax Highlighting
- [x] GrammarKit BNF
- [x] Parser
- [x] PSI Generation
- [x] ParserDefinition
- [ ] References
- [ ] Completion
- [ ] Rename
- [ ] Find Usages
- [ ] Type Inference
- [ ] Inspections
- [ ] Formatter

## Compiler Files Reviewed

| Compiler File                   | Importance | Grammar Changes Needed | Semantic Features Discovered          | Plugin Impact                             |
|---------------------------------|------------|------------------------|---------------------------------------|-------------------------------------------|
| Compact language grammar source | High       | Yes                    | Syntax model for Compact programs     | Lexer, BNF grammar, parser, generated PSI |
| lexer.l                         | High       | Yes                    | Tokenization rules for Compact syntax | JFlex lexer and token types               |

## Missing

- Go To Declaration using Compact references
- Reference resolution for imports, modules, declarations, and type references
- Completion for keywords, declarations, imports, types, and struct fields
- Rename support
- Find Usages support
- Type inference
- Inspections
- Formatter

## Decisions

- Parser follows the official Compact language grammar where available.
- Compiler optimizations are ignored unless they affect IDE-visible semantics.
- Generated PSI under `src/main/gen` must never be edited manually.
- References should be implemented using IntelliJ `PsiReference` APIs.
- `AGENTS.md` is the permanent engineering notebook and must be updated after every completed task that changes project
  knowledge or status.

## Current Architecture

```text
Compact.flex
↓
JFlex CompactLexer
↓
CompactTokenTypes
↓
Compact.bnf
↓
Grammar-Kit CompactParser
↓
Generated PSI
↓
CompactParserDefinition
↓
CompactFileType
↓
CompactSyntaxHighlighter
↓
References (pending)
↓
Completion (pending)
↓
Inspections (pending)
↓
Formatter (pending)
```

## Next Task

Implement Go To Declaration using Compact `PsiReference` support.

## Technical Debt

- `README.md` still contains mostly generated IntelliJ plugin template content.
- No automated parser or lexer regression tests are recorded in the project tree.
- Compiler file review history needs exact upstream file names and feature notes as future compiler files are analyzed.

## Lessons Learned

- Keep generated PSI changes out of manual edits; update the Grammar-Kit BNF and regenerate instead.
- Keep this notebook concise and focused on engineering continuity, not end-user README content.

## Session Log

### 2026-07-29

- Improved `src/main/grammar/Compact.bnf` against `references/compact-grammar.mdx`, cross-checked with
  `references/lsrc.json`: factored repeated Grammar-Kit rules, added targeted pins/recovery, preserved accepted syntax,
  and verified with `.\gradlew.bat build` from repo root.
- Synchronized the plugin with the `Lsrc.json` `External-Declaration` (`edecl`) and `Program-Element` sections by adding
  handwritten support for source `external` declarations.
- Changed `Compact.bnf` and `Compact.flex` so `external` is a real Compact keyword and parses as
  `export? external function-name type-param* arg* type` using the existing generic parameter, simple parameter list,
  and return type syntax; Kotlin token/highlighter wrappers were left unchanged until generated `CompactTypes` is
  refreshed.
- Initially verified `Lsrc.json` `External-Declaration` and `Program-Element` coverage against the plugin; the later
  generated grammar review added the missing `contract implements type;` keyword accuracy described below.
- Reconciled the broader source syntax references: `compact-grammar.mdx` documents `contract implements type;`, so
  `implements` is now a dedicated grammar/lexer token instead of being accepted through the broad `RESERVED_KEYWORD`
  token. This preserves the `implements_declaration` PSI rule name while making PSI regeneration add/use an `IMPLEMENTS`
  token.
- Noted a reference mismatch: `lsrc.json` includes standalone `External-Declaration` (`edecl`) in `Program-Element`,
  while `compact-grammar.mdx` does not list a standalone external declaration. The plugin keeps `external_declaration`
  because `lsrc.json` is the AST/source-language reference for that node, but this section is only partially
  synchronized across all references until the upstream grammar references agree.
- Created `AGENTS.md` as the permanent engineering notebook for the Midnight Compact IntelliJ plugin.
- Recorded current implemented plugin layers: lexer, syntax highlighting, Grammar-Kit BNF, parser, generated PSI, and
  parser definition.
- Recorded pending IDE features: references, completion, rename, find usages, type inference, inspections, and
  formatter.
- Debugged a Grammar-Kit parser failure where `export enum ...` after an import caused the parser to report
  `'import' unexpected`: generated `export_form` was pinned on `EXPORT`, so it committed before `enum_declaration`
  could parse exported declarations. Changed `export_form` in `Compact.bnf` to pin after `EXPORT LBRACE` (`pin=2`).
  Parser/PSI regeneration is still required because no Grammar-Kit generator task or jar is checked into the project.

### 2026-07-30

- Audited `Compact.bnf` against `references/compact-grammar.mdx`, `references/lsrc.json`, `references/lexer.ss`, and
  syntax examples in `references/type-example.compact`.
- Updated `nat` handling in `Compact.bnf` so plugin lexer tokens for decimal, hex, binary, and octal field literals are
  accepted everywhere the official compiler grammar consumes `nat`/`field`: version atoms, generic size arguments, type
  sizes, term field literals, `slice<...>`, and `pad(...)`.
- Fixed Grammar-Kit ordered-choice behavior for `if/else` statements by trying `stmt0` before the one-armed `if`
  fallback, preserving the official dangling-else grammar while avoiding a stranded `else` token.
- Verified with `.\gradlew.bat build`; parser/PSI regeneration is still required after the BNF changes.

# Development Constraints

## Source of Truth

- Never modify generated files.
- Always modify the source from which generated files are produced.

## Forbidden Files

Never edit any file under:

- src/main/gen/**
- build/**
- out/**
- .gradle/**
- .idea/**

Never edit generated parser artifacts, including:

- *_Parser.java
- *_Parser.kt
- *_Types.java
- *_Types.kt
- *_TokenTypes.java
- *_ElementTypes.java
- generated PSI implementations
- generated visitors
- generated factories

These files must only change by regeneration.

## Grammar

If parser behavior needs to change:

1. Compare with `references/compact-grammar.mdx`; use `references/lsrc.json` when AST shape matters.
2. Modify `Compact.bnf`
3. Regenerate parser and PSI
4. Never patch generated parser code manually

Grammar-Kit notes:

- Preserve official Compact syntax exactly; do not simplify ambiguous syntax without proof from references.
- Prefer factoring repeated prefixes, removing left recursion, and adding targeted `pin` / `recoverWhile`.
- Be careful pinning generic arguments after `<`; comparisons like `a < b` need backtracking.

## Lexer

If tokenization changes:

1. Modify the JFlex `.flex` file.
2. Regenerate the lexer.
3. Never edit generated lexer code.

## PSI

If PSI needs new elements:

- Update `Compact.bnf`
- Regenerate PSI
- Extend handwritten PSI only when necessary

Never edit generated PSI classes directly.

## Existing Code

Prefer modifying existing handwritten files rather than creating new ones.

Avoid duplicate implementations.

Reuse existing utilities whenever possible.

## Before Creating Files

Before creating any new file:

1. Search the project for an existing implementation.
2. Extend it if appropriate.
3. Create a new file only if no suitable location exists.

## Before Changing Code

Always verify:

- the feature is not already implemented
- the compiler file actually requires the change
- the change belongs in an IntelliJ plugin rather than the compiler

## Allowed Locations

Handwritten source only:

- src/main/kotlin/**
- src/main/java/**
- src/main/resources/**
- src/main/grammars/**

## Regeneration Rule

Whenever `Compact.bnf` changes:

- Regenerate Parser
- Regenerate PSI

Whenever `.flex` changes:

- Regenerate Lexer

Do not manually synchronize generated code.

## Scope

Only implement features that affect the editor:

- parsing
- PSI
- references
- completion
- rename
- find usages
- inspections
- formatting

Do not implement compiler passes, optimizations, lowering, code generation, or runtime behavior.

## Every Change Must

- compile successfully
- preserve existing functionality
- minimize modified files
- include an explanation of why the change was necessary

# References

- the references directory includes files from the official code base.
