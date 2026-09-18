<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

### Added
- Added dynamic file template pragma version resolution via `CompactDefaultTemplatePropertiesProvider` and `CompactCreateFileAction`, automatically resolving `COMPACT_LANGUAGE_VERSION` and `COMPILER_VERSION` from active project toolchain configurations with robust `0.26.0` fallback.
- Added contextual code completion for `language_version` and `compiler_version` directives immediately after the `pragma` keyword at top level and inside incomplete pragma forms, with automatic trailing space insertion.
- Added rich HTML quick documentation (`Ctrl+Q` / `F1`) for pragma directives (`language_version` and `compiler_version`) both on declaration hover and within completion lookup popups, explaining semantic version constraints, supported comparison operators (`>=`, `>`, `^`, `~`, `==`), and tooling requirements.
- Added Ctrl+Click and Go to Declaration navigation for `CompactStandardLibrary` in import statements (`import CompactStandardLibrary;`), jumping directly to the bundled `standard-library.compact` source file.
- Added comprehensive in-editor documentation comments across the entire Compact Standard Library (`standard-library.compact`), providing detailed descriptions, parameter documentation, and usage examples for core types (`Maybe`, `Either`), Merkle tree verification, and shielded token operations (`receiveShielded`, `sendShielded`, `mintShieldedToken`, etc.).
- Added Quick Documentation (Ctrl+Q / F1) support for standard library imports and file headers, displaying rich documentation overviews of the Midnight Compact Standard Library.
- Added automatic quotation mark pairing (`"` and `'`) with caret positioning between paired quotes and smart closing quote step-over during editor typing.

### Fixed
- Fixed an issue where changing the Compact compiler version via quick-fix (Alt+Enter) in the editor did not update the active version highlight and cards in the Compact Compiler tool window side panel.
- Fixed an issue where circuit parameters and in-scope variables were omitted from autocompletion inside return statements if their type differed from the return type.
- Fixed false-positive warnings reporting that sealed ledger fields cannot be modified outside constructors when initializing state inside Compact library modules.
- Fixed false-positive type mismatch errors when returning ternary expressions (cond ? a : b) from circuits and witnesses.

## [1.3.2] - 2026-09-16

### Fixed
- Fixed an issue where the editor cursor would unexpectedly jump to the beginning of the line when pressing the spacebar or typing on an indented empty line in Compact files.
- Fixed premature stripping of trailing spaces and indentation during active editing by eliminating forced background file saves and compiling live document buffers seamlessly in memory.

## [1.3.1] - 2026-09-16

### Added
- Added automatic delimiter and structural punctuation skipping during editor typing for closing delimiters (`)`, `]`, `}`, `>`), colons (`:`), semicolons (`;`), commas (`,`), and closing quotes (`"`, `'`), advancing the cursor without inserting duplicate symbols.
- Added automatic angle bracket insertion and caret positioning inside brackets for sized types (such as `Bytes` and `Uint`) with contextual size and bit-width suggestions.
- Added automatic angle bracket (`<>`) pairing, cursor placement, overtyping, and paired backspace deletion for generic types and parameterized expressions.
- Added automatic data type completion popups when tabbing through declaration live template variables.
- Added quick live template scaffolding for exported ledger declarations (`expled`).
- Added built-in type completions for `State`, `Counter`, `Void`, `JubjubPoint`, and `Secp256k1Point`.
- Added context-aware declaration completions offering both exported and unexported variations at the top level.
- Added intelligent autocompletion for all Compact export declarations (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`) and export selection blocks.
- Added automatic numbering for newly generated declarations with scope-aware gap filling.

### Changed
- Improved completion after `export` keywords to show only valid declarations and modifiers without duplicate `export` keywords.
- Improved top-level `ledger` completion to insert standard unexported ledger declarations.
- Updated declaration live templates to calculate context-aware default identifiers dynamically.
- Aligned Compact grammar completion rules with contemporary language specifications.

### Fixed
- Fixed editor caret freeze and dropped typing events (such as after typing the initial keyword of a line or pressing Space) when the Compact Compiler tool window is active.
- Optimized editor change notifications and compiler panel responsiveness to eliminate unnecessary card re-renders while typing in the editor.
- Fixed editor threading exceptions when completing sized types or assert statements within active live templates.
- Fixed deprecated platform message bundle initialization to ensure compatibility with future IntelliJ platform releases.
