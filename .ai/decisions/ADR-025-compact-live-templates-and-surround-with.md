# ADR-025: Parametric Live Templates & Surround-With Statement Wrappers

## Status
Accepted (Phase 19 / Complete)

## Context & Problem Statement
Authoring boilerplate constructs in smart contract development slows down prototyping and increases typographical syntax errors:
1. Writing a new contract with full structure (contract header, ledger block, constructor, exported circuits) from scratch requires typing dozens of lines of standard syntax.
2. Zero-knowledge constructs like `witness` signatures or `transient_hash` require exact parameter signatures.
3. Wrapping existing statements or blocks inside control structures (e.g. wrapping statements inside `if (...) { ... }` or block `{ ... }`) via standard selection (**Surround With**, `Ctrl+Alt+T` / `Cmd+Alt+T`) must preserve code indentation and avoid syntax corruption.

## Authoritative References
1. **Compact Standard Idioms**:
   - `contract Name { ... }`, `ledger { ... }`, `constructor() { ... }`, `export circuit name(): Void { ... }`.
2. **IntelliJ Template & Surrounder Architecture**:
   - `TemplateContextType`, `DefaultLiveTemplatesProvider`
   - `SurroundDescriptor`, `Surrounder`
3. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsLiveTemplatesContext.kt`, `RsSurroundDescriptor.kt`.

## Decision
1. **Isolated Live Template Context (`CompactLiveTemplateContextType`)**:
   - Restricts Compact live templates strictly to `.compact` files (`file instanceof CompactFile || file.getLanguage().isKindOf(CompactLanguage.INSTANCE)`).
   - Prevents template pollution in other languages within polyglot projects.
2. **Standard Live Templates Bundle (`Compact.xml`)**:
   - Pre-configures mnemonic shortcuts with interactive tab-stops:
     - `cct`: Full contract template with interface prototype, ledger block, constructor, and export circuit.
     - `ccti`: Contract implementing an interface.
     - `mod`: Exported module declaration.
     - `cir`: Exported circuit template.
     - `wit`: Private witness declaration.
     - `led`: Ledger state block.
     - `str`: Struct declaration with fields.
     - `enu`: Enum declaration with variants.
     - `thash`: Transient Poseidon hash calculation.
3. **Smart Surround Descriptor (`CompactSurroundDescriptor`)**:
   - `CompactIfSurrounder`: Surrounds selected statements with `if (<caret>) { ... }`.
   - `CompactBlockSurrounder`: Surrounds selected statements with `{ ... }` and reformats indentation.
   - Clamps offsets and supports single-caret selection by expanding to the enclosing statement.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Live template variables (`$NAME$`, `$CIRCUIT$`, `$END$`) use IntelliJ's dynamic macro engine, and surrounders manipulate AST statement blocks via `CodeStyleManager`.
- **Is it non-destructive?**: Yes. Existing statements maintain their exact tokens and are safely indented.

## Feature Implementation Map
- Live Template Context: [`CompactLiveTemplateContextType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateContextType.java)
- Template Definitions: `src/main/resources/liveTemplates/Compact.xml`
- Surround Descriptor: [`CompactSurroundDescriptor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactSurroundDescriptor.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<liveTemplateContext>`, `<defaultLiveTemplates>`, `<lang.surroundDescriptor>`)
- Unit Tests: [`CompactLiveTemplateTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateTest.java)

## Consequences & Future Maintenance
- Additional contract snippets (e.g. ZK mint/burn, state machine patterns) can be added to `Compact.xml` without code modifications.
