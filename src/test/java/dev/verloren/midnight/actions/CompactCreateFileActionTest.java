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
}
