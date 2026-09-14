# Current Handoff

## Current Feature
Mandatory Release-Time Changelog Cleanup Process, Git Worktree Lifecycle, & Bug Knowledge Base Integration (AI Operating System).

## Status
- **Mandatory Release-Time Changelog Cleanup Architecture**:
  - Established the distinction between **Development `CHANGELOG.md`** (working collection under `## [Unreleased]` answering *"What changed for me?"*), **Release `CHANGELOG.md`** (curated, consolidated user-facing release notes), and **Internal Engineering Knowledge** ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/), [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/), [`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/), git commit history, and code documentation).
  - Added **Critical Invariant #14 (User-Facing Release Changelog Hygiene)** to [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) prohibiting release tags from being created or pushed while `CHANGELOG.md` contains engineering notes, ADR numbers, class names, test names, compiler lines, or AI restructuring details.
  - Authored comprehensive Section 8 in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md):
    - 8.1 Core Principle: Public user-facing standard answering *"What changed for me?"*.
    - 8.2 Mandatory Release Execution Order (The Tagging Guard).
    - 8.3 The Technical Blacklist: Required removals/translations (ADR numbers, phases, classes, methods, test fixtures, paths, compiler lines, AI restructuring, test counts) with concrete before/after translation examples.
    - 8.4 Internal-Only Changes Policy: Zero leaks of private tooling, refactoring, or documentation changes to the public changelog.
    - 8.5 Grouping & Consolidation: Combining multi-step technical implementations into single, cohesive user-facing bullet points.
    - 8.6 Standard Keep-a-Changelog Categories & ISO date formatting; strict ban on custom categories like `### Tested`.
    - 8.7 Mandatory Release-Time Changelog Review Gate (6 evaluation questions).
    - 8.8 Final Release Changelog Checklist (10 items before tagging).
    - 8.9 Release metadata synchronization (`gradle.properties`, `plugin.xml` fallback `<change-notes>`) and verification (`./gradlew test`, `./gradlew buildPlugin`).
  - Integrated into [`.ai/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md) (Layer 7 and Fresh-Agent Quick Orientation Question 6).
  - Curated and cleaned [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) `## [Unreleased]`, purging ADR references, `parser.ss` citations, unit test names, test counters, and internal AI restructuring notes in favor of clean, user-facing release notes.
- **Mandatory Git Worktree Session Lifecycle Architecture**:
  - Established the mandatory session lifecycle:
    `SESSION START -> Create dedicated worktree & branch -> Work ONLY in worktree -> Test / document -> Commit branch -> Merge into master -> Verify master -> Cleanup -> SESSION COMPLETE`.
  - Added **Critical Invariant #13** to [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) and Section 2 in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md).
- **Persistent Bug Knowledge Base Architecture**:
  - Established [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) as a permanent, searchable historical debugging knowledge base.
  - Added **Critical Invariant #12** to [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) and registered in [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml).

## Relevant Context
- Master AI Router: [.ai/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md)
- User-Facing Changelog: [CHANGELOG.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)
- Task Lifecycle & Release Gate: [.ai/workflow.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md)
- Permanent Rules & Invariants: [AGENTS.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)
- Bug Knowledge Base: [.ai/bugs/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md)
- Master ADR Index: [.ai/decisions/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md)
- Machine State: [.ai/project-state.yaml](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)
- Current State: [.ai/context/current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)
- Reference Map: [.ai/context/reference-map.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md)
- Compact Semantics: [.ai/context/compact-semantics.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)
- IntelliJ Patterns: [.ai/context/intellij-patterns.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)

## Immediate Next Priorities
1. For any incoming development session modifying repository files, **create a dedicated Git worktree and branch** (`../midnight-plugin-wt-<task-slug>`, `ai/<task-slug>`) before modifying any files.
2. Maintain `CHANGELOG.md` `## [Unreleased]` with concise, user-facing descriptions; never record internal classes, test names, ADRs, or AI restructuring.
3. Before cutting or tagging a release, execute the mandatory release changelog cleanup process in `.ai/workflow.md` Section 8.
4. Search `.ai/bugs/` whenever debugging a defect, and record qualifying resolved bugs in `.ai/bugs/`.
5. Proceed with planned Phase 31 (Stub Indexing & Large Workspace Caching) when directed.
