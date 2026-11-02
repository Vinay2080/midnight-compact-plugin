# Current Handoff

## Current Feature / Bug Fix
Run Action Document Synchronization and Subdirectory Hyperlink Resolution (`ai/fix-run-hyperlink-and-doc-sync` -> `master`).

## Status
- **Accomplished**:
  - **Run Configuration Document Synchronization & Hyperlink Resolution (v1.4.2)**:
    - `CompactRunProfileState`: Synchronously flushes dirty in-memory editor documents to disk (`FileDocumentManager.getInstance().saveAllDocuments()`) before spawning `compactc`, eliminating coordinate drift and stale compilation. Passes target contract file path to `CompactConsoleFilter`.
    - `CompactConsoleFilter`: Added multi-tier file resolution: target-file matching, project-relative lookup, and fallback iterative breadth-first search (`findFileInTree`) to resolve bare filenames emitted for contracts in nested subdirectories. Added range coordinate parsing (`:line:col-endcol` and `:line:col:endline:endcol`).
    - `CompactConsoleFilterTest`: Added 3 unit tests (`testBareFilenameInSubdirectoryResolvedViaTargetFile`, `testBareFilenameInSubdirectoryResolvedViaProjectTreeSearch`, `testDiagnosticRangeFormat`). All 10 tests pass.
    - Total test suite: 775 tests across 69 suites with 100% pass rate.
    - All verification gates passed (Gate 0, Gate 1, Gate 1b, Gate 2, Gate 3).
  - **Run Configuration Auto-Navigation on Compiler Failure (v1.4.2)**:
    - `CompactConsoleFilter`: Added thread-safe tracking of the first resolved error hyperlink via `AtomicReference<OpenFileHyperlinkInfo>`.
    - `CompactRunProfileState`: Integrated automated editor navigation into `processTerminated` when the compiler process finishes with a non-zero exit code (`event.getExitCode() != 0`). Safely dispatches `firstError.navigate(project)` onto the EDT via `ApplicationManager.getApplication().invokeLater(..., ModalityState.any())`.
  - **First-Class Type Hierarchy & Substitutor (Phase 31 / v1.4.2)**:
    - Sealed `CompactType` hierarchy, `CompactTypeVariable`, and `CompactTypeSubstitutor` in `dev.verloren.midnight.type`.
    - Integrated with `CompactTypeInferenceUtil` with primitive priority and nominal base matching.
  - **Compact 0.35.0 & Language 0.27.0 Support (v1.4.1)**:
    - `CompactVersionManager`: Added release entry `("0.35.0", "0.27.0", "Midnight Ledger 9", false)` and added `"0.35.0"` to `KNOWN_VERSIONS`.
    - Built-in types `PublicAddress`, `Secp256r1`, `Curve25519` and cryptographic functions `secp256r1EcdsaVerify`, `ed25519Verify`, `sha512`.

## Immediate Next Priorities
1. **Phase 32 Modular Completion Providers**: Decompose monolithic `CompactCompletionContributor` into distinct `CompletionProvider` subclasses.
2. **Phase 33 Stub Indexing**: Implement project-wide stub indexing (`CompactStubIndex`) for cross-file declarations without parsing full AST.
