<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]
### Added
- **Comprehensive Export Declarations, Modifiers, & Scaffolding**:
  - Expanded `export` completion in `CompactCompletionContributor` to support all upstream Compact exportable declaration types (`circuit`, `ledger`, `const`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and export selection forms (`{`).
  - Added dedicated completion contexts to `CompactCompletionContext`:
    - `Kind.AFTER_SEALED`: Suggests `ledger`.
    - `Kind.AFTER_PURE`: Suggests `circuit`.
    - `Kind.AFTER_NEW`: Suggests `type`.
    - `Kind.AFTER_EXPORT`: Filters out invalid file headers (`pragma`, `include`, `import`) and offers all exportable declarations and modifiers.
  - Added top-level declaration completions offering both bare (`circuit`, `ledger`, `const`, `struct`, `enum`, `type`, `module`, `contract`, `witness`) and exported variants (`export circuit`, `export ledger`, etc.) with `CompactDeclarationInsertHandler` live template scaffolding.
  - Added live templates in `Compact.xml` and bundle descriptions in `MyMessageBundle.properties`:
    - `expconst`: `export const $NAME$: $TYPE$ = $VALUE$;`
    - `expstr`: `export struct $NAME$ { $FIELDS$ }`
    - `expen`: `export enum $NAME$ { $MEMBERS$ }`
    - `expt`: `export type $NAME$ = $TYPE$;`
    - `expw`: `export witness $NAME$($PARAMS$): $RET$;`
    - `const`: `const $NAME$: $TYPE$ = $VALUE$;`
    - `exp`: `export $END$`
  - Expanded `CompactToggleExportIntention` to support `CompactElementTypes.CONST_STATEMENT`, enabling in-editor `Alt+Enter` toggling of `export` on `const` declarations.
  - Expanded `CompactDeclarationType` to support `CONST_STATEMENT` in `fromPsi` and `fromElementType`.
  - Updated architectural decision record `ADR-026` to document the full architecture of comprehensive export completions, modifiers, and live templates.
- **Generalized Declaration Name Auto-Numbering & Scope Analysis (Phase 29)**:
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
  - Registered architectural decision record `ADR-027` documenting the declaration auto-numbering architecture, scope analysis, live template macros, and anti-hardcoding evaluation.
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
- **Live Templates Dynamic Auto-Numbering Alignment (`Compact.xml`)**:
  - Updated all declaration live templates (`cir`, `wit`, `en`, `str`, `mod`, `cct`, `ccti`, `type`, `led`, `ledg`, `ledger`) to calculate auto-numbered names dynamically via `compactDeclarationName(...)` with numbered fallbacks (`circuit1`, `witness1`, `struct1`, `enum1`, `module1`, `contract1`, `type1`, `ledger1`).
  - Replaced obsolete block syntax in `led` template (`ledger { ... }`) with contemporary exported ledger syntax `export ledger $NAME$: $TYPE$;`.
  - Updated `ledg` live template to `export ledger $NAME$: $TYPE$;`, removing reference to non-existent `Cell` type.
  - Updated `cct` contract skeleton live template to declare `export ledger $STATE$: $TYPE$;` instead of obsolete `ledger { ... }` block.
  - Updated bundle descriptions in `MyMessageBundle.properties`.

### Tested
- Created unit tests in `CompactCompletionTest` verifying:
  - Context classification for `AFTER_EXPORT`, `AFTER_SEALED`, `AFTER_PURE`, `AFTER_NEW`.
  - `export ` suggests all 9 declaration types, 3 modifiers, and `{`.
  - `export sealed ` suggests `ledger`.
  - `export pure ` suggests `circuit`.
  - `export new ` suggests `type`.
  - Top-level suggests `export` variants with live template scaffolding.
  - Insertions for `export circuit`, `export const`, `export struct`, `export enum`, `export type`, and `export witness`.
- Created unit test in `CompactPhase28IntentionsTest` verifying `CompactToggleExportIntention` toggles `export` on `const` declarations.
- Created `CompactDeclarationNameGeneratorTest` covering auto-numbering, gaps, explicit names, and scope isolation.
- Full test suite verified with `./gradlew test` passing 100% cleanly.

## [1.2.4] - 2026-09-09
