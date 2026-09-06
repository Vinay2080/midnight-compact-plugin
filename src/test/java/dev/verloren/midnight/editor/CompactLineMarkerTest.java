package dev.verloren.midnight.editor;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.icons.AllIcons;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.*;

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

  public void testContractImplementsAndInterfaceLineMarkers() {
    String code = """
        contract ExternalToken {
          circuit transfer(): [];
        }
        contract implements ExternalToken;
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();

    // 1. Check 'implements' keyword line marker pointing up to interface
    PsiElement implementsToken = null;
    for (PsiElement child : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), PsiElement.class)) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactTokenTypes.IMPLEMENTS) {
        implementsToken = child;
        break;
      }
    }
    assertNotNull("implements token should exist", implementsToken);
    LineMarkerInfo<?> implementsMarker = provider.getLineMarkerInfo(implementsToken);
    assertNotNull("Implements keyword should have navigation line marker", implementsMarker);
    assertEquals(AllIcons.Gutter.ImplementingMethod, implementsMarker.getIcon());
    assertTrue(implementsMarker.getLineMarkerTooltip().contains("ExternalToken"));

    // 2. Check interface declaration identifier pointing down to implementation
    CompactExternalContractDeclaration iface = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactExternalContractDeclaration.class);
    assertNotNull(iface);
    assertNotNull(iface.getNameIdentifier());
    LineMarkerInfo<?> ifaceMarker = provider.getLineMarkerInfo(iface.getNameIdentifier());
    assertNotNull("Interface declaration should have line marker pointing down to implementations", ifaceMarker);
    assertEquals(AllIcons.Gutter.ImplementedMethod, ifaceMarker.getIcon());
    assertTrue(ifaceMarker.getLineMarkerTooltip().contains("implemented by"));
  }

  public void testUnimplementedContractInterfaceHasNoLineMarker() {
    String code = "contract UnimplementedToken {}";
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactExternalContractDeclaration iface = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactExternalContractDeclaration.class);
    assertNotNull(iface);
    assertNotNull(iface.getNameIdentifier());

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();
    LineMarkerInfo<?> ifaceMarker = provider.getLineMarkerInfo(iface.getNameIdentifier());
    assertNull("Unimplemented interface should not show implementation marker", ifaceMarker);
  }

  public void testCircuitInterfaceAndImplementationLineMarkers() {
    String code = """
        contract ExternalToken {
          circuit transfer(): [];
        }
        contract implements ExternalToken;
        export circuit transfer(): [] {}
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactLineMarkerProvider provider = new CompactLineMarkerProvider();

    // 1. Interface circuit should point down to implementation circuit
    CompactExternalContractDeclaration iface = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactExternalContractDeclaration.class);
    assertNotNull(iface);
    assertEquals(1, iface.getCircuits().size());
    CompactExternalCircuit ifaceCircuit = iface.getCircuits().getFirst();
    assertNotNull(ifaceCircuit.getNameIdentifier());

    LineMarkerInfo<?> ifaceCircuitMarker = provider.getLineMarkerInfo(ifaceCircuit.getNameIdentifier());
    assertNotNull("Interface circuit should have marker pointing down to concrete circuit", ifaceCircuitMarker);
    assertEquals(AllIcons.Gutter.ImplementedMethod, ifaceCircuitMarker.getIcon());
    assertTrue(ifaceCircuitMarker.getLineMarkerTooltip().contains("transfer"));

    // 2. Concrete circuit should point up to interface circuit
    CompactCircuitDefinition concreteCircuit = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactCircuitDefinition.class);
    assertNotNull(concreteCircuit);
    assertNotNull(concreteCircuit.getNameIdentifier());

    LineMarkerInfo<?> concreteCircuitMarker = provider.getLineMarkerInfo(concreteCircuit.getNameIdentifier());
    assertNotNull("Concrete circuit should have marker pointing up to interface", concreteCircuitMarker);
    assertEquals(AllIcons.Gutter.ImplementingMethod, concreteCircuitMarker.getIcon());
    assertTrue(concreteCircuitMarker.getLineMarkerTooltip().contains("ExternalToken"));
  }
}
