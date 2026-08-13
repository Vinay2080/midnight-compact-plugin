package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;

import java.util.Collection;

public class CompactCompletionTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactParserDefinition()
    );
  }

  public void testKeywordContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE, "<caret>");
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    pos = pos == null ? myFixture.getFile() : pos;
    assertEquals(CompactCompletionContext.Kind.KEYWORD, CompactCompletionContext.classify(pos));
  }

  public void testTypeContextClassificationAndCollection() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct CustomType {}
                    circuit mint(amount: <caret>) {}
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.TYPE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> typeDecls = CompactResolveUtil.collectTypeDeclarations(pos);
    Collection<String> names = typeDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Should collect 'CustomType'", names.contains("CustomType"));
  }

  public void testValueContextClassificationAndCollection() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const myVar = 42;
                      const copy = <caret>;
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.VALUE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> valueDecls = CompactResolveUtil.collectValueDeclarations(pos);
    Collection<String> names = valueDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Should collect 'myVar'", names.contains("myVar"));
  }

  public void testMemberContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Status { Active, Suspended, Closed }
                    circuit check() {
                      const s = Status.<caret>;
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.MEMBER, CompactCompletionContext.classify(pos));
  }
}
