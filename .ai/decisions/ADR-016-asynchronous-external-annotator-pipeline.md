# ADR-016: 3-Phase Asynchronous External Annotator Pipeline

## Status
Accepted (Phases 9–10 / Complete)

## Context & Problem Statement
While local inspections verify syntax and basic semantics instantly in the AST, the official upstream Midnight compiler (`compact`) performs deep typechecking, zero-knowledge constraint checks, and ledger state verification that cannot be replicated without running the compiler.
However, executing an external compiler process directly inside an editor keystroke loop poses serious hazards:
1. Running `process.waitFor()` or reading process output on the Event Dispatch Thread (EDT) completely freezes the IntelliJ user interface.
2. Incomplete keystrokes during rapid typing trigger multiple concurrent processes, depleting system resources.
3. If a compilation takes 500ms, results arriving after the user has continued typing will place error squiggles on outdated offsets.

## Authoritative References
1. **IntelliJ External Annotator Architecture**:
   - `com.intellij.lang.annotation.ExternalAnnotator<InitialInfo, AnnotationResult>`.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsExternalAnnotator.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/annotator/RsExternalAnnotator.kt) — 3-phase pipeline running `cargo check` in the background with cancellation checks and modification stamp validation.

## Decision
1. **Phase 1: `collectInformation` (EDT / ReadAction)**:
   - Verifies target file is a `CompactFile` stored on a local virtual file system.
   - Captures snapshot metadata in an immutable Java 25 record: `InitialInfo(Project, VirtualFile, String compilerPath, boolean skipZk, long modificationStamp)`.
   - Fast and non-blocking (completes in <1ms).
2. **Phase 2: `doAnnotate` (Background Thread Pool)**:
   - Invoked on a background pooled thread managed by IntelliJ's `ProgressManager`.
   - Constructs a non-destructive compilation command (`--skip-zk` to optimize speed) writing to a temporary scratch directory.
   - Parses stderr and stdout lines into `CompactCompilerDiagnostic` records (line, column, severity, message).
   - Periodically calls `ProgressManager.checkCanceled()` to terminate the process immediately if the user switches files or cancels.
3. **Phase 3: `apply` (EDT / Annotator Callback)**:
   - Checks if the file's current modification stamp matches `result.modificationStamp()`. If the user typed further during compilation, the results are discarded to prevent misplaced annotations.
   - Maps 1-based (line, column) compiler coordinates to editor `TextRange` offsets.
   - Creates inline `HighlightSeverity.ERROR` or `WARNING` annotations with full compiler diagnostic descriptions.

## Scalability & Anti-Hardcoding Evaluation
- **Is it thread-safe?**: Yes. Follows IntelliJ's strict concurrency model (PSI read on EDT, process execution on background thread, annotation application on EDT).
- **Does it handle stale results?**: Yes. Modification stamp checking guarantees that annotations are never applied to stale documents.

## Feature Implementation Map
- Annotator: [`CompactExternalAnnotator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java)
- Diagnostic Model: [`CompactCompilerDiagnostic.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactCompilerDiagnostic.java)
- Problem Utilities: [`CompactProblemUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactProblemUtil.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<externalAnnotator>`)
- Unit Tests: [`CompactExternalAnnotatorTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/annotator/CompactExternalAnnotatorTest.java)

## Consequences & Future Maintenance
- Background compilation requires a valid compiler toolchain configured. If no compiler is found, `collectInformation` returns `null` cleanly without throwing errors.
