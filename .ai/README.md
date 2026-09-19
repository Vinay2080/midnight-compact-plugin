# AI Knowledge & Instructions Directory (`.ai/`)

This directory is the primary, single source of truth for architectural context, design decisions, verified language semantics, testing patterns, automated prompts, and machine-readable project state for the **Midnight Compact Language Plugin**.

---

## 1. Directory Tree & Architecture

```text
.
├── AGENTS.md                    # Authoritative root agent rules, invariants & protected boundaries
├── scripts/
│   └── verify-patch.ps1         # Automated multi-gate verification runner (JSON output)
├── .agents/
│   ├── rules/                   # Path-scoped invariant rules
│   │   ├── psi-parser.rules.md  # Parser advancement & incomplete AST guards
│   │   ├── threading.rules.md   # ReadAction / WriteAction & static PSI prohibition
│   │   ├── modern-java25.rules.md # Records, sequenced collections, pattern matching
│   │   ├── inspections-annotators.rules.md # Previews SideEffectGuard & annotator lifecycles
│   │   └── toolchain-wsl.rules.md # Windows compact collision filter & WSL translation
│   └── config/
│       ├── mcp-gateways.json    # MCP tool permissions, scopes, and security manifest
│       └── model-routing.yaml   # Asymmetric local model routing (Utility/Workhorse/Reasoner)
└── .ai/
    ├── README.md                # This document: Master overview & index
    ├── workflow.md              # Task lifecycle, worktree protocols, release changelog cleanup, & completion gates
    ├── project-state.yaml       # Machine-readable test counts, suites, and phase statuses
    ├── handoff.md               # Current active task, worktree state, and next session priorities
    ├── prompts/                 # Reusable prompts and execution playbooks
    │   ├── README.md            # Prompts catalog, cadence, & routing matrix
    │   ├── bug-fix.md           # Bug diagnosis, test reproduction, & KB record
    │   ├── feature-implementation.md # Feature planning, Java 25 implementation, state sync
    │   ├── architecture-decision.md # Trade-off analysis & ADR authoring
    │   ├── sync-context-and-decisions.md # Periodic drift audit & doc synchronization
    │   └── push-and-release.md  # Master AI prompt for inspection, changelog cleanup, tagging & pushing
    ├── context/                 # Deep subsystem context and verified technical references
    │   ├── architecture.md      # Plugin pipeline, PSI wrappers, indexing, threading
    │   ├── compact-semantics.md # Authoritative Compact grammar, typing rules, and examples
    │   ├── current-state.md     # Narrative snapshot of implemented features & limitations
    │   ├── intellij-patterns.md # Comparison with reference plugins & IntelliJ best practices
    │   └── reference-map.md     # File index of reference plugins and compiler ground truth
    ├── decisions/               # Architectural Decision Records (ADRs)
    │   ├── README.md            # ADR master table, status, and subsystem mapping
    │   ├── 001-parser-architecture.md
    │   └── ...                  # ADR-001 through ADR-034
    ├── bugs/                    # Persistent Bug Knowledge Base
    │   ├── README.md            # Searchable master index, trigger conditions, & schema
    │   ├── recurring-patterns.md# Bug escalation ladder & chronic anti-pattern tracker
    │   └── ...                  # Chronological bug records (YYYY-MM-DD-<feature>-<title>.md)
    ├── testing/                 # Test Governance Subsystem
    │   ├── test-inventory.md    # Catalog of test suites, tiers, commands, and latencies
    │   └── flakiness-log.md     # Quarantine register for non-deterministic tests
    ├── security/                # Security Boundaries & Policies
    │   ├── threat-boundaries.md # Threat vectors (WSL injection, Windows binary collision, etc.)
    │   └── vulnerability-rules.md # Prohibited API calls and insecure code patterns
    ├── operations/              # Operational & Deployment Knowledge
    │   └── deployment-runbooks.md # Build verification, signing, packaging, and publishing
    └── meta/                    # Architecture Governance & Lifecycles
        ├── drift-audit-ledger.md # Periodic architecture and ADR drift audit records
        └── deprecation-register.md # Platform API deprecations and migration tracking
```

---

## 2. Core Design Principle: Separation of Concerns

To prevent instruction fatigue, stale documentation, and scattered rules, the repository strictly divides responsibilities across distinct layers:

1. **Permanent Invariants & Rules ([`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md))**:
   - Permanent architectural invariants (e.g. handwritten parser preservation, namespace separation, incomplete PSI tolerance).
   - Mandatory Git worktree isolation for repository-modifying sessions.
   - User-facing release changelog hygiene and tagging guard (Invariant 14).
   - Continuous inspection after every edit and before every push (Invariant 6 & 15).
   - Modern Java (Java 25) coding standards.
   - Threading and concurrency rules (ReadAction, WriteCommandAction, background processes).
   - Tool selection priorities (IntelliJ MCP `idea` server vs CLI).
   - Reference repository selection matrix.
   - Critical pitfalls and anti-patterns.
   - Code review quality bar (anti-bikeshedding).
   - Persistent bug knowledge recording rule.
   - System governance, path-scoped rules, and protected artifacts guard.

2. **Execution Process & Checklists ([`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md))**:
   - Mandatory Git worktree session lifecycle (creation, isolated worktree implementation, commit, merge, master verification, cleanup).
   - Sequential task lifecycle from SESSION START to SESSION COMPLETE.
   - Task-specific context loading checklists (including pre-debug search in `.ai/bugs/`).
   - Ground truth inspection requirements.
   - Intelligent Documentation Decision Rules (which files must be updated and when).
   - Anti-assumption re-read verification rule.
   - The Mandatory Final Completion Gate (Task Gate, Git Session Gate, Bug Completion Gate) and status table.
   - Release-time changelog cleanup process (curated user-facing notes vs. engineering history), semantic version tag decision matrix, and release publication protocol.

3. **Path-Scoped Architectural Rules ([`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/))**:
   - Localized, granular rules enforced dynamically when working in specific directories (PSI, threading, inspections, toolchains).

4. **Automated Verification Harness ([`scripts/verify-patch.ps1`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/scripts/verify-patch.ps1))**:
   - Multi-gate compilation, structure verification, and test execution producing machine-readable `build/verification-report.json`.

5. **Operational Knowledge & Governance Subsystems**:
   - Living registries for bug patterns, test suites, threat boundaries, deployment runbooks, deprecations, and drift audits.

---

## 3. Fresh-Agent Quick Orientation

If you are a fresh AI agent starting a new session:

1. **What is this project?**
   - The official IntelliJ IDEA language plugin for Midnight's Compact smart contract language (`dev.verloren.midnight`), built on Java 25.
2. **What rules must I never violate?**
   - Read [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section 3 (Critical Invariants), Section 6 (Threading & Concurrency Rules), and Section 13 (Protected Artifacts Guard).
3. **What is the current state of the project?**
   - Read [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md).
4. **How do I execute a development task?**
   - For tasks modifying the repository, create a dedicated Git worktree and branch (`ai/<task-slug>`) as required by [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) and [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md). Work exclusively in the worktree, test, commit, merge to `master`, verify `master`, and delete the worktree/branch.
   - Consult [`.ai/prompts/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/README.md) to choose the dedicated prompt playbook for your task.
5. **How do I verify changes?**
   - Run `powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick` or with `-TestPattern <class>`.
