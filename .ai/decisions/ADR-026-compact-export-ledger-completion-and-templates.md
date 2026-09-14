# ADR-026: Comprehensive Export Declarations, Contextual Filtering, & Parametric Live Templates

## Status
Accepted (v1.2.5 / Complete)

## Context & Problem Statement
In Compact smart contracts and modules, `export` is not restricted to ledger fields. The upstream Compact grammar (`compact/compiler/parser.ss`) supports exporting all top-level declaration constructs, optional modifiers, and export selection forms:
```compact
export ledger authority: Bytes<32>;
export circuit transfer(): Void { ... }
export pure circuit verify(): Boolean { ... }
export sealed ledger state: State;
export const MAX_SUPPLY: Uint<64> = 1000;
export struct Account { ... }
export enum Role { ... }
export type Balance = Uint<64>;
export new type TokenId = Bytes<32>;
export module Utils { ... }
export contract Token { ... }
export witness getSecret(): Field;
export { symbol1, symbol2 };
```
Prior to this architectural expansion:
1. **Scope Limitation**: Autocompletion and intentions treated `export` narrowly or exclusively around `ledger`.
2. **Outdated Live Templates**:
   - `led` expanded to an obsolete block structure (`ledger { state: Field; }`).
   - `ledg` defaulted to non-existent `Cell<Field>`.
   - `cct` embedded an obsolete `ledger { $STATE$ }` block instead of an exported ledger field.
   - Missing live templates for exported struct (`expstr`), enum (`expen`), type (`expt`), const (`expconst`), witness (`expw`), and bare const (`const`).
3. **Context-Blind Autocompletion**:
   - Typing `export ` offered indiscriminate top-level keywords (`pragma`, `import`, `include`), which are syntactically invalid following `export`.
   - After modifiers (`export sealed `, `export pure `, `export new `), completions offered unrelated tokens.
   - Top-level autocompletion lacked interactive template scaffolding for exported declaration variants.
4. **Intention Coverage**:
   - `CompactToggleExportIntention` did not support `const` declarations.

## Authoritative References
1. **Compact Compiler Grammar & Semantics**:
   - [`compact/compiler/parser.ss:146-187`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): Dispatch for `EXPORT`:
     - `LBRACE` &rarr; `parseExportForm` (`export { ... };`)
     - Modifiers `PURE`, `SEALED`, `NEW`
     - Constructs: `CIRCUIT`, `LEDGER`, `CONST`, `STRUCT`, `ENUM`, `TYPE`, `MODULE`, `CONTRACT`, `WITNESS`.
   - [`compact/compiler/midnight-ledger.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/midnight-ledger.ss): State variable declaration requirements and type constraints.
2. **JetBrains Platform Completion & Live Template Architecture**:
   - `InsertHandler<LookupElement>`: Non-destructive token insertion, caret positioning, and template delegation.
   - `TemplateManager` & `Template`: Interactive in-editor parameter substitution (`$NAME$`, `$TYPE$`).
   - `PrioritizedLookupElement`: Weighting contextual completions above generic keywords.
3. **Reference Implementations**:
   - `intellij-rust`: [`RsKeywordCompletionContributor.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/lang/core/completion/RsKeywordCompletionContributor.kt) — Contextual keyword filtering following modifiers (`pub`, `async`, `unsafe`).
   - `intellij-scala`: [`ScalaKeywordCompletionContributor.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/lang/completion/ScalaKeywordCompletionContributor.scala).

## Decision
1. **Contextual Classification (`CompactCompletionContext.Kind`)**:
   - Added `AFTER_EXPORT`, `AFTER_SEALED`, `AFTER_PURE`, and `AFTER_NEW` to `CompactCompletionContext.Kind`.
   - When the previous visible token is `EXPORT`, context is classified as `AFTER_EXPORT`.
   - When preceded by `SEALED`, `PURE`, or `NEW`, context is classified respectively as `AFTER_SEALED`, `AFTER_PURE`, or `AFTER_NEW`.
   - In `AFTER_EXPORT`, invalid file headers (`pragma`, `import`, `include`, `export`) are strictly filtered out; all valid exported entities and modifiers are suggested: `ledger`, `circuit`, `const`, `struct`, `enum`, `type`, `module`, `contract`, `witness`, `pure`, `sealed`, `new`, and `{`.
   - In `AFTER_SEALED`, specifically suggests `ledger`.
   - In `AFTER_PURE`, specifically suggests `circuit`.
   - In `AFTER_NEW`, specifically suggests `type`.
2. **Smart Insert Handlers**:
   - `CompactLedgerInsertHandler`: Prepend check for `export `, lookahead guard, interactive live template session with tab-stops ` $NAME$: $TYPE$;` (defaulting to canonical type based on context).
   - `CompactDeclarationInsertHandler`: Universal declaration inserter for all `CompactDeclarationType` values, dynamically building live templates with intelligent scope-based numbering (`compactDeclarationName(...)`) and fallbacks.
3. **Dual Trigger & Fuzzy Lookups**:
   - Registered top-level declaration completions for both bare (`circuit`, `ledger`, `const`, `struct`, `enum`, `type`, `module`, `contract`, `witness`) and exported variants (`export circuit`, `export ledger`, `export const`, etc.) with prioritized weighting.
4. **Parametric Live Templates (`Compact.xml` & `MyMessageBundle.properties`)**:
   - `led`, `ledg`, `ledger`: `export ledger $NAME$: $TYPE$;`
   - `cir`: `export circuit $NAME$($PARAMS$): $RET$ { $END$ }`
   - `expw`: `export witness $NAME$($PARAMS$): $RET$;`
   - `expstr`: `export struct $NAME$ { $FIELDS$ }`
   - `expen`: `export enum $NAME$ { $MEMBERS$ }`
   - `expt`: `export type $NAME$ = $TYPE$;`
   - `const`: `const $NAME$: $TYPE$ = $VALUE$;`
   - `expconst`: `export const $NAME$: $TYPE$ = $VALUE$;`
   - `exp`: `export $END$`
   - `cct`: Contract skeleton updated to use `export ledger $STATE$: $TYPE$;`.
5. **Intention Expansion (`CompactToggleExportIntention`)**:
   - Expanded `findExportableDeclaration` to recognize `CompactElementTypes.CONST_STATEMENT` in addition to existing declarations, enabling in-editor `Alt+Enter` toggling of `export` on `const` statements.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Context classification inspects AST token types (`CompactTokenTypes.EXPORT`, `SEALED`, `PURE`, `NEW`) rather than ad-hoc string comparisons. Insert handlers utilize IntelliJ's native `TemplateManager` and `CompactDeclarationNameGenerator` dynamic numbering engine.
- **Is it thread-safe?**: Yes. Context classification is read-only on the PSI tree (safe on background completion threads). Insert handlers and intentions operate within IntelliJ's write actions on the EDT without retaining stale PSI references.
- **Does it prevent clutter?**: Yes. Disallows illegal statements after `export` and modifiers, and prevents statement keywords from leaking into top-level declarations.

## Feature Implementation Map
- Completion Context: [`CompactCompletionContext.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContext.java)
- Completion Contributor: [`CompactCompletionContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java)
- Universal Declaration Registry: [`CompactDeclarationType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactDeclarationType.java)
- Dynamic Name Generator: [`CompactDeclarationNameGenerator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactDeclarationNameGenerator.java)
- Insert Handlers: [`CompactLedgerInsertHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactLedgerInsertHandler.java), [`CompactDeclarationInsertHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactDeclarationInsertHandler.java)
- Intention Action: [`CompactToggleExportIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/intention/CompactToggleExportIntention.java)
- Live Templates: [`Compact.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/liveTemplates/Compact.xml)
- Message Bundle: [`MyMessageBundle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/messages/MyMessageBundle.properties)
- Unit Tests:
  - [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java)
  - [`CompactLiveTemplateTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateTest.java)
  - [`CompactDeclarationNameGeneratorTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/templates/CompactDeclarationNameGeneratorTest.java)
  - [`CompactPhase28IntentionsTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/intention/CompactPhase28IntentionsTest.java)

## Consequences & Future Maintenance
- The architecture is fully unified: whenever a new declaration type or modifier is introduced into the Compact language, it only needs to be registered in `CompactDeclarationType` and `CompactCompletionContext` to gain completion, template generation, auto-numbering, and intention toggling.
