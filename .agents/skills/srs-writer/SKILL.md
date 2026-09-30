---
name: srs-writer
description: Author comprehensive, standards-compliant Software Requirements Specifications (SRS) conforming to ISO/IEC/IEEE 29148:2018 and IEEE Std 830-1998 in LaTeX and Markdown.
---

# SRS Writer Skill

## Purpose
Guide the authoring of industrial-grade Software Requirements Specifications (SRS) conforming to international standards (ISO/IEC/IEEE 29148:2018, IEEE Std 830-1998, and IEEE Std 1233-1998). Provides structured templates, requirement syntax standards, categorization rules, and LaTeX typesetting frameworks.

---

## 1. Requirement Identification & Syntax Standard

### 1.1 Identifier Convention
Every requirement must have a globally unique, persistent, machine-readable identifier:
```
[REQ-<SUBSYSTEM>-<CATEGORY>-<INDEX>]
```
- `<SUBSYSTEM>`: 3-5 letter subsystem code (e.g., `LEX`, `PRS`, `PSI`, `RES`, `TYP`, `INS`, `CMP`, `IDE`, `SEC`, `PERF`).
- `<CATEGORY>`: `FUN` (Functional), `NFR` (Non-Functional), `INT` (Interface), `CON` (Constraint).
- `<INDEX>`: 3-digit zero-padded sequence number (e.g., `001`, `002`).

Example: `[REQ-PRS-FUN-004]`, `[REQ-TYP-NFR-002]`

### 1.2 The EARS (Easy Approach to Requirements Syntax) Rule
Requirements must be phrased using unambiguous EARS patterns:
1. **Ubiquitous**: "The `<system>` shall `<action>`."
2. **Event-driven**: "When `<trigger>`, the `<system>` shall `<action>`."
3. **State-driven**: "While `<in state>`, the `<system>` shall `<action>`."
4. **Unwanted behavior**: "If `<error condition>`, then the `<system>` shall `<action>`."
5. **Optional feature**: "Where `<feature is enabled>`, the `<system>` shall `<action>`."

### 1.3 Prescriptive Wording Standards (RFC 2119 / IEEE)
- **SHALL**: Mandatory requirement. Absolute obligation.
- **SHOULD**: Recommendation or target requirement with acceptable justification for deviations.
- **MAY**: Permissible optional behavior.
- **SHALL NOT**: Absolute prohibition.

---

## 2. Requirement Specification Template
Each requirement must specify the following metadata attributes:
- **Title**: Descriptive name of the requirement.
- **Statement**: Precise normative statement conforming to EARS.
- **Rationale**: The engineering or business motivation.
- **Inputs**: Preconditions, tokens, user actions, or data feeds.
- **Outputs**: Observable postconditions, UI changes, AST mutations, or diagnostic artifacts.
- **Priority**: High (Must-have for release), Medium (Important), Low (Nice-to-have).
- **Verification Method**: Inspection, Analysis, Demonstration, or Test.
- **Acceptance Criteria**: Formatted as Given / When / Then assertions.

---

## 3. Standard SRS Document Structure (ISO/IEC/IEEE 29148)

A comprehensive SRS must include the following sections:

### Section 1: Introduction
- 1.1 Purpose
- 1.2 Document Conventions (Typography, Identifier scheme, RFC 2119 keywords)
- 1.3 Intended Audience & Reading Suggestions
- 1.4 Project Scope & Objectives
- 1.5 References & Standards (ISO/IEC/IEEE 29148, IEEE 830, Domain specs)

### Section 2: Overall Description
- 2.1 Product Perspective (System context, boundary, relationship to parent ecosystems)
- 2.2 Product Functions (High-level functional summary)
- 2.3 User Classes & Personas (Skill level, responsibilities, use frequencies)
- 2.4 Operating Environment (Hardware, OS, runtime versions, host IDE platforms)
- 2.5 Design & Implementation Constraints (Language, memory, threading, SDK invariants)
- 2.6 Assumptions & Dependencies (External compilers, upstream platform lifecycles)

### Section 3: System Features & Functional Requirements
Grouped logically by architectural subsystem:
- 3.1 Subsystem A (e.g., Lexical & Syntactic Analysis)
- 3.2 Subsystem B (e.g., PSI & AST Model)
- 3.3 Subsystem C (e.g., Scoped Symbol Resolution & Type Inference)
- 3.4 Subsystem D (e.g., Code Insight, Navigation & Refactoring)
- 3.5 Subsystem E (e.g., Static Semantic Inspections & Quick-Fixes)
- 3.6 Subsystem F (e.g., Toolchain Manager & Build Panel)

### Section 4: External Interface Requirements
- 4.1 User Interfaces (Tool windows, popups, syntax highlighting, gutter markers, status bar)
- 4.2 Hardware Interfaces (Platform architectures: x86_64, aarch64, memory bounds)
- 4.3 Software Interfaces (IntelliJ Platform SDK APIs, Process Execution, OS CLI)
- 4.4 Communications Interfaces (HTTPS downloads, IPC, WSL bridging)

### Section 5: Non-Functional Requirements (System Qualities)
- 5.1 Performance Requirements (Latency bounds, throughput, allocation budget)
- 5.2 Reliability & Fault Tolerance (Crash recovery, error synchronization, zero EDT freezes)
- 5.3 Security & Sandbox Isolation (Process execution safety, path traversal prevention)
- 5.4 Maintainability & Extensibility (Downward dependency invariant, anti-hardcoding rule)
- 5.5 Portability & Compatibility (Cross-platform support: Windows, macOS, Linux, WSL)

### Section 6: Verification & Traceability Matrix
- Traceability mapping `[REQ-ID]` $\to$ Architectural Component $\to$ Implementation Class $\to$ Automated Test Suite.

### Section 7: Appendices
- Appendix A: Glossary & Domain Acronyms
- Appendix B: Formal Grammar / EBNF Reference
- Appendix C: Revision History & Changelog

---

## 4. LaTeX Typesetting Best Practices

When rendering the SRS into LaTeX:
- Use standard, robust packages: `amsmath`, `amssymb`, `booktabs`, `tabularx`, `hyperref`, `xcolor`, `fancyhdr`, `geometry`, `listings`, `tikz`.
- Set margins with `\geometry{margin=1in}` or `a4paper`.
- Provide hyperlinked cross-references (`\label{sec:...}`, `\ref{sec:...}`).
- Use `booktabs` (`\toprule`, `\midrule`, `\bottomrule`) for clean, publication-grade tables.
- Render diagrams directly via native `tikz` environments for deterministic vector graphics.
