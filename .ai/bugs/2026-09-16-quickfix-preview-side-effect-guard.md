# SideEffectGuard INVOKE_LATER Exception During Quick-Fix and Intention Previews

- **Date:** 2026-09-16
- **Feature / Component:** annotator / intention / version
- **Severity:** High
- **Status:** Resolved

## Symptoms

When viewing intention or quick-fix popups in the IntelliJ editor for Compact pragma mismatches (e.g. `CompactSwitchCompilerQuickFix`), the IDE threw an unhandled exception:
```text
java.lang.RuntimeException: Side effect not allowed: INVOKE_LATER
    at com.intellij.model.SideEffectGuard.checkSideEffectAllowed(SideEffectGuard.kt:21)
    at com.intellij.openapi.application.impl.LaterInvocator.invokeLater(LaterInvocator.java:119)
    at com.intellij.openapi.application.impl.ApplicationImpl.invokeLater(ApplicationImpl.java:554)
    at dev.verloren.midnight.annotator.CompactProblemUtil.clearProblemsAndRestart(CompactProblemUtil.java:28)
    at dev.verloren.midnight.version.CompactVersionManager.switchAndApplyVersion(CompactVersionManager.java:788)
    at dev.verloren.midnight.version.CompactVersionManager.ensureAndSwitchVersion(CompactVersionManager.java:764)
    at dev.verloren.midnight.annotator.CompactSwitchCompilerQuickFix.invoke(CompactSwitchCompilerQuickFix.java:74)
    at com.intellij.codeInsight.intention.IntentionAction.generatePreview(IntentionAction.java:116)
```
This crashed preview generation in the editor, preventing preview popups from rendering and triggering error notifications in JetBrains IDEs (IntelliJ 2023.2+).

## Context

A user reported this crash occurring on September 15th when hovering over or selecting quick-fixes to adjust the Compact compiler toolchain in response to `pragma language_version` inspection warnings.

## Root Cause

In modern IntelliJ IDEA (2023.2+ / 2024+ / 2026+), hovering over an intention or quick-fix in the Alt+Enter menu generates an intention preview via `IntentionPreviewComputable.invokePreview` inside `SideEffectGuard.computeWithoutSideEffects`.

By default:
1. `IntentionAction.generatePreview` calls `invoke(project, editor, previewFile)` to simulate changes on a non-physical copy of the file.
2. `CompactSwitchCompilerQuickFix` and `CompactSwitchCompilerVersionIntention` do not modify the PSI document of the file; instead, they modify project settings (`MidnightProjectSettings`) or trigger asynchronous compiler downloads via `CompactVersionManager.ensureAndSwitchVersion` and invoke `CompactProblemUtil.clearProblemsAndRestart`.
3. `CompactProblemUtil.clearProblemsAndRestart` calls `ApplicationManager.getApplication().invokeLater(...)` to restart the daemon code analyzer.
4. Calling `invokeLater` or mutating global/project settings inside `SideEffectGuard.computeWithoutSideEffects` triggers a strict `SideEffectGuard.checkSideEffectAllowed: INVOKE_LATER` runtime exception.

## Investigation

Inspecting `IntentionAction.java` and IntelliJ preview APIs revealed:
1. `IntentionAction.generatePreview(Project, Editor, PsiFile)` and `LocalQuickFix.generatePreview(Project, ProblemDescriptor)` can be explicitly overridden to return `IntentionPreviewInfo.EMPTY` for actions that do not produce a document diff (such as project setting changes or external tool downloads).
2. `com.intellij.codeInsight.intention.preview.IntentionPreviewUtils.isIntentionPreviewActive()` returns `true` during preview computation.
3. If an intention or quick-fix is invoked during preview, or if downstream utility methods are reached, checking `IntentionPreviewUtils.isIntentionPreviewActive()` allows short-circuiting any daemon restarts or side effects cleanly.

## Solution

The fix applies defense-in-depth across the preview lifecycle:

1. **Explicit Intention Preview Suppression**:
   - In [`CompactSwitchCompilerQuickFix`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactSwitchCompilerQuickFix.java), overrode both `generatePreview(Project, ProblemDescriptor)` and `generatePreview(Project, Editor, PsiFile)` to return `IntentionPreviewInfo.EMPTY`. Added an early return in `invoke()` if `IntentionPreviewUtils.isIntentionPreviewActive()` is true.
   - In [`CompactSwitchCompilerVersionIntention`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/intention/CompactSwitchCompilerVersionIntention.java), overrode `generatePreview(Project, Editor, PsiFile)` to return `IntentionPreviewInfo.EMPTY`. Added an early return in `invoke()` if preview is active.

2. **Daemon Restart Suppression**:
   - In [`CompactProblemUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactProblemUtil.java), guarded `clearProblemsAndRestart(Project, VirtualFile)` with `if (project.isDisposed() || IntentionPreviewUtils.isIntentionPreviewActive()) return;`.

3. **Toolchain Version Switching Guard**:
   - In [`CompactVersionManager`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionManager.java), guarded both `ensureAndSwitchVersion` and `switchAndApplyVersion` with `if (IntentionPreviewUtils.isIntentionPreviewActive()) return;`.

4. **Document Pragma Update Quick-Fixes**:
   - In [`CompactUpdatePragmaQuickFix`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactUpdatePragmaQuickFix.java) and [`CompactUpdatePragmaVersionIntention`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/intention/CompactUpdatePragmaVersionIntention.java), preserved the document replacement preview while suppressing the post-fix `clearProblemsAndRestart` daemon restart when `IntentionPreviewUtils.isIntentionPreviewActive()` is true.

## Verification

1. **Automated Unit & Integration Tests**:
   - Added [`CompactQuickFixPreviewSideEffectTest`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/annotator/CompactQuickFixPreviewSideEffectTest.java) to verify:
     - `CompactSwitchCompilerQuickFix.generatePreview()` returns `IntentionPreviewInfo.EMPTY`.
     - `CompactSwitchCompilerVersionIntention.generatePreview()` returns `IntentionPreviewInfo.EMPTY`.
     - Calling `myFixture.getIntentionPreviewText()` on mismatch inspection quick-fixes does not throw `SideEffectGuard` or `INVOKE_LATER` exceptions.
     - `CompactProblemUtil.clearProblemsAndRestart()` safely handles preview contexts.
2. **Full Regression Suite**:
   - Ran `./gradlew test` with all 59+ test classes passing cleanly (`BUILD SUCCESSFUL in 2m 6s`, 0 failures, 0 errors).

## Prevention / Lesson

- Any intention action or quick-fix that does not mutate the current document (e.g. actions that modify project configuration, launch background tasks, trigger HTTP downloads, or refresh background daemons) **MUST** explicitly override `generatePreview()` and return `IntentionPreviewInfo.EMPTY` or `IntentionPreviewInfo.HtmlInfo`.
- Utility methods that invoke `ApplicationManager.getApplication().invokeLater(...)` or mutate external IDE state should guard against active intention previews with `IntentionPreviewUtils.isIntentionPreviewActive()`.

## Related Files

- `src/main/java/dev/verloren/midnight/annotator/CompactProblemUtil.java`
- `src/main/java/dev/verloren/midnight/annotator/CompactSwitchCompilerQuickFix.java`
- `src/main/java/dev/verloren/midnight/annotator/CompactUpdatePragmaQuickFix.java`
- `src/main/java/dev/verloren/midnight/intention/CompactSwitchCompilerVersionIntention.java`
- `src/main/java/dev/verloren/midnight/intention/CompactUpdatePragmaVersionIntention.java`
- `src/main/java/dev/verloren/midnight/version/CompactVersionManager.java`
- `src/test/java/dev/verloren/midnight/annotator/CompactQuickFixPreviewSideEffectTest.java`

## Related ADRs / Context

- None
