# Bug: Swallowed IncorrectOperationException and Missing File Collision Validation in CompactCreateFileAction

- **Date**: 2026-10-02
- **Feature / Subsystem**: actions / file-templates / ui / validation
- **Severity**: Medium
- **Status**: Resolved
- **Affected Version**: Plugin v0.1.0+
- **Resolved In Branch**: `ai/fix-create-file-already-exists-exception`

---

## 1. Symptoms & Failure Behavior

When attempting to create a new Compact file with a name that already exists in the destination directory (or in a specified subfolder):
1. **Unnecessary Exception Logging**: IntelliJ logged a warning with a full stack trace to `idea.log`:
   ```text
   WARN - #dev.verloren.midnight.actions.CompactCreateFileAction - Failed to create Compact file from template Compact Contract.compact
   com.intellij.util.IncorrectOperationException: File '...\what.compact' already exists.
       at com.intellij.psi.impl.file.PsiDirectoryImpl.checkCreateFile(PsiDirectoryImpl.java:467)
       ...
       at dev.verloren.midnight.actions.CompactCreateFileAction.createFile(CompactCreateFileAction.java:160)
   ```
2. **Missing User Feedback**: The popup silently closed, and `ElementCreator` never received the `IncorrectOperationException` because `CompactCreateFileAction.createFile` caught `Exception e` and returned `null`. As a consequence, `ElementCreator.handleException` was bypassed and `Messages.showMessageDialog` was never shown, leaving the user without an explanation of why file creation failed.
3. **Missing Pre-Flight Collision Validation**: In the "New Compact File" dialog, input validation (`validateFileName` and `validateIdentifier`) only checked syntax, illegal characters, traversal segments, and reserved keywords. It did not check whether a file with that name already existed in the target directory or subdirectory, permitting the user to press Enter or click OK on colliding file names.

---

## 2. Root Cause Analysis

- **Unnecessary Catch and Suppression**: In commit `def0704`, `createFileFromTemplate(...)` was wrapped in `try { ... } catch (Exception e) { LOG.warn("Failed to create Compact file from template " + templateName, e); return null; }`. While `ProcessCanceledException` was later rethrown, `IncorrectOperationException` was still caught. In IntelliJ Platform SDK architecture, `CreateFileFromTemplateAction` relies on `ElementCreator.tryCreate` to catch `IncorrectOperationException`, log it at `INFO` level (normal user interaction), and show a standard user error message dialog (`File '...' already exists`). Swallowing `IncorrectOperationException` and logging `LOG.warn` both polluted `idea.log` with alarming stack traces and suppressed user dialog notification.
- **Unchecked Target Directory**: Neither `fileValidator` nor `identifierValidator` inspected the target `PsiDirectory` to proactively detect existing `.compact` files or subdirectories before form submission.

---

## 3. Investigation & Comparative Reference Analysis

Analysis of reference plugins (`intellij-rust`, `intellij-scala`, `intellij-solidity`):
- **IntelliJ Rust** (`RsCreateFileAction.kt`): Delegates `createFile` to platform defaults without catching or swallowing `IncorrectOperationException`.
- **IntelliJ Scala** (`NewScalaFileAction.scala`): Calls `directory.add(file)` directly and lets any `IncorrectOperationException` bubble up to the platform command handler.
- **Platform ElementCreator** (`com.intellij.ide.actions.ElementCreator`):
  ```java
  Exception exception = executeCommand(name, ...);
  if (exception != null) {
      handleException(exception);
      return PsiElement.EMPTY_ARRAY;
  }
  ```
  where `handleException` logs `LOG.info(e)` and invokes `Messages.showMessageDialog(myProject, message, myErrorTitle, Messages.getErrorIcon())`.

---

## 4. Solution & Implementation

1. **Rethrow `IncorrectOperationException` in `CompactCreateFileAction.createFile`**:
   ```java
   } catch (ProcessCanceledException | IncorrectOperationException e) {
     throw e;
   } catch (Exception e) {
     LOG.warn("Failed to create Compact file from template " + templateName, e);
     return null;
   }
   ```
2. **Proactive File Collision Detection**:
   Added `validateFileCollision(@Nullable String inputString, @NotNull PsiDirectory directory)` and `findTargetDirectory(@NotNull PsiDirectory baseDir, @NotNull String path)` to resolve target subdirectories and verify that neither an existing file nor directory with `simpleName + ".compact"` exists.
3. **Dialog Validator Wiring**:
   Updated `createFileValidator(@NotNull PsiDirectory directory)` and `createIdentifierValidator(@NotNull Project project, @NotNull PsiDirectory directory)` in `buildDialog` to check both syntax and target collision in real time as the user types.
4. **Localized Messages**:
   Added `action.dev.verloren.midnight.actions.CompactCreateFileAction.error.already.exists=File ''{0}'' already exists` in `MyMessageBundle.properties`.

---

## 5. Verification & Tests Added

- `testValidateFileCollisionDetectsExistingFile`: Verifies bare name, extension-suffixed name, nested path in existing subdirectory, and new subdirectories.
- `testCreateFileThrowsIncorrectOperationExceptionWhenFileExists`: Verifies that programmatic invocations propagate `IncorrectOperationException` cleanly rather than logging a `LOG.warn` stack trace and returning `null`.
- Verification harness `scripts/verify-patch.ps1` and full test suite run cleanly.
