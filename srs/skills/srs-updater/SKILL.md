---
name: srs-updater
description: Manage the evolution, versioning, impact analysis, and synchronized updates of Software Requirements Specifications (SRS) across engineering lifecycles.
---

# SRS Updater Skill

## Purpose
Govern the modification, versioning, delta tracking, and impact analysis of Software Requirements Specifications (SRS). Ensures that any requirement modification, addition, or deprecation maintains semantic consistency, document integrity, architectural alignment, and bidirectional traceability.

---

## 1. Requirement Lifecycle State Model

Every requirement in an evolving SRS moves through a formal state machine:

```
[ Proposed / Draft ] ──(Review Passed)──> [ Approved ]
         │                                      │
         │ (Rejected)                           │ (Implementation Begun)
         ▼                                      ▼
    [ Rejected ]                          [ In Progress ]
                                                │
                                                │ (Code Complete)
                                                ▼
                                         [ Implemented ]
                                                │
                                                │ (Harness Tests Pass)
                                                ▼
                                          [ Verified ]
                                                │
                                                │ (Scope Changed / Refactored)
                                                ▼
                                    [ Deprecated / Superseded ]
```

### State Definitions
- **Draft**: Newly authored requirement under peer review.
- **Approved**: Accepted by systems architect and engineering lead.
- **In Progress**: Active development underway in the target subsystem.
- **Implemented**: Code implemented and submitted to branch.
- **Verified**: Passing automated regression and verification harness tests.
- **Deprecated / Superseded**: Replaced by a newer requirement ID (e.g., `Superseded by [REQ-PRS-FUN-012]`).

---

## 2. Change Impact Analysis (CIA) Protocol

Before applying any update to an SRS, execute a mandatory 4-layer impact analysis:

### Layer 1: Requirements Impact
- Identify dependent, overlapping, or conflicting requirements within the SRS.
- Check if upstream preconditions or downstream outputs are altered.

### Layer 2: Architectural & Invariant Impact
- Verify compliance with architectural invariants (e.g. downward dependency, anti-hardcoding rule, single source of truth for types).
- Assess whether changes alter public APIs, AST contracts, or module boundaries.

### Layer 3: Implementation Impact
- Identify target source files, packages, and extension points in `plugin.xml` affected by the change.
- Evaluate memory footprint and threading constraints (e.g. EDT vs background thread).

### Layer 4: Verification & Test Impact
- Identify test suites requiring updates or new companion mirror tests.
- Ensure test assertions match new acceptance criteria.

---

## 3. SRS Semantic Versioning Standard

The SRS document adheres to Semantic Versioning (`v<MAJOR>.<MINOR>.<PATCH>`):

- **MAJOR (vX.0.0)**:
  - Fundamental architectural overhaul (e.g. migration from ANTLR to handwritten Pratt parser; rewriting the type inference engine).
  - Deletion or fundamental redefinition of core functional capabilities.
  - Incompatible changes to external interfaces.
- **MINOR (v1.X.0)**:
  - Addition of new functional requirements or subsystems (e.g. adding a new static semantic inspection, adding WSL support, adding parameter inlay hints).
  - Non-breaking extensions to existing interfaces or tool windows.
- **PATCH (v1.0.X)**:
  - Clarification of ambiguous wording, correction of typographical errors, updating diagram styling, or refining acceptance criteria without changing system behavior.

---

## 4. Change Control & Revision History Procedure

For every modification to the SRS:
1. **Document the Delta**: Record the change in the Revision History / Changelog section:
   - Revision Version (e.g. `v1.2.0`)
   - Effective Date
   - Author / Contributor
   - Summary of Changes & Rationale
   - List of Modified / Added / Deprecated Requirement IDs
2. **Preserve Historical Traceability**:
   - Never silently delete an existing requirement ID.
   - If a requirement is no longer applicable, set its status to `DEPRECATED` or `SUPERSEDED` and record the rationale.
3. **Synchronize Traceability Matrix**:
   - Update rows in the Requirements Traceability Matrix (RTM) reflecting changed classes and test fixtures.
4. **Update System Diagrams**:
   - Re-render or update TikZ/Mermaid diagrams to reflect modified component interactions or data flows.
