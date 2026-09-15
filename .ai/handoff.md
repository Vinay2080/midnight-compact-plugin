# Current Handoff

## Current Feature
Changelog Hygiene, Deep Code Inspection, Semantic Tagging & Custom Push Prompt Protocol (`ai/push-and-release-protocol`).

## Status
- **Accomplished**:
  - Defined the custom prompt and execution pipeline in [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md) covering:
    1. Comprehensive project inspection (zero errors, zero warnings, zero weak warnings, zero grammar/spelling errors, zero unapplied language level change suggestions like modern Java 25 features).
    2. Continuous fix loop: before doing work, during edits, after edits, and before releasing/pushing.
    3. Dual-tier changelog structure: clean, minimal, human-centric user [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) answering "What changed for me?" vs. detailed engineering knowledge preserved strictly in `.ai/`, git commits, and code comments for AI and developers.
    4. Semantic Versioning tag determination matrix (Major / Minor / Patch), version syncing, test verification, annotated tag creation (`vX.Y.Z`), and pushing code and tags.
  - Updated [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md):
    - Strengthened Step 0 Pre-Flight Guard and Invariant 6 (Zero Errors, Zero Warnings, Zero Weak Warnings, Zero Grammar Errors, Zero Language Level Suggestions).
    - Expanded Invariant 14 (User-Facing Release Changelog Hygiene & Semantic Tagging Guard).
    - Added Invariant 15 and enhanced Section 5 with the Continuous Inspection-Fix Loop.
    - Added Section 12 detailing the Release, Changelog Hygiene, Semantic Tagging & Code Push Protocol.
    - Updated Section 11 Context Navigation Map.
  - Updated [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md):
    - Updated Section 1 task lifecycle and Section 7 final task gate.
    - Enriched Section 8 (Release & Publication Protocol) with the Semantic Versioning matrix, continuous pre-push inspection, technical blacklist, Keep-a-Changelog structure, 14-item release checklist, and tag push sequence.
  - Updated [`.ai/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md):
    - Registered `.ai/prompts/` and `push-and-release.md` in directory tree, separation of concerns, and quick orientation.
  - Cleaned [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md):
    - Stripped all internal class names, method signatures, test counts, and exception names from `## [Unreleased]`, leaving a clean, minimal, human-centric user changelog.

## Relevant Context
- Master AI Router: [.ai/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md)
- Custom Push & Release Prompt: [.ai/prompts/push-and-release.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md)
- User-Facing Changelog: [CHANGELOG.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)
- Task Lifecycle & Release Gate: [.ai/workflow.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md)
- Permanent Rules & Invariants: [AGENTS.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)
- Bug Knowledge Base: [.ai/bugs/README.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md)
- Machine State: [.ai/project-state.yaml](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)
- Current State: [.ai/context/current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)

## Immediate Next Priorities
1. When executing future releases or pushing code, invoke the custom prompt from [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md).
2. Continue feature development (e.g., Phase 31 Stub Indexing & Large Workspace Caching) following the established protocol.
