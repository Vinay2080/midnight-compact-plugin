# Automated AI Prompts Directory (`.ai/prompts/`)

This directory contains standardized, copy-pasteable master prompts and execution playbooks for AI agents and developers working on the **Midnight Compact Language Plugin**.

---

## 1. Prompt Catalog & Routing Matrix

| Playbook | Purpose | When to Run | Primary Artifacts Affected |
|:---|:---|:---|:---|
| **[`bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md)** | Diagnosing, fixing, testing, and documenting bugs | Whenever investigating a bug or test failure | `src/`, `src/test/`, `.ai/bugs/`, `CHANGELOG.md` |
| **[`feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md)** | Designing and implementing new language features, templates, or intentions | When developing a planned feature or enhancement | `src/`, `src/test/`, `current-state.md`, `project-state.yaml`, `CHANGELOG.md` |
| **[`architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md)** | Evaluating trade-offs and authoring Architectural Decision Records | When introducing a new subsystem, AST redesign, or indexing pattern | `.ai/decisions/`, `architecture.md`, `project-state.yaml` |
| **[`sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md)** | Periodic audit to eliminate drift between code and documentation | Every few commits (2–5 commits), at milestone completion, or before release | `current-state.md`, `project-state.yaml`, `architecture.md`, `CHANGELOG.md` |
| **[`push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md)** | Deep code inspection, clean changelog, semantic tagging, and pushing | When publishing code, cutting a release, or pushing a git tag | `gradle.properties`, `plugin.xml`, `CHANGELOG.md`, git tags |

---

## 2. Recommended Cadence: The Hybrid Cadence

To maintain zero documentation drift without slowing down fast code development:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. Micro-Edits (Every file change)                                          │
│    - Run IntelliJ inspection (`get_file_problems` & `lint_files`)           │
│    - Maintain 0 errors, 0 warnings, 0 weak warnings, 0 grammar errors       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. Task Boundaries (At feature completion or bug resolution)                │
│    - If Bug: run `bug-fix.md` (record in `.ai/bugs/` + clean changelog)     │
│    - If Feature: run `feature-implementation.md` (state updates + changelog)│
│    - If Architecture Shift: run `architecture-decision.md` (author ADR)     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. Periodic Sync (Every few commits / end of milestone)                     │
│    - Run `sync-context-and-decisions.md` to catch any drift between code    │
│      and architecture.md, current-state.md, and project-state.yaml          │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Release / Push Time                                                      │
│    - Run `push-and-release.md` for full project inspection, user changelog  │
│      rewrite, semantic version bump (vX.Y.Z), and remote git push           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. What Other Files Help AI in this Repository?

Beyond the prompt playbooks, the AI system is empowered by:
1. **[`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)**: Permanent architectural invariants, threading rules, and coding standards.
2. **[`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/)**: Persistent Bug Knowledge Base preventing recurring debugging loops.
3. **[`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/)**: Architectural Decision Records documenting the rationale behind existing designs.
4. **[`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)**: Verified Compact grammar and type rules citing compiler source truth.
5. **[`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)**: Reference patterns from IntelliJ Rust, Scala, Elixir, and Rplugin.
