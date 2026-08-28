package dev.verloren.midnight.editor;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;

public class CompactLineMarkerTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testWitnessLineMarker() {
    String code = "witness fetchSecretKey(): Bytes<32>;";
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactWitnessDeclaration witness = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactWitnessDeclaration.class);
    assertNotNull(witness);
    PsiElement name = witness.getNameIdentifier();
    assertNotNull(name);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();
    LineMarkerInfo<?> info = provider.getLineMarkerInfo(name);

    assertNotNull("Witness should have line marker", info);
    assertTrue(info.getLineMarkerTooltip().contains("witness"));
  }

  public void testExportedCircuitLineMarker() {
    String code = "export circuit mintToken(): Void {}";
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactCircuitDefinition circuit = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactCircuitDefinition.class);
    assertNotNull(circuit);
    PsiElement name = circuit.getNameIdentifier();
    assertNotNull(name);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();
    LineMarkerInfo<?> info = provider.getLineMarkerInfo(name);

    assertNotNull("Exported circuit should have line marker", info);
    assertTrue(info.getLineMarkerTooltip().contains("circuit"));
  }

  public void testLedgerStateLineMarker() {
    String code = "sealed ledger balance: Uint<64>;";
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactLedgerDeclaration ledger = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactLedgerDeclaration.class);
    assertNotNull(ledger);
    PsiElement name = ledger.getNameIdentifier();
    assertNotNull(name);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();
    LineMarkerInfo<?> info = provider.getLineMarkerInfo(name);

    assertNotNull("Ledger field should have line marker", info);
    assertTrue(info.getLineMarkerTooltip().contains("ledger"));
  }

  public void testDiscloseLineMarker() {
    String code = """
        witness query(): Field;
        circuit test(): [] {
            const v = disclose(query());
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    PsiElement discloseToken = null;
    for (PsiElement child : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), PsiElement.class)) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactTokenTypes.DISCLOSE) {
        discloseToken = child;
        break;
      }
    }
    assertNotNull("Disclose keyword token should exist", discloseToken);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();
    LineMarkerInfo<?> info = provider.getLineMarkerInfo(discloseToken);

    assertNotNull("Disclose token should have line marker", info);
    assertTrue(info.getLineMarkerTooltip().contains("Zero-Knowledge"));
  }
}
