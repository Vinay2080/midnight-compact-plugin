# Bug: IncorrectOperationException During File Creation Reformatting in CompactCreateFileAction

- **Date**: 2026-09-20
- **Feature / Subsystem**: actions / file-templates / threading
- **Severity**: High
- **Status**: Resolved
- **Affected Version**: Plugin v0.1.0+
- **Resolved In Branch**: `ai/fix-create-file-psi-command`

---

## 1. What Happened & Symptoms

When a user creates a new Compact file (e.g. `New -> Compact Contract / Module / Interface / Empty File`) using the IntelliJ New File dialog, an `IncorrectOperationException` is thrown:

```text
com.intellij.util.IncorrectOperationException: Must not change PSI outside command or undo-transparent action. See com.intellij.openapi.command.WriteCommandAction or com.intellij.openapi.command.CommandProcessor
	at com.intellij.pom.core.impl.PomModelImpl.startTransaction(PomModelImpl.java:296)
	...
	at com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl.reformat(CodeStyleManagerImpl.java:106)
	at dev.verloren.midnight.actions.CompactCreateFileAction.postProcess(CompactCreateFileAction.java:146)
	at dev.verloren.midnight.actions.CompactCreateFileAction.postProcess(CompactCreateFileAction.java:33)
	at com.intellij.ide.actions.CreateFromTemplateAction.postProcess(CreateFromTemplateAction.java:144)
	at com.intellij.ide.actions.CreateFromTemplateAction.lambda$actionPerformed$0(CreateFromTemplateAction.java:117)
```

The exception prevents successful post-creation formatting and caret positioning, and presents a fatal red error notification / log entry to the user.

---

## 2. Context

During file creation via CreateFileFromTemplateAction, after the new file is generated from the velocity template, IntelliJ Platform invokes postProcess(PsiFile createdElement, String templateName, Map<String, String> customProperties). In CompactCreateFileAction, postProcess reformats the generated file according to project code style settings and places the editor caret at the identifier position.

---

## 3. Root Cause Analysis

In IntelliJ Platform, CreateFromTemplateAction.actionPerformed runs createFile under write action, but then executes postProcess outside the file creation write command.

In IntelliJ Platform, `CreateFromTemplateAction.actionPerformed` runs `createFile` under write action, but then executes `postProcess` outside the file creation write command.

`CodeStyleManager.getInstance(project).reformat(createdElement)` performs AST leaf and whitespace substitutions (`PsiBasedFormattingModel.replaceWithPsiInLeaf` -> `ChangeUtil.prepareAndRunChangeAction` -> `PomModelImpl.startTransaction`). In IntelliJ Platform, any PSI model transaction requires an active command on the undo stack or an explicit undo-transparent command. Because `postProcess` was calling `CodeStyleManager.reformat` directly without wrapping it in `WriteCommandAction.runWriteCommandAction(project, () -> ...)`, `PomModelImpl` detected that no command transaction was active and threw `IncorrectOperationException`: Must not change PSI outside command or undo-transparent action.

---

## 4. Solution

In `dev.verloren.midnight.actions.CompactCreateFileAction`:
1. Imported `com.intellij.openapi.command.WriteCommandAction`.
2. Wrapped `CodeStyleManager.getInstance(project).reformat(createdElement)` inside `WriteCommandAction.runWriteCommandAction(project, () -> { ... })`:

```java
  @Override
  protected void postProcess(
      @NotNull PsiFile createdElement,
      String templateName,
      Map<String, String> customProperties
  ) {
    super.postProcess(createdElement, templateName, customProperties);
    Project project = createdElement.getProject();
    WriteCommandAction.runWriteCommandAction(project, () -> {
      CodeStyleManager.getInstance(project).reformat(createdElement);
    });

    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null && editor.getDocument() == createdElement.getViewProvider().getDocument()) {
      CompactNamedElement named = PsiTreeUtil.findChildOfType(createdElement, CompactNamedElement.class);
      if (named != null && named.getNameIdentifier() != null) {
        editor.getCaretModel().moveToOffset(named.getNameIdentifier().getTextRange().getEndOffset());
      }
    }
  }
```

---

## 5. Verification

1. **Reproduction Test**: Created `src/test/java/dev/verloren/midnight/actions/CompactCreateFileActionTest.java` invoking `action.createFile(...)` followed by `action.postProcess(...)` without external command wrapping, confirming that postProcess manages its write command safely.
2. **File Template Test Suite**: Verified `CompactFileTemplateTest.java` across contract, module, interface, and empty template creations.
3. **Verification Harness**: Executed `verify-patch.ps1 -TestPattern "dev.verloren.midnight.actions.CompactCreateFileActionTest"` and `verify-patch.ps1 -Quick` with all gates passing (Java 25 compilation, structure validation, unit tests).
4. **Code Inspection**: Executed IntelliJ MCP tools `get_file_problems` and `lint_files` on modified files with 0 errors and 0 warnings.

---

## 6. Prevention & Lessons

- **Invariant 1 (`threading.rules.md`)**: All PSI modifications, reformatting, and leaf substitutions must be performed on the EDT and wrapped inside `WriteCommandAction` or executed inside an active command transaction.
- Never assume framework callbacks (`postProcess`, listeners, background notifications) are running within an open write command transaction even if called during an action workflow. Wrap PSI mutation methods defensively in `WriteCommandAction.runWriteCommandAction(project, () -> ...)`.

---

## 7. Modified Files

- `src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java`
- `src/test/java/dev/verloren/midnight/actions/CompactCreateFileActionTest.java`
- `src/test/java/dev/verloren/midnight/ide/fileTemplates/CompactFileTemplateTest.java`