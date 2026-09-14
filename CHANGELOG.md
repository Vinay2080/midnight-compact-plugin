<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]
### Added
- **Strict Upstream Compact Grammar Alignment for Export & Const Declarations (ADR-028)**:
  - Cross-verified top-level declaration constructs against the official Compact compiler grammar (`compact/compiler/parser.ss:240-270, 460-480`).
  - Confirmed `Program-element` does not permit `const` at top level; `const` is strictly a block-scoped statement inside circuit/function bodies (`Statement0`).
  - Confirmed `export const` is invalid in Compact; constants at module scope are canonically declared and exported using `export pure circuit CONST_NAME(): Type { return ...; }`.
  - Added unit test `testExportConstNotSuggestedAtTopLevel` in `CompactCompletionTest` verifying `export const` and `const` after `export` are not suggested.
  - Added unit test `testToggleExportNotAvailableOnConst` in `CompactPhase28IntentionsTest` ensuring `Alt+Enter` export intention is not available on `const` statements.
  - Registered `ADR-028: Prohibit Top-Level Export Const & Strict Compact Grammar Alignment`.
- **Comprehensive Export Declarations, Modifiers, & Scaffolding (ADR-026)**:
  - Expanded `export` completion in `CompactCompletionContributor` to support all upstream Compact exportable declaration types (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and export selection forms (`{`).
  - Added dedicated completion contexts to `CompactCompletionContext` (`Kind.AFTER_SEALED`, `Kind.AFTER_PURE`, `Kind.AFTER_NEW`, `Kind.AFTER_EXPORT`).
  - Added top-level declaration completions offering both bare and exported variants with `CompactDeclarationInsertHandler` live template scaffolding.
  - Added live templates in `Compact.xml` and bundle descriptions in `MyMessageBundle.properties`:
    - `expstr`: `export struct $NAME$ { $FIELDS$ }`
    - `expen`: `export enum $NAME$ { $MEMBERS$ }`
    - `expt`: `export type $NAME$ = $TYPE$;`
    - `expw`: `export witness $NAME$($PARAMS$): $RET$;`
    - `const`: `const $NAME$: $TYPE$ = $VALUE$;` (statement context only)
    - `exp`: `export $END$`
- **Generalized Declaration Name Auto-Numbering & Scope Analysis (Phase 29, ADR-027)**:
  - Created `CompactDeclarationNameGenerator` providing universal auto-numbering (`circuit1`, `circuit2`, `witness1`, etc.) for newly generated declarations.
  - Implemented lowest-available-integer gap filling (e.g. if `circuit1` and `circuit3` exist, generates `circuit2`).
  - Implemented exact user intent preservation: explicit names are preserved verbatim without alteration.
  - Implemented scope container isolation across blocks, modules, contracts, and top-level files.
  - Created extensible `CompactDeclarationType` registry supporting standard constructs (`circuit`, `witness`, `struct`, `enum`, `module`, `contract`, `type`, `ledger`, `const`) and custom runtime type registrations (`registerCustomType`).
  - Registered live template macros in `plugin.xml`:
    - `compactDeclarationName(declarationType)`: Evaluates dynamic numbered identifier based on context and declaration type.
    - `circuitName()`: Dedicated macro generating `circuit1`, `circuit2`, etc.
    - `witnessName()`: Dedicated macro generating `witness1`, `witness2`, etc.
  - Created `CompactDeclarationInsertHandler` for auto-numbered declaration insertion during code completion.
- **AI Instruction & Context System Restructuring**:
  - Streamlined `AGENTS.md` to focus exclusively on permanent, high-priority rules, critical architectural invariants, Java 25 standards, threading models, and tool selection priorities.
  - Eliminated transient project state, hardcoded test counters, and fragmented checklists from `AGENTS.md`, establishing single sources of truth.
  - Created `.ai/workflow.md` providing an explicit 11-step task lifecycle, task-specific context loading matrix, Intelligent Documentation Decision Rules, and a mandatory 9-step Final Task Gate.
  - Established `.ai/README.md` as the unified directory index and fresh-agent entry point for the `.ai/` knowledge system.
- **Context-Aware `export ledger` Autocompletion & Structural Insert Handler**:
  - Registered `CompactLedgerInsertHandler` to scaffold `export ledger <name>: <type>;` with interactive IntelliJ live template tab-stops for field name and type (defaulting to `ledger1: State;`).
  - Added lookahead guard to avoid overwriting or corrupting trailing identifiers or types if the user already started typing on the same line.
  - Implemented automatic `export ` prefix injection if invoked at top-level without typing `export` first.
  - Added `Kind.AFTER_EXPORT` contextual classification to `CompactCompletionContext` to offer only valid exportable constructs (`ledger`, `circuit`, `module`, `contract`, `sealed`) while suppressing invalid file headers (`pragma`, `include`, `import`).
  - Added `ledger` live template alias expanding to `export ledger $NAME$: $TYPE$;` with dynamic auto-numbering via `compactDeclarationName("ledger")`.

### Changed
- **Disallowed Top-Level `export const` and `const` Suggestions**:
  - Removed `export const` and `const` from top-level declaration completions in `CompactCompletionContributor`.
  - Removed `const` from completions after `export`.
  - Removed obsolete `expconst` live template from `Compact.xml` and bundle descriptions.
  - Restricted `CompactToggleExportIntention` to exportable top-level declarations only (excluding `const`).
  - Updated `CompactDeclarationType.isExportable()` to return `false` for `CONST`.
- **Live Templates Dynamic Auto-Numbering Alignment (`Compact.xml`)**:
  - Updated all declaration live templates (`cir`, `wit`, `en`, `str`, `mod`, `cct`, `ccti`, `type`, `led`, `ledg`, `ledger`) to calculate auto-numbered names dynamically via `compactDeclarationName(...)` with numbered fallbacks (`circuit1`, `witness1`, `struct1`, `enum1`, `module1`, `contract1`, `type1`, `ledger1`).
  - Replaced obsolete block syntax in `led` template (`ledger { ... }`) with contemporary exported ledger syntax `export ledger $NAME$: $TYPE$;`.
  - Updated `ledg` live template to `export ledger $NAME$: $TYPE$;`, removing reference to non-existent `Cell` type.
  - Updated `cct` contract skeleton live template to declare `export ledger $STATE$: $TYPE$;` instead of obsolete `ledger { ... }` block.
  - Updated bundle descriptions in `MyMessageBundle.properties`.

### Tested
- Created unit tests in `CompactCompletionTest` verifying:
  - Context classification for `AFTER_EXPORT`, `AFTER_SEALED`, `AFTER_PURE`, `AFTER_NEW`.
  - `export ` suggests all exportable declaration types, modifiers, and `{`, but strictly excludes `const`.
  - Top-level suggests `export` variants but strictly excludes `export const`.
  - Insertions for `export circuit`, `export struct`, `export enum`, `export type`, `export witness`, `export ledger`.
- Created unit test in `CompactPhase28IntentionsTest` verifying `CompactToggleExportIntention` is NOT available on `const` statements.
- Created `CompactDeclarationNameGeneratorTest` covering auto-numbering, gaps, explicit names, and scope isolation.
- Full test suite verified with `./gradlew test` passing 100% cleanly (519 tests passing).

## [1.2.4] - 2026-09-09
