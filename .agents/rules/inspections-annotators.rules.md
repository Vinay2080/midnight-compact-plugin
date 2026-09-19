# Path Rules: Inspections, Annotators & Editor Intentions

**Scope:** `src/main/java/dev/verloren/midnight/inspection/**`, `src/main/java/dev/verloren/midnight/annotator/**`, `src/main/java/dev/verloren/midnight/intention/**`

## Invariants & Guardrails

1. **Quick-Fix Preview SideEffectGuard**:
   - In IntelliJ Platform, intention and quick-fix previews run in a simulated headless context wrapped in `SideEffectGuard`.
   - Never dispatch `invokeLater` or trigger background tasks inside quick-fix previews. Inspect `preview` state before modifying external resources.

2. **External Annotator 3-Phase Lifecycle**:
   - `collectInformation(PsiFile)`: Runs on background thread with ReadAction. Gathers file text, virtual file, and toolchain paths.
   - `doAnnotate(InitialInfo)`: Runs on background thread without ReadAction. Executes the external linter/compiler CLI process.
   - `apply(PsiFile, AnnotationResult, AnnotationHolder)`: Applies annotations back onto the editor.

3. **Performance & Highlighting Lag**:
   - Never block or perform synchronous network/IPC calls during in-editor annotators.
   - Guard against stale highlights by validating file modification stamps before applying external annotations.
