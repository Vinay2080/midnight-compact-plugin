# ADR-002: PSI-Based Single-File Reference Resolution

## Context
Compact programs require symbol navigation (Go To Declaration, Find Usages, Rename) and contextual auto-completion. IntelliJ supports multiple resolution mechanisms, including central `PsiReferenceContributor`, index-based stubs (`StubIndex`), and direct PSI-owned references.

## Decision
1. PSI nodes own their references directly (e.g. `CompactReferenceExprImpl`, `CompactTypeReferenceImpl`, `CompactStructFieldReference`, `CompactEnumMemberReference`).
2. Scope resolution is executed via single-file AST tree traversal in `CompactResolveUtil` with separate `VALUE` and `TYPE` namespaces and innermost-first lexical shadowing.
3. Declarations from external sources (e.g. builtins, stdlib, unindexed includes) are marked soft-unresolved rather than crashing or causing false-positive warnings.

## Alternatives Considered
1. **Central `PsiReferenceContributor`**: Registering reference providers globally via XML.
2. **Immediate Multi-File StubIndex**: Building full stub indexes before single-file semantics are complete.

## Why
- Keeps symbol resolution deterministic, fast, and testable without complex stub serializations.
- Prevents cross-file concurrency issues during initial development phases.
- Maintains strict separation between `TYPE` (structs, enums) and `VALUE` (variables, parameters, circuits) namespaces matching upstream compiler behavior.

## Consequences
- Single-file resolution works out of the box for all language constructs.
- Cross-file imports (`import ... from`, `include`) will be layered on top via StubIndex in a future phase without breaking existing reference contracts.
