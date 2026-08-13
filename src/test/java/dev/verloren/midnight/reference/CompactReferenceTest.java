package dev.verloren.midnight.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactStructDefinition;

public class CompactReferenceTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
      dev.verloren.midnight.CompactLanguage.INSTANCE,
      new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testValueReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit calc() {
                      const a = 5;
                      const b = <caret>a;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactValueReference.class);
    assertEquals("a", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertEquals("a", ((CompactNamedElement) target).getName());
  }

  public void testTypeReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Data { val: Field }
                    circuit process(d: <caret>Data) {}
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);
    assertEquals("Data", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertInstanceOf(target, CompactStructDefinition.class);
    assertEquals("Data", ((CompactNamedElement) target).getName());
  }

  public void testEnumMemberReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Color { Red, Green, Blue }
                    circuit test() {
                      const c = Color.<caret>Green;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactEnumMemberReference.class);
    assertEquals("Green", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertEquals("Green", ((CompactNamedElement) target).getName());
  }

  public void testImportReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    module Utils {
                      export const MAX = 100;
                    }
                    import { MAX as LIMIT } from U<caret>utils;
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertTrue(ref instanceof CompactImportReference);
    CompactImportReference importRef = (CompactImportReference) ref;

    PsiElement target = importRef.resolve();
    assertNotNull("Module import should resolve to module definition", target);
    assertTrue(target instanceof dev.verloren.midnight.psi.CompactModuleDefinition);
    assertEquals("Utils", ((dev.verloren.midnight.psi.CompactModuleDefinition) target).getName());
  }
}
