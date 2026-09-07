# Model Context Protocol (MCP) Server Specification

This document specifies the architecture, protocol design, and tool registry for the **Midnight Compact IntelliJ PSI-Powered MCP Server**.

---

## 1. Executive Summary & Rationale

External AI coding agents (such as Antigravity, Claude, Junie, Cursor, and Copilot) typically rely on generic CLI terminal tools (`grep`, `cat`, `find`, `sed`, regex substitutions) to explore and edit codebases.

For complex domain-specific smart-contract languages like **Midnight Compact**, generic CLI operations fail repeatedly because:
1. **No Scope Awareness**: CLI grep cannot distinguish between a local variable `amount`, a parameter `amount`, a ledger field `amount`, or a word inside a doc comment.
2. **Dual Namespace Ignorance**: Compact strictly separates `Namespace.VALUE` and `Namespace.TYPE`. Text search conflates `struct Token` (a type) with `const Token` or `circuit Token` (values).
3. **Cross-File Blindness**: Files connected via `include "math.compact"` or `import { add } from Math` cannot be traversed semantically with simple text finders.
4. **Dangerous Text Replacements**: CLI `sed` or naive string replaces easily create syntax errors, invalid AST structures, broken indentation, and unclosed braces.
5. **Slow Feedback Loops**: Verifying a change via external CLI compilation (`compactc`) is heavy and slow compared to in-memory IntelliJ inspections.

### The Solution: IntelliJ PSI-Powered MCP Server
By embedding an **MCP (Model Context Protocol) Server** inside the Midnight Compact IntelliJ Plugin:
- External AI assistants communicate directly with IntelliJ's **Program Structure Interface (PSI)**.
- Search queries use IntelliJ's live in-memory symbol tables and word scanners.
- Semantic resolution evaluates exact declaration targets using [`CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java).
- Code edits are executed atomically inside IntelliJ `WriteCommandAction`s using [`CompactElementFactory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/impl/CompactElementFactory.java), ensuring AST validity, formatting preservation, and Undo/Redo tracking.
- Diagnostics are retrieved in **zero milliseconds** directly from IntelliJ's live inspection passes (`CompactTypeMismatch`, `CompactPureCircuit`, `CompactSealedFieldMutation`, `CompactUndisclosedWitness`).

---

## 2. Architectural Architecture

```text
\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510
\u2502 AI Coding Assistant (Antigravity / Claude / Cursor)                 \u2502
\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518
                               \u2502
                               \u2502 JSON-RPC 2.0 (MCP Protocol via Stdio or Loopback HTTP)
                               \u25bc
\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510
\u2502 IntelliJ IDEA Process: CompactMcpServerService                   \u2502
\u2502  \u251c\u2500\u2500 Protocol Dispatcher (MCP Handshake & Tool Registry)           \u2502
\u2502  \u2514\u2500\u2500 Concurrency Dispatcher:                                        \u2502
\u2502      \u251c\u2500\u2500 ReadAction (Pooled Background Thread)                      \u2502
\u2502      \u2514\u2500\u2500 WriteCommandAction (EDT Thread Safe Mutation)              \u2502
\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518
                               \u2502
        \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510
        \u25bc                                                    \u25bc
\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510   \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510
\u2502 PSI Semantic Analysis Engine   \u2502   \u2502 AST Mutation & Refactoring      \u2502
\u2502 - CompactResolveUtil           \u2502   \u2502 - CompactElementFactory         \u2502
\u2502 - CompactTypeInferenceUtil     \u2502   \u2502 - CompactNamedElement.setName() \u2502
\u2502 - CompactInspections (In-Memory)\u2502   \u2502 - CompactFormattingModel        \u2502
\u2502 - PsiSearchHelper / WordScanner\u2502   \u2502 - UndoManager                   \u2502
\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518   \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518
```

---

## 3. Tool Registry Specification

The plugin will expose a dedicated suite of MCP tools registered under the `midnight_compact` namespace:

### 3.1 Symbol & AST Exploration Tools

#### 1. `compact_search_symbols`
- **Purpose**: Search for any declared Compact symbol (contracts, circuits, witnesses, structs, enum variants, ledger fields) across the project or in standard library.
- **Parameters**:
  - `query` (string, required): Symbol name pattern (e.g. `transfer`, `Counter`).
  - `kind` (string, optional): Filter by kind (`\"circuit\"`, `\"witness\"`, `\"ledger\"`, `\"struct\"`, `\"enum\"`, `\"type\"`).
  - `namespace` (string, optional): `\"VALUE\"` or `\"TYPE\"`.
- **IntelliJ Engine**: Delegates to `PsiShortNamesCache` / `CompactResolveUtil` / `CompactNamedElementIndex`.
- **Returns**: Array of symbol descriptors containing `name`, `kind`, `filePath`, `line`, `column`, `signature`, and `docComment`.

#### 2. `compact_get_ast`
- **Purpose**: Returns the structured PSI AST tree of a `.compact` file or declaration.
- **Parameters**:
  - `filePath` (string, required): Relative or absolute path to `.compact` file.
  - `elementOffset` (integer, optional): Offset of a specific declaration (e.g. circuit) to serialize only that subtree.
  - `maxDepth` (integer, optional, default=10): Depth limit for AST serialization.
- **IntelliJ Engine**: Walks `PsiElement.getChildren()` constructing typed JSON nodes with `elementType`, `textRange`, and parsed properties.
- **Returns**: Hierarchical JSON AST representation.

#### 3. `compact_resolve_reference`
- **Purpose**: Resolves the exact declaration target for an identifier or expression at a given file location.
- **Parameters**:
  - `filePath` (string, required): Path to source file.
  - `line` (integer, required): 1-indexed line number.
  - `column` (integer, required): 1-indexed column number.
- **IntelliJ Engine**: Calls `PsiFile.findElementAt(offset).getParent()`, extracts `PsiReference`, and resolves via `CompactResolveUtil.resolve()`.
- **Returns**: Target declaration metadata (`targetFile`, `targetLine`, `targetKind`, `signature`).

#### 4. `compact_find_usages`
- **Purpose**: Finds all usage sites of a symbol across the entire IntelliJ project.
- **Parameters**:
  - `filePath` (string, required): Source file where symbol is defined.
  - `symbolName` (string, required): Name of the symbol.
  - `scope` (string, optional): `\"project\"` or `\"file\"`.
- **IntelliJ Engine**: Uses `ReferencesSearch.search(namedElement)` with `GlobalSearchScope`.
- **Returns**: Array of usage locations (`filePath`, `line`, `column`, `contextSnippet`).

#### 5. `compact_get_type_info`
- **Purpose**: Returns the static inferred Compact type for any expression, variable, or circuit return.
- **Parameters**:
  - `filePath` (string, required): Source file.
  - `line` (integer, required): Line number.
  - `column` (integer, required): Column number.
- **IntelliJ Engine**: Invokes `CompactTypeInferenceUtil.inferType(expr)` returning `CompactPrimitiveType`, `CompactUintType`, or `CompactNamedType`.
- **Returns**: Detailed type descriptor (`typeName`, `isSigned`, `bitWidth`, `isPrimitive`, `typeCategory`).

---

### 3.2 Semantic Diagnostics & Verification Tools

#### 6. `compact_get_diagnostics`
- **Purpose**: Instantly evaluates all in-process inspections across one or more `.compact` files without running slow external CLI compilers.
- **Parameters**:
  - `files` (array of strings, optional): Paths to inspect (defaults to all `.compact` files).
- **IntelliJ Engine**: Runs inspection passes in memory:
  - `CompactUnresolvedReferenceInspection`
  - `CompactDuplicateDeclarationInspection`
  - `CompactUnusedLocalVariableInspection`
  - `CompactTypeMismatchInspection`
  - `CompactPureCircuitInspection`
  - `CompactSealedFieldMutationInspection`
  - `CompactRecursiveCircuitInspection`
  - `CompactConstructorRestrictionInspection`
  - `CompactUndisclosedWitnessInspection`
  - `CompactPragmaVersionInspection`
- **Returns**: Array of diagnostics:
  ```json
  [
    {
      "filePath": "src/contracts/Vault.compact",
      "line": 42,
      "column": 12,
      "severity": "ERROR",
      "inspectionId": "CompactSealedFieldMutation",
      "message": "Direct mutation of sealed ledger field 'totalBalance' is prohibited in pure circuit."
    }
  ]
  ```

---

### 3.3 Semantic AST Refactoring & Replacement Tools

#### 7. `compact_psi_rename`
- **Purpose**: Safely renames a Compact declaration and updates all usages, call sites, and imports project-wide.
- **Parameters**:
  - `filePath` (string, required): File containing declaration.
  - `symbolName` (string, required): Old name.
  - `newName` (string, required): Proposed new identifier.
- **IntelliJ Engine**:
  - Validates `newName` against `CompactNamesValidator` (rejects keywords).
  - Executes inside `WriteCommandAction` on the EDT.
  - Calls `CompactNamedElement.setName(newName)` propagating changes across all references.
- **Returns**: Status and list of updated files and offsets.

#### 8. `compact_psi_replace_element`
- **Purpose**: Replaces an exact AST element (e.g. statement, expression, or circuit body) with new valid code, guaranteeing AST node integrity.
- **Parameters**:
  - `filePath` (string, required): Target file.
  - `elementOffset` (integer, required): Offset of node to replace.
  - `replacementCode` (string, required): New Compact code snippet.
  - `reformat` (boolean, optional, default=true): Auto-format replaced code according to code style rules.
- **IntelliJ Engine**:
  - Parses `replacementCode` into a temporary PSI subtree via `CompactElementFactory`.
  - Replaces target `PsiElement` with the new node inside a `WriteCommandAction`.
  - Re-formats via `CodeStyleManager.getInstance(project).reformat(newElement)`.
- **Returns**: Success status and newly generated text range.

#### 9. `compact_insert_declaration`
- **Purpose**: Programmatically inserts a new circuit, witness, struct, or ledger field into a contract.
- **Parameters**:
  - `filePath` (string, required): Contract file.
  - `contractName` (string, required): Name of target contract.
  - `declarationCode` (string, required): Full code snippet for the declaration (e.g. `@export circuit withdraw(...) { ... }`).
- **IntelliJ Engine**: Finds `CompactContractDefinition`, locates body block, parses declaration via `CompactElementFactory`, and inserts as child element.
- **Returns**: Confirmation with new element offset and line number.

---

### 3.4 Toolchain & Compiler Management Tools

#### 10. `compact_switch_compiler`
- **Purpose**: Programmatically switch the active or project-pinned Compact compiler version.
- **Parameters**:
  - `version` (string, required): SemVer string (e.g. `\"0.34.0\"`).
- **IntelliJ Engine**: Updates `MidnightProjectSettings.getInstance(project).compilerVersion` and refreshes toolchain status.
- **Returns**: Status confirming active compiler path and WSL association.

#### 11. `compact_simulate_circuit`
- **Purpose**: Simulates a circuit call against mock in-memory ledger state and returns state changes and witness trace.
- **Parameters**:
  - `contractPath` (string, required): File path.
  - `circuitName` (string, required): Circuit name.
  - `args` (object, required): Map of parameter names to values.
  - `witnesses` (object, optional): Map of witness function names to return values.
- **IntelliJ Engine**: Invokes in-memory simulator engine and returns state diff.

---

## 4. Concurrency, Threading & Safety Guarantees

The MCP server adheres to strict IntelliJ Platform threading rules:

1. **Read Safety**:
   - All symbol queries, AST inspections, and type checks are wrapped in `ReadAction.compute()` or `ReadAction.run()`.
   - Operations regularly invoke `ProgressManager.checkCanceled()` so user typing in the IDE retains priority without lag.
2. **Write Safety**:
   - All mutations (`compact_psi_rename`, `compact_psi_replace_element`, `compact_insert_declaration`) are scheduled onto the Event Dispatch Thread (EDT) via `WriteCommandAction.runWriteCommandAction()`.
   - Modifying PSI on background threads is strictly prevented.
3. **Rollback & Undo Integration**:
   - Every PSI replacement action registers a named command with IntelliJ's `CommandProcessor`, enabling the developer to press `Ctrl + Z` in IntelliJ to instantly undo any AI action.

---

## 5. Integration with External AI Assistants

To connect an AI assistant (such as Antigravity, Claude Desktop, or Cursor) to the Midnight MCP Server:

### MCP Client Configuration (`mcp.json` / Claude Config)
```json
{
  "mcpServers": {
    "midnight-compact": {
      "command": "idea",
      "args": ["mcp-server", "--project", "/path/to/project", "--plugin", "dev.verloren.midnight"]
    }
  }
}
```
*(Or connecting via the loopback HTTP/SSE endpoint hosted by the plugin on `http://127.0.0.1:4545/mcp`)*
