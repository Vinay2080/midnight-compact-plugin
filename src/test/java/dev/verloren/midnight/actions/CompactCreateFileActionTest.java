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
}
