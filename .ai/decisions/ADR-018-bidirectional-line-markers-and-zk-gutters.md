# ADR-018: Bidirectional Gutter Line Markers & ZK Privacy Indicators

## Status
Accepted (Phase 13 / Complete)

## Context & Problem Statement
Compact is a zero-knowledge smart contract language with unique architectural concepts:
1. **Privacy Boundaries**: Code transitions between private off-chain computation (`witness`), public zero-knowledge proof generation (`circuit`), and irreversible privacy de-anonymization (`disclose`). Developers need clear visual cues in the editor gutter indicating when private data is disclosed to the public circuit.
2. **Interface Implementation Navigation**: Compact contracts declare interfaces (`contract Foo { ... }`) implemented by concrete contracts (`contract Bar implements Foo { ... }`). Navigating between interface declarations and concrete implementations is essential for contract modularity.
3. **Ledger Immutability Cues**: `sealed` state variables represent permanent initialization that cannot be overwritten. Visual padlock icons enhance state safety during contract reviews.

## Authoritative References
1. **Compact Privacy & Interface Grammar**:
   - [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): `implements`, `disclose`, `witness`, `circuit`, `sealed`, and `ledger`.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsLineMarkerProvider.kt` — interface/trait implementation navigation via `NavigationGutterIconBuilder`.
   - `intellij-scala`: Implementing/overriding method markers.

## Decision
1. **Bidirectional Interface Navigation (`NavigationGutterIconBuilder`)**:
   - On `implements` keyword: Creates a gutter icon (`AllIcons.Gutter.ImplementingMethod`) jumping directly to the declared interface definition.
   - On interface contract header: Scans project scope to find concrete implementors and adds an icon (`AllIcons.Gutter.ImplementedMethod`) navigating to concrete contracts.
   - On circuit level: Connects interface circuit prototypes to concrete circuit bodies.
2. **Domain-Specific Gutter Badges**:
   - **`disclose`**: Places `AllIcons.Nodes.KeymapEditor` with tooltip: *"Zero-Knowledge boundary: disclosing private witness data into circuit"*.
   - **`witness`**: Places `AllIcons.Nodes.AnonymousClass` with tooltip: *"Private off-chain witness query"*.
   - **`sealed`**: Places `AllIcons.Nodes.Padlock` with tooltip: *"Immutable sealed ledger state cell"*.
3. **Safe Leaf-Element Anchoring**:
   In accordance with IntelliJ threading and performance rules, line markers are anchored only to atomic leaf keyword tokens (`DISCLOSE`, `IMPLEMENTS`, `WITNESS`, `SEALED`), avoiding redundant evaluations across complex composite AST expressions.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Resolves references dynamically through `CompactResolveUtil` and `PsiTreeUtil`.
- **Is it non-blocking?**: Yes. Leaf-level filtering guarantees negligible overhead during typing.

## Feature Implementation Map
- Line Marker Provider: [`CompactLineMarkerProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactLineMarkerProvider.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<codeInsight.lineMarkerProvider>`)
- Unit Tests: [`CompactLineMarkerTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactLineMarkerTest.java)

## Consequences & Future Maintenance
- When adding new contract paradigms (e.g. library imports or contract inheritance), extend `CompactLineMarkerProvider` with corresponding `NavigationGutterIconBuilder` targets.
