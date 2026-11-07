---
name: compact-architecture-guard
description: Enforces architectural boundaries, dependency direction, generalization invariant, and code quality standards in the Midnight Compact plugin.
---

# Compact Architecture Guard

## Purpose
Enforce core architectural invariants and code quality standards for all development within the `midnight-compact-plugin` repository.

## Non-Negotiable Invariants

### 1. Strict Downward Dependency Hierarchy
Dependencies must flow **strictly downward**:
```
[UI / Completion / Inspection / Editor]
                  │
                  ▼
         [Resolution & Scopes]
                  │
                  ▼
            [Type Engine]
                  │
                  ▼
         [PSI / Parser / Lexer]
```
- **Prohibited**: Importing completion, editor, annotator, or inspection classes in `dev.verloren.midnight.psi`, `dev.verloren.midnight.type`, or `dev.verloren.midnight.resolve`.
- **Prohibited**: Inlining type inference or scope resolution logic inside UI or completion classes. Always delegate to `CompactTypeInferenceUtil` and `CompactResolveUtil`.

### 2. The Rule of Generalization (Anti-Hardcoding Invariant)
Language features (struct literals, constructor inference, generic arguments, member completion) must apply uniformly to **all** user-defined and standard library types.
- **Prohibited**: Hardcoding standard library names (`"Either"`, `"Maybe"`, `"Vector"`, `"default"`) in general compiler, parser, completion, or inspection code to produce unique handling.
- **Prohibited**: Special-casing struct literal generation or constructor completion solely for standard library types.
- **Mandatory Mirror Tests**: Any test exercising standard library constructs must have an identical companion test exercising user-defined structs (e.g. `Result<TVal, TErr>`, `Pair<A, B>`).

### 3. Type System as Single Source of Truth
- All type representations must be instances of `CompactType`.
- **Prohibited**: Passing around raw string signatures (e.g. `"Either<Field, Boolean>"`) and decomposing them via string splits or ad-hoc regex.
- All type compatibility and unification must go through `CompactTypeInferenceUtil.isAssignable(...)`.

### 4. Code Size & Complexity Guardrails
- **Max File Length**: All production and new test classes must remain <= 400 lines.
- **Max Method Length**: All methods must remain <= 40 lines.
- Classes exceeding limits must be decomposed into dedicated strategy providers, delegates, or visitors.

### 5. Memory & Threading Lifecycle
- **Prohibited**: Storing `PsiElement`, `PsiFile`, `Document`, or `Project` in static fields, global collections, or non-disposable caches.
- Read operations on PSI must execute within a `ReadAction` or on background threads.
- Modifications must be wrapped in `WriteCommandAction` on the Event Dispatch Thread (EDT).

### 6. Strict Tooling Precedence: JetBrains MCP Over CLI
- AI agents must strictly prioritize JetBrains MCP tools first (`get_file_problems`, `lint_files`, `reformat_file`, `rename_refactoring`, `search_symbol`, `get_symbol_info`, `execute_run_configuration`).
- Terminal CLI commands are secondary verification or fallback tools. CLI commands must NEVER take priority over JetBrains MCP tools.

### 7. UI, Theme & Tool Window Invariants
- **Two-Axis Viewport Fluidity**: Any tool window or resizable panel must be wrapped in a borderless `JBScrollPane` (`VERTICAL_SCROLLBAR_AS_NEEDED`, `HORIZONTAL_SCROLLBAR_NEVER`) and implement `javax.swing.Scrollable` with `getScrollableTracksViewportWidth() == true` to prevent horizontal clipping while enabling vertical scrolling when shrunk or docked at the bottom.
- **Platform Theme Agnosticism**: Prohibited: `new Color(...)` or raw hex codes in UI components. All colors must derive from platform semantic tokens (`JBColor.namedColor(...)`, `JBUI.CurrentTheme.*`, or platform look-and-feel keys).
- **High-DPI Metric Invariant**: Prohibited: raw pixel sizes for margins, paddings, borders, or preferred dimensions. Always wrap with `JBUI.scale(...)`, `JBUI.Borders.*`, or `JBUI.insets(...)`.
- **Native Component Styling**: Prefer platform standard components with client properties (e.g. `putClientProperty("gotItButton", true)`) over custom `paintComponent` overrides that wash out across different IDE themes.
- **Deterministic Fallbacks & Active Protection**: User cancellation of any dependency download must return the UI to an executable, deterministic fallback state. Currently active project toolchains are strictly protected from deletion.
