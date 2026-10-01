# Bug: Path Traversal Vulnerability, Swallowed Cancellation, and Hardcoded Strings in CompactCreateFileAction

- **Date**: 2026-10-01
- **Feature / Subsystem**: actions / file-templates / security / localization
- **Severity**: High
- **Status**: Resolved
- **Affected Version**: Plugin v0.1.0+
- **Resolved In Branch**: `ai/fix-create-file-action`

---

## 1. Symptoms & Failure Behavior

1. **Path Traversal & Control Characters Vulnerability**: In the "New Compact File" dialog, input validation in `CompactCreateFileAction.validateFileName` allowed relative directory traversal segments (`..` and `.`) such as `../evil` or `contracts/../Token`, as well as control characters (ASCII `< 32` like newlines and tabs).
2. **ProcessCanceledException Swallowing**: `CompactCreateFileAction.createFile` enclosed template instantiation in `try { ... } catch (Exception e) { LOG.warn(...); return null; }`. Because `ProcessCanceledException` extends `RuntimeException`, it was caught and swallowed, breaking IntelliJ platform cancellation semantics and causing thread hangs or masking project disposal.
3. **Hardcoded User-Facing Text**: The action menu item, dialog title, template kinds, and all validation error messages used hardcoded English string literals instead of the centralized resource bundle (`CompactBundle` / `MyMessageBundle.properties`).
4. **Case-Sensitive File Extension Stripping**: Entering `Token.COMPACT` or `Token.Compact` caused `.compact` to be appended again, creating files like `Token.COMPACT.compact`.
5. **Per-Keystroke Validator Allocation**: Every character typed in the file name input box allocated a new instance of `CompactNamesValidator`.
6. **Obsolete Checkstyle Suppression**: An obsolete suppression for `MethodLength` remained in `config/checkstyle/checkstyle-suppressions.xml` despite all methods in `CompactCreateFileAction` being $\le 32$ lines.

---

## 2. Root Cause Analysis

- **Path Validation**: `validateFileName` checked `c == 0` instead of `c < 32` and checked `segment.trim().isEmpty()` without rejecting `"."` or `".."` path segments.
- **Cancellation**: Generic `catch (Exception e)` in IntelliJ actions violates the platform SDK contract requiring all `ControlFlowException` / `ProcessCanceledException` instances to be re-thrown.
- **Localization**: UI text strings were defined inline instead of referenced via `CompactBundle.message(...)` and `CompactBundle.messagePointer(...)`.
- **Extension Suffix**: `name.endsWith(".compact")` did not ignore case (`toLowerCase(Locale.ROOT)`).

---

## 3. Investigation & Comparative Reference Analysis

A comprehensive comparative audit was performed against reference plugins present locally:
- **IntelliJ Scala** (`NewScalaFileAction.scala`): Explicitly catches `ControlFlowException` and re-throws it:
  ```scala
  catch {
    case c: ControlFlowException => throw c
    case e: Exception => ...
  }
  ```
- **IntelliJ Rust** (`RsCreateFileAction.kt`): Routes all UI strings and dialog item labels through `RsBundle.message(...)`.
- **IntelliJ Solidity** (`createFile.kt`): Sanitizes file and module names using platform utilities (`FileUtil.sanitizeFileName`).
- **R Plugin** (`NewRScriptAction.kt`): Fully externalizes script and notebook templates and dialog strings to `RBundle`.

---

## 4. Solution & Implementation

In `dev.verloren.midnight.actions.CompactCreateFileAction`:
1. **Rethrow ProcessCanceledException**:
   ```java
   } catch (ProcessCanceledException pce) {
     throw pce;
   } catch (Exception e) {
     LOG.warn("Compact file template not found: " + templateName, e);
     return null;
   }
   ```
2. **Harden Path Validation**:
   - Reject characters with ASCII code `< 32`.
   - Reject empty directory segments as well as `"."` and `".."` traversal segments.
3. **Case-Insensitive Extension Stripping**:
   - Added `stripCompactExtension(@NotNull String name)` inspecting `toLowerCase(Locale.ROOT).endsWith(".compact")`.
4. **Localize UI Strings**:
   - Added 14 keys in `src/main/resources/messages/MyMessageBundle.properties` under `action.dev.verloren.midnight.actions.CompactCreateFileAction.*`.
   - Used `CompactBundle.messagePointer(...)` for action text/description and `CompactBundle.message(...)` for dialog kinds and validation errors.
5. **Static Validator Constant**:
   - Reused `private static final CompactNamesValidator NAMES_VALIDATOR = new CompactNamesValidator();`.
6. **Suppression Cleanup**:
   - Removed obsolete `MethodLength` suppression in `config/checkstyle/checkstyle-suppressions.xml`.

---

## 5. Verification & Tests Added

1. **Reproduction Unit Tests in `CompactCreateFileActionTest.java`**:
   - `testValidateFileNameRejectsDirectoryTraversal`: Verifies rejection of `../evil`, `contracts/../Token`, and `contracts/./Token`.
   - `testValidateFileNameRejectsControlCharacters`: Verifies rejection of `\n` and `\t`.
   - `testExtractSimpleNameCaseInsensitive`: Verifies stripping of `.COMPACT` and `.Compact`.
   - `testStripCompactExtension`: Verifies stripping behavior and passthrough of non-compact extensions.
   - `testCreateFileEdgeCases`: Verifies null/empty name handling and missing template resilience.
   - `testActionMetadata`: Verifies action naming and equality.
2. **Template Tests**: `CompactFileTemplateTest` verified passing with 0 regressions.
3. **Static Analysis**: `checkstyleMain` and `checkstyleTest` verified passing with 0 warnings.
4. **Verification Harness**: `scripts/verify-patch.ps1` completed with `PASSED` across all gates.

---

## 6. Prevention & Key Lessons Learned

- **Never Catch Generic `Exception` Unconditionally in IntelliJ Code**: Always catch `ProcessCanceledException` first and re-throw, or check `if (e instanceof ControlFlowException) throw e;`.
- **Validate File Name Inputs Thoroughly**: Never assume modal inputs will only contain simple filenames; sanitize directory separators, traversal segments (`..`), and non-printable control characters.
- **Resource Bundle Rigor**: All user-facing text in actions, dialogs, and validators must be defined in `MyMessageBundle.properties` from inception.
