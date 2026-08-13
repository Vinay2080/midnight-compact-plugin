package dev.verloren.midnight.findUsages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;

import java.util.Collection;

public class CompactFindUsagesTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactParserDefinition()
    );
    com.intellij.lang.findUsages.LanguageFindUsages.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactFindUsagesProvider()
    );
  }

  private Collection<UsageInfo> findUsagesAtCaret() {
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull("Caret element parent should be CompactNamedElement", namedElement);
    return myFixture.findUsages(namedElement);
  }

  public void testFindUsagesForCircuit() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit m<caret>int() {}
                    circuit caller() {
                      mint();
                      mint();
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesForParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit mint(a<caret>mount: Uint<32>) {
                      const x = amount;
                      const y = amount;
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesForStruct() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct P<caret>oint { x: Field }
                    circuit draw(p: Point) {
                      const p2 = Point { x: 0 };
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesIgnoresCommentsAndStrings() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    // mint is a great circuit
                    const str = "mint";
                    circuit m<caret>int() {}
                    circuit caller() {
                      mint();
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals("Should find exactly 1 code usage reference, ignoring comment and string literal", 1, usages.size());
  }

  public void testFindUsagesProviderElementTypes() {
    CompactFindUsagesProvider provider = new CompactFindUsagesProvider();

    myFixture.configureByText(CompactFileType.INSTANCE,
            "struct <caret>Point { x: Field }\n"
    );
    PsiElement element = myFixture.getElementAtCaret();
    assertTrue(provider.canFindUsagesFor(element));
    assertEquals("Point", provider.getDescriptiveName(element));
    assertEquals("struct", provider.getType(element));
  }
}
