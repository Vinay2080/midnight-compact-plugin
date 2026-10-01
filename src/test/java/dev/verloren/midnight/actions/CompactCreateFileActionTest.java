package dev.verloren.midnight.actions;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory;

import java.util.Map;

public class CompactCreateFileActionTest extends BasePlatformTestCase {

  public void testPostProcessWithoutCommand() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    PsiFile created = action.createFile("Token", CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir);
    assertNotNull(created);

    action.postProcess(created, CompactFileTemplateGroupFactory.COMPACT_CONTRACT, Map.of());
  }

  public void testValidateFileNameRejectsDirectoryTraversal() {
    assertNotNull("Traversal '../evil' must be rejected", CompactCreateFileAction.validateFileName("../evil"));
    assertNotNull("Traversal 'contracts/../Token' must be rejected", CompactCreateFileAction.validateFileName("contracts/../Token"));
    assertNotNull("Current dir 'contracts/./Token' must be rejected", CompactCreateFileAction.validateFileName("contracts/./Token"));
  }

  public void testValidateFileNameRejectsControlCharacters() {
    assertNotNull("Newline must be rejected", CompactCreateFileAction.validateFileName("bad\nname"));
    assertNotNull("Tab must be rejected", CompactCreateFileAction.validateFileName("bad\tname"));
  }

  public void testExtractSimpleNameCaseInsensitive() {
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("Token.COMPACT"));
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("Token.Compact"));
  }

  public void testStripCompactExtension() {
    assertEquals("Token", CompactCreateFileAction.stripCompactExtension("Token.compact"));
    assertEquals("Token", CompactCreateFileAction.stripCompactExtension("Token.COMPACT"));
    assertEquals("Token", CompactCreateFileAction.stripCompactExtension("Token.Compact"));
    assertEquals("Other.txt", CompactCreateFileAction.stripCompactExtension("Other.txt"));
  }

  public void testCreateFileEdgeCases() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    assertNull(action.createFile(null, CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir));
    assertNull(action.createFile("   ", CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir));
    assertNull(action.createFile("Token", "NonExistentTemplate", dir));
  }

  public void testActionMetadata() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    String actionName = action.getActionName(dir, "MyToken", CompactFileTemplateGroupFactory.COMPACT_CONTRACT);
    assertTrue(actionName.contains("MyToken"));
    assertEquals(new CompactCreateFileAction(), action);
    assertEquals(new CompactCreateFileAction().hashCode(), action.hashCode());
  }

  public void testDetermineInitialCaretOffsetInModule() {
    PsiFile moduleFile = myFixture.configureByText(
        "Module.compact",
        "pragma language_version >= 0.26.0;\n\nimport CompactStandardLibrary;\n\nexport module Foo {\n  \n}\n"
    );
    int offset = CompactCreateFileAction.determineInitialCaretOffset(moduleFile);
    String text = moduleFile.getText();
    int lbrace = text.indexOf('{');
    int rbrace = text.indexOf('}');
    assertTrue("Caret offset must be after opening brace", offset > lbrace);
    assertTrue("Caret offset must be before closing brace", offset < rbrace);
  }

  public void testDetermineInitialCaretOffsetInInterface() {
    PsiFile ifaceFile = myFixture.configureByText(
        "Interface.compact",
        "pragma language_version >= 0.26.0;\n\nimport CompactStandardLibrary;\n\ncontract Foo {\n  \n}\n"
    );
    int offset = CompactCreateFileAction.determineInitialCaretOffset(ifaceFile);
    String text = ifaceFile.getText();
    int lbrace = text.indexOf('{');
    int rbrace = text.indexOf('}');
    assertTrue("Caret offset must be after opening brace", offset > lbrace);
    assertTrue("Caret offset must be before closing brace", offset < rbrace);
  }

  public void testDetermineInitialCaretOffsetInTopLevelFile() {
    PsiFile file = myFixture.configureByText(
        "Contract.compact",
        "pragma language_version >= 0.26.0;\n\nimport CompactStandardLibrary;\n\n"
    );
    int offset = CompactCreateFileAction.determineInitialCaretOffset(file);
    assertEquals("Caret offset for top-level file should be at end of file", file.getText().length(), offset);
  }

  public void testLastTemplatePropertyMatchesRegisteredKind() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();

    // 1. Contract template persistence
    PsiFile createdContract = action.createFile("Token", CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir);
    assertNotNull(createdContract);
    String savedContract = com.intellij.ide.util.PropertiesComponent.getInstance(getProject())
        .getValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY);
    assertEquals("Saved template property must match registered template kind",
        CompactFileTemplateGroupFactory.COMPACT_CONTRACT, savedContract);

    // 2. Module template persistence
    PsiFile createdModule = action.createFile("Mod", CompactFileTemplateGroupFactory.COMPACT_MODULE, dir);
    assertNotNull(createdModule);
    String savedModule = com.intellij.ide.util.PropertiesComponent.getInstance(getProject())
        .getValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY);
    assertEquals(CompactFileTemplateGroupFactory.COMPACT_MODULE, savedModule);

    // 3. Interface template persistence
    PsiFile createdIface = action.createFile("Iface", CompactFileTemplateGroupFactory.COMPACT_INTERFACE, dir);
    assertNotNull(createdIface);
    String savedIface = com.intellij.ide.util.PropertiesComponent.getInstance(getProject())
        .getValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY);
    assertEquals(CompactFileTemplateGroupFactory.COMPACT_INTERFACE, savedIface);

    // 4. File template persistence
    PsiFile createdFile = action.createFile("Plain", CompactFileTemplateGroupFactory.COMPACT_FILE, dir);
    assertNotNull(createdFile);
    String savedFile = com.intellij.ide.util.PropertiesComponent.getInstance(getProject())
        .getValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY);
    assertEquals(CompactFileTemplateGroupFactory.COMPACT_FILE, savedFile);
  }

  public void testGetDefaultTemplateNameStripsLegacyCompactExtension() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    com.intellij.ide.util.PropertiesComponent props = com.intellij.ide.util.PropertiesComponent.getInstance(getProject());

    props.setValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY, "Compact Contract.compact");
    assertEquals("Legacy .compact suffix must be stripped for dialog selection",
        CompactFileTemplateGroupFactory.COMPACT_CONTRACT, action.getDefaultTemplateName(dir));

    props.setValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY, "Compact Module.compact");
    assertEquals(CompactFileTemplateGroupFactory.COMPACT_MODULE, action.getDefaultTemplateName(dir));

    props.setValue(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY, "Compact Module");
    assertEquals("Exact template name without suffix must be preserved",
        CompactFileTemplateGroupFactory.COMPACT_MODULE, action.getDefaultTemplateName(dir));
  }

  public void testValidateFileCollisionDetectsExistingFile() {
    PsiFile dummy = myFixture.configureByText("Existing.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    // Bare name should be flagged as already existing
    String error = CompactCreateFileAction.validateFileCollision("Existing", dir);
    assertNotNull("Existing file must be detected by collision validator", error);
    assertTrue("Error must mention Existing.compact", error.contains("Existing.compact"));

    // Name with extension should also be detected
    String errorWithExt = CompactCreateFileAction.validateFileCollision("Existing.compact", dir);
    assertNotNull("Existing file with extension must be detected", errorWithExt);

    // Non-existing file should be accepted
    assertNull("Non-existing file must pass collision check",
        CompactCreateFileAction.validateFileCollision("BrandNewFile", dir));

    // Nested path collision in existing subdirectory
    PsiDirectory subDir = com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
        getProject(),
        (com.intellij.openapi.util.ThrowableComputable<PsiDirectory, RuntimeException>) () -> dir.createSubdirectory("sub")
    );
    assertNotNull(subDir);
    myFixture.addFileToProject("sub/Nested.compact", "pragma language_version >= 0.26.0;");

    String errorNested = CompactCreateFileAction.validateFileCollision("sub/Nested", dir);
    assertNotNull("Nested existing file must be detected", errorNested);
    assertTrue(errorNested.contains("Nested.compact"));

    // Nested non-existing file in existing subdirectory
    assertNull(CompactCreateFileAction.validateFileCollision("sub/Other", dir));

    // Nested non-existing subdirectory
    assertNull(CompactCreateFileAction.validateFileCollision("newsub/Other", dir));
  }

  public void testCreateFileThrowsIncorrectOperationExceptionWhenFileExists() {
    PsiFile dummy = myFixture.configureByText("AlreadyThere.compact", "pragma language_version >= 0.26.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    try {
      action.createFile("AlreadyThere", CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir);
      fail("Expected IncorrectOperationException when creating file that already exists");
    } catch (com.intellij.util.IncorrectOperationException e) {
      assertTrue("Exception message should mention existing file", e.getMessage().contains("already exists"));
    }
  }
}
