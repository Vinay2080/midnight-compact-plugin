# Bug: File Template Selection Persistence Defaulting to Empty File in CompactCreateFileAction

- **Date**: 2026-10-02
- **Feature / Subsystem**: actions / file-templates / persistence
- **Severity**: Medium
- **Status**: Resolved
- **Affected Version**: Plugin v0.1.0+
- **Resolved In Branch**: `ai/fix-create-file-template-persistence`

---

## 1. Symptoms & Failure Behavior

When creating a new Compact file via the **New -> Compact File** context menu dialog, selecting any non-default template kind (such as **Contract**, **Module**, or **Interface**) failed to persist across invocations.

Upon reopening the "New Compact File" dialog, the selected kind invariably reset back to the first entry ("Compact File"), ignoring the user's previously chosen template kind.

---

## 2. Root Cause Analysis

The IntelliJ Platform SDK handles template selection persistence in `com.intellij.ide.actions.CreateFileFromTemplateAction` and `com.intellij.ide.actions.newclass.CreateWithTemplatesDialogPanel` through the following mechanism:

1. **Storage of Selected Template**:
   When a file is created via `CreateFileFromTemplateAction.createFileFromTemplate(...)`, the platform persists the template's name in `PropertiesComponent`:
   ```java
   PropertiesComponent.getInstance(project).setValue(defaultTemplateProperty, template.getName());
   ```
   `FileTemplate.getName()` returns the bare template name **without file extension** (e.g. `"Compact Contract"`, `"Compact Module"`, `"Compact Interface"`).

2. **Template Registration Mismatch**:
   In `dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory`, template constants were declared with the `.compact` file extension:
   ```java
   public static final String COMPACT_CONTRACT = "Compact Contract.compact";
   ```
   These constants were passed to `builder.addKind(kindName, icon, CompactFileTemplateGroupFactory.COMPACT_CONTRACT, validator)`.

3. **Dialog Presentation Matching Failure**:
   When `CreateWithTemplatesDialogPanel` initializes to restore the previous selection, it iterates through its registered template presentations and compares the persisted property with `presentation.templateName()`:
   ```java
   if (StringUtil.equals(defaultTemplateName, presentation.templateName())) {
       myTemplatesList.setSelectedIndex(i);
       return;
   }
   // ...
   myTemplatesList.setSelectedIndex(0); // Fallback to index 0
   ```
   Because `defaultTemplateName` was `"Compact Contract"` and `presentation.templateName()` was `"Compact Contract.compact"`, `StringUtil.equals` evaluated to `false`. Finding no match, the dialog panel dropped to the fallback: `setSelectedIndex(0)` ("Compact File").

---

## 3. Investigation & Comparative Reference Analysis

Ground truth verification was conducted against local reference plugins and IntelliJ platform classes:
- **IntelliJ Solidity** (`intellij-solidity/.../createFile.kt`): Passes bare template names (`const val SMART_CONTRACT_TEMPLATE = "Solidity Contract"`, not `"Solidity Contract.sol"`).
- **IntelliJ Rust** (`intellij-rust/.../RsCreateFileAction.kt`): Passes `"Rust File"` (not `"Rust File.rs"`).
- **IntelliJ Platform plugin.xml**: Registers `<internalFileTemplate name="Compact Contract"/>` (bare name).
- **FileTemplateManager**: `FTManager.findTemplateByName(String)` supports both bare names and qualified names when looking up templates, but `FileTemplate.getName()` invariably returns the bare name.

---

## 4. Solution & Implementation

1. **Bare Template Names in `CompactFileTemplateGroupFactory`**:
   Updated template name constants to bare names matching `FileTemplate.getName()`, while providing distinct `COMPACT_*_NAME` constants for the settings page descriptor:
   ```java
   public static final String COMPACT_FILE = "Compact File";
   public static final String COMPACT_CONTRACT = "Compact Contract";
   public static final String COMPACT_MODULE = "Compact Module";
   public static final String COMPACT_INTERFACE = "Compact Interface";

   public static final String COMPACT_FILE_NAME = "Compact File.compact";
   public static final String COMPACT_CONTRACT_NAME = "Compact Contract.compact";
   public static final String COMPACT_MODULE_NAME = "Compact Module.compact";
   public static final String COMPACT_INTERFACE_NAME = "Compact Interface.compact";
   ```

2. **Backward-Compatible Property Migration in `CompactCreateFileAction`**:
   Overrode `getDefaultTemplateName(@NotNull PsiDirectory dir)` to strip any legacy `.compact` suffix present in existing developer workspaces from earlier plugin releases:
   ```java
   @Override
   protected @Nullable String getDefaultTemplateName(@NotNull PsiDirectory dir) {
     String property = getDefaultTemplateProperty();
     if (property == null) {
       return null;
     }
     String value = PropertiesComponent.getInstance(dir.getProject()).getValue(property);
     if (value != null && value.endsWith(COMPACT_EXTENSION)) {
       return value.substring(0, value.length() - COMPACT_EXTENSION.length());
     }
     return value;
   }
   ```

---

## 5. Verification & Tests

1. **Reproduction Unit Test**:
   Authored `testLastTemplatePropertyMatchesRegisteredKind()` in `CompactCreateFileActionTest`. Prior to the fix, execution failed with:
   `ComparisonFailure: Saved template property must match registered template kind expected:<Compact Contract[.compact]> but was:<Compact Contract[]>`
2. **Comprehensive Kinds Verification**:
   Verified persistence across all four template kinds: `COMPACT_CONTRACT`, `COMPACT_MODULE`, `COMPACT_INTERFACE`, and `COMPACT_FILE`.
3. **Legacy Migration Unit Test**:
   Authored `testGetDefaultTemplateNameStripsLegacyCompactExtension()` to verify that legacy `.compact` suffixes in `PropertiesComponent` are cleanly stripped for dialog presentation matching.
4. **Automated Verification Harness**:
   Ran `verify-patch.ps1` — compilation, static analysis (Checkstyle), plugin verification, and tests all passed with 0 errors and 0 warnings.
