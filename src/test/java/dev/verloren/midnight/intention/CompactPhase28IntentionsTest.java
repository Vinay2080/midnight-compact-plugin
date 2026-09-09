package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class CompactPhase28IntentionsTest extends BasePlatformTestCase {

  public void testTogglePureCircuitToPure() {
    myFixture.configureByText("test.compact", "cir<caret>cuit transfer(): Void {}");
    IntentionAction intention = myFixture.findSingleIntention("Make circuit pure");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult("pure circuit transfer(): Void {}");
  }

  public void testTogglePureCircuitToNonPure() {
    myFixture.configureByText("test.compact", "pu<caret>re circuit transfer(): Void {}");
    IntentionAction intention = myFixture.findSingleIntention("Make circuit non-pure");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult("circuit transfer(): Void {}");
  }

  public void testTogglePureCircuitFromParenOnHeaderLine() {
    myFixture.configureByText("test.compact", "circuit transfer()<caret>: Void {}");
    IntentionAction intention = myFixture.findSingleIntention("Make circuit pure");
    assertNotNull("Should be available anywhere on header line", intention);
  }

  public void testToggleExportOnContract() {
    myFixture.configureByText("test.compact", "con<caret>tract Token {}");
    IntentionAction intention = myFixture.findSingleIntention("Add 'export' modifier");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult("export contract Token {}");

    IntentionAction removeIntention = myFixture.findSingleIntention("Remove 'export' modifier");
    assertNotNull(removeIntention);
    myFixture.launchAction(removeIntention);
    myFixture.checkResult("contract Token {}");
  }

  public void testToggleExportOnCircuit() {
    myFixture.configureByText("test.compact", "cir<caret>cuit mint(): Void {}");
    IntentionAction intention = myFixture.findSingleIntention("Add 'export' modifier");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult("export circuit mint(): Void {}");

    // And toggle back
    IntentionAction removeIntention = myFixture.findSingleIntention("Remove 'export' modifier");
    assertNotNull(removeIntention);
    myFixture.launchAction(removeIntention);
    myFixture.checkResult("circuit mint(): Void {}");
  }

  public void testSurroundWithDisclose() {
    myFixture.configureByText("test.compact",
        "circuit verify(): Void {\n" +
        "  const x = sec<caret>retKey;\n" +
        "}"
    );
    IntentionAction intention = myFixture.findSingleIntention("Surround with disclose(...)");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult(
        "circuit verify(): Void {\n" +
        "  const x = disclose(secretKey);\n" +
        "}"
    );
  }

  public void testGeneralAndSpecificSuggestionsCoexistOnLine() {
    myFixture.configureByText("test.compact",
        "circuit verify(): Void {\n" +
        "  const x = 1<caret>00;\n" +
        "}"
    );
    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    boolean hasDisclose = intentions.stream().anyMatch(it -> it.getText().equals("Surround with disclose(...)"));
    boolean hasSpecifyType = intentions.stream().anyMatch(it -> it.getText().startsWith("Specify type explicitly"));
    assertTrue("Should provide specific suggestion (disclose) for word under caret", hasDisclose);
    assertTrue("Should provide general line suggestion (specify type) for the const statement", hasSpecifyType);
  }

  public void testInvertIf() {
    myFixture.configureByText("test.compact",
        "circuit check(a: Field, b: Field): Void {\n" +
        "  i<caret>f (a == b) {\n" +
        "    return 1;\n" +
        "  } else {\n" +
        "    return 2;\n" +
        "  }\n" +
        "}"
    );
    IntentionAction intention = myFixture.findSingleIntention("Invert 'if' condition");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResult(
        "circuit check(a: Field, b: Field): Void {\n" +
        "  if (a != b) {\n" +
        "    return 2;\n" +
        "  } else {\n" +
        "    return 1;\n" +
        "  }\n" +
        "}"
    );
  }

  public void testInvertIfFromConditionExpression() {
    myFixture.configureByText("test.compact",
        "circuit check(a: Field, b: Field): Void {\n" +
        "  if (a ==<caret> b) {\n" +
        "    return 1;\n" +
        "  } else {\n" +
        "    return 2;\n" +
        "  }\n" +
        "}"
    );
    IntentionAction intention = myFixture.findSingleIntention("Invert 'if' condition");
    assertNotNull("Should be available anywhere on if line", intention);
  }

  public void testSpecifyTypeExplicitlyFromConstKeyword() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  co<caret>nst amount = 100;\n" +
        "}"
    );
    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    IntentionAction intention = intentions.stream()
        .filter(it -> it.getText().startsWith("Specify type explicitly"))
        .findFirst()
        .orElse(null);
    assertNotNull("Should find specify type explicitly intention from 'const' keyword", intention);
    myFixture.launchAction(intention);
    assertEquals("circuit test(): Void {\n  const amount: Field = 100;\n}", myFixture.getEditor().getDocument().getText());
  }

  public void testRemoveRedundantTypeFromConstKeyword() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  co<caret>nst amount: Uint<64> = 100;\n" +
        "}"
    );
    IntentionAction intention = myFixture.findSingleIntention("Remove type annotation");
    assertNotNull("Should find remove type annotation from 'const' keyword on the line", intention);
    myFixture.launchAction(intention);
    myFixture.checkResult(
        "circuit test(): Void {\n" +
        "  const amount = 100;\n" +
        "}"
    );
  }
}
