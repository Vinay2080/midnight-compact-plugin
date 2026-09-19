# Architecture Drift Audit Ledger

This ledger records periodic audits comparing actual repository implementation against the architecture defined in `.ai/context/architecture.md` and accepted ADRs in `.ai/decisions/`.

---

## 1. Audit Protocol

- **Cadence:** Executed at major milestones or every 90 days.
- **Audit Procedure:**
  1. Inspect package structure in `src/main/java/dev/verloren/midnight/`.
  2. Verify that all newly created packages or subsystems have corresponding records in `.ai/context/architecture.md`.
  3. Verify that all architectural invariants in `AGENTS.md` (e.g. namespace separation, handwritten parser) remain intact.
  4. Ensure all new dependencies in `build.gradle.kts` are justified by an accepted ADR.

---

## 2. Historical Audit Log

### Audit: 2026-09-19 — Baseline Architectural Alignment
- **Status:** **PASS (Zero Drift)**
- **Findings:**
  - Lexer and Parser remain 100% handwritten and aligned with `compact/compiler/`.
  - Namespace separation (`VALUE` vs `TYPE`) strictly maintained in `CompactResolveUtil`.
  - All 34 ADRs in `.ai/decisions/` accurately reflect the current production implementations.
  - Path-scoped rules introduced in `.agents/rules/` to enforce local invariants.
- **Signed off by:** AI Architecture Auditor & Maintainer
