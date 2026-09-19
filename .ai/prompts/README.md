# Automated AI Prompts Directory (`.ai/prompts/`)

This directory contains standardized, copy-pasteable master prompts and execution playbooks for AI agents and developers working on the **Midnight Compact Language Plugin**.

---

## 1. Task Type Routing Matrix (10 Classes)

Before modifying the repository, the AI agent **MUST** classify the user request into one of the 10 defined task classes and follow its designated playbook and verification path:

| Task Class | Description | Primary Playbook / Guide | Verification Gate | Reusable Knowledge Target |
|:---|:---|:---|:---|:---|
| **`BUG`** | Defect, incorrect behavior, UI freeze, or test failure | [`.ai/prompts/bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md) | Reproducing test + `verify-patch.ps1` | [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) & [`recurring-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/recurring-patterns.md) |
| **`FEATURE`** | New language construct, completion, template, or tool | [`.ai/prompts/feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md) | Multi-tier tests + `verify-patch.ps1` | [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml), [`current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md), `CHANGELOG.md` |
| **`REFACTOR`** | Structural code reorganization without functional change | [`.ai/prompts/feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md) | Full regression: `verify-patch.ps1 -AllTests` | [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md) |
| **`ARCHITECTURE`** | Subsystem design, new indexing model, AST overhaul | [`.ai/prompts/architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md) | Gate 0/1 compilation + human ADR approval | [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) & [`decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md) |
| **`DEPENDENCY`** | Gradle, IntelliJ SDK platform, or toolchain update | [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md) | Full suite `verify-patch.ps1 -AllTests` | [`.ai/meta/deprecation-register.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/meta/deprecation-register.md) |
| **`TESTING`** | Test suite expansion, flakiness triage, benchmark | [`.ai/testing/test-inventory.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/testing/test-inventory.md) | `verify-patch.ps1 -TestPattern` | [`.ai/testing/flakiness-log.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/testing/flakiness-log.md) |
| **`SECURITY`** | Vulnerability patch, trust boundary, process escape guard | [`.ai/security/threat-boundaries.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/security/threat-boundaries.md) | Dedicated regression test + Gate 1-3 | [`.ai/security/vulnerability-rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/security/vulnerability-rules.md) |
| **`RELEASE`** | Version bump, release notes cleanup, tagging, push | [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md) | `buildPlugin` + `verify-patch.ps1 -AllTests` | [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md), [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties), Git tag |
| **`DOCUMENTATION`** | Syncing state, architecture docs, or developer guide | [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md) | Anti-assumption re-read verification | Authoritative `.ai/` doc, [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) |
| **`MAINTENANCE`** | Periodic drift audit, dead code cleanup, deprecations | [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md) | `verify-patch.ps1 -Quick` | [`.ai/meta/drift-audit-ledger.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/meta/drift-audit-ledger.md) |

---

## 2. In-Workspace Branch Isolation Standard

When operating with AI agents inside IDE / ACP environments:
1. **Branching Model**:
   - Always isolate tasks using branches within the repository workspace (`git checkout -b ai/<slug>`).
   - Avoid creating external worktrees outside the project directory (`../midnight-plugin-wt-...`) because client file tools and IntelliJ MCP inspection tools (`get_file_problems`, `lint_files`) operate strictly within the opened IDE project workspace boundary.
2. **Continuous Static Inspection Loop**:
   - Immediately after every file edit, invoke `get_file_problems` and `lint_files` via IntelliJ MCP to verify 0 errors, 0 warnings, 0 weak warnings, and 0 grammar/spelling errors.
3. **Atomic Conventional Commits**:
   - Separate concerns cleanly into distinct, focused commits:
     * `feat(<subsystem>): ...` / `fix(<subsystem>): ...` for code and automated tests.
     * `docs(bugs): ...` for bug knowledge base entries.
     * `docs(adr): ...` for architectural decision records.
     * `docs(changelog): ...` for user-facing release notes in `CHANGELOG.md`.
     * `docs(context): ...` for machine state (`project-state.yaml`), architecture guide (`architecture.md`), and narrative state (`current-state.md`).
4. **Push & Tag Verification**:
   - Every completed workflow merges cleanly into `master`, verifies `scripts/verify-patch.ps1` passes 100%, and pushes to `origin master` only when explicitly instructed or releasing.

---

## 3. Playbook Execution Protocols

Each playbook in this directory is hardened as an explicit executable protocol containing:
- **Preconditions**: Entry criteria before editing begins.
- **Sequential Steps**: Exact ordered actions.
- **Mandatory Gates**: Hard checkpoints that cannot be skipped.
- **Evidence Requirements**: Concrete outputs the AI must produce to prove completion.
- **Failure Conditions**: Explicit criteria that halt execution.
- **Recovery Rules**: Actions to recover from failures.
- **Completion Conditions**: Exit criteria before declaring done.
- **Final Artifact**: The mandatory structured completion report.
