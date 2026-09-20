# Executable Protocol: Architectural Decision Record (ADR) Creation & Evaluation

This document defines the mandatory, deterministic execution protocol for analyzing architectural trade-offs, designing new subsystems, evaluating reference plugin implementations, and authoring Architectural Decision Records (ADRs) in `midnight-compact-plugin`.

---

## 1. Preconditions

Before authoring or modifying architectural records, the AI agent **MUST** verify:
1. `git status` is clean on `master` (no unstaged or uncommitted user edits).
2. Read and strictly follow all contracts in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/), especially [`architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md).
3. Existing ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) have been reviewed to prevent duplicating or violating established designs (ADR-001 through ADR-034).
4. The proposed architectural change is grounded in upstream Compact compiler sources (`compact/compiler/`) or IntelliJ Platform SDK standards.

---

## 2. Sequential Steps

The agent must execute these 7 steps in exact order:

```text
[Step 1: Workspace Isolation] ── Create branch ai/adr-<slug>
        │
        ▼
[Step 2: Ground Truth & Rules] Inspect compact/compiler/, .agents/rules/ & local reference plugins
        │
        ▼
[Step 3: Option Trade-Offs] ─── Evaluate 2-3 alternatives (correctness, threading, modularity <=400 lines)
        │
        ▼
[Step 4: Author ADR File] ───── Create .ai/decisions/ADR-NNN-...md with standard schema
        │
        ▼
[Step 5: Register in Index] ─── Update .ai/decisions/README.md (chronological & matrix)
        │
        ▼
[Step 6: Sync State & Docs] ─── Update architecture.md & project-state.yaml
        │
        ▼
[Step 7: Commit, Merge & Clean] Stage, commit docs(adr), merge to master, delete branch
```

### Step 1: Workspace Isolation
- Create and switch to a dedicated branch:
  ```bash
  git checkout -b ai/adr-<slug>
  ```

### Step 2: Ground Truth & Reference Plugin Benchmarks
- Inspect upstream Compact compiler ground truth:
  - `compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`, or `standard-library.compact`.
- Load [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md).
- Benchmark against proven local reference plugin architectures:
  - `intellij-elixir/`: Handwritten Lexer/Parser, `LexerBase`, PSI wrappers, element factory.
  - `intellij-rust/`: Type system hierarchy (`Ty`), contextual completion scoping, `ExternalAnnotator`, cargo execution.
  - `intellij-solidity/`: Contract/struct PSI models, resolve scopes, type navigation.
  - `intellij-scala/`: Subsystem completion contributors, compiler daemon IPC, REPL.
  - `Rplugin/`: Toolchain discovery, WSL path translation, project generators.

### Step 3: Option Analysis & Trade-Off Evaluation
- Evaluate at least 2–3 concrete implementation alternatives against:
  - **Layer Isolation**: Strict downward-only dependencies; zero type logic in UI layers.
  - **Modularity Budget**: Class lengths $\le$ 400 lines; single responsibility per class.
  - **Generalization**: Zero hardcoded stdlib names; works uniformly for user-defined structs.
  - **AST Fidelity**: Golden syntax tree preservation and error recovery.
  - **Threading Safety**: `ReadAction`, UI thread responsiveness, cancellation checks.
  - **Memory & Lifecycle**: Prohibition of static PSI leaks, use of `CachedValuesManager`.
  - **Java 25 Alignment**: Idiomatic records, sequenced collections, pattern matching.

### Step 4: Author the ADR File
- Determine next sequential ADR number (e.g. `ADR-035`).
- Create `.ai/decisions/ADR-NNN-<subsystem>-<title>.md` matching the standard schema:
  - Status: Proposed / Accepted
  - Date: YYYY-MM-DD
  - Context & Problem Statement
  - Decision Drivers & Constraints
  - Evaluated Options & Trade-Offs
  - Decision Outcome & Justification
  - Compiler Ground Truth & Reference Repo Citations
  - Implementation Map
  - Consequences & Trade-Offs

### Step 5: Register in ADR Master Index
- Update [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md):
  - Add to the master chronological table.
  - Add to the subsystem classification matrix.

### Step 6: Synchronize Project Documentation
- If modifying core architecture, update [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md).
- Update both `architecture.adrs` and `architecture.decisions` dictionaries in [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml).
- Execute the anti-assumption re-read verification on all modified files.

### Step 7: Atomic Commit, Merge to Master, & Cleanup
- Stage and commit documentation:
  ```bash
  git add .ai/decisions/ .ai/context/architecture.md .ai/project-state.yaml
  git commit -m "docs(adr): add ADR-NNN <title> and synchronize architecture catalog"
  ```
- Switch to master and merge:
  ```bash
  git checkout master
  git merge --ff-only ai/adr-<slug>
  ```
- Delete temporary task branch:
  ```bash
  git branch -d ai/adr-<slug>
  ```
- Verify `git status` on master is clean.

---

## 3. Mandatory Gates & Evidence Requirements

| Gate | Requirement | Mandatory Evidence in Agent Output |
|:---|:---|:---|
| **Gate 0: Isolation** | Branch created | Branch name `ai/adr-...` |
| **Gate 1: Ground Truth** | Compiler / plugin cited | Specific file paths and lines from `compact/compiler/` or reference plugins |
| **Gate 2: Trade-Offs** | $\ge 2$ alternatives evaluated | Summary of pros and cons for each alternative |
| **Gate 3: File Authoring** | Schema compliant ADR | Path to `.ai/decisions/ADR-NNN-...md` |
| **Gate 4: Index Registration** | Master index updated | Diff or updated line in `.ai/decisions/README.md` |
| **Gate 5: State Synchronization** | Machine state updated | Entry in `.ai/project-state.yaml` and `architecture.md` |
| **Gate 6: Re-Read Verification** | Anti-assumption check | Explicit confirmation that all updated files were re-read and validated |

---

## 4. Failure Conditions

The workflow **MUST HALT** if:
1. The proposed decision violates any of the Immutable Invariants in `AGENTS.md` (e.g. replacing handwritten parser with Grammar-Kit, introducing static PSI references).
2. The ADR modifies or overwrites an existing accepted ADR in place rather than superseding it.
3. Git working tree is dirty before branch creation.

---

## 5. Recovery Rules

- **If an Invariant is Violated**: Redesign the option to preserve the invariant (e.g. keep handwritten parser structure, use weak references or `CachedValuesManager`).
- **If an Existing ADR Conflicts**: Mark the new ADR as `Supersedes ADR-XXX` and update the status of the superseded ADR to `Superseded by ADR-NNN`.

---

## 6. Completion Conditions

The task is complete **ONLY** when:
1. The ADR is authored and conforms to the standard schema.
2. Both `.ai/decisions/README.md` tables are updated.
3. `project-state.yaml` and `architecture.md` are synchronized.
4. All files are committed, merged cleanly to `master`, and task branch deleted.
