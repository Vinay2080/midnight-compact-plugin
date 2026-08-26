package dev.verloren.midnight.editor;

import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.psi.*;

public class CompactBreadcrumbsTest extends BasePlatformTestCase {

  public void testBreadcrumbsLanguages() {
    CompactBreadcrumbsProvider provider = new CompactBreadcrumbsProvider();
    Language[] languages = provider.getLanguages();
    assertEquals(1, languages.length);
    assertEquals(CompactLanguage.INSTANCE, languages[0]);
  }

  public void testBreadcrumbsElementInfo() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export contract GameContract {
              circuit play(): Void;
            }
            
            ledger {
              score: Field;
            }
            
            constructor() {
            }
            
            export circuit play(): Void {
              if (true) {
                return;
              }
            }
            
            struct Hero {
              hp: Field;
            }
            
            enum Status {
              ALIVE,
              DEAD
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactBreadcrumbsProvider provider = new CompactBreadcrumbsProvider();

    CompactExternalContractDeclaration contract = PsiTreeUtil.findChildOfType(file, CompactExternalContractDeclaration.class);
    assertNotNull(contract);
    assertTrue(provider.acceptElement(contract));
    assertEquals("contract GameContract", provider.getElementInfo(contract));
    assertEquals("contract GameContract", provider.getElementTooltip(contract));

    CompactLedgerDeclaration ledger = PsiTreeUtil.findChildOfType(file, CompactLedgerDeclaration.class);
    assertNotNull(ledger);
    assertTrue(provider.acceptElement(ledger));
    assertEquals("ledger", provider.getElementInfo(ledger));

    CompactConstructorDeclaration constructor = PsiTreeUtil.findChildOfType(file, CompactConstructorDeclaration.class);
    assertNotNull(constructor);
    assertTrue(provider.acceptElement(constructor));
    assertEquals("constructor", provider.getElementInfo(constructor));

    CompactCircuitDefinition circuit = PsiTreeUtil.findChildOfType(file, CompactCircuitDefinition.class);
    assertNotNull(circuit);
    assertTrue(provider.acceptElement(circuit));
    assertEquals("circuit play", provider.getElementInfo(circuit));

    CompactStructDefinition structDef = PsiTreeUtil.findChildOfType(file, CompactStructDefinition.class);
    assertNotNull(structDef);
    assertTrue(provider.acceptElement(structDef));
    assertEquals("struct Hero", provider.getElementInfo(structDef));

    CompactEnumDefinition enumDef = PsiTreeUtil.findChildOfType(file, CompactEnumDefinition.class);
    assertNotNull(enumDef);
    assertTrue(provider.acceptElement(enumDef));
    assertEquals("enum Status", provider.getElementInfo(enumDef));
  }
}
