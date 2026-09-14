# `.ai/` Knowledge & AI Operating System

Welcome to the `.ai/` system for the **Midnight Compact Language Plugin for IntelliJ IDEA** (`midnight-compact-plugin`).

This directory forms an integrated, layered knowledge base and execution engine designed to guide AI agents and developers through planning, implementation, verification, documentation, and handoff without relying on scattered rules or guesswork.

---

## 1. Directory Structure & File Responsibilities

```text
.ai/
├── README.md                    # This file: Directory map, router, and fresh-agent entry point
├── workflow.md                  # Task lifecycle, checklists, decision rules, & final completion gate
├── project-state.yaml           # Machine-readable current project state, phases, and test metrics
├── handoff.md                   # Session-to-session continuation notes, active feature, and next priorities
├── context/                     # Verified domain knowledge & technical references
│   ├── architecture.md          # End-to-end plugin architecture, subsystem specifications, & extension points
│   ├── compact-semantics.md     # Verified Compact language specifications, typing rules, and grammar
│   ├── current-state.md         # Narrative snapshot of implemented features, active phases, and limitations
│   ├── intellij-patterns.md     # Established IntelliJ platform APIs, threading patterns, and anti-patterns
│   └── reference-map.md         # Targeted index to upstream compiler and 4 reference plugins
├── decisions/                   # Architectural Decision Records (ADRs)
│   ├── README.md                # Master ADR catalog, indexing ADR-001 through ADR-026
│   └── ADR-001...ADR-026        # Historical architectural decision records with upstream/reference citations
└── mcp/
    └── mcp.json                 # Project MCP configuration
```

---

## 2. Core Design Principle: Separation of Concerns

To prevent instruction fatigue, stale documentation, and scattered rules, the repository strictly divides responsibilities across five distinct layers:

1. **Permanent Invariants & Rules ([`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md))**:
   - Permanent architectural invariants (e.g. handwritten parser preservation, namespace separation, incomplete PSI tolerance).
   - Modern Java (Java 25) coding standards.
   - Threading and concurrency rules (ReadAction, WriteCommandAction, background processes).
   - Tool selection priorities (IntelliJ MCP `idea` server vs CLI).
   - Reference repository selection matrix.
   - Critical pitfalls and anti-patterns.
   - Code review quality bar (anti-bikeshedding).
   - *Never store transient project state, active test counts, or frequently changing feature statuses here.*

2. **Execution Process & Checklists ([`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md))**:
   - Sequential 11-step task lifecycle from START to DONE.
   - Task-specific context loading checklists.
   - Ground truth inspection requirements.
   - Intelligent Documentation Decision Rules (which files must be updated and when).
   - Anti-assumption re-read verification rule.
   - The Mandatory 9-step Final Completion Gate and status table.
   - Release and Marketplace publication protocol.

3. **Verified Project Knowledge ([`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/))**:
   - Subsystem design and extension points ([`architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)).
   - Authoritative Compact language semantics ([`compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
   - Narrative implementation snapshot and test breakdowns ([`current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)).
   - IntelliJ Platform patterns, APIs, and comparative analysis ([`intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)).
   - Targeted reference repo index ([`reference-map.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md)).

4. **Historical Architecture Records ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))**:
   - Master index of all 26 architectural subsystems in [`decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md).
   - Durable, immutable records of design choices, ground-truth citations, scalability evaluations, and implementation maps.

5. **Machine State & Session Continuation**:
   - Machine-readable status of phases, test numbers, and test suites ([`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)).
   - Current session status, completed changes, unresolved discoveries, and next priorities ([`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)).

---

## 3. Fresh-Agent Quick Orientation

If you are a fresh AI agent starting a new session:

1. **What is this project?**
   - The official IntelliJ IDEA language plugin for Midnight's Compact smart contract language (`dev.verloren.midnight`), built on Java 25.
2. **What rules must I never violate?**
   - Read [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section 3 (Critical Invariants) and Section 5 (Threading & Concurrency Rules).
3. **What is the current state of the project?**
   - Read [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md).
4. **How do I execute a task?**
   - Follow [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md).
5. **How do I finish a task?**
   - Fulfill the Mandatory Final Task Gate in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 6 and provide the Artifact Status Table in your final response.
