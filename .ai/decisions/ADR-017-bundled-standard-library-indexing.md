# ADR-017: Bundled Compact Standard Library & In-Memory Virtual Indexing

## Status
Accepted (Phases 11–12 / Complete)

## Context & Problem Statement
Compact contracts rely on a rich set of built-in standard library constructs:
- Cryptographic hash primitives (`transient_hash`, `persistent_hash`).
- Native ledger state cells (`Cell<T>`, `Map<K, V>`).
- ZKIR (Zero-Knowledge Intermediate Representation) v3 circuits.

If the plugin depends on a local compiler installation just to resolve standard library types:
1. New developers opening a project without a compiler configured immediately see red squiggly errors on all standard library references.
2. Code completion for standard library circuits is unavailable offline.
3. Writing temporary disk files for stdlib definitions risks cache pollution and permission conflicts.

## Authoritative References
1. **Official Upstream Compact Standard Library**:
   - [`compact/compiler/standard-library.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/standard-library.compact): Official declarations of standard library functions, types, and circuits.
   - [`compact/compiler/zkir-v3-library.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/zkir-v3-library.compact): Low-level ZK primitives and constraints.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: Bundled standard library source resolution using in-memory virtual files.

## Decision
1. **Bundled Resource Assets**:
   - Bundle `standard-library.compact` and `zkir-v3-library.compact` directly in the plugin JAR under `/stdlib/`.
2. **Project-Level Virtual Indexing Service (`CompactStdlibService`)**:
   - Annotated with `@Service(Service.Level.PROJECT)` for lifecycle management.
   - Creates in-memory `LightVirtualFile` instances and parses them into `CompactFile` ASTs via `PsiFileFactory.getInstance(project).createFileFromText()`.
   - Thread-safe caching with double-checked locking, automatically invalidating when PSI trees expire.
3. **Seamless Resolution Integration**:
   - `CompactResolveUtil` queries `CompactStdlibService.getInstance(project).getStandardLibraryFiles()` as the final fallback in the scope chain.
   - Allows Go to Declaration to jump directly to the read-only standard library file, showing official signatures and documentation.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. The standard library files are real `.compact` source files parsed by the exact same `CompactParser` and `CompactLexer`, ensuring 100% fidelity with language syntax.
- **Is it memory-efficient?**: Yes. Cached in memory per project and shared across all files without touching the local disk.

## Feature Implementation Map
- Service: [`CompactStdlibService.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java)
- Bundled Files:
  - `src/main/resources/stdlib/standard-library.compact`
  - `src/main/resources/stdlib/zkir-v3-library.compact`
- Resolver Hook: `CompactResolveUtil.collectDeclarations`
- Registration: Service declared in [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)
- Unit Tests: [`CompactStdlibServiceTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/stdlib/CompactStdlibServiceTest.java)

## Consequences & Future Maintenance
- When upstream Midnight updates standard library definitions, update the `.compact` files under `src/main/resources/stdlib/` and rerun `./gradlew test`.
