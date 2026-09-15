# AI Knowledge & Instructions Directory (`.ai/`)

This directory is the primary, single source of truth for architectural context, design decisions, verified language semantics, testing patterns, automated prompts, and machine-readable project state for the **Midnight Compact Language Plugin**.

---

## 1. Directory Tree & Architecture

```text
.ai/
├── README.md                    # This document: Master overview & index
├── workflow.md                  # Task lifecycle, worktree protocols, release changelog cleanup, & completion gates
├── project-state.yaml           # Machine-readable test counts, suites, and phase statuses
├── handoff.md                   # Current active task, worktree state, and next session priorities
├── prompts/                     # Reusable prompts and execution playbooks
│   ├── README.md                # Prompts catalog, cadence, & routing matrix
│   ├── bug-fix.md               # Bug diagnosis, test reproduction, & KB record
│   ├── feature-implementation.md # Feature planning, Java 25 implementation, state sync
│   ├── architecture-decision.md # Trade-off analysis & ADR authoring
│   ├── sync-context-and-decisions.md # Periodic drift audit & doc synchronization
│   └── push-and-release.md      # Master AI prompt for inspection, changelog cleanup, tagging & pushing
├── context/                     # Deep subsystem context and verified technical references
│   ├── architecture.md          # Plugin pipeline, PSI wrappers, indexing, threading
│   ├── compact-semantics.md     # Authoritative Compact grammar, typing rules, and examples
│   ├── current-state.md         # Narrative snapshot of implemented features & limitations
│   ├── intellij-patterns.md     # Comparison with reference plugins & IntelliJ best practices
│   └── reference-map.md         # File index of reference plugins and compiler ground truth
├── decisions/                   # Architectural Decision Records (ADRs)
│   ├── README.md                # ADR master table, status, and subsystem mapping
│   ├── 001-parser-architecture.md
│   └── ...                      # ADR-001 through ADR-028
├── bugs/                        # Persistent Bug Knowledge Base
│   ├── README.md                # Searchable master index, trigger conditions, & schema
│   └── ...                      # Chronological bug records (YYYY-MM-DD-<feature>-<title>.md)
└── .agents/
    └── mcp.json                 # Project MCP configuration
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
   - *Never store transient project state, active test counts, or frequently changing feature statuses here.*

2. **Execution Process & Checklists ([`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md))**:
   - Mandatory Git worktree session lifecycle (creation, isolated worktree implementation, commit, merge, master verification, cleanup).
   - Sequential task lifecycle from SESSION START to SESSION COMPLETE.
   - Task-specific context loading checklists (including pre-debug search in `.ai/bugs/`).
   - Ground truth inspection requirements.
   - Intelligent Documentation Decision Rules (which files must be updated and when).
   - Anti-assumption re-read verification rule.
   - The Mandatory Final Completion Gate (Task Gate, Git Session Gate, Bug Completion Gate) and status table.
   - Release-time changelog cleanup process (curated user-facing notes vs. engineering history), semantic version tag decision matrix, and release publication protocol.

3. **Automated Prompts & Execution Playbooks ([`.ai/prompts/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/))**:
   - Standardized copy-pasteable prompts and execution guides:
     * [`README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/README.md): Master catalog, cadence guidelines, and routing matrix.
     * [`bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md): Bug diagnosis, failing test reproduction, root cause analysis, bug record writing, and changelog update.
     * [`feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md): Worktree isolation, modern Java 25 implementation, multi-tier tests, state updates.
     * [`architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md): Option analysis, compiler citations, reference plugin comparisons, and ADR authoring.
     * [`sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md): Periodic drift audit scanning commits/diffs to synchronize `architecture.md`, `current-state.md`, `project-state.yaml`, and `decisions/`.
     * [`push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md): Deep project inspection, user-facing changelog hygiene, semantic tag determination, and git push sequence.

4. **Verified Project Knowledge ([`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/))**:
   - Subsystem design and extension points ([`architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)).
   - Authoritative Compact language semantics ([`compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
   - Narrative implementation snapshot and test breakdowns ([`current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)).
   - IntelliJ Platform patterns, APIs, and comparative analysis ([`intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)).
   - Targeted reference repo index ([`reference-map.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md)).

5. **Historical Architecture Records ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))**:
   - Master index of all architectural subsystems in [`decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md).
   - Durable, immutable records of design choices, ground-truth citations, scalability evaluations, and implementation maps.

6. **Historical Bug Knowledge Base ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/))**:
   - Master chronological index of resolved bugs in [`bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).
   - Searchable, permanent records documenting symptoms → investigation → root cause → solution → verification → prevention lessons.
   - Mandatory pre-debugging search and completion gate integration.

7. **Machine State & Session Continuation**:
   - Machine-readable status of phases, test numbers, and test suites ([`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)).
   - Current session status, completed changes, unresolved discoveries, and next priorities ([`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)).

8. **Public User Changelog ([`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**:
   - Curated for human users of the IntelliJ plugin answering *"What changed for me?"*.
   - Free of ADR numbers, class names, test names, compiler lines, and internal AI restructuring details.
   - Cleaned and consolidated during the mandatory release workflow before any release tag is pushed.

---

## 3. Fresh-Agent Quick Orientation

If you are a fresh AI agent starting a new session:

1. **What is this project?**
   - The official IntelliJ IDEA language plugin for Midnight's Compact smart contract language (`dev.verloren.midnight`), built on Java 25.
2. **What rules must I never violate?**
   - Read [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section 3 (Critical Invariants, including Continuous Inspection, Git Worktree Isolation & Release Changelog Hygiene) and Section 6 (Threading & Concurrency Rules).
3. **What is the current state of the project?**
   - Read [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md).
4. **How do I execute a development task?**
   - For tasks modifying the repository, create a dedicated Git worktree and branch (`ai/<task-slug>`) as required by [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) and [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md). Work exclusively in the worktree, test, commit, merge to `master`, verify `master`, and delete the worktree/branch.
   - Consult [`.ai/prompts/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/README.md) to choose the dedicated prompt playbook for your task (`feature-implementation.md`, `bug-fix.md`, `architecture-decision.md`, `sync-context-and-decisions.md`, or `push-and-release.md`).
5. **How do I debug or resolve a bug?**
   - Use [`.ai/prompts/bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md): search previous bugs in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md), follow [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md), and record qualifying resolved bugs in [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/).
6. **How do I handle `CHANGELOG.md`, releases, and pushing code?**
   - Use the standardized prompt and protocol in [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md).
   - During development, collect notable user-facing changes under `## [Unreleased]` answering *"What changed for me?"*.
   - Never record internal classes, test names, ADRs, or AI restructuring in `CHANGELOG.md` (keep them in `.ai/`).
   - Clean and consolidate `CHANGELOG.md`, determine semantic tag (`vX.Y.Z`), verify `./gradlew test`, commit, tag, and push.
7. **How do I prevent context and architectural decisions from drifting?**
   - Run the periodic sync playbook in [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md) after every few commits or milestone completions.
8. **How do I finish a task?**
   - Fulfill the Mandatory Final Task Gate in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 7 (including the Git Session Completion Gate and Bug Completion Gate) and provide the Artifact Status Table in your final response.
