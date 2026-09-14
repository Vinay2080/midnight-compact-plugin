package dev.verloren.midnight.ide.templates;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;

public class CompactDeclarationTemplateTriggerTest extends BasePlatformTestCase {

  public void testBareCircuitPrefixExpansionOnEnter() {
    myFixture.configureByText(CompactFileType.INSTANCE, "cir<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'cir' to circuit template, but got: " + text,
        text.contains("circuit circuit1") && text.contains("Void"));
  }

  public void testCiPrefixExpansionOnEnter() {
    myFixture.configureByText(CompactFileType.INSTANCE, "ci<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'ci' to circuit template, but got: " + text,
        text.contains("circuit circuit1") && text.contains("Void"));
  }

  public void testFullCircuitKeywordExpansionOnEnter() {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'circuit' to circuit template, but got: " + text,
        text.contains("circuit circuit1") && text.contains("Void"));
  }

  public void testWitnessPrefixesExpansionOnEnter() {
    // wi
    myFixture.configureByText(CompactFileType.INSTANCE, "wi<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'wi' to witness template, but got: " + text,
        text.contains("witness witness1") && text.contains("Field;"));

    // wit
    myFixture.configureByText(CompactFileType.INSTANCE, "wit<caret>");
    myFixture.type('\n');
    text = myFixture.getFile().getText();
    assertTrue("Should expand 'wit' to witness template, but got: " + text,
        text.contains("witness witness1") && text.contains("Field;"));

    // witness
    myFixture.configureByText(CompactFileType.INSTANCE, "witness<caret>");
    myFixture.type('\n');
    text = myFixture.getFile().getText();
    assertTrue("Should expand 'witness' to witness template, but got: " + text,
        text.contains("witness witness1") && text.contains("Field;"));
  }

  public void testCompactModifierTriggersExpansionOnEnter() {
    // exci
    myFixture.configureByText(CompactFileType.INSTANCE, "exci<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'exci' to export circuit template, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));

    // excir
    myFixture.configureByText(CompactFileType.INSTANCE, "excir<caret>");
    myFixture.type('\n');
    text = myFixture.getFile().getText();
    assertTrue("Should expand 'excir' to export circuit template, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));

    // exportci
    myFixture.configureByText(CompactFileType.INSTANCE, "exportci<caret>");
    myFixture.type('\n');
    text = myFixture.getFile().getText();
    assertTrue("Should expand 'exportci' to export circuit template, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));

    // exportcir
    myFixture.configureByText(CompactFileType.INSTANCE, "exportcir<caret>");
    myFixture.type('\n');
    text = myFixture.getFile().getText();
    assertTrue("Should expand 'exportcir' to export circuit template, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));
  }

  public void testSeparatedModifierTriggersExpansionOnEnter() {
    // export circ
    myFixture.configureByText(CompactFileType.INSTANCE, "export circ<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand 'export circ' to export circuit template, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));
  }

  public void testInvalidExportConstDoesNotExpand() {
    myFixture.configureByText(CompactFileType.INSTANCE, "exconst<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertFalse("Should NOT expand invalid 'exconst'", text.contains("exconst1") || text.contains("const1"));
  }

  public void testTriggerInsideCommentDoesNotExpand() {
    myFixture.configureByText(CompactFileType.INSTANCE, "// excir<caret>");
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertFalse("Should NOT expand trigger inside comments", text.contains("export circuit circuit1"));
  }

  public void testCompletionPopupAcceptanceForCompactTrigger() {
    myFixture.configureByText(CompactFileType.INSTANCE, "exci<caret>");
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand via completion popup, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));
  }

  public void testCompletionPopupAcceptanceForSeparatedTrigger() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export circ<caret>");
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("Should expand via completion popup for separated trigger, but got: " + text,
        text.contains("export circuit circuit1") && text.contains("Void"));
  }
}
