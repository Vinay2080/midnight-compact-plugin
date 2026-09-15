<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

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
- Fixed sized type completion inserting bare type names without required size parameter brackets.
- Fixed issue where data type completion was incorrectly suppressed in exported declaration headers.
- Fixed code completion, keyword suggestions, and template expansion popping up inside comments and documentation blocks.
- Fixed editor crash when previewing compiler switch quick-fixes and intention actions.
- Fixed stale diagnostic underlines by automatically synchronizing editor buffers prior to external compilation.
- Fixed compiler diagnostic range mapping when running under WSL on Windows.
- Fixed duplicate `export export` insertion when autocompleting declarations after an existing `export` keyword.
- Fixed invalid top-level `export const` suggestions in code completion and intention actions.

## [1.2.4] - 2026-09-09
