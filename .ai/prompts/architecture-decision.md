# Custom AI Prompt: Architectural Decision Record (ADR) Creation & Evaluation Protocol

This document defines the standardized prompt and execution playbook for analyzing architectural trade-offs, designing new subsystems, evaluating reference plugin implementations, and authoring Architectural Decision Records (ADRs) in `midnight-compact-plugin`.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent to design a subsystem or formalize an architectural decision, use the following prompt:

```text
You are tasked with evaluating an architectural decision and authoring an Architectural Decision Record (ADR) in midnight-compact-plugin following the official ADR Protocol. Execute these steps sequentially with zero exceptions:

1. ARCHITECTURAL CONTEXT & PROBLEM FORMULATION:
   - Identify the subsystem being designed or modified (e.g. Parser, Lexer, PSI, Resolver, Indexing, Completion, Linter, Run Configurations, Daemon).
   - Formulate the architectural problem, scalability constraints, threading challenges, and IntelliJ Platform integration requirements.
   - Review existing ADRs in `.ai/decisions/` to avoid contradicting established patterns or re-litigating settled decisions.

2. GROUND TRUTH & REFERENCE PLUGIN CITATIONS:
   - Upstream Compiler Ground Truth: Cite specific compiler sources (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`, `midnight-ledger.ss`) or official Compact specifications.
   - Local Reference Plugin Benchmarks: Compare against proven patterns in local reference codebases:
     * `intellij-elixir/`: Handwritten Lexer/Parser, `LexerBase`, PSI wrappers, element factory.
     * `intellij-rust/`: Contextual completion scoping, `ExternalAnnotator`, cargo run configurations.
     * `intellij-scala/`: Subsystem completion contributors, compiler daemon IPC, REPL.
     * `Rplugin/`: Toolchain discovery, WSL path translation, project generators.

3. OPTION ANALYSIS & TRADE-OFF EVALUATION:
   - Evaluate at least 2–3 concrete implementation alternatives.
   - Score each alternative against:
     * Correctness and AST fidelity.
     * Threading safety (ReadAction, cancellation, UI thread responsiveness).
     * Performance and memory footprint (avoiding static PSI leaks, leveraging CachedValuesManager).
     * Maintainability and alignment with modern Java 25 standards.

4. AUTHOR THE ADR FILE:
   - Determine the next sequential ADR number (e.g. `ADR-029`).
   - Create `.ai/decisions/NNN-<subsystem>-<title>.md` matching the standard ADR schema:
     * Status: Proposed / Accepted
     * Date: YYYY-MM-DD
     * Context & Problem Statement
     * Decision Drivers & Invariants
     * Evaluated Options & Trade-Offs
     * Decision Outcome & Justification
     * Compiler Ground Truth & Reference Repo Citations
     * Implementation Map (files created/modified, PSI wrappers, tests)
     * Scalability, Memory & Threading Impact

5. REGISTER IN ADR MASTER INDEX:
   - Update `.ai/decisions/README.md`:
     * Add the new ADR to the master chronological table.
     * Add the new ADR to the subsystem classification matrix.

6. SYNCHRONIZE PROJECT DOCUMENTATION:
   - If the decision modifies plugin architecture, update `.ai/context/architecture.md`.
   - Update `.ai/project-state.yaml` with the newly registered ADR.
   - Re-read all updated files using `client_view_file` to confirm formatting, links, and integrity.
```

---

## 2. Standard ADR Schema

```markdown
# ADR-NNN: <Title>

- **Status**: Accepted
- **Date**: YYYY-MM-DD
- **Subsystem**: [Lexer / Parser / PSI / Resolve / Completion / Semantic / Annotator / Indexing / Run]
- **Related ADRs**: ADR-XXX (or None)

---

## 1. Context & Problem Statement
- Describe the engineering problem, platform requirement, or language feature necessitating this architectural decision.

## 2. Decision Drivers & Constraints
- Platform threading rules (ReadAction, WriteCommandAction).
- Compact compiler grammar rules and standard library specifications.
- Memory constraints (no static PSI references) and caching requirements.

## 3. Evaluated Options
- **Option 1**: <Description, Pros, Cons>
- **Option 2**: <Description, Pros, Cons>
- **Option 3**: <Description, Pros, Cons>

## 4. Decision Outcome
- Selected option and detailed technical rationale.

## 5. Compiler Ground Truth & Reference Plugin Citations
- Upstream Compact compiler source lines.
- Reference plugin files (`intellij-rust`, `intellij-elixir`, `intellij-scala`, `Rplugin`).

## 6. Implementation Map
- Files to create or modify.
- Threading and lifecycle model.
- Test suites required for validation.

## 7. Consequences & Trade-Offs
- Positive impacts.
- Negative impacts or limitations.
```
