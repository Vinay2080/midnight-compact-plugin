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

  private static <T extends CompactNamedElement> void assertNamed(T element, String name, Class<?> implementationClass) {
    assertNotNull(element);
    assertInstanceOf(element, implementationClass);
    assertEquals(name, element.getName());
    assertNotNull(element.getNameIdentifier());
    assertEquals(name, element.getNameIdentifier().getText());
  }
}