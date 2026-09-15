# Current Handoff

## Current Feature
Live Template and Declaration Data Type Completion Resolution (`ai/template-type-completion`).

## Status
- **Root Cause & Fixes**:
  - Live template variables in `CompactLedgerInsertHandler` and `CompactDeclarationInsertHandler` used raw `ConstantNode("State")`, which does not supply lookup items when tabbing through template variables. Created `CompactTypeExpression` and registered `CompactTypeMacro` (`compactType(...)`) in `plugin.xml` and `Compact.xml` templates (`expled`, `led`, `cir`, `wit`, `type`, `const`, etc.).
  - `CompactCompletionContributor` had an aggressive `isExportPreceding` check that hijacked all completion in exported declarations (e.g. `export ledger foo: <caret>;`) and suggested only exportable keywords instead of data types. Removed the pre-check and hardened `CompactCompletionContext` backward walk and regex.
  - Added `isTypePosition` recognition for ledger type slots, struct field types, const bindings, and type aliases.
  - Expanded `BUILTIN_TYPES` with `State`, `Counter`, `Void`, `JubjubPoint`, `Secp256k1Point`.
- **Verification**:
  - Added 8 targeted regression tests in `CompactCompletionTest.java`.
  - All 561 tests pass cleanly via `./gradlew test`.
- **Documentation**:
  - Documented bug in `.ai/bugs/2026-09-16-template-type-completion-suppression.md` and registered in `.ai/bugs/README.md`.
  - Updated `CHANGELOG.md` under `## [Unreleased]`.
  - Updated `.ai/project-state.yaml` and `.ai/context/current-state.md`.

## Relevant Context
- Master AI Router: [.ai/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md)
- User-Facing Changelog: [CHANGELOG.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)
- Task Lifecycle & Release Gate: [.ai/workflow.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md)
- Permanent Rules & Invariants: [AGENTS.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)
- Bug Knowledge Base: [.ai/bugs/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md)
- Machine State: [.ai/project-state.yaml](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)
- Current State: [.ai/context/current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)

## Immediate Next Priorities
1. Maintain `CHANGELOG.md` `## [Unreleased]` with clean user-facing descriptions.
2. Proceed with planned Phase 31 (Stub Indexing & Large Workspace Caching) or other user requests.
