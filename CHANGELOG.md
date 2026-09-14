<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

### Added
- Added intelligent autocompletion for all supported Compact export declarations (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers (`pure`, `sealed`, `new`), and export selection blocks.
- Added automatic numbering for newly generated declarations (e.g. `circuit1`, `circuit2`, `witness1`) with smart gap-filling that respects surrounding scope and preserves user-typed names.
- Added context-aware `export ledger` completion with interactive scaffolding for ledger field names and state types.

### Changed
- Improved declaration live templates (`cir`, `wit`, `en`, `str`, `mod`, `cct`, `type`, `ledger`) to dynamically calculate context-aware default names.
- Updated ledger declaration templates to contemporary Compact exported syntax (`export ledger <name>: <type>;`).
- Improved Compact grammar validation to disallow top-level `export const` and `const` completion suggestions, aligning with Compact module-scope rules.

### Fixed
- Fixed invalid top-level `export const` and `const` suggestions in code completion and intention actions.

## [1.2.4] - 2026-09-09
