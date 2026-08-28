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
                    import { MAX as LIMIT } from U<caret>tils;
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

  public void testGenericParameterReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    type Container<#N> = Vector<#<caret>N, Field>;
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);
    assertEquals("N", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertInstanceOf(target, dev.verloren.midnight.psi.CompactGenericParameterImpl.class);
    assertEquals("N", ((CompactNamedElement) target).getName());
  }

  public void testStructFieldReferenceRangeAndTarget() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point { x: Field, y: Field }
                    circuit draw(p: Point) {
                      const val = p.<caret>x;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Struct field reference should exist at caret", ref);
    assertInstanceOf(ref, CompactStructFieldReference.class);
    assertEquals("x", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull("Struct field reference should resolve to field declaration", target);
    assertEquals("x", ((CompactNamedElement) target).getName());
  }

  public void testStructFieldReferenceFromLedgerDeclaration() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Node { right: Field }
                    ledger root: Node;
                    circuit check() {
                      const r = root.<caret>right;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Struct field reference should exist on ledger base", ref);
    assertInstanceOf(ref, CompactStructFieldReference.class);
    assertEquals("right", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull("Struct field reference should resolve to right field", target);
    assertEquals("right", ((CompactNamedElement) target).getName());
  }

  public void testStructFieldReferenceFromTypeAlias() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Node { right: Field }
                    type AliasNode = Node;
                    circuit check(n: AliasNode) {
                      const r = n.<caret>right;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Struct field reference should exist on type alias base", ref);
    assertInstanceOf(ref, CompactStructFieldReference.class);
    assertEquals("right", ref.getCanonicalText());

    PsiElement target = ref.resolve();
    assertNotNull("Struct field reference should resolve through type alias", target);
    assertEquals("right", ((CompactNamedElement) target).getName());
  }

  public void testStructFieldReferenceIdempotent() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Node { right: Field }
                    circuit test(node: Node) {
                      const a = node.<caret>right;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactStructFieldReference.class);
    CompactStructFieldReference structRef = (CompactStructFieldReference) ref;

    // Verify multiple sequential calls return identical results (idempotence)
    var results1 = structRef.multiResolve(false);
    var results2 = structRef.multiResolve(false);
    assertEquals(1, results1.length);
    assertEquals(1, results2.length);
    assertEquals(results1[0].getElement(), results2[0].getElement());
  }
}
