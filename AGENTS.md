## 3. Critical Invariants

1. **Do not replace working architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without an explicit directive.
2. **Reuse existing PSI and resolve infrastructure**: Always use `CompactResolveUtil` for symbol lookups and `CompactElementFactory` for PSI node generation.
3. **Strict namespace separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling.
4. **Tolerance for incomplete code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes.
5. **Do not invent Compact language semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and `.ai/context/compact-semantics.md`).
6. **Preserve existing tests**: All unit tests must pass before finishing any feature (`./gradlew test`).
7. **Inspect before modifying**: Read targeted production files before making code edits.
8. **`.gitignore` and Hidden Folders (`.ai/`, `compact/`, `intellij-*/`) are NOT `.aiignore`**: Standard tools (`grep_search` / `find_by_name`) automatically ignore hidden dot-folders and patterns in `.gitignore`. AI agents MUST NOT treat these files as excluded, ignored, or nonexistent. All files under `.ai/decisions/` (such as `ADR-001-handwritten-lexer-and-parser.md`), `.ai/context/`, `docs/`, and gitignored reference codebases (`compact/`, `intellij-elixir/`, `intellij-rust/`, `intellij-scala/`, `intellij-solidity/`, `Rplugin/`) are vital project assets that must be directly accessed via `client_view_file`, `view_file`, or un-ignored commands.
9. **MANDATORY Definition of Done (DoD) & Changelog Gate**:
   - **Never close a task without updating `CHANGELOG.md`**: Any feature, improvement, or bugfix MUST immediately be recorded under `## [Unreleased]` in [`CHANGELOG.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) following Keep-a-Changelog conventions.
   - **Synchronize `.ai/` state**: Update `.ai/context/current-state.md` and `.ai/project-state.yaml` with the latest passing test count and feature status.
   - **Zero Warnings**: Ensure `./gradlew test` passes 100% with zero failures and zero compiler warnings.
10. **Multi-Tier Testing Strategy (Production Standard)**:
   - **Tier 1 (Golden Tree Conformance)**: Every core syntax construct must be backed by a fixture test validating the full PSI tree node-by-node against golden `.txt` trees without errors.
   - **Tier 2 (Partial Parsing & Error Recovery)**: Every parser component must be tested with deliberately broken and incomplete syntax (e.g. missing semicolons, trailing commas, unclosed braces) ensuring `PsiErrorElement` is registered without dropping surrounding contract definitions or freezing the EDT.
   - **Tier 3 (Operator Precedence & Associativity Matrix)**: Test complex binary, bitwise, boolean, relational, and range expression trees to guarantee correct AST hierarchy.
   - **Tier 4 (Stress & Stack Resilience)**: Test resilience against deeply nested parentheses, brackets, and blocks (preventing `StackOverflowError` DoS).
   - **Tier 5 (Non-Blocking Concurrency)**: All inspections, external linters, and index operations must yield immediately upon cancellation without UI thread lag.
