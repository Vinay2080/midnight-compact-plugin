# ADR-005: AbstractBlock Formatter & Smart Indentation

## Context
Compact code formatting must conform to the canonical 2-space indentation style used in official Midnight smart contracts, with standard binary operator spacing, delimiter spacing, and block indentation.

## Decision
1. Implement `CompactFormattingModelBuilder` producing a `DocumentBasedFormattingModel` over a recursive `CompactBlock` AST block hierarchy.
2. Delegate spacing rules to an IntelliJ `SpacingBuilder` configured with language tokens (`CompactTokenTypes`).
3. Set default code style settings via `CompactLanguageCodeStyleSettingsProvider` (2-space indent, 2-space continuation indent, no tabs).
4. Compute child attributes in `CompactBlock.getChildAttributes` for smart Enter indentation after opening braces and keywords.

## Alternatives Considered
1. **External Prettier/Formatter Process**: Invoking an external Node or Scheme script for formatting.
2. **Ad-hoc Regex / Line-based Formatter**: String replacements across lines.

## Why
- Full native IntelliJ integration (`Ctrl + Alt + L`, format on save, auto-indent on typing).
- Zero external runtime dependencies.
- Perfect idempotency: `format(format(code)) == format(code)`.

## Consequences
- Formatting is instant, resilient, and tested across all official contract examples.
