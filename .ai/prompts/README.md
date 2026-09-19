# Automated AI Prompts Directory (`.ai/prompts/`)

This directory contains standardized, copy-pasteable master prompts and execution playbooks for AI agents and developers working on the **Midnight Compact Language Plugin**.

---

## 1. Prompt Catalog & Routing Matrix

| Playbook | Purpose | When to Run | Primary Artifacts Affected | Git & Push Workflow |
|:---|:---|:---|:---|:---|
| **[`bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md)** | Diagnosing, fixing, testing, and documenting bugs | Whenever investigating a bug or test failure | `src/`, `src/test/`, `.ai/bugs/`, `CHANGELOG.md` | In-workspace branch (`ai/fix-...`), atomic commits (`fix(...)`, `docs(bugs)`), merge to master, `git push origin master` |
| **[`feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md)** | Designing and implementing new language features, templates, or intentions | When developing a planned feature or enhancement | `src/`, `src/test/`, `current-state.md`, `project-state.yaml`, `CHANGELOG.md` | In-workspace branch (`ai/...`), atomic commits (`feat(...)`, `docs(...)`), merge to master, `git push origin master` |
| **[`architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md)** | Evaluating trade-offs and authoring Architectural Decision Records | When introducing a new subsystem, AST redesign, or indexing pattern | `.ai/decisions/`, `architecture.md`, `project-state.yaml` | In-workspace branch (`ai/adr-...`), atomic commit (`docs(adr)`), merge to master, `git push origin master` |
| **[`sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md)** | Periodic audit to eliminate drift between code and documentation | Every few commits (2–5 commits), at milestone completion, or before release | `current-state.md`, `project-state.yaml`, `architecture.md`, `CHANGELOG.md` | In-workspace execution or branch, atomic commits (`docs(changelog)`, `docs(context)`), `git push origin master` |
| **[`push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md)** | Deep code inspection, clean changelog, semantic tagging, and pushing | When publishing code, cutting a release, or pushing a git tag | `gradle.properties`, `plugin.xml`, `CHANGELOG.md`, git tags | Pre-flight drift audit, release commit (`chore(release)`), annotated tag (`vX.Y.Z`), `git push origin master && git push origin vX.Y.Z` |

---

## 2. Workspace & Git Discipline: In-Workspace Branching & Atomic Commits

When operating with AI agents inside IDE / ACP environments:
1. **In-Workspace Branch Isolation**:
   - Always isolate tasks using branches within the repository workspace (`git checkout -b ai/<slug>`) or gitignored subdirectories (`.worktrees/<slug>`).
   - Avoid external worktrees (`../midnight-plugin-wt-...`) because client file tools and IntelliJ MCP inspection tools (`get_file_problems`, `lint_files`) operate strictly within the opened IDE project workspace boundary.
2. **Continuous Static Inspection Loop**:
   - Immediately after every file edit, invoke `get_file_problems` and `lint_files` to verify 0 errors, 0 warnings, 0 weak warnings, and 0 grammar/spelling errors.
3. **Atomic Conventional Commits**:
   - Separate concerns cleanly into distinct, focused commits:
     * `feat(<subsystem>): ...` / `fix(<subsystem>): ...` for code and automated tests.
     * `docs(bugs): ...` for bug knowledge base entries.
     * `docs(adr): ...` for architectural decision records.
     * `docs(changelog): ...` for user-facing release notes in `CHANGELOG.md`.
     * `docs(context): ...` for machine state (`project-state.yaml`), architecture guide (`architecture.md`), and narrative state (`current-state.md`).
4. **Push & Tag Verification**:
   - Every completed workflow merges cleanly into `master`, verifies `./gradlew test` passes 100%, and pushes immediately to `git push origin master` (plus tags when releasing).

---

## 3. Recommended Cadence: The Hybrid Cadence

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
│    - Commit atomically, merge to master, verify tests, git push origin master│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. Periodic Sync (Every few commits / end of milestone)                     │
│    - Run `sync-context-and-decisions.md` to catch any drift between code    │
│      and architecture.md, current-state.md, and project-state.yaml          │
│    - Commit atomically: docs(changelog) and docs(context)                   │
│    - Push clean state: git push origin master                               │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Release / Push Time                                                      │
│    - Run `push-and-release.md` for full project inspection, user changelog  │
│      rewrite, semantic version bump (vX.Y.Z), and remote git push           │
│    - Push master and git tag: git push origin master && git push origin v...│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. What Other Files Help AI in this Repository?

Beyond the prompt playbooks, the AI system is empowered by:
1. **[`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)**: Permanent architectural invariants, threading rules, and coding standards.
2. **[`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/)**: Persistent Bug Knowledge Base preventing recurring debugging loops.
3. **[`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/)**: Architectural Decision Records documenting the rationale behind existing designs.
4. **[`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)**: Verified Compact grammar and type rules citing compiler source truth.
5. **[`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)**: Reference patterns from IntelliJ Rust, Scala, Elixir, and Rplugin.
