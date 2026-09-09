<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [1.2.4] - 2026-09-09
### Added
- **Comprehensive Architectural Decision Records (ADR-001 through ADR-025)**:
  - Documented the entire plugin architecture in `.ai/decisions/` according to strict production standards (`AGENTS.md` Invariant 9).
  - Grounded every architectural decision in official upstream compiler sources (`compact/compiler/lexer.ss`, `parser.ss`, `langs.ss`, `midnight-ledger.ss`) and local reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).
  - Added explicit Anti-Hardcoding & Scalability evaluations and Feature Implementation Maps linking production code to test suites:
    - `ADR-001`: Handwritten Lexer and Recursive-Descent Parser Engine
    - `ADR-002`: Multi-File Symbol Resolution & Split Namespace Architecture
    - `ADR-003`: Lightweight Structural Type Inference Engine
    - `ADR-004`: Resilient Semantic Inspections Suite & Quick-Fixes
    - `ADR-005`: Abstract Block Formatter & Indentation Model
    - `ADR-006`: Non-Destructive Intent-Preserving Smart Enter Completion
    - `ADR-007`: Non-Conflicting Comment & Doc Enter Scaffolding
    - `ADR-008`: Dual Line and Word Scoping Model for Editor Intentions
    - `ADR-009`: Strict Alignment with Upstream Compact Type Grammar
    - `ADR-010`: File Creation Action, Nested MkDirs, & Template Sanitization
    - `ADR-011`: Toolchain Discovery, WSL Translation, & Windows compact.exe Evasion
    - `ADR-012`: Isolated Multi-Version Compiler Management & SemVer Normalization
    - `ADR-013`: Remix-Style Compiler Tool Window & Dynamic File Tracking
    - `ADR-014`: Status Bar Toolchain Monitor & Rapid Version Switching Popup
    - `ADR-015`: Run Configurations & Context-Aware Gutter Play Actions
    - `ADR-016`: 3-Phase Asynchronous External Annotator Pipeline
    - `ADR-017`: Bundled Compact Standard Library & Virtual Indexing
    - `ADR-018`: Bidirectional Gutter Line Markers & ZK Privacy Indicators
    - `ADR-019`: Context-Aware Code Completion & Structural Scoping
    - `ADR-020`: Symbol Navigation, Global Search, & Type Declaration Provider
    - `ADR-021`: Find Usages Engine & In-Place Rename Refactoring
    - `ADR-022`: Structural Editor Suite: Declarative Inlay Hints, Structure View, Breadcrumbs, & Folding
    - `ADR-023`: Quick Documentation Provider & Interactive Parameter Info
    - `ADR-024`: Two-Tier Semantic Syntax Highlighting & Color Settings Page
    - `ADR-025`: Parametric Live Templates & Surround-With Statement Wrappers
- **Smart Enter & Complete Current Statement (`Ctrl+Shift+Enter` / `Cmd+Shift+Enter`) (Phase 28)**:
  - Registered `lang.smartEnterProcessor` via `CompactSmartEnterProcessor`.
  - Intelligently completes circuit signatures without corrupting developer intent:
    - Automatically appends `: Void {\n  \n}` when return type and colon are completely omitted after parameter list `)`.
    - Preserves user intent when a trailing colon (`:`) is typed: never forces `Void`; formats spacing `: ` and positions caret directly at the return type slot, automatically triggering code completion for types (`AutoPopupController`).
    - Intelligently handles incomplete type annotations conforming to Compact type grammar:
      - `Byte`/`Bytes` completes to `Bytes<>` with caret positioned inside `<` `<caret>` `>` to enter byte size (e.g. `32`).
      - `Uint` completes to `Uint<>` with caret inside `<` `<caret>` `>` to enter bit size (e.g. `64`).
      - `Vector` completes to `Vector<>` with caret inside `<` `<caret>` `>`.\n    - Automatically closes unclosed angle brackets on parameterized types (e.g. `Bytes<32` -> `Bytes<32>`, `Uint<64` -> `Uint<64>`) and attaches the indented body block.
    - Automatically closes unclosed bracketed tuple types (e.g. `[Field, Boolean` -> `[Field, Boolean]`) and attaches the indented body block.
    - Appends indented body braces when complete return types are specified (e.g. `Uint<64>`, `Bytes<32>`, `Field`, `Boolean`).
  - Const statement completion intelligence:
    - Never appends premature semicolons to uninitialized `const` bindings (e.g. `const x` or `const x: Field`). Instead, appends ` = ` and moves the caret for immediate expression input.
    - Accurately appends `;` when an initializer expression is present (e.g. `const x = 10;`).
  - Automatically completes body blocks for `contract`, `struct`, `enum`, `module`, and `constructor` definitions.
  - Guarded bare declaration keywords (`contract`, `struct`, `enum`, `module`, `circuit`, `constructor`) against erroneous semicolon insertion.
  - Automatically closes unclosed parentheses and completes block braces for control-flow statements (`if`, `for`).
  - Automatically inserts missing semicolons on statements (`return`, `assert`, `witness`, `ledger`, `type`, `import`, `include`, `emit`).
  - Exact dynamic caret calculation to position the cursor directly on the indented blank line inside the body block ready for immediate typing.
- **Intelligent Doc Comment Enter Handler (`CompactDocCommentEnterHandler`) (Phase 28)**:
  - Registered `enterHandlerDelegate` for smart newline handling in doc comments (`/** ... */`) and block comments (`/* ... */`).
  - Automatically scaffolds multiline doc comment blocks upon typing `/**` (or `/*`) followed by `Enter`, inserting leading asterisks, indentation, and closing `*/`.
  - Continues comment lines cleanly while preventing duplicate asterisks (`* *`) by checking platform commenter continuation state (`CodeDocumentationAwareCommenter`).
- **Compact In-Editor Intentions (`Alt+Enter`) with Dual Line & Word Scoping (Phase 28)**:
  - Intelligent scoping model providing both **general construct suggestions** across the entire line/header and **specific token suggestions** for words/expressions directly under the caret:
    - `CompactTogglePureCircuitIntention`: Toggle between `circuit` and `pure circuit` from anywhere on the circuit signature header line (not just the keyword).
    - `CompactToggleExportIntention`: Toggle `export` modifier from anywhere on the header line of top-level contracts, circuits, structs, enums, modules, and type definitions.
    - `CompactSurroundWithDiscloseIntention`: Surround target expression with `disclose(...)` to make private witness values public when the caret is on an expression, while smoothly coexisting with line-level intentions.
    - `CompactInvertIfIntention`: Invert `if-else` conditions and swap corresponding `then` and `else` branches from anywhere on the `if (...)` header line or condition expression.
    - `CompactSpecifyTypeExplicitlyIntention`: Add explicit type annotations to untyped `const` bindings from anywhere on the `const` statement line (including on `const`, identifier, `=`, or `;`).
    - `CompactRemoveRedundantTypeIntention`: Remove redundant type annotations from anywhere on the `const` statement line.
  - Full HTML description guides and before/after code previews are provided for all six intentions in `intentionDescriptions`.

### Fixed
- **Doc Comment Enter Scaffolding & Multi-Asterisk Deduplication**:
  - Fixed duplicate asterisks (`* *`) and extraneous trailing closing `*/` on multi-asterisk doc comments (`/**`, `/****`) by delegating doc comment generation exclusively to the platform's `CodeDocumentationAwareCommenter`.
  - Fixed block comment enters scaffolding when opening a new block comment `/*` on the same line after an existing closed comment `*/` (e.g. `/* first */ /*` or ` * but */ /*`).
  - Added lookahead check ensuring existing closing `*/` lower in the document is not duplicated when pressing Enter inside unclosed block comments.

### Tested
- Comprehensive unit testing suite across `CompactDocCommentEnterTest`, `CompactSmartEnterTest`, and `CompactPhase28IntentionsTest` (raising the total test suite to 472 passing tests with 0 warnings).

## [1.2.3] - 2026-09-09
