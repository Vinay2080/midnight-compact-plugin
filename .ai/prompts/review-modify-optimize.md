# Engineering Review, Dependency Analysis & Code Optimization Prompt

> **Purpose**: Use this prompt after code has been authored or modified by AI (or any developer) to conduct a thorough dependency investigation, critical architectural review, and execute necessary modifications, optimizations, and test additions.

---

## 1. Target of Analysis (USER INPUT)

**Specify the code or changes to investigate and optimize:**

```text
[TARGET SPECIFICATION]
- File(s) / Component(s): [e.g., dev.verloren.midnight.psi.impl.CompactLexer, CompactParser, CompactResolveUtil]
- Recent AI Changes / Diff: [Paste diff, describe recent changes, or specify git commit/branch]
- Primary Focus / Questions: [e.g., Verify performance, check for memory leaks, eliminate hardcoding, ensure downward dependency compliance, optimize token handling]
- Execution Mode: [Select one: "REVIEW ONLY" (produce report & diff proposals) OR "REVIEW & APPLY" (execute the modifications, refactor, and update tests)]
```

---

## 2. Role & Engineering Mindset

Act as a **Senior Principal Software Engineer & IntelliJ Platform Language Plugin Architect**.

Your objective is to:
1. **Investigate comprehensively**: Trace the target's usage, dependencies, and data/control flow throughout the entire codebase before judging or altering it.
2. **Evaluate objectively**: Apply uncompromising engineering judgment. Challenge hidden assumptions, detect silent regressions, eliminate hardcoding, and enforce architectural invariants.
3. **Optimize pragmatically**: Prioritize simplicity, correctness, and maintainability. Avoid premature abstractions or refactoring for its own sake.
4. **Deliver high-confidence results**: Formulate concrete, production-grade improvements and (when in REVIEW & APPLY mode) execute them safely with zero regressions and complete test coverage.

---

## 3. Phase 1: Dependency & Usage Analysis

Before modifying or finalizing any code, investigate how the target interacts with the surrounding system. Do not isolate the target file—trace its entire ecosystem.

### 3.1 Usage Discovery
* Search for every class, method, interface, and component that directly or indirectly references, instantiates, or calls the target.
* Check IntelliJ Platform extension points (`plugin.xml`), registrations, listeners, and service injections.
* Identify whether consumers rely on direct concrete references, interfaces, or dynamic lookups.
* Inspect all related unit tests, test fixtures, and test utilities.

### 3.2 Dependency Mapping
Establish relationships with surrounding subsystem layers:
* **Lexer / Tokens**: Token types (`CompactTokenTypes`), token sets, raw text boundaries, whitespace/comment filtering.
* **Parser / PSI**: Parser definition, AST nodes, composite elements, element factories, error element recovery.
* **Resolve & Scoping**: Scope hierarchy, namespace separation (`VALUE` vs `TYPE`), cross-file imports/includes.
* **Type Engine**: Structural type inference (`CompactType`), unification, assignability checks.
* **IDE Presentation**: Syntax highlighting, annotators, inspections, completion contributors, intentions, formatters.
* **Platform SDK**: Threading guards (`ReadAction`, `WriteCommandAction`), `CachedValuesManager`, modification trackers.

### 3.3 Data and Control Flow
Trace the runtime lifecycle:
* **Trigger**: What event or component invokes the target? (e.g., user typing, indexing, syntax pass, action execution).
* **Inputs**: What inputs, thread contexts, and state invariants are passed in?
* **Outputs**: What outputs are produced, returned, or mutated?
* **Downstream Consumers**: Which components consume these outputs, and what implicit assumptions do they make about ordering, immutability, token boundaries, or nullability?
* **Failure Modes**: What happens when the input is incomplete, malformed, or cancelled mid-execution?

### 3.4 Change Impact Assessment
Before modifying code, determine:
* Which files, interfaces, and extension points will be affected.
* Whether existing public APIs or binary/source contracts would break.
* What existing functionality or edge cases could silently regress.
* What downstream consumers require accompanying updates.
* Which existing tests must be updated and which new tests must be created.

### 3.5 Dependency Findings Matrix
Summarize the dependency landscape in a structured table:

| Component / Consumer | Relationship to Target | Current Usage / Call Site | Potential Impact of Changes | Verification Status |
| -------------------- | ---------------------- | ------------------------- | --------------------------- | ------------------- |
| *e.g. CompactParser* | *Direct consumer*      | *Calls advanceLexer()*    | *High - Token stream shift* | *Verified*          |

---

## 4. Phase 2: Engineering Judgment & Code Quality Framework

Evaluate the target and any newly authored AI code across the following 10 dimensions:

### A. Correctness & Error Tolerance
* Does the code correctly solve the intended problem under all valid and invalid inputs?
* **Tolerance for Incomplete Code**: Does it defensively handle `null`, missing elements, empty files, and `PsiErrorElement` without throwing NPEs or IndexOutOfBoundsExceptions?
* **Loop Safety**: In parser/lexer/tree traversals, is progress guaranteed on every iteration (e.g. anti-freeze advance guards)?

### B. Hardcoding & Rule of Generalization
* Are identifiers, standard library type names (e.g., `"Either"`, `"Maybe"`, `"Vector"`), configuration values, or paths hardcoded?
* **Rule of Generalization**: Does the feature treat user-defined and standard library types uniformly?
* Are constants, configuration parameters, or abstractions used where appropriate without over-parameterizing trivial details?

### C. Scalability & Memory Lifecycle
* **No Memory Leaks**: Are `PsiElement`, `PsiFile`, `Document`, or `Project` instances stored in static fields, global collections, or non-disposable caches?
* **Smart Caching**: Are expensive computations cached using `CachedValuesManager` with appropriate dependencies (e.g., `PsiModificationTracker.MODIFICATION_COUNT`)?
* Does memory consumption or recursion depth scale safely with file size and project complexity?

### D. Maintainability & Code Size Guardrails
* **File Length Limit**: Does any production or test file exceed **400 lines**? If so, decompose into delegates, strategy providers, or visitors.
* **Method Length Limit**: Does any method exceed **40 lines**? If so, refactor into cohesive sub-routines.
* Are responsibilities cleanly divided according to the Single Responsibility Principle (SRP)?
* Are variable and method names clear, precise, and self-documenting?

### E. Performance & Algorithmic Efficiency
* Are there redundant AST/PSI tree walks, quadratic iterations ($O(N^2)$), or repeated regex allocations in hot paths?
* Can lookups be converted to indexed maps or token bit-sets?
* Is unnecessary string concatenation or repetitive parsing avoided?
* Are long-running or blocking CLI/I/O operations strictly prohibited on the Event Dispatch Thread (EDT)?

### F. Extensibility & Boundary Compliance
* **Strict Downward Dependency Hierarchy**:
  ```text
  [UI / Features / Inspections / Completion]
                      │
                      ▼
            [Resolution & Scoping]
                      │
                      ▼
               [Type Engine]
                      │
                      ▼
            [PSI / Parser / Lexer]
  ```
* Are lower layers (PSI, Lexer, Type Engine) completely decoupled from upper layers (UI, Editor, Completion)?
* Can new syntax or language constructs be added without modifying unrelated features?

### G. Testability & Mirror Test Coverage
* Can components be tested in complete isolation using unit tests or light platform fixtures?
* Are edge cases, empty states, syntax errors, and whitespace variants covered?
* **Mandatory Mirror Tests**: For any test asserting behavior on standard library types, is there an identical companion test exercising user-defined types?

### H. Architectural Consistency & Platform SDK Adherence
* Does the code use modern IntelliJ Platform SDK idioms (2025.1+ / 2026.2+)?
* **Threading Contract**: Are PSI reads performed under `ReadAction` and PSI writes performed under `WriteCommandAction` on the EDT?
* Are extension points, disposable lifecycles, and icon/resource loaders configured correctly?

### I. Overengineering & Simplicity
* Are there gratuitous layers, pointless interfaces, single-implementation abstractions, or over-abstracted patterns?
* Does the introduced complexity pay for itself?
* Can a simpler, more direct implementation achieve the exact same outcome with fewer lines and higher clarity?

### J. Security, Concurrency & Reliability
* Is the code safe against race conditions during background indexing, re-parsing, or dumb mode?
* Are external processes or WSL interactions properly sanitized, timed out, and disposed?
* Are exceptions caught at the appropriate granularity without swallowing critical platform cancellations (`ProcessCanceledException`)?

---

## 5. Phase 3: Improvement Principles

Adhere to these rules when proposing or making modifications:

1. **Simplicity First**: Prefer clean, readable, straightforward code over clever abstractions.
2. **Never Break Working Code Without Rationale**: Preserve established behavior, contracts, and conventions unless fixing a confirmed defect.
3. **Surgical Refactoring**: Make targeted, minimal-diff improvements rather than broad, disruptive rewrites.
4. **Decompose Safely**: When extracting delegates to meet the 400-line rule, maintain backward-compatible facade methods on the parent class.
5. **No Regressions**: Every optimization or change must be verified against existing and new tests.

---

## 6. Output & Execution Protocol

### Step 1: Executive Summary
* A concise assessment of the current code quality, architectural compliance, and critical findings.
* High-level verdict: **[READY / REQUIRES MODIFICATION / CRITICAL BLOCKED]**.

### Step 2: Dependency & Impact Summary
* Summary of direct consumers and downstream risks.
* Dependency Findings Matrix (from § 3.5).

### Step 3: Detailed Review Findings Table

| Location | Finding / Anti-Pattern | Category | Impact | Concrete Recommendation | Priority (Critical / High / Med / Low) |
| -------- | ---------------------- | -------- | ------ | ----------------------- | -------------------------------------- |
| *file:line* | *e.g., Hardcoded "Vector" type* | *Generalization* | *Fails on user custom structs* | *Use CompactResolveUtil to look up struct* | *High* |

### Step 4: Existing Strengths to Preserve
* List well-designed decisions, patterns, or safeguards in the current code that should **not** be modified.

### Step 5: Recommended Action Plan & Optimizations
* Prioritized list of concrete modifications.
* Code diffs or replacement snippets showing the before and after state.
* If file/method limits are exceeded, specify the exact decomposition strategy.

### Step 6: Code Execution (If Mode = "REVIEW & APPLY")
* Execute the edits surgically using safe file replacement tools.
* Add or update unit tests (including mirror tests and error recovery tests).
* Ensure all files conform to the line limit (<= 400 lines) and method limit (<= 40 lines).
* Run project verification (e.g., `./scripts/verify-patch.ps1 -Quick` or `./gradlew test`).
* Document all verified changes.

### Step 7: Verification & Test Checklist
* [ ] All tests passing with zero failures.
* [ ] No new compiler warnings, deprecations, or lint errors.
* [ ] Incomplete code / error resilience validated.
* [ ] Mirror test coverage confirmed for standard vs. custom types.
* [ ] Threading invariants (`ReadAction` / `WriteCommandAction` / zero EDT blocking) verified.
