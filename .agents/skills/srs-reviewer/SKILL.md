---
name: srs-reviewer
description: Review, audit, and verify Software Requirements Specifications (SRS) against IEEE 29148 / IEEE 830 quality attributes, ambiguity anti-patterns, testability, and traceability.
---

# SRS Reviewer Skill

## Purpose
Systematically review, audit, score, and validate Software Requirements Specifications (SRS). Ensures every requirement meets rigorous quality standards before design, implementation, and verification phases commence.

---

## 1. IEEE 29148 Quality Attributes Checklist

Every requirement and specification section must be audited against the following eight core quality characteristics:

| Attribute | Definition | Pass Criteria | Typical Failure Mode |
| :--- | :--- | :--- | :--- |
| **1. Unambiguous** | Only one single interpretation is possible. | Specific numeric limits or discrete deterministic outcomes stated. | Use of subjective adjectives: "fast", "efficient", "intuitive", "user-friendly". |
| **2. Complete** | All necessary conditions, inputs, outputs, constraints, and exceptions are specified. | Covers happy path, boundary values, error states, and invalid inputs. | Missing specification of what occurs when an error or network drop happens. |
| **3. Consistent** | Free of conflicts with other requirements, constraints, or parent standards. | No contradictory terms, mutually exclusive states, or divergent taxonomies. | REQ-A says type inference is strictly nominal; REQ-B specifies structural duck-typing. |
| **4. Verifiable / Testable** | A finite, cost-effective process exists to prove satisfaction. | Definite verification method specified (Test, Demo, Analysis, Inspection) with quantitative pass/fail threshold. | "The system shall be easy to navigate." (Non-quantifiable). |
| **5. Traceable** | Bidirectional linkage exists backwards to stakeholder needs and forwards to code and tests. | Unique `[REQ-xxx]` ID, mapped in Traceability Matrix to design components and unit/integration tests. | Requirements without identifiers or missing from test coverage matrices. |
| **6. Modifiable** | Structure and style allow changes to be made completely and consistently. | Redundancy eliminated; cross-references used instead of copy-pasting requirements. | Duplicated requirement clauses in multiple sections becoming out of sync. |
| **7. Ranked for Importance / Stability** | Identified by priority and likelihood of future evolution. | High / Medium / Low priority tagging; Core vs. Optional vs. Future release demarcations. | All requirements treated as equal emergency priority. |
| **8. Feasible** | Capable of being implemented within constraints, budget, and technological limits. | Architecturally viable within the host platform SDK (e.g. IntelliJ threading & memory constraints). | Demanding synchronous network calls or full project indexation on the UI thread. |

---

## 2. Forbidden Words & Ambiguity Detection Heuristics

The reviewer must flag and reject the following lexical anti-patterns:

### 2.1 Subjective Adjectives & Adverbs
- ❌ "Fast", "Performant", "Ultra-fast" $\to$ ✅ "Complete execution within $\le 50\text{ ms}$ under a 10,000-line buffer."
- ❌ "User-friendly", "Intuitive", "Modern" $\to$ ✅ "Require $\le 2$ user interactions (keyboard shortcuts) to invoke."
- ❌ "Robust", "Resilient" $\to$ ✅ "Handle syntax error tokens by resynchronizing at the nearest statement boundary without throwing `ProcessCanceledException`."
- ❌ "Reasonable", "Appropriate", "Sufficient" $\to$ ✅ Define exact numeric thresholds or explicit enumeration of valid states.

### 2.2 Evasive & Non-Committal Modals
- ❌ "Might", "Could", "Should if possible", "May optionally consider" $\to$ ✅ Use strict RFC 2119 modals: `SHALL`, `SHOULD`, `MAY`, `SHALL NOT`.
- ❌ "Etc.", "And so on", "Including but not limited to" $\to$ ✅ Provide an exhaustive closed-set enumeration or explicit extension contract.

---

## 3. Systematic Review Procedure

When executing an SRS audit:
1. **Structural Audit**: Verify presence of all standard ISO/IEC/IEEE 29148 sections (Introduction, Context, Features, Interfaces, NFRs, Traceability, Appendices).
2. **Grammar & Identification Audit**: Verify all requirement IDs conform to `[REQ-<SUB>-<CAT>-<NUM>]`. Verify EARS structure.
3. **Completeness & Boundary Audit**: Ensure every functional feature specifies:
   - Preconditions / Triggering event.
   - Valid inputs and processing logic.
   - Outputs / observable results.
   - Error handling, boundary conditions, and exception behavior.
4. **Non-Functional & Constraint Audit**: Verify that latency, memory, threading, and security requirements are explicitly quantified with testable metrics.
5. **Diagram Audit**: Verify that all architectural, structural, and behavioral diagrams match the normative text without contradictions.
6. **Traceability Audit**: Ensure every functional requirement maps to at least one test case or verification gate.

---

## 4. SRS Review Report Output Template

```markdown
# SRS Quality Audit Report: <System Name>

## Executive Summary
- Total Requirements Inspected: <N>
- Pass: <N> | Needs Revision: <N> | Critical Rejections: <N>
- Ambiguity Index: <Score / 100>
- Testability Coverage: <% of requirements with quantifiable acceptance criteria>

## Detailed Findings by Subsystem

### [REQ-XXX-YYY-NNN] <Title>
- **Finding Severity**: [Critical / Major / Minor / Recommendation]
- **Issue**: <Explanation of ambiguity, incompleteness, or inconsistency>
- **Violation**: <Specific quality attribute violated, e.g., IEEE 29148 Testability>
- **Remediation**: <Concrete rewritten requirement statement conforming to EARS>

## Verification & Traceability Status
- [ ] Bidirectional Traceability Complete
- [ ] Non-Functional Metrics Quantified
- [ ] Threading & Memory Invariants Enforced
```
