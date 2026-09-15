# Bug Knowledge Base

Welcome to the persistent Bug Knowledge Base for the **Midnight Compact Language Plugin for IntelliJ IDEA** (`midnight-compact-plugin`).

This directory preserves historical debugging knowledge across development sessions. Whenever a concrete defect is identified, investigated, fixed, and verified, it is documented here as permanent, searchable engineering knowledge.

---

## 1. Purpose & Core Value

Debugging knowledge is often lost across conversational sessions. This repository ensures that every resolved bug leaves a permanent trace explaining:
- **What happened & symptoms**: How the bug manifested and what failed.
- **Context**: What was being worked on or tested when the defect surfaced.
- **Root cause**: The fundamental technical reason for the failure (not just a restatement of symptoms).
- **Investigation**: The reasoning, telemetry, experiments, or discoveries that uncovered the root cause.
- **Solution**: Exactly what code was changed and why it resolved the underlying issue.
- **Verification**: Concrete tests, reproduction steps, and commands verifying the fix.
- **Prevention / Lesson**: Generalizable principles or defensive programming techniques to avoid similar defects.

---

## 2. Searching Previous Bugs Before Debugging

> [!IMPORTANT]
> **MANDATORY PRE-DEBUG SEARCH**
> Before investigating any non-trivial bug or unexpected failure mode, agents **MUST** search `.ai/bugs/` for previous occurrences and related debugging context.

### What to Search For
Search `.ai/bugs/` and this `README.md` index for matches on:
1. **Feature / Subsystem**: (e.g., `parser`, `resolver`, `completion`, `formatter`, `toolchain`, `inspections`)
2. **Class, Interface, or Method**: (e.g., `CompactResolveUtil`, `CompactCompletionContributor`, `CompactToolchainUtil`)
3. **Error Message or Exception**: (e.g., `IncorrectOperationException`, `IndexOutOfBoundsException`, `NullPointerException`, `ProcessCanceledException`)
4. **Failure Mode**: (e.g., `freeze on EDT`, `type namespace collision`, `infinite loop in parser`, `missing semicolon recovery`)
5. **Architectural Area**: (e.g., `WSL path translation`, `live template macro`, `export modifier`)

### How to Search
- Scan the [Chronological Bug Index](#8-chronological-bug-index) below.
- Run a targeted ripgrep search:
  ```bash
  rg -i "<search-term>" .ai/bugs/
  ```
- In AI tool calls: use `grep_search` with `SearchPath: ".ai/bugs/"`.

### Caution on Reusing Solutions
Prior bug records provide invaluable debugging context, but **never blindly copy a previous solution**. Always verify that the technical root cause genuinely applies to the current defect.

---

## 3. When Bug Documentation Is Mandatory

Documenting a bug is **mandatory** whenever **all 5** of the following conditions are met:

1. **Concrete Defect Identified**: An actual defect, regression, incorrect behavior, crash, or test failure was observed.
2. **Investigation Conducted**: The defect was diagnosed through investigation, code inspection, or debugging.
3. **Root Cause Determined**: The underlying technical flaw was found and understood.
4. **Fix Implemented**: Code or configuration changes were applied to fix the defect.
5. **Fix Verified**: The fix was validated with passing tests, reproduction verification, or runtime checks.

> [!NOTE]
> **Automatic Documentation Requirement**
> When the 5 conditions are satisfied, creating the bug record and updating this index is an integral part of completing the task. The user **does NOT need to explicitly request** documentation. The AI agent must recognize the resolution and create the record automatically.

---

## 4. Noise Prevention: When NOT to Create Bug Records

To maintain high signal-to-noise ratio, **DO NOT** create bug records for:
- **Normal Feature Implementation**: Routine development of new capabilities according to specification.
- **Expected Behavior**: Expected test failures during initial test-driven development (TDD) before code is written.
- **Conceptual Questions**: Discussions or questions about how code works.
- **Speculative Problems**: Theoretical issues or hypothetical edge cases not observed in actual execution.
- **Discarded Prototypes**: Approaches or intermediate experiments that were simply abandoned in favor of better designs.
- **Trivial Typos**: Simple syntax typos or minor spelling errors that have no reusable debugging value.

The goal is to preserve **real, reusable debugging intelligence**.

---

## 5. Bug Filename Convention

Every bug record must be stored directly in `.ai/bugs/` using this strict naming format:

```text
YYYY-MM-DD-<feature-or-component>-<short-bug-title>.md
```

### Examples
- `2026-09-15-file-template-default-naming.md`
- `2026-09-15-parser-error-recovery.md`
- `2026-09-15-completion-wrong-scope.md`
- `2026-09-15-resolver-type-namespace-collision.md`

### Prohibited Names
Never use generic or opaque names such as:
- ❌ `bug1.md`
- ❌ `fix.md`
- ❌ `issue.md`
- ❌ `problem.md`
- ❌ `2026-09-15.md`

The filename must be descriptive and instantly identifiable when browsing the directory months later.

---

## 6. Required Bug Record Structure

Every bug document must follow this exact Markdown schema:

```markdown
# <Bug Title>

- **Date:** YYYY-MM-DD
- **Feature / Component:** <feature or subsystem>
- **Severity:** <Low | Medium | High | Critical>
- **Status:** Resolved

## Symptoms

Describe exactly what was observed and what behavior was incorrect.

## Context

Describe what was being implemented, changed, or tested when the bug appeared.

## Root Cause

Explain the actual technical cause.
Do not merely restate the symptom.

## Investigation

Document the important debugging discoveries and reasoning that led to the root cause.
Do not copy the entire conversation. Preserve useful debugging knowledge.

## Solution

Explain what was changed and why the change fixes the underlying problem.
Mention relevant files, classes, methods, or components where useful.

## Verification

Explain how the fix was verified.
Include relevant tests, commands, reproduction steps, or other verification evidence.

## Prevention / Lesson

Record the reusable lesson that could help prevent the same class of bug in the future.

## Related Files

- `<path>`
- `<path>`

## Related ADRs / Context

- `<relevant reference if applicable>`
```

*(Do not fabricate information for fields that are not applicable; specify `None` or omit optional context if irrelevant.)*

---

## 7. Mandatory Task Completion Gate

In accordance with [`.ai/workflow.md`](.ai/workflow.md), every task lifecycle requires evaluating the Bug Completion Gate:

```text
Bug identified? Yes / No
Bug resolved? Yes / No
Bug record required? Yes / No
Bug record created/updated? Yes / No / Not applicable
Bug index updated? Yes / No / Not applicable
Root cause documented? Yes / No / Not applicable
Solution documented? Yes / No / Not applicable
Verification documented? Yes / No / Not applicable
```

> [!CAUTION]
> If a qualifying bug was resolved during the task and its record has not been created in `.ai/bugs/` or registered in this index, **THE TASK CANNOT BE CONSIDERED COMPLETE**.

### Post-Creation Verification (Anti-Assumption Rule)
After creating or updating a bug record:
1. Re-read the created Markdown file.
2. Confirm the root cause is technically explained (not a restatement of the symptom).
3. Confirm the solution is detailed with relevant file and method links.
4. Confirm verification evidence (tests, reproduction results) is present.
5. Confirm the bug is entered in the Chronological Index below.
6. Check `git diff` to ensure clean formatting.

---

## 8. Chronological Bug Index

| Date | Feature / Subsystem | Bug Title & Summary | Document Link | Severity | Status |
|:---|:---|:---|:---|:---|:---|
| 2026-09-16 | completion / parameterized-types | Sized data type completion missing angle brackets, caret positioning, and size options | [2026-09-16-sized-type-completion-angle-brackets.md](2026-09-16-sized-type-completion-angle-brackets.md) | Medium | Resolved |
| 2026-09-16 | editor / brace-matching | Missing angle bracket (<>) auto-closing, cursor placement, and overtyping | [2026-09-16-editor-angle-bracket-pairing.md](2026-09-16-editor-angle-bracket-pairing.md) | Medium | Resolved |
| 2026-09-16 | completion / live-templates | Data type completion suppression in live template navigation and declaration type positions | [2026-09-16-template-type-completion-suppression.md](2026-09-16-template-type-completion-suppression.md) | High | Resolved |
| 2026-09-16 | completion / live-templates | Code and declaration completions triggering inside comments and documentation blocks | [2026-09-16-comment-completion-suppression.md](2026-09-16-comment-completion-suppression.md) | Medium | Resolved |
| 2026-09-16 | annotator / intention / version | SideEffectGuard INVOKE_LATER exception during quick-fix & intention previews | [2026-09-16-quickfix-preview-side-effect-guard.md](2026-09-16-quickfix-preview-side-effect-guard.md) | High | Resolved |

*(Bugs are listed in reverse chronological order with the most recent at the top.)*
