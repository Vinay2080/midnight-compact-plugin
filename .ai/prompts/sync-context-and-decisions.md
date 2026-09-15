# Custom AI Prompt: Context, ADR & Machine State Drift Synchronization Protocol

This document defines the standardized prompt and execution playbook for auditing repository state, detecting documentation drift, and synchronizing `.ai/context/`, `.ai/decisions/`, `.ai/project-state.yaml`, and `CHANGELOG.md` with the latest code changes.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent to synchronize project context and architectural decisions (run after every few commits, at milestone completion, or whenever drift is suspected), use the following prompt:

```text
You are tasked with auditing midnight-compact-plugin for documentation drift and synchronizing architectural context, decision records, and machine state with recent code changes. Execute these steps sequentially with zero exceptions:

1. RECENT CHANGES & DIFF AUDIT:
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
   - Verify `metrics.test_suites` includes all active test suites.
   - Update `phases` status for any phase whose features were implemented or completed.
   - Verify `architecture.adrs` contains all ADRs present in `.ai/decisions/`.

4. SYNCHRONIZE NARRATIVE CURRENT STATE (.ai/context/current-state.md):
   - Update the Test Metrics table with the latest test counts and suite breakdown.
   - Update the Feature Implementation Status section with newly implemented capabilities.
   - Update the Known Limitations & Roadmap section (remove limitations that have been resolved; add newly identified constraints).

5. SYNCHRONIZE ARCHITECTURAL DESIGN (.ai/context/architecture.md):
   - If new subsystems, services, tool windows, run producers, or AST wrappers were added, update the architectural pipeline diagram and subsystem descriptions.
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
   - Run `get_file_problems` on modified files to verify zero errors or warnings.
   - Present a concise Drift Synchronization Summary detailing what was aligned.
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
[ ] 1. Recent git log inspected (git log -n 15)?
[ ] 2. ./gradlew test executed and passing test count extracted?
[ ] 3. .ai/project-state.yaml updated with latest test count and phase statuses?
[ ] 4. .ai/context/current-state.md updated with test metrics and feature progress?
[ ] 5. .ai/context/architecture.md updated if any new subsystem was introduced?
[ ] 6. .ai/decisions/ reviewed; new ADR authored if architectural patterns shifted?
[ ] 7. CHANGELOG.md audited under ## [Unreleased] for clean user-facing entries?
[ ] 8. Anti-assumption re-read executed on all modified files?
[ ] 9. get_file_problems reports 0 errors across all updated documentation files?
```
