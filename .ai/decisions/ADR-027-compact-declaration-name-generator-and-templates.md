# ADR-027: Universal Declaration Name Auto-Numbering, Scope Analysis, and Live Template Macros

## Status
Accepted (v1.2.6 / Complete)

## Context & Problem Statement
In Compact smart contracts, code generation and live templates previously generated declarations with generic placeholder names such as `circuitName()` and `witnessName()`, or hardcoded names such as `state` for ledgers.
This behavior led to repetitive identifier collisions and manual renaming friction whenever a developer authored multiple declarations of the same type within the same file or module.

The required behavior:
1. When the developer does not explicitly provide a name, the generator must suggest and use an automatically numbered name based on the declaration type:
   - First declaration &rarr; `circuit1`
   - If `circuit1` already exists in scope &rarr; `circuit2`
   - Continue incrementing until an unused identifier is found.
2. The auto-numbering must fill gaps in numbering: e.g., if `circuit1` and `circuit3` exist, `circuit2` must be generated.
3. The same strategy must apply universally to every declaration type (`circuit`, `witness`, `struct`, `enum`, `module`, `contract`, `type`, `ledger`, `const`).
4. Explicitly provided names must be preserved without alteration.
5. Sibling scopes must remain isolated (e.g., `circuit1` in `ModuleA` does not prevent `circuit1` from being generated in `ModuleB`).
6. The naming logic must be generalized, non-hardcoded, and extensible so newly supported declaration types can plug into the same mechanism without bespoke logic.

## Authoritative References
1. **JetBrains Platform Macro & Live Template Architecture**:
   - `com.intellij.codeInsight.template.Macro`: Pluggable template expression functions evaluated interactively during live template expansion.
   - `com.intellij.codeInsight.template.TemplateManager` & `com.intellij.codeInsight.template.impl.ConstantNode`: Interactive multi-variable template orchestration.
2. **Compact Language Grammar & AST Structure**:
   - [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): Top-level and module declarations (`contract`, `circuit`, `witness`, `struct`, `enum`, `module`, `type`, `ledger`).
   - [`CompactElementTypes.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactElementTypes.java): AST element definitions.
   - [`CompactResolveUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java): Scope traversal and named element filtering.

## Decision
1. **Extensible Declaration Type Registry (`CompactDeclarationType`)**:
   - Defines enum constants for all standard Compact declaration constructs: `CIRCUIT`, `WITNESS`, `STRUCT`, `ENUM`, `MODULE`, `CONTRACT`, `TYPE`, `LEDGER`, `CONST`.
   - Each entry maps to its canonical base name (`circuit`, `witness`, `ledger`, etc.).
   - Provides runtime extensibility via `registerCustomType(typeKey, baseName)` and `resolveBaseName(typeOrKeyword)`.
   - Provides bidirectional mapping helpers from keywords, AST element types, and concrete PSI classes (`CompactCircuitDefinition`, `CompactWitnessDeclaration`, `CompactStructDefinition`, etc.).

2. **Universal Scope-Aware Name Generator (`CompactDeclarationNameGenerator`)**:
   - `generateName(baseName, explicitName, context)`:
     - If `explicitName` is provided, returns it unchanged.
     - Resolves the innermost scope container (`findScopeRoot`) among `CompactBlock`, `CompactModuleDefinition`, `CompactExternalContractDeclaration`, and `CompactFile`.
     - Collects existing symbols in scope through both parsed PSI elements (`CompactNamedElement`) and universal scope text token scanning (`IDENTIFIER_PATTERN`), ensuring safety even during uncommitted typing.
     - Iterates sequentially starting at index 1 (`candidate = baseName + index`) until the lowest unused integer is found, naturally backfilling gaps.

3. **Pluggable Live Template Macros (`CompactDeclarationNameMacro`, `CompactCircuitNameMacro`, `CompactWitnessNameMacro`)**:
   - Registered under the `<liveTemplateMacro>` extension point in `plugin.xml`.
   - `compactDeclarationName(declarationType)`: Evaluates dynamic expressions in live templates (e.g., `compactDeclarationName("circuit")` &rarr; `circuit1`, `circuit2`).
   - `circuitName()` and `witnessName()`: Backward-compatible specialized macros delegating to the unified name generator.

4. **Declaration Code Completion Handlers**:
   - Created `CompactDeclarationInsertHandler` for auto-numbered template completions of `circuit`, `witness`, `struct`, `enum`, `module`, `contract`, `type`, and `ledger`.
   - Updated `CompactLedgerInsertHandler` to use `CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, psiContext)`, generating `ledger1`, `ledger2`, etc., instead of static `"state"`.

5. **Parametric Live Templates (`Compact.xml`)**:
   - Updated all declaration templates (`cir`, `wit`, `cct`, `ccti`, `mod`, `str`, `en`, `type`, `led`, `ledg`, `ledger`) to utilize `compactDeclarationName(...)` with standard fallbacks (`"circuit1"`, `"witness1"`, `"struct1"`, etc.).

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Declaration types are managed through the extensible `CompactDeclarationType` registry. Base names and scope analyzers operate on abstract identifiers without bespoke naming switches per declaration construct.
- **Is it thread-safe?**: Yes. The custom type registry utilizes `ConcurrentHashMap`, and PSI inspection relies purely on read-only platform navigation.
- **Does it preserve user intent?**: Yes. Explicit user-provided names bypass numbering completely.

## Feature Implementation Map
- Registry: [`CompactDeclarationType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactDeclarationType.java)
- Name Generator & Scope Analyzer: [`CompactDeclarationNameGenerator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactDeclarationNameGenerator.java)
- Live Template Macros:
  - [`CompactDeclarationNameMacro.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactDeclarationNameMacro.java)
  - [`CompactCircuitNameMacro.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactCircuitNameMacro.java)
  - [`CompactWitnessNameMacro.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactWitnessNameMacro.java)
- Completion Handlers:
  - [`CompactDeclarationInsertHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactDeclarationInsertHandler.java)
  - [`CompactLedgerInsertHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactLedgerInsertHandler.java)
  - [`CompactCompletionContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java)
- Live Templates: [`Compact.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/liveTemplates/Compact.xml)
- Extension Registrations: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)
- Unit Tests:
  - [`CompactDeclarationNameGeneratorTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/templates/CompactDeclarationNameGeneratorTest.java)
  - [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java)
  - [`CompactLiveTemplateTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/templates/CompactLiveTemplateTest.java)
