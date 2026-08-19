package dev.verloren.midnight.psi;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.parser.DeclarationParserTest;

public class DeclarationPsiTest extends ParsingTestCase {
  public DeclarationPsiTest() {
    super("declarations", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testDeclarationPsiWrappersAndNames() {
    PsiFile file = parseFile("DeclarationPsi", DeclarationParserTest.declarationFixture());

    assertInstanceOf(PsiTreeUtil.findChildOfType(file, CompactIncludeDeclaration.class), CompactIncludeDeclarationImpl.class);
    assertInstanceOf(PsiTreeUtil.findChildOfType(file, CompactImportDeclaration.class), CompactImportDeclarationImpl.class);
    assertInstanceOf(PsiTreeUtil.findChildOfType(file, CompactExportDeclaration.class), CompactExportDeclarationImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactModuleDefinition.class), "Math", CompactModuleDefinitionImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactStructDefinition.class), "Pair", CompactStructDefinitionImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactEnumDefinition.class), "Choice", CompactEnumDefinitionImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactExternalContractDeclaration.class), "Token", CompactExternalContractDeclarationImpl.class);
    assertInstanceOf(PsiTreeUtil.findChildOfType(file, CompactContractImplementsDeclaration.class), CompactContractImplementsDeclarationImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactTypeDefinition.class), "Amount", CompactTypeDefinitionImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactLedgerDeclaration.class), "inner", CompactLedgerDeclarationImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactWitnessDeclaration.class), "secret", CompactWitnessDeclarationImpl.class);
    assertInstanceOf(PsiTreeUtil.findChildOfType(file, CompactConstructorDeclaration.class), CompactConstructorDeclarationImpl.class);
    assertNamed(PsiTreeUtil.findChildOfType(file, CompactCircuitDefinition.class), "mint", CompactCircuitDefinitionImpl.class);
  }

  public void testTypedPsiAccessors() {
    PsiFile file = parseFile("TypedAccessors", """
        struct Point {
          x: Field,
          y: Field
        }
        enum State {
          Open,
          Closed
        }
        type Balance = Uint<64>;
        ledger counter: Field;
        witness oracle(id: Field): Boolean;
        constructor(init: Field) {
        }
        circuit add(a: Field, b: Field): Field {
          return a + b;
        }
        """);

    CompactStructDefinition structDef = PsiTreeUtil.findChildOfType(file, CompactStructDefinition.class);
    assertNotNull(structDef);
    assertEquals("Point", structDef.getName());
    assertEquals(2, structDef.getFields().size());

    CompactEnumDefinition enumDef = PsiTreeUtil.findChildOfType(file, CompactEnumDefinition.class);
    assertNotNull(enumDef);
    assertEquals("State", enumDef.getName());
    assertEquals(2, enumDef.getMembers().size());

    CompactTypeDefinition typeDef = PsiTreeUtil.findChildOfType(file, CompactTypeDefinition.class);
    assertNotNull(typeDef);
    assertEquals("Balance", typeDef.getName());
    assertNotNull(typeDef.getTargetTypeElement());

    CompactLedgerDeclaration ledgerDecl = PsiTreeUtil.findChildOfType(file, CompactLedgerDeclaration.class);
    assertNotNull(ledgerDecl);
    assertEquals("counter", ledgerDecl.getName());
    assertNotNull(ledgerDecl.getTypeElement());

    CompactWitnessDeclaration witnessDecl = PsiTreeUtil.findChildOfType(file, CompactWitnessDeclaration.class);
    assertNotNull(witnessDecl);
    assertEquals("oracle", witnessDecl.getName());
    assertEquals(1, witnessDecl.getParameters().size());
    assertNotNull(witnessDecl.getReturnTypeElement());

    CompactConstructorDeclaration constructorDecl = PsiTreeUtil.findChildOfType(file, CompactConstructorDeclaration.class);
    assertNotNull(constructorDecl);
    assertEquals(1, constructorDecl.getParameters().size());
    assertNotNull(constructorDecl.getBody());

    CompactCircuitDefinition circuitDef = PsiTreeUtil.findChildOfType(file, CompactCircuitDefinition.class);
    assertNotNull(circuitDef);
    assertEquals("add", circuitDef.getName());
    assertEquals(2, circuitDef.getParameters().size());
    assertNotNull(circuitDef.getBody());
    assertNotNull(circuitDef.getReturnTypeElement());
  }

  private static <T extends CompactNamedElement> void assertNamed(T element, String name, Class<?> implementationClass) {
    assertNotNull(element);
    assertInstanceOf(element, implementationClass);
    assertEquals(name, element.getName());
    assertNotNull(element.getNameIdentifier());
    assertEquals(name, element.getNameIdentifier().getText());
  }
}