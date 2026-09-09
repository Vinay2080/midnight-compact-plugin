# ADR-023: Quick Documentation Provider & Interactive Parameter Info

## Status
Accepted (Phases 15–16 / Complete)

## Context & Problem Statement
Smart contract developers rely on rapid API discovery while writing and auditing code:
1. **Quick Documentation (`Ctrl+Q` / `F1` / Hover)**: Hovering over a circuit, witness, ledger field, or standard library hash function must immediately render a clean HTML tooltip displaying its full signature, return type, and preceding doc comment explanation.
2. **Reader Mode**: Modern IntelliJ versions allow rendering doc comments directly in the editor as formatted HTML blocks.
3. **Parameter Info Tooltip (`Ctrl+P`)**: When calling a circuit with multiple parameters, developers need an interactive popup highlighting the parameter currently being typed based on comma counting.

## Authoritative References
1. **IntelliJ Documentation & Parameter Info APIs**:
   - `AbstractDocumentationProvider`
   - `DocumentationMarkup`
   - `ParameterInfoHandler`
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsDocumentationProvider.kt`, `RsParameterInfoHandler.kt`.
   - `intellij-scala`: `ScalaDocDocumentationProvider.scala`.

## Decision
1. **Rich HTML Documentation Generator (`CompactDocumentationProvider`)**:
   - Extracts preceding doc comments (`/** ... */` or `///`) and associated Javadoc-style tags (`@param`, `@return`, `@throws`, `@see`, `@deprecated`).
   - Converts Markdown inline formatting (code backticks, bold, italics, links) into sanitized HTML using `DocumentationMarkup.DEFINITION_ELEMENT`, `CONTENT_ELEMENT`, and `SECTIONS_TABLE`.
   - For circuits/witnesses: renders signature, export modifier, parameter list with inferred types, and return type.
   - For structs: renders field list and types.
   - For enums: renders variant list.
   - Supports Reader Mode via `generateRenderedDoc(PsiComment)`.
2. **Interactive Parameter Info (`CompactParameterInfoHandler`)**:
   - Triggers inside parentheses of `CompactCallExpr` argument lists.
   - Highlights the active parameter in bold according to the caret offset relative to commas.
   - Accurately tracks nested parentheses inside argument sub-expressions.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Signatures are dynamically reconstructed from the live PSI model and inferred types.
- **Does it support stdlib?**: Yes. Hovering over built-in functions (`transient_hash`, `persistent_hash`) renders the full upstream documentation bundled in `CompactStdlibService`.

## Feature Implementation Map
- Documentation Provider: [`CompactDocumentationProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/documentation/CompactDocumentationProvider.java)
- Parameter Info: [`CompactParameterInfoHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parameterInfo/CompactParameterInfoHandler.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.documentationProvider>`, `<codeInsight.parameterInfo>`)
- Unit Tests:
  - [`CompactDocumentationProviderTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/documentation/CompactDocumentationProviderTest.java)
  - [`CompactParameterInfoHandlerTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/parameterInfo/CompactParameterInfoHandlerTest.java)

## Consequences & Future Maintenance
- When adding new language features or doc tags, ensure `CompactDocumentationProvider` formats them inside the appropriate section block.
