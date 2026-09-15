<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

### Added
- Added context-aware dual completion options (both with and without `export`) when typing declaration abbreviations (e.g. `wit` suggests both `witness` and `export witness`, `cir` suggests both `circuit` and `export circuit`, etc.) at the top level when `export` is absent.
- Added intelligent autocompletion for all supported Compact export declarations (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and export selection blocks.
- Added automatic numbering for newly generated declarations (e.g. `circuit1`, `circuit2`, `witness1`) with smart gap-filling that respects surrounding scope and preserves user-typed names.
- Added context-aware `export ledger` completion with interactive scaffolding for ledger field names and state types.

### Changed
- Context-aware keyword completion after `export`: when `export` already exists on the line, completions only suggest bare declarations and modifiers, eliminating redundant `export <decl>` options and automatically deduplicating `export` keywords to prevent duplicated `export export` statements.
- Bare `ledger` keyword completion at the top level now inserts unexported ledger declarations (`ledger <name>: <type>;`) without forcing the `export` keyword.
- Improved declaration live templates (`cir`, `wit`, `en`, `str`, `mod`, `cct`, `type`, `ledger`) to dynamically calculate context-aware default names.
- Updated ledger declaration templates to contemporary Compact exported syntax (`export ledger <name>: <type>;`).
- Improved Compact grammar validation to disallow top-level `export const` and `const` completion suggestions, aligning with Compact module-scope rules.

### Fixed
- Suppressed code completions, keyword suggestions, and declaration template expansions (e.g. typing `ledg` + Enter or Tab) inside single-line comments, block comments, and documentation blocks.
- Fixed unhandled `SideEffectGuard: INVOKE_LATER` runtime exception and editor crash when previewing compiler switch quick-fixes and intentions in IntelliJ IDEA 2023.2+.
- Suppressed daemon code analyzer restarts and project SDK mutation during intention preview generation (`IntentionPreviewInfo.EMPTY`).
- Fixed stale compiler error underlines by automatically flushing dirty editor document buffers to disk before external compilation.
- Added WSL `/mnt/<drive>/...` path translation and normalized virtual file paths in `CompactExternalAnnotator` for accurate diagnostic mapping on Windows.
- Added process termination listeners in run configuration and compiler panel to refresh code analysis diagnostics immediately when a compile execution finishes.
- Handled boundary and empty line offsets safely in annotator text range calculation to prevent index out of bounds exceptions.
- Fixed duplicate `export export` bug when autocompleting declarations after an existing `export` keyword.
- Fixed invalid top-level `export const` and `const` suggestions in code completion and intention actions.

## [1.2.4] - 2026-09-09
