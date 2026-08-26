package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;

public class CompactLiveTemplateTest extends BasePlatformTestCase {

  public void testLiveTemplateResourceExists() {
    assertNotNull("liveTemplates/Compact.xml should exist in resources",
            getClass().getClassLoader().getResource("liveTemplates/Compact.xml"));
  }

  public void testLiveTemplateContextInCompactFile() {
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, "pragma language_version >= 0.20.0;\n");
    CompactLiveTemplateContextType contextType = new CompactLiveTemplateContextType();

    TemplateActionContext actionContext = TemplateActionContext.create(file, myFixture.getEditor(), 0, 0, false);
    assertTrue("Should be in context for Compact files", contextType.isInContext(actionContext));
  }

  public void testLiveTemplateContextNotInOtherFiles() {
    PsiFile txtFile = myFixture.configureByText("test.txt", "Some plain text");
    CompactLiveTemplateContextType contextType = new CompactLiveTemplateContextType();

    TemplateActionContext actionContext = TemplateActionContext.create(txtFile, myFixture.getEditor(), 0, 0, false);
    assertFalse("Should not be in context for non-Compact files", contextType.isInContext(actionContext));
  }
}
