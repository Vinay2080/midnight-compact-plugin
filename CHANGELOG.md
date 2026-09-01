<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

## [1.0.0-SNAPSHOT]
### Added
- Initial release of Midnight Compact Language support for IntelliJ IDEA.
- Handwritten Lexer, Parser, and PSI support for Compact syntax.
- Code completion, reference resolution, and cross-file `include` resolution.
- 9 semantic inspections with automated quick-fixes:
  - Type mismatch detection
  - Unresolved reference and duplicate declaration checks
  - Pure circuit mutation and sealed field validations
  - Recursive circuit detection and undisclosed witness warnings
  - Unused local variable highlighting with quick-fixes
- Code formatting, smart indentation, structure view, and inlay parameter hints.
- Toolchain run configurations and gutter execution for the Compact compiler.
- File templates, live templates, breadcrumbs, and hover documentation.
