# AI & Developer Instructions — Midnight Language Plugin

## 1. Project Identity
- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 17+ (IntelliJ Platform Gradle Plugin).
- **Core Status**: Lexer, Parser, PSI, References, Completion, Refactoring, Find Usages, Type Inference, Inspections, Formatter, Smart Indentation, Structure View, Documentation Provider, and Cross-File Resolution are implemented and verified (212/212 unit tests passing).

---

## 2. Architecture Pipeline

```text
Compact Source Text (.compact)
  ↓
[Lexer] CompactLexer (extends LexerBase) + CompactTokenTypes
  ↓
[Parser] CompactParser (implements PsiParser) + CompactElementTypes
  ↓
[PSI] CompactPsiElement / CompactFile / Typed AST Wrappers (dev.verloren.midnight.psi.impl.*)
  ↓
[Resolve & Scope] CompactResolveUtil (Innermost shadowing, split VALUE/TYPE namespaces, cross-file includes)
  ↓
[Semantic Layer] CompactTypeInferenceUtil + Semantic Inspections & Quick-Fixes
  ↓
[IDE Features] Completion, Rename, Find Usages, Formatter, Smart Indent, Structure View, Docs
```

---

## 3. Critical Invariants

1. **Do not replace working architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without explicit directive.
2. **Reuse existing PSI and resolve infrastructure**: Always use `CompactResolveUtil` for symbol lookups and `CompactElementFactory` for PSI node generation.
3. **Strict namespace separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling.
4. **Tolerance for incomplete code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes.
5. **Do not invent Compact language semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and `.ai/context/compact-semantics.md`).
6. **Preserve existing tests**: All 212 unit tests must pass before finishing any feature (`./gradlew test`).
7. **Inspect before modifying**: Read targeted production files before making code edits.

---

## 4. Layered Context System

Deconstructive context files are located in `.ai/`:

| Topic | Context File | Description |
| :--- | :--- | :--- |
| **Machine State** | `.ai/project-state.yaml` | Concise, machine-readable feature and test status |
| **Architecture** | `.ai/context/architecture.md` | Detailed subsystem design, classes, and invariants |
| **Current State** | `.ai/context/current-state.md` | Implementation snapshot, test breakdown, known limitations |
| **Compact Semantics** | `.ai/context/compact-semantics.md` | Verified Compact language behavior and typing rules |
| **Reference Map** | `.ai/context/reference-map.md` | Index of compiler (`compact/`) and IDE reference code |
| **IntelliJ Patterns** | `.ai/context/intellij-patterns.md` | Plugin-specific IntelliJ Platform patterns and APIs |
| **Decisions (ADRs)** | `.ai/decisions/` | Architectural Decision Records (ADR-001 through ADR-005) |
| **Latest Handoff** | `.ai/handoff.md` | Session-to-session continuation summary |

---

## 5. Context Loading Workflows

- **For planning a new feature**:
  1. Read `AGENTS.md` + `.ai/project-state.yaml` + `.ai/context/current-state.md`.
  2. Consult `.ai/context/architecture.md` for existing subsystem boundaries.
  3. Load targeted entries from `.ai/context/compact-semantics.md` and `.ai/context/reference-map.md`.
- **For implementing a planned feature**:
  1. Inspect only the relevant production and test packages in `src/`.
  2. Implement code adhering to `.ai/context/intellij-patterns.md`.
  3. Run `./gradlew test` to verify zero regressions.
  4. Update `.ai/context/current-state.md` and `.ai/handoff.md`.
- **For language-semantic questions**:
  1. Check `.ai/context/compact-semantics.md`.
  2. If unknown, use `.ai/context/reference-map.md` to pinpoint 1–2 reference files in `compact/compiler/` and inspect targeted lines.
- **For IntelliJ platform questions**:
  1. Consult `.ai/context/intellij-patterns.md` and referenced patterns in `intellij-rust/` or `intellij-elixir/`.

---

## 6. Context Efficiency & Token Rules

1. **Targeted lookups**: Search before opening large files; use line-range slices for large sources.
2. **No bulk loading**: Never read entire reference repositories (`compact/`, `intellij-rust/`, `intellij-elixir/`) into context.
3. **No duplicate documentation**: Update existing `.ai/` context files rather than creating overlapping notes.
4. **Summaries over raw code**: Extract verified rules into `compact-semantics.md` rather than pasting raw Scheme/Rust snippets.
5. **Exact references**: Use precise symbol and class names (`dev.verloren.midnight.psi.impl.CompactReferenceExprImpl`).
