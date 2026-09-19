# Custom AI Prompt: Context, ADR & Machine State Drift Synchronization Protocol

This document defines the standardized prompt and execution playbook for auditing repository state, detecting documentation drift, and synchronizing `.ai/context/`, `.ai/decisions/`, `.ai/project-state.yaml`, and `CHANGELOG.md` with the latest code changes.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent to synchronize project context and architectural decisions (run after every few commits, at milestone completion, or whenever drift is suspected), use the following prompt:

```text
You are tasked with auditing midnight-compact-plugin for documentation drift and synchronizing architectural context, decision records, and machine state with recent code changes. Execute these steps sequentially with zero exceptions:

1. WORKSPACE STATUS & RECENT CHANGES AUDIT:
   - Check `git status` in the active workspace root; ensure the working tree is understood before modifying files.
   - For isolated development, switch to an in-workspace branch:
     git checkout -b ai/sync-context
     (Avoid external worktrees outside the repository root as they break IDE MCP tools and workspace tool boundaries).
   - Inspect recent git history: `git log -n 15 --oneline` and `git status`.
   - Inspect code changes across modified subsystems in `src/main/` and `src/test/`.
   - Identify newly introduced features, resolved bugs, refactorings, or architecture changes that have not yet been documented.

2. TEST SUITE & METRIC EXTRACTION:
   - Run `./gradlew test`.
   - Extract the exact number of passing tests, failing tests, and test suites across the project.
   - Note any new test classes added in `src/test/java/dev/verloren/midnight/`.

3. SYNCHRONIZE MACHINE STATE (.ai/project-state.yaml):
   - Update `metrics.passing_tests` with the verified test count.
   - Update `metrics.failing_tests` (must be 0).
   - Update `metrics.test_suites` with the verified active test suite count.
   - Update `phases` status for any phase whose features were implemented or completed.
   - Synchronize BOTH `architecture.adrs` and `architecture.decisions` dictionaries with all ADRs present in `.ai/decisions/` and `.ai/decisions/README.md`.

4. SYNCHRONIZE NARRATIVE CURRENT STATE (.ai/context/current-state.md):
   - Update the Test Metrics summary and the exhaustive Test Suite Breakdown table with the latest test counts and suite names (never drop suites or rows).
   - Update the Implementation Snapshot / Feature Implementation Status section with newly implemented capabilities.
   - Update the Known Limitations & Roadmap section (remove limitations that have been resolved; keep future phase roadmap intact).
   - Anti-Truncation Guard: Confirm that the entire document remains complete from top to bottom.

5. SYNCHRONIZE ARCHITECTURAL DESIGN (.ai/context/architecture.md):
   - If new subsystems, services, tool windows, run producers, or AST wrappers were added, update the architectural pipeline diagram and subsystem descriptions.
   - Update the Section 3 Extension Points catalog to match `plugin.xml` (actions, typed handlers, intentions, file template providers).
   - Update the Section 4 Test Structure table with the current test suite breakdown and passing test count.
   - Ensure the threading model and cache design reflect the actual implementation.

6. AUDIT ARCHITECTURAL DECISIONS (.ai/decisions/):
   - Check if any major architectural change, new extension point, or PSI design was added without an ADR.
   - If an ADR is missing for a completed subsystem, author the ADR following `.ai/prompts/architecture-decision.md` and register it in `.ai/decisions/README.md`.

7. AUDIT PUBLIC USER CHANGELOG (CHANGELOG.md):
   - Check if any notable user-facing feature or bug fix is missing from `## [Unreleased]`.
   - Ensure all entries under `## [Unreleased]` are written strictly from the user perspective answering "What changed for me?".
   - Eliminate any internal class names, method signatures, test counts, or ADR numbers that may have slipped into `CHANGELOG.md`.

8. ANTI-ASSUMPTION RE-READ & VERIFICATION:
   - Re-read every modified documentation file with `client_view_file`.
   - Confirm that metrics, dates, and cross-references are mutually consistent across all files.
   - If any production or test Java files were modified, run `get_file_problems` via `execute_tool` to confirm zero errors and zero warnings.

9. ATOMIC COMMITS, MERGE & PUSH TO REMOTE:
   - Stage and commit documentation drift fixes using atomic, conventional commits:
     * If CHANGELOG.md was refined:
       git add CHANGELOG.md && git commit -m "docs(changelog): <clean user-facing summary of unreleased adjustments>"
     * For state, architecture, and narrative synchronization:
       git add .ai/project-state.yaml .ai/context/current-state.md .ai/context/architecture.md && git commit -m "docs(context): synchronize project state, architecture guide, and current state with <version> (<metrics>, <ADRs>)"
     * If an ADR was added:
       git add .ai/decisions/ && git commit -m "docs(adr): add ADR-XXX <title> and update architecture catalog"
   - If operating on an in-workspace branch (`ai/sync-context`):
     * git checkout master
     * git merge ai/sync-context
     * git branch -d ai/sync-context
   - Verify `git status` on master is clean.
   - Push verified commits to remote:
     git push origin master
   - Present a concise Drift Synchronization Summary detailing what was aligned, committed, and pushed.
```

---

## 2. Synchronization Cadence & Best Practices

### When to Run this Prompt
- **After Every Few Commits**: After completing 2–5 commits of feature or bug work, run this prompt to prevent documentation from falling behind code.
- **At Feature / Milestone Boundaries**: Before closing a major task or marking a phase complete.
- **Before Preparing a Release**: Run this audit prior to running `.ai/prompts/push-and-release.md` to ensure that all metadata, changelog entries, and test counts are 100% synchronized before tagging.

---

## 3. Drift Synchronization Checklist

```text
DRIFT AUDIT CHECKLIST:
[ ] 1. Workspace status checked (clean baseline) & in-workspace branch established if isolating?
[ ] 2. Recent git log inspected (git log -n 15)?
[ ] 3. ./gradlew test executed and exact passing test count extracted?
[ ] 4. .ai/project-state.yaml updated with latest test count, phase statuses, and ADR registries?
[ ] 5. .ai/context/current-state.md updated with test metrics, full suite breakdown, and feature progress?
[ ] 6. .ai/context/architecture.md updated if any new subsystem or extension point was introduced?
[ ] 7. .ai/decisions/ reviewed; new ADR authored if architectural patterns shifted?
[ ] 8. CHANGELOG.md audited under ## [Unreleased] for clean user-facing entries (zero technical jargon)?
[ ] 9. Anti-assumption re-read executed on all modified files?
[ ] 10. Atomic commits created for changelog and context documentation?
[ ] 11. Merged to master (if branched) and pushed to remote (git push origin master)?
```
