package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
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

  public void testLedLiveTemplateFormat() {
    TemplateImpl template = TemplateSettings.getInstance().getTemplate("led", "Compact");
    assertNotNull("Live template 'led' should exist", template);
    assertEquals("export ledger $NAME$: $TYPE$;", template.getString());
  }

  public void testLedgLiveTemplateFormat() {
    TemplateImpl template = TemplateSettings.getInstance().getTemplate("ledg", "Compact");
    assertNotNull("Live template 'ledg' should exist", template);
    assertEquals("export ledger $NAME$: $TYPE$;", template.getString());
  }

  public void testLedgerLiveTemplateFormat() {
    TemplateImpl template = TemplateSettings.getInstance().getTemplate("ledger", "Compact");
    assertNotNull("Live template 'ledger' should exist", template);
    assertEquals("export ledger $NAME$: $TYPE$;", template.getString());
  }

  public void testCctLiveTemplateContainsExportLedger() {
    TemplateImpl template = TemplateSettings.getInstance().getTemplate("cct", "Compact");
    assertNotNull("Live template 'cct' should exist", template);
    assertTrue("Contract template 'cct' should declare 'export ledger'",
        template.getString().contains("export ledger $STATE$: $TYPE$;"));
  }

  public void testCirLiveTemplateExpansionFirstDeclaration() {
    myFixture.configureByText(CompactFileType.INSTANCE, "<caret>");
    myFixture.type("cir\t");
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should contain 'circuit1' but was: " + text, text.contains("circuit1"));
  }
}
