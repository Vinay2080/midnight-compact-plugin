# AI Knowledge & Instructions Directory (`.ai/`)

This directory is the primary, single source of truth for architectural context, design decisions, verified language semantics, testing patterns, and machine-readable project state for the **Midnight Compact Language Plugin**.

---

## 1. Directory Tree & Architecture

```text
.ai/
├── README.md                    # This document: Master overview & index
├── workflow.md                  # Task lifecycle, worktree protocols, release changelog cleanup, & completion gates
├── project-state.yaml           # Machine-readable test counts, suites, and phase statuses
├── handoff.md                   # Current active task, worktree state, and next session priorities
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
   - Release-time changelog cleanup process (curated user-facing notes vs. engineering history) and release publication protocol.

3. **Verified Project Knowledge ([`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/))**:
   - Subsystem design and extension points ([`architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)).
   - Authoritative Compact language semantics ([`compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
   - Narrative implementation snapshot and test breakdowns ([`current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)).
   - IntelliJ Platform patterns, APIs, and comparative analysis ([`intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)).
   - Targeted reference repo index ([`reference-map.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md)).

4. **Historical Architecture Records ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))**:
   - Master index of all architectural subsystems in [`decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md).
   - Durable, immutable records of design choices, ground-truth citations, scalability evaluations, and implementation maps.

5. **Historical Bug Knowledge Base ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/))**:
   - Master chronological index of resolved bugs in [`bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).
   - Searchable, permanent records documenting symptoms → investigation → root cause → solution → verification → prevention lessons.
   - Mandatory pre-debugging search and completion gate integration.

6. **Machine State & Session Continuation**:
   - Machine-readable status of phases, test numbers, and test suites ([`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)).
   - Current session status, completed changes, unresolved discoveries, and next priorities ([`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)).

7. **Public User Changelog ([`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**:
   - Curated for human users of the IntelliJ plugin answering *"What changed for me?"*.
   - Free of ADR numbers, class names, test names, compiler lines, and internal AI restructuring details.
   - Cleaned and consolidated during the mandatory release workflow before any release tag is pushed.

---

## 3. Fresh-Agent Quick Orientation

If you are a fresh AI agent starting a new session:

1. **What is this project?**
   - The official IntelliJ IDEA language plugin for Midnight's Compact smart contract language (`dev.verloren.midnight`), built on Java 25.
2. **What rules must I never violate?**
   - Read [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section 3 (Critical Invariants, including Git Worktree Isolation & Release Changelog Hygiene) and Section 5 (Threading & Concurrency Rules).
3. **What is the current state of the project?**
   - Read [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md).
4. **How do I execute a development task?**
   - For tasks modifying the repository, create a dedicated Git worktree and branch (`ai/<task-slug>`) as required by [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) and [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md). Work exclusively in the worktree, test, commit, merge to `master`, verify `master`, and delete the worktree/branch.
5. **How do I debug or resolve a bug?**
   - Search previous bugs in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md), follow [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md), and record qualifying resolved bugs in [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/).
6. **How do I handle `CHANGELOG.md` and releases?**
   - During development, collect notable user-facing changes under `## [Unreleased]` answering *"What changed for me?"*. Never record internal classes, test names, ADRs, or AI restructuring. Before tagging or cutting a release, execute the mandatory release changelog cleanup process in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 8 to produce curated user-facing release notes. Never push a release tag with uncleaned engineering notes.
7. **How do I finish a task?**
   - Fulfill the Mandatory Final Task Gate in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 7 (including the Git Session Completion Gate and Bug Completion Gate) and provide the Artifact Status Table in your final response.
