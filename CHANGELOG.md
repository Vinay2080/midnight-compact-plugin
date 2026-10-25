<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

### Added
- Added support for Compact compiler toolchain v0.35.0 (Compact language version 0.27.0, Midnight Ledger 9), enabling version management, official release downloads, WSL translation, and automatic pragma inspection matching.
- Added first-class type hierarchy and generic type substitution support, providing accurate type variable representation, generic parameter instantiation, and robust type checking for generic structs and circuits.
- Added autocompletion and semantic highlighting for new Compact language 0.27.0 builtin types (`PublicAddress`, `Secp256r1`, `Curve25519`) and cryptographic builtin functions (`secp256r1EcdsaVerify`, `ed25519Verify`, `sha512`).
- Added `PublicAddress` struct declaration to the Compact Standard Library reference (`standard-library.compact`).
- Updated New Compact File template default fallback pragma directives from `0.26.0` to `0.27.0`.

### Changed
- Refined Compact compiler version cards in the sidebar tool window to present clear version descriptions, target language specification, and Midnight Ledger levels.
- Streamlined New Compact File templates (Contract, Module, Interface, and File) to eliminate boilerplate dummy code, ensuring clean starting skeletons and placing the editor cursor directly inside the declaration block ready for typing.

### Fixed
- Fixed toolchain output directory matching on Windows and case-insensitive platforms so output folders aligned with project source roots are recognised correctly regardless of path casing.
- Fixed false-negative type checking where mismatched types in return statements and expressions were not flagged as errors due to overly broad generic type parameter detection.
- Fixed code completion ranking in return statements so variables matching the expected return type correctly appear ahead of unrelated types.
- Fixed an issue where cancelling a compiler toolchain download on Windows could fail due to file locks, leaving corrupted temporary files behind.
- Hardened archive extraction for downloaded compiler toolchains against directory traversal risks.
- Improved responsiveness and stability when switching or detecting Compact compiler versions across local systems and WSL environments.
- Fixed an issue where creating a file with an already existing name logged an unexpected warning stack trace in the IDE log; the dialogue now proactively checks for file collisions in real time, and underlying operation exceptions are propagated to display standard user error notifications.
- Fixed template selection persistence in the New Compact File dialogue so the IDE properly remembers the user's last selected file type (such as Contract, Module, or Interface) instead of defaulting back to the empty file option.
- Fixed semantic syntax highlighting test assertions for version literals in pragma directives following lexer character advancement improvements, ensuring automated verification passes reliably.
- Fixed validation in the New Compact File dialogue to prevent invalid directory traversal and non-printable control characters, support case-insensitive file extensions, and improve responsiveness and localisation during file creation.
- Fixed an issue where typing an invalid or unrecognised character (such as `@`, `~`, or backticks) in the editor would accidentally consume and corrupt the following token.

## [1.4.0] - 2026-09-29

### Added
- Added typed generic `right<L, R>(...)` and `left<L, R>(...)` constructor helper completions when returning or assigning to `Either<L, R>` types, automatically inferring and populating concrete type arguments, positioning the caret within parentheses, and registering tab-out scopes.
- Added member autocompletion (`target.<caret>`) for parameterised and generic struct variables (e.g., suggesting `is_left`, `left`, `right` for `Either<Bytes<32>, ContractAddress>`).
- Added smart typed and generic `Either` struct literal completions (`Either<T1, T2> { is_left: true, left: ..., right: default<T2> }` and `Either { ... }`) with bracket-aware type argument parsing, expected type detection, and interactive live templates across type parameters and field values (`is_left`, `left`, `right`).
- Added structured and contextual code completions for `default<Type>` with angle brackets, type parameter assistance, and smart type inference in expression contexts.
- Added structured struct literal completions (`Either { is_left: true, left: ..., right: default }`) with multi-field live template tabbing across `is_left`, `left`, and `right`, as well as constructor helper completions (`left(val)` and `right(val)`) and parameterised type completions (`Either<Left, Right>`).
- Added boolean literal completions for `true` and `false` with dynamic prioritisation in boolean condition contexts (e.g. `if (...)`, `assert(...)`, `is_left:` fields).

### Fixed
- Fixed false-positive unresolved struct field warnings (`left`, `right`, `is_left`) and type mismatch errors when using generic structs (such as `Either<A, B>`), struct literals, and generic circuit calls with concrete type arguments.

## [1.3.5] - 2026-09-27

### Fixed
- Fixed an issue where creating a new Compact file from a template could fail with an `IncorrectOperationException` during post-creation formatting.
- Fixed an issue where WSL compiler paths with short root mount locations were not correctly translated back to local Windows paths.

## [1.3.4] - 2026-09-23

### Added
- Added prioritised suggestions for already installed Compact compilers satisfying pragma constraints (`pragma language_version` and `pragma compiler_version`) in inspections and context intentions, presenting satisfying local versions sorted descending before external download options and eliminating redundant downloads when the required version is installed.
- Added dynamic pragma version resolution to newly created Compact file templates, automatically populating the required language version directive based on your currently configured project compiler toolchain.
- Added contextual code completion for `language_version` and `compiler_version` directives immediately after the `pragma` keyword at top level and inside incomplete pragma forms, with automatic trailing space insertion.
- Added rich HTML quick documentation (`Ctrl+Q` / `F1`) for pragma directives (`language_version` and `compiler_version`) both on declaration hover and within completion lookup popups, explaining semantic version constraints, supported comparison operators (`>=`, `>`, `^`, `~`, `==`), and tooling requirements.
- Added Ctrl+Click and Go to Declaration navigation for `CompactStandardLibrary` in import statements (`import CompactStandardLibrary;`), jumping directly to the bundled `standard-library.compact` source file.
- Added comprehensive in-editor documentation comments across the entire Compact Standard Library (`standard-library.compact`), providing detailed descriptions, parameter documentation, and usage examples for core types (`Maybe`, `Either`), Merkle tree verification, and shielded token operations (`receiveShielded`, `sendShielded`, `mintShieldedToken`, etc.).
- Added Quick Documentation (Ctrl+Q / F1) support for standard library imports and file headers, displaying rich documentation overviews of the Midnight Compact Standard Library.

### Fixed
- Fixed an issue where changing the Compact compiler version via quick-fix (Alt+Enter) in the editor did not update the active version highlight and cards in the Compact Compiler tool window side panel.
- Fixed an issue where circuit parameters and in-scope variables were omitted from autocompletion inside return statements if their type differed from the return type.

## [1.3.3] - 2026-09-21

### Added
- Added automatic quotation mark pairing (`"` and `'`) with caret positioning between paired quotes and smart closing quote step-over during editor typing.

### Fixed
- Fixed false-positive warnings reporting that sealed ledger fields cannot be modified outside constructors when initialising state inside Compact library modules.
- Fixed false-positive type mismatch errors when returning ternary expressions (cond ? a: b) from circuits and witnesses.

## [1.3.2] - 2026-09-16

### Fixed
- Fixed an issue where the editor cursor would unexpectedly jump to the beginning of the line when pressing the spacer or typing on an indented empty line in Compact files.
- Fixed premature stripping of trailing spaces and indentation during active editing by eliminating forced background file saves and compiling live document buffers seamlessly in memory.

## [1.3.1] - 2026-09-16

### Added
- Added automatic delimiter and structural punctuation skipping during editor typing for closing delimiters (`)`, `]`, `}`, `>`), colons (`:`), semicolons (`;`), commas (`,`), and closing quotes (`"`, `'`), advancing the cursor without inserting duplicate symbols.
- Added automatic angle bracket insertion and caret positioning inside brackets for sized types (such as `Bytes` and `Uint`) with contextual size and bit-width suggestions.
- Added automatic angle bracket (`<>`) pairing, cursor placement, overtyping, and paired backspace deletion for generic types and parameterised expressions.
- Added automatic data type completion popups when tabbing through the declaration of live template variables.
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
- Fixed the editor caret freeze and dropped typing events (such as after typing the initial keyword of a line or pressing Space) when the Compact Compiler tool window is active.
- Optimised editor change notifications and compiler panel responsiveness to eliminate unnecessary card re-renders while typing in the editor.
- Fixed editor threading exceptions when completing sized types or assert statements within active live templates.
- Fixed the deprecated platform message bundle initialisation to ensure compatibility with future IntelliJ platform releases.
