---
name: intellij-sdk-expert
description: Guides development on IntelliJ Platform SDK 2025.1+ adhering to threading, indexing, action system, and PSI lifecycles.
---

# IntelliJ Platform SDK Expert

## Purpose
Guide engineers and autonomous agents when implementing features, inspections, completions, or toolchains against IntelliJ Platform SDK 2025.1+ (Java 25).

## Key Principles & Best Practices

### 1. Threading Model (ReadAction vs WriteAction)
- **Read Operations**: Reading the PSI tree, resolve operations, or finding references must occur under a `ReadAction` or on background threads. Never lock the UI thread for expensive PSI traversals.
- **Write Operations**: Modifying documents or PSI structures must always be wrapped in `WriteCommandAction.runWriteCommandAction(project, () -> ...)`.
- **Cancellation**: Long-running loops or traversals must invoke `ProgressManager.checkCanceled()` periodically. Never swallow `ProcessCanceledException`.

### 2. SideEffectGuard & Intention Previews
- IntelliJ Platform renders intention previews and quick-fix diffs in a simulated headless context.
- Never trigger UI popups, asynchronous background tasks, or `SwingUtilities.invokeLater` inside intention previews or `generatePreview`.
- Check if `element.getProject().isDefault()` or use `IntentionPreviewUtils.isPreviewElement(element)` when performing actions that interact with disk.

### 3. External Annotators & Linters
- Follow the 3-phase lifecycle:
  1. `collectInformation(PsiFile)`: Gathers document text, VirtualFile, toolchain configs under ReadAction.
  2. `doAnnotate(InitialInfo)`: Executes the external process (e.g. `compact compile`) asynchronously without holding a ReadAction.
  3. `apply(PsiFile, AnnotationResult, AnnotationHolder)`: Applies resulting highlighting annotations onto the editor.

### 4. Memory Management & PSI Lifetimes
- **Prohibited**: Never cache `PsiElement`, `PsiFile`, or `Document` across invocations or in static collections.
- Cache computed values with `CachedValuesManager.getCachedValue(element, () -> CachedValueProvider.Result.create(data, PsiModificationTracker.MODIFICATION_COUNT))`.
- For editor disposables, always register with `Disposer.register(parentDisposable, childDisposable)`.

### 5. Custom FakePsiElement & Reader Mode
- For doc comments and synthetic AST nodes, implement `FakePsiElement` and implement `getOwner()`, `getParent()`, `getTextRange()`.
- Implement `PsiDocCommentBase` to integrate seamlessly with Reader Mode and rendered documentation previews.
