# ADR-004: Resilient Local Inspections & Quick-Fixes

## Context
Developers writing Compact smart contracts need real-time feedback for unresolved references, duplicate declarations, unused local variables, and invalid type operations. In an IDE, code is frequently syntactically incomplete while the user is typing.

## Decision
1. Implement 4 core `LocalInspectionTool` subclasses:
   - `CompactUnresolvedReferenceInspection`
   - `CompactDuplicateDeclarationInspection`
   - `CompactUnusedLocalVariableInspection` (with `CompactRemoveUnusedVariableFix`)
   - `CompactTypeMismatchInspection`
2. Include strict AST resilience guards: if a subtree contains `PsiErrorElement` or unresolved types evaluate to `UNKNOWN`, skip inspection on that node to prevent false positives.

## Alternatives Considered
1. **Aggressive Compiler Error Emulation**: Flagging everything that the strict compiler would reject, even during editing.
2. **External Linter Only**: Running the `compactc` CLI on save without editor-level PSI inspections.

## Why
- Immediate interactive feedback without false positive noise.
- Quick-fixes allow instantaneous code repair (`Alt + Enter`).
- Safe execution in background document commit threads.

## Consequences
- Clean, noise-free developer experience.
- Inspections pass cleanly across all valid and partially-typed test contracts.
