<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

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
- Fixed 'assert' completion and 'ass' live template to insert enclosing parentheses ('assert()'), position the caret inside for condition and message input, avoid duplicate parentheses, and register a tab-out scope.
- Fixed sized type completion inserting bare type names without required size parameter brackets.
- Fixed issue where data type completion was incorrectly suppressed in exported declaration headers.
- Fixed automatic declaration numbering to skip existing numbered declarations and reuse deleted numbers.
- Fixed compiler diagnostic range mapping when running under WSL on Windows.
- Fixed duplicate `export export` insertion when autocompleting declarations after an existing `export` keyword.
- Fixed invalid top-level `export const` suggestions in code completion and intention actions.

## [1.3.0] - 2026-09-16 [Withdrawn]
- Superseded by 1.3.1 due to editor responsiveness fix. See 1.3.1 for full notes.
