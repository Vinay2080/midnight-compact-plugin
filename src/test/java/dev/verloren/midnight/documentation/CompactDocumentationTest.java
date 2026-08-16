package dev.verloren.midnight.documentation;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

public class CompactDocumentationTest extends BasePlatformTestCase {

  private CompactDocumentationProvider docProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    docProvider = new CompactDocumentationProvider();
  }

  public void testCircuitDocumentationWithDocComments() {
    String code = """
        /// Transfers tokens to a recipient address.
        /// @param to Destination address
        circuit <caret>transfer(to: Address, amount: Uint): Boolean {
          return true;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull("Custom doc element should resolve to circuit", element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("circuit transfer"));
    assertTrue(doc.contains("Transfers tokens to a recipient address."));
    assertTrue(doc.contains("@param to Destination address"));

    String quickInfo = docProvider.getQuickNavigateInfo(element, null);
    assertNotNull(quickInfo);
    assertTrue(quickInfo.contains("circuit transfer"));
  }

  public void testWitnessDocumentation() {
    String code = """
        // Queries the user's private key
        witness <caret>getSecretKey(): Field {
          return 42;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("witness getSecretKey"));
    assertTrue(doc.contains("Queries the user&#39;s private key"));
  }

  public void testStructDocumentationWithFields() {
    String code = """
        /// A 2D point representation
        struct <caret>Point {
          x: Field,
          y: Field,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("struct Point"));
    assertTrue(doc.contains("A 2D point representation"));
    assertTrue(doc.contains("Fields:"));
    assertTrue(doc.contains("x: Field"));
    assertTrue(doc.contains("y: Field"));
  }

  public void testStructFieldDocumentation() {
    String code = """
        struct Point {
          <caret>x: Field,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("struct field Point.x: Field"));
  }

  public void testEnumDocumentationWithVariants() {
    String code = """
        /// Color state
        enum <caret>Color {
          Red,
          Green,
          Blue,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("enum Color"));
    assertTrue(doc.contains("Color state"));
    assertTrue(doc.contains("Variants:"));
    assertTrue(doc.contains("Red"));
    assertTrue(doc.contains("Green"));
    assertTrue(doc.contains("Blue"));
  }

  public void testEnumMemberDocumentation() {
    String code = """
        enum Color {
          <caret>Red,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("enum variant Color.Red"));
  }

  public void testTypeAliasDocumentation() {
    String code = """
        /// 32-byte cryptographic hash
        type <caret>Hash = Bytes<32>;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("type Hash"));
    assertTrue(doc.contains("32-byte cryptographic hash"));
  }

  public void testConstBindingDocumentation() {
    String code = """
        /// Max retry attempts
        const <caret>MAX_COUNT = 100;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("const MAX_COUNT"));
    assertTrue(doc.contains("Max retry attempts"));
  }

  public void testDocOnReferenceUsage() {
    String code = """
        /// Secret helper
        circuit helper(): Void {}
        
        circuit main(): Void {
          <caret>helper();
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull("Hover on helper reference should resolve to declaration", element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("circuit helper"));
    assertTrue(doc.contains("Secret helper"));
  }
}
