package dev.verloren.midnight.stdlib;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.documentation.CompactDocumentationProvider;
import dev.verloren.midnight.navigation.CompactGotoDeclarationHandler;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactStructDefinition;

import java.util.Objects;

public class CompactStandardLibraryTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testResolveStandardLibraryStructMaybe() {
    String code = """
        circuit test(): Maybe<Field> {
            return none<Field>();
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    int offset = code.indexOf("Maybe");
    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull("Identifier element should exist", element);

    // Direct reference on identifier element
    PsiReference ref = element.getReference();
    if (ref == null) {
      ref = myFixture.getFile().findReferenceAt(offset);
    }
    assertNotNull("Reference on Maybe element should exist", ref);

    PsiElement target = ref.resolve();
    assertNotNull("Maybe should resolve to standard library struct definition", target);
    assertTrue("Target should be CompactStructDefinition", target instanceof CompactStructDefinition);
    assertEquals("Maybe", ((CompactStructDefinition) target).getName());
    assertEquals("standard-library.compact", target.getContainingFile().getName());

    // Verify GotoDeclarationHandler
    CompactGotoDeclarationHandler handler = new CompactGotoDeclarationHandler();
    PsiElement[] targets = handler.getGotoDeclarationTargets(element, offset, myFixture.getEditor());
    assertNotNull("GotoDeclarationHandler should return targets for Maybe", targets);
    assertTrue("Targets should not be empty", targets.length > 0);
    assertEquals("Maybe", ((CompactNamedElement) targets[0]).getName());
  }

  public void testResolveStandardLibraryCircuitSome() {
    String code = """
        circuit test(v: Field): [] {
            const m = some(42);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    int offset = code.indexOf("some(42)");
    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull("Identifier element 'some' should exist", element);

    // Direct reference on identifier element
    PsiReference ref = element.getReference();
    if (ref == null) {
      ref = myFixture.getFile().findReferenceAt(offset);
    }
    assertNotNull("Reference on some should exist", ref);

    PsiElement target = ref.resolve();
    assertNotNull("some should resolve to standard library circuit", target);
    assertTrue("Target should be CompactCircuitDefinition", target instanceof CompactCircuitDefinition);
    CompactCircuitDefinition circuit = (CompactCircuitDefinition) target;
    assertEquals("some", circuit.getName());
    assertEquals("standard-library.compact", circuit.getContainingFile().getName());

    // Verify GotoDeclarationHandler
    CompactGotoDeclarationHandler handler = new CompactGotoDeclarationHandler();
    PsiElement[] targets = handler.getGotoDeclarationTargets(element, offset, myFixture.getEditor());
    assertNotNull("GotoDeclarationHandler should return targets for some(42)", targets);
    assertTrue("Targets should not be empty", targets.length > 0);
    assertEquals("some", ((CompactNamedElement) targets[0]).getName());
  }

  public void testResolveZkirV3LibraryCircuit() {
    String code = """
        circuit test(): [] {
            const v = secp256k1EcdsaVerify;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    int offset = code.indexOf("secp256k1EcdsaVerify");
    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull("Identifier element for secp256k1EcdsaVerify should exist", element);

    // Direct reference on identifier element
    PsiReference ref = element.getReference();
    if (ref == null) {
      ref = myFixture.getFile().findReferenceAt(offset);
    }
    assertNotNull("Reference on secp256k1EcdsaVerify should exist", ref);

    PsiElement target = ref.resolve();
    assertNotNull("secp256k1EcdsaVerify should resolve", target);
    assertTrue("Target should be CompactCircuitDefinition", target instanceof CompactCircuitDefinition);
    CompactCircuitDefinition circuit = (CompactCircuitDefinition) target;
    assertEquals("secp256k1EcdsaVerify", circuit.getName());
    assertEquals("zkir-v3-library.compact", circuit.getContainingFile().getName());

    // Verify GotoDeclarationHandler
    CompactGotoDeclarationHandler handler = new CompactGotoDeclarationHandler();
    PsiElement[] targets = handler.getGotoDeclarationTargets(element, offset, myFixture.getEditor());
    assertNotNull("GotoDeclarationHandler should return targets for secp256k1EcdsaVerify", targets);
    assertTrue("Targets should not be empty", targets.length > 0);
    assertEquals("secp256k1EcdsaVerify", ((CompactNamedElement) targets[0]).getName());
  }

  public void testLexicalPrecedenceOverStandardLibrary() {
    String code = """
        circuit test(some: Field): Field {
            return some;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    int offset = code.lastIndexOf("some");
    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull(element);

    PsiReference ref = element.getReference();
    if (ref == null) {
      ref = myFixture.getFile().findReferenceAt(offset);
    }
    assertNotNull(ref);

    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertFalse("Should resolve to local parameter, not stdlib circuit", target instanceof CompactCircuitDefinition);
    assertEquals(myFixture.getFile().getName(), target.getContainingFile().getName());
  }

  public void testStandardLibraryDocumentation() {
    String code = """
        circuit test(): Maybe<Field> {
            return none<Field>();
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    int offset = code.indexOf("Maybe");
    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull(element);

    CompactDocumentationProvider provider = new CompactDocumentationProvider();
    PsiElement target = Objects.requireNonNull(myFixture.getFile().findReferenceAt(offset)).resolve();
    assertNotNull(target);

    String doc = provider.generateDoc(target, element);
    assertNotNull("Documentation for standard library struct should be generated", doc);
    assertTrue("Doc should contain struct Maybe", doc.contains("Maybe"));
  }
}
