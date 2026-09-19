# Recurring Bug Patterns & Escalation Ladder

This ledger tracks recurring failure modes across debugging incidents in `.ai/bugs/` and governs their escalation into permanent automated enforcement gates.

---

## 1. The 3-Tier Escalation Ladder

```text
Level 1: Single Incident
  └── Action: Document in .ai/bugs/YYYY-MM-DD-*.md
  └── Action: Add unit regression test to project test suite.

Level 2: Recurrence (2 Occurrences of the Same Pattern)
  └── Action: Mark as "Emerging Anti-Pattern" in this ledger.
  └── Action: Add targeted path rule in .agents/rules/*.rules.md.
  └── Action: Add property-based / edge-case stress test.

Level 3: Chronic Anti-Pattern (>= 3 Occurrences)
  └── Action: Propose deterministic IntelliJ inspection or Qodana rule.
  └── Action: Elevate invariant to root AGENTS.md.
  └── Action: Create refactor ticket to eliminate pattern structurally.
```

---

## 2. Active Pattern Tracker

### Pattern PAT-001: EDT UI Freeze / Threading Inversion
- **Category:** Threading & Concurrency
- **Occurrences:** 3 incidents
  1. `2026-09-16-caret-freeze-on-spacebar.md` (EDT slow indexing)
  2. `2026-09-16-annotator-trailing-space-caret-jump.md` (Annotator whitespace stripping on EDT)
  3. `2026-09-16-external-annotator-lag-and-stale-underlines.md` (Synchronous process execution)
- **Root Cause:** Executing disk I/O, regex indexing, or process waiting directly on the Event Dispatch Thread (EDT).
- **Escalation Level:** **Level 3 (Chronic Anti-Pattern)**
- **Enforcement:**
  - Invariant enshrined in `AGENTS.md` Section 6 and `.agents/rules/threading.rules.md`.
  - Qodana inspection enabled for EDT blocking calls.

### Pattern PAT-002: Autocompletion Scope Leakage & Suppression
- **Category:** Completion & Live Templates
- **Occurrences:** 4 incidents
  1. `2026-09-16-comment-completion-suppression.md` (Completions in comments)
  2. `2026-09-16-template-type-completion-suppression.md` (Suppression in live template navigation)
  3. `2026-09-16-sized-type-completion-angle-brackets.md` (Angle bracket auto-insertion)
  4. `2026-09-18-completion-return-value-declaration-filtering.md` (Return statement variable filtering)
- **Root Cause:** Missing `psiElement().inside(...)` pattern matching guards in `CompactCompletionContributor`.
- **Escalation Level:** **Level 3 (Chronic Anti-Pattern)**
- **Enforcement:**
  - Structural pattern matching mandatory in all completion contributors.
  - Test suites in `CompactCompletionTest` require negative assertions for comment and string scopes.

### Pattern PAT-003: SideEffectGuard Violation in Quick-Fix Previews
- **Category:** Editor Intentions & Annotators
- **Occurrences:** 1 incident
  1. `2026-09-16-quickfix-preview-side-effect-guard.md` (INVOKE_LATER in preview)
- **Root Cause:** Invoking asynchronous EDT dispatch (`ApplicationManager.getApplication().invokeLater(...)`) during intention/quick-fix preview rendering.
- **Escalation Level:** **Level 1 (Single Incident - Resolved)**
- **Enforcement:**
  - Rule codified in `.agents/rules/inspections-annotators.rules.md`.

### Pattern PAT-004: False Positive Inspections on Complex Expressions
- **Category:** Semantic Analysis & Type Inference
- **Occurrences:** 2 incidents
  1. `2026-09-18-inspection-sealed-ledger-module-mutation.md` (Module scoping)
  2. `2026-09-18-inspection-ternary-return-type-mismatch.md` (Ternary expressions)
- **Root Cause:** Inspecting syntactic leaf nodes without evaluating the enclosing AST container type or module boundary.
- **Escalation Level:** **Level 2 (Emerging Anti-Pattern)**
- **Enforcement:**
  - Rule codified in `.agents/rules/psi-parser.rules.md`. All inspections must verify enclosing declaration context before flagging errors.
