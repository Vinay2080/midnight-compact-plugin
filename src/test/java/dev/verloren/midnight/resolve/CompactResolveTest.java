package dev.verloren.midnight.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactTypeReference;
import dev.verloren.midnight.reference.CompactValueReference;

public class CompactResolveTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            dev.verloren.midnight.CompactLanguage.INSTANCE,
            new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testLocalConstResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const x = 10;
                      const y = <caret>x;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);
    assertInstanceOf(ref, CompactValueReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull("Local const 'x' should resolve", resolved);
    assertTrue(resolved instanceof CompactNamedElement);
    assertEquals("x", ((CompactNamedElement) resolved).getName());
  }

  public void testParameterResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit mint(amount: Uint<32>) {
                      const total = <caret>amount;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);
    assertInstanceOf(ref, CompactValueReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull("Parameter 'amount' should resolve", resolved);
    assertTrue(resolved instanceof CompactNamedElement);
    assertEquals("amount", ((CompactNamedElement) resolved).getName());
  }

  public void testBlockScopeAndShadowing() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit shadowTest() {
                      const val = 1;
                      {
                        const val = 2;
                        const check = <caret>val;
                      }
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);

    PsiElement resolved = ref.resolve();
    assertNotNull("Shadowed 'val' should resolve", resolved);
    assertTrue(resolved instanceof CompactNamedElement);
    assertEquals("val", ((CompactNamedElement) resolved).getName());
    assertTrue("Resolved element should be the inner scope binding", resolved.getTextRange().getStartOffset() > 35);
  }

  public void testDisjointTypeAndValueNamespaces() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Item { id: Field }
                    const Item = 42;
                    circuit process(x: <caret>Item) {
                      const v = Item;
                    }
                    """
    );
    PsiReference typeRef = myFixture.getReferenceAtCaretPosition();
    assertNotNull(typeRef);
    assertInstanceOf(typeRef, CompactTypeReference.class);

    PsiElement resolvedType = typeRef.resolve();
    assertNotNull("Type reference to 'Item' should resolve to struct", resolvedType);
    assertInstanceOf(resolvedType, CompactStructDefinition.class);
    assertEquals("Item", ((CompactNamedElement) resolvedType).getName());

    // Now check the value reference
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Item { id: Field }
                    const Item = 42;
                    circuit process() {
                      const v = <caret>Item;
                    }
                    """
    );
    PsiReference valRef = myFixture.getReferenceAtCaretPosition();
    assertNotNull(valRef);
    assertInstanceOf(valRef, CompactValueReference.class);

    PsiElement resolvedVal = valRef.resolve();
    assertNotNull("Value reference to 'Item' should resolve to const", resolvedVal);
    assertInstanceOf(resolvedVal, CompactPatternImpl.class);
    assertEquals("Item", ((CompactNamedElement) resolvedVal).getName());
  }

  public void testTypeAliasResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    type Amount = Uint<32>;
                    circuit transfer(a: <caret>Amount) {}
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactTypeDefinition.class);
    assertEquals("Amount", ((CompactNamedElement) resolved).getName());
  }

  public void testChainedTypeAliasResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    type Base = Field;
                    type Alias = <caret>Base;
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactTypeDefinition.class);
    assertEquals("Base", ((CompactNamedElement) resolved).getName());
  }

  public void testEnumDefinitionResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Choice { OptionA, OptionB }
                    circuit pick(c: <caret>Choice) {}
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactEnumDefinition.class);
    assertEquals("Choice", ((CompactNamedElement) resolved).getName());
  }

  public void testExternalContractResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    contract ExternalToken {}
                    contract implements <caret>ExternalToken;
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactExternalContractDeclaration.class);
    assertEquals("ExternalToken", ((CompactNamedElement) resolved).getName());
  }

  public void testGenericParameterResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            "type Container<#N> = Vector<#<caret>N, Field>;\n"
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactGenericParameterImpl.class);
    assertEquals("N", ((CompactNamedElement) resolved).getName());
  }

  public void testSoftUnresolvedBuiltins() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            "circuit test(b: <caret>UnknownType) {}\n"
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Type reference object should exist even for unresolved custom type", ref);
    PsiElement resolved = ref.resolve();
    assertNull("Unresolved custom type is soft-unresolved (returns null)", resolved);
  }

  public void testEnumMemberAccessResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Status { Active, Inactive }
                    circuit check() {
                      const s = Status.<caret>Active;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactEnumMemberReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull("Enum member 'Active' should resolve", resolved);
    assertInstanceOf(resolved, CompactEnumMemberImpl.class);
    assertEquals("Active", ((CompactNamedElement) resolved).getName());
  }

  public void testPrefixedModuleImportResolution() {






    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    module M {
                      export circuit helper() {}
                    }
                    import M prefix $;
                    circuit main() {
                      <caret>$helper();
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);

    PsiElement resolved = ref.resolve();
    assertNotNull("Prefixed import '$helper' should resolve to M.helper", resolved);
    assertInstanceOf(resolved, CompactCircuitDefinition.class);
    assertEquals("helper", ((CompactNamedElement) resolved).getName());
  }

  public void testSelectedModuleImportResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    module Utils {
                      export const MAX = 100;
                    }
                    import { MAX as LIMIT } from Utils;
                    circuit check() {
                      const cap = L<caret>IMIT;
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);

    PsiElement resolved = ref.resolve();
    assertNotNull("Selected import reference 'LIMIT' should resolve", resolved);
    assertTrue(resolved instanceof CompactNamedElement);
    assertEquals("LIMIT", ((CompactNamedElement) resolved).getName());
  }

  public void testStructLiteralTypeResolution() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point { x: Field, y: Field }
                    circuit draw() {
                      const p = <caret>Point { x: 0, y: 0 };
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    assertInstanceOf(ref, CompactTypeReference.class);

    PsiElement resolved = ref.resolve();
    assertNotNull(resolved);
    assertInstanceOf(resolved, CompactStructDefinition.class);
    assertEquals("Point", ((CompactNamedElement) resolved).getName());
  }
}
