# Current Handoff

## Current Feature
External npm Dependency Resolution for Compact Smart Contracts.

## Status
Implemented first-class external npm dependency resolution for regular and scoped npm packages (e.g. `vitest`, `@midnight-ntwrk/compact-runtime`) inside `node_modules`. Properly resolves package entries via `package.json` (`types`, `typings`, `exports`, `module`, `main`), extracts exported symbols from `.d.ts`, `.ts`, and `.js` declarations with full re-export support (`export * from '...'`, `export { ... } from '...'`, ambient `declare module '...'`), differentiates valid from non-existent imported symbols without suppressing inspections, preserves local Compact cross-file resolution, and enables reference navigation (Go To Declaration). All 280 automated unit tests passing (100% success rate across 27 test suites).

## Recently Completed
- **Subsystem Architecture (`dev.verloren.midnight.resolve.npm.*`)**:
  - [`CompactNpmPackageSpec`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmPackageSpec.java): Robust parsing of regular and scoped package names and subpaths.
  - [`CompactNpmPackageFinder`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmPackageFinder.java): Upward directory walker from file directory, project content roots, project base path, and `@types/*` packages.
  - [`CompactNpmPackageMetadata`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmPackageMetadata.java): Reads `package.json`, resolves conditional exports (`exports["."]`, `"types"`, `"import"`, `"default"`), `types`/`typings`, `module`, `main`, and companion `.d.ts` alongside `.js`.
  - [`CompactNpmSymbolExtractor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmSymbolExtractor.java): Fast, resilient parser extracting exported symbols (functions, constants, classes, interfaces, types, enums, ambient modules, re-exports) with cycle guards.
  - [`CompactNpmSymbolElement`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmSymbolElement.java): Synthetic PSI declaration element implementing `CompactNamedElement` for references, inspections, and declaration offset navigation.
  - [`CompactNpmResolver`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/npm/CompactNpmResolver.java): Integration facade with real-time `CachedValuesManager` caching on `PsiModificationTracker.MODIFICATION_COUNT`.
- **PSI & Scope Integration**:
  - Integrated with [`CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java) `resolveImportElementSource` and `isInNamespace`.
  - Integrated with [`CompactImportReference`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactImportReference.java) for package path navigation.
  - Integrated with [`CompactSymbols`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/symbol/CompactSymbols.java) and [`CompactHighlightingAnnotator`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactHighlightingAnnotator.java).
- **Unit Test Suite (`CompactNpmDependencyResolveTest`)**:
  - 13 comprehensive tests covering valid/invalid local imports, valid/invalid `vitest` imports, valid/invalid scoped `@midnight-ntwrk/compact-runtime` imports, uninstalled packages, multiple symbols, relative vs external coexistence, real-time edit responsiveness, navigation, and ambient declarations.

## Tests
- **280/280 tests passing** (0 failures, 0 skipped, 100% success rate across 27 test suites).
- Verified via `./gradlew test`.

## Next Feature Options
1. **Standard Library Integration**: Index and bundle built-in definitions from `compact/compiler/standard-library.compact` and `zkir-v3-library.compact` so standard library symbols have first-class resolution and docs.
2. **Compiler CLI Integration (`compactc`)**: Background external linter and diagnostics.

## Relevant Context
- Architecture details: [architecture.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)
- Current state: [current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)
- IntelliJ patterns: [intellij-patterns.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)
