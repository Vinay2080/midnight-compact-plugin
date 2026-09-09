# ADR-020: Symbol Navigation, Global Search, & Type Declaration Provider

## Status
Accepted (Phases 7, 25 / Complete)

## Context & Problem Statement
Fast code navigation is vital when reading large smart contract projects:
1. **Go to Declaration (`Ctrl+B` / `Cmd+B`)**: Developers expect clicking an identifier or reference to navigate immediately to its declaration, whether defined locally, in an included file, or in the bundled standard library.
2. **Go to Type Declaration (`Ctrl+Shift+B` / `Cmd+Shift+B`)**: When reading a statement like `const user: Account = fetchAccount();`, invoking Go to Declaration on `user` navigates to `const user`. However, developers frequently want to jump directly to `struct Account` definition itself without navigating to the type annotation first.
3. **Global Search (`GotoClass` & `GotoSymbol`)**: Navigating across large codebases requires searching for contracts (`Ctrl+N`) or symbols (`Ctrl+Alt+Shift+N`) by name with fuzzy search.

## Authoritative References
1. **IntelliJ Navigation APIs**:
   - `GotoDeclarationHandler`
   - `TypeDeclarationProvider`
   - `ChooseByNameContributorEx` / `GotoClassContributor` / `GotoSymbolContributor`
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsGotoDeclarationHandler.kt`, `RsTypeDeclarationProvider.kt`.
   - `intellij-scala`: `ScalaTypeDeclarationProvider.scala`.

## Decision
1. **Multi-Stage Declaration Resolver (`CompactGotoDeclarationHandler`)**:
   - Inspects references directly on the leaf token.
   - If not found, inspects references on parent expression nodes (`CompactReferenceExpr`, `CompactTypeReference`, `CompactCallExpr`).
   - If invoked on a raw identifier token, queries `CompactResolveUtil.resolveValue` followed by `CompactResolveUtil.resolveType`, returning declaration targets across files and standard library sources.
2. **Type Declaration Provider (`CompactTypeDeclarationProvider`)**:
   - Implements `TypeDeclarationProvider.getSymbolTypeDeclarations(PsiElement)`.
   - Evaluates the structural type of the target element using `CompactTypeInferenceUtil.inferType(expr)`.
   - If the type is a user-defined struct, enum, or contract interface, resolves and navigates directly to the defining type declaration AST element.
3. **Global Symbol Indexing (`CompactGotoClassContributor` & `CompactGotoSymbolContributor`)**:
   - Scans project scope using IntelliJ's `FileTypeIndex.processFiles(CompactFileType.INSTANCE, ...)` to find all matching declaration names.
   - `GotoClass`: Indexes contracts, modules, structs, and enums.
   - `GotoSymbol`: Indexes contracts, circuits, witnesses, ledger fields, and type aliases.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Navigation leverages dynamic PSI tree walking and `CompactResolveUtil` rather than hardcoded string lookup tables.
- **Does it support standard library?**: Yes. Navigation extends into bundled virtual files in `CompactStdlibService`.

## Feature Implementation Map
- Goto Declaration: [`CompactGotoDeclarationHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/navigation/CompactGotoDeclarationHandler.java)
- Type Declaration: [`CompactTypeDeclarationProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/navigation/CompactTypeDeclarationProvider.java)
- Goto Class: [`CompactGotoClassContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/navigation/CompactGotoClassContributor.java)
- Goto Symbol: [`CompactGotoSymbolContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/navigation/CompactGotoSymbolContributor.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<gotoDeclarationHandler>`, `<typeDeclarationProvider>`, `<gotoClassContributor>`, `<gotoSymbolContributor>`)
- Unit Tests: [`CompactNavigationTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/navigation/CompactNavigationTest.java)

## Consequences & Future Maintenance
- When adding new nominal types (e.g. interfaces or unions), ensure `CompactTypeDeclarationProvider` maps them to their respective declaration PSI nodes.
