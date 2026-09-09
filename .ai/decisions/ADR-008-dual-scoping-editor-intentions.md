# ADR-008: Dual Line and Word Scoping Model for Editor Intentions

## Status
Accepted (Implemented in Phase 28 / v1.2.4)

## Context & Problem Statement
IntelliJ quick-fixes and intentions (`Alt+Enter`) can be scoped either:
1. **Strict Token Scope**: Only available when the caret is placed directly on a specific keyword or identifier (e.g. only on the `circuit` keyword).
2. **Line / Construct Scope**: Available anywhere on the declaration header line or containing statement.

Strict token scoping frustrated developers who pressed `Alt+Enter` anywhere on a circuit header or `const` line expecting contextual refactoring options. Conversely, overly broad scoping can clutter the intention menu with irrelevant options when focusing on a specific sub-expression.

## Authoritative References
1. **IntelliJ Platform UX Guidelines on Intention Actions**:
   - High-level declarations (modifiers, exports, function purity) should be accessible anywhere on the header or signature to minimize required cursor precision.
   - Expression-level transformations (`disclose(...)`, expression negation) should focus on the target token or selected expression.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: Intentions such as `AddElseBranchIntention` or `SpecifyTypeExplicitlyIntention` search up the PSI tree from the element at the caret to find the containing statement or header, allowing invocation from any position within the statement.
   - `intellij-scala`: Invert `if` and toggle visibility intentions check if the caret is anywhere on the signature or condition rather than strictly on the keyword.

## Decision
Adopt a **dual scoping model** across the entire Compact intention suite:
1. **Construct / Line-Level Intentions** (Header-wide availability):
   - `CompactTogglePureCircuitIntention`: Available anywhere on the circuit signature line.
   - `CompactToggleExportIntention`: Available anywhere on the header line of top-level contracts, circuits, structs, enums, modules, and type definitions.
   - `CompactSpecifyTypeExplicitlyIntention`: Available anywhere on the `const` declaration line (on `const`, the identifier, `=`, or `;`).
   - `CompactRemoveRedundantTypeIntention`: Available anywhere on the typed `const` statement line.
   - `CompactInvertIfIntention`: Available anywhere on the `if (...)` header or condition expression.
2. **Token / Expression-Specific Intentions**:
   - `CompactSurroundWithDiscloseIntention`: Activated specifically when the caret or selection targets a private witness expression or variable within a circuit.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Intentions navigate up the PSI tree via `PsiTreeUtil.getParentOfType` or line-bounds checks within the AST node, ensuring they apply cleanly regardless of indentation, comments, or line-wrapping.
- **Does it cause menu pollution?**: No. Construct intentions only appear when the containing construct matches the semantic prerequisites (e.g. `CompactTogglePureCircuitIntention` only on circuits; `CompactSpecifyTypeExplicitlyIntention` only on untyped `const`s).

## Feature Implementation Map
- Intentions Suite:
  - [`CompactTogglePureCircuitIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactTogglePureCircuitIntention.java)
  - [`CompactToggleExportIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactToggleExportIntention.java)
  - [`CompactSurroundWithDiscloseIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactSurroundWithDiscloseIntention.java)
  - [`CompactInvertIfIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactInvertIfIntention.java)
  - [`CompactSpecifyTypeExplicitlyIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactSpecifyTypeExplicitlyIntention.java)
  - [`CompactRemoveRedundantTypeIntention.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/intentions/CompactRemoveRedundantTypeIntention.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<intentionAction>`)
- Descriptions & Previews: [`src/main/resources/intentionDescriptions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/intentionDescriptions/)
- Unit Tests: [`CompactPhase28IntentionsTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactPhase28IntentionsTest.java)

## Consequences & Future Maintenance
- When adding new intentions in future phases, classify them upfront as either construct-level (line-scoped) or expression-level (token-scoped) according to this ADR.
