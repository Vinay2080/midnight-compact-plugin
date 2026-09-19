# Path Rules: Concurrency, Threading & Memory Lifecycle

**Scope:** `src/main/java/dev/verloren/midnight/**`

## Invariants & Guardrails

1. **PSI Read vs Write Threading Rules**:
   - **PSI Read**: Background thread or EDT with `ReadAction` (`ReadAction.run()` or `ReadAction.compute()`).
   - **PSI Mutation**: Strictly on Event Dispatch Thread (EDT) wrapped in `WriteCommandAction` (`WriteCommandAction.runWriteCommandAction(project, () -> ...)`).
   - **STRICTLY FORBIDDEN**: Modifying PSI elements on background threads or without a write command.

2. **External Process Execution**:
   - **STRICTLY FORBIDDEN**: Calling `process.waitFor()` or running blocking CLI operations (e.g. `compact compile`, `wsl`) on the EDT. This causes immediate IDE UI freezes.
   - External processes must be invoked via background tasks (`Task.Backgroundable`, `CommandLineState.startProcess()`, or asynchronous handlers).

3. **Cancellation Support**:
   - In long loops, indexing passes, or iterative searches across files, call `ProgressManager.checkCanceled()` periodically.
   - Catch and re-throw `ProcessCanceledException` without swallowing it.

4. **Memory Leak Prevention (Static PSI Prohibition)**:
   - **STRICTLY FORBIDDEN**: Storing `PsiElement`, `PsiFile`, `Document`, or `Project` references in static fields, long-lived caches, or non-disposable listeners.
   - Use `CachedValuesManager`, `UserDataHolder`, or register disposables with `Disposer.register(parentDisposable, childDisposable)`.
