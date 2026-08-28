package dev.verloren.midnight.documentation;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.editor.CompactCommenter;
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
    assertTrue(doc.contains("Params:"));
    assertTrue(doc.contains("<code>to</code>"));
    assertTrue(doc.contains("Destination address"));

    String quickInfo = docProvider.getQuickNavigateInfo(element, null);
    assertNotNull(quickInfo);
    assertTrue(quickInfo.contains("circuit transfer"));
  }

  public void testBlockDocCommentWithFullTagsAndMarkdown() {
    String code = """
        /**
         * Transfers `amount` tokens to **recipient** address.
         *
         * Ensures sender has *sufficient* balance before transferring.
         *
         * @param to Destination address
         * @param amount The token amount to transfer
         * @return `true` if transfer was successful
         * @throws InsufficientBalance When balance is too low
         * @see balance
         * @since 1.0.0
         * @deprecated Use `transferWithFee` instead
         * @notice Public transfer endpoint
         * @dev Emits Transfer event on ledger
         */
        circuit <caret>transfer(to: Address, amount: Uint): Boolean {
          return true;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("circuit transfer"));
    assertTrue(doc.contains("<code>amount</code>"));
    assertTrue(doc.contains("<b>recipient</b>"));
    assertTrue(doc.contains("<i>sufficient</i>"));
    assertTrue(doc.contains("Params:"));
    assertTrue(doc.contains("<code>to</code> &ndash; Destination address"));
    assertTrue(doc.contains("<code>amount</code> &ndash; The token amount to transfer"));
    assertTrue(doc.contains("Returns:"));
    assertTrue(doc.contains("<code>true</code> if transfer was successful"));
    assertTrue(doc.contains("Throws:"));
    assertTrue(doc.contains("<code>InsufficientBalance</code> &ndash; When balance is too low"));
    assertTrue(doc.contains("See also:"));
    assertTrue(doc.contains("balance"));
    assertTrue(doc.contains("Since:"));
    assertTrue(doc.contains("1.0.0"));
    assertTrue(doc.contains("Deprecated:"));
    assertTrue(doc.contains("Use <code>transferWithFee</code> instead"));
    assertTrue(doc.contains("Notice:"));
    assertTrue(doc.contains("Public transfer endpoint"));
    assertTrue(doc.contains("Dev:"));
    assertTrue(doc.contains("Emits Transfer event on ledger"));
  }

  public void testDocOnCommentItself() {
    String code = """
        /**
         * <caret>Transfers tokens to a recipient address.
         * @param to Target address
         */
        circuit transfer(to: Address): Boolean {
          return true;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull("Hovering on doc comment should resolve to documented declaration", element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("circuit transfer"));
    assertTrue(doc.contains("Transfers tokens to a recipient address."));
    assertTrue(doc.contains("Target address"));
  }

  public void testParameterDocInheritanceFromCircuitDoc() {
    String code = """
        /**
         * Computes hash.
         * @param preimage The input value to hash
         */
        circuit hash(<caret>preimage: Field): Field {
          return preimage;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("parameter preimage: Field"));
    assertTrue(doc.contains("The input value to hash"));
  }

  public void testStructFieldDocInheritanceFromStructDoc() {
    String code = """
        /**
         * Represents 2D coordinates.
         * @param x The horizontal coordinate
         * @param y The vertical coordinate
         */
        struct Point {
          <caret>x: Field,
          y: Field,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("struct field Point.x: Field"));
    assertTrue(doc.contains("The horizontal coordinate"));
  }

  public void testRenderedDoc() {
    String code = """
        /**
         * Transfers `amount` tokens.
         * @param to Destination
         */
        circuit transfer(to: Address, amount: Uint): Boolean { return true; }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement comment = file.findElementAt(5);
    while (comment != null && !(comment instanceof PsiComment)) {
      comment = comment.getParent();
    }
    assertNotNull(comment);

    String rendered = docProvider.renderDocCommentOnly((PsiComment) comment);
    assertNotNull(rendered);
    assertTrue(rendered.contains("Transfers <code>amount</code> tokens."));
    assertTrue(rendered.contains("Params:"));
    assertTrue(rendered.contains("Destination"));
  }

  public void testCommenterCodeDocumentationSupport() {
    CompactCommenter commenter = new CompactCommenter();
    assertFalse(commenter.isDocumentationComment(null));
    assertEquals("/**", commenter.getDocumentationCommentPrefix());
    assertEquals("*", commenter.getDocumentationCommentLinePrefix());
    assertEquals("*/", commenter.getDocumentationCommentSuffix());
  }

  public void testCollectDocCommentsForReaderMode() {
    String code = """
        /**
         * Top-level doc comment.
         */
        circuit main(): Void {}

        /// Single-line doc comment.
        circuit helper(): Void {}
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    java.util.List<com.intellij.psi.PsiDocCommentBase> collected = new java.util.ArrayList<>();
    docProvider.collectDocComments(file, collected::add);

    assertEquals(2, collected.size());
    assertTrue(collected.getFirst().getText().contains("Top-level doc comment."));
    String quickInfo = docProvider.getQuickNavigateInfo(collected.getFirst(), null);
    assertNotNull(quickInfo);
    assertTrue(quickInfo.contains("circuit main"));

    com.intellij.psi.PsiDocCommentBase found = docProvider.findDocComment(file, collected.getFirst().getTextRange());
    assertNotNull(found);
    assertEquals(collected.getFirst().getText(), found.getText());

    String renderedDoc = docProvider.generateRenderedDoc(collected.getFirst());
    assertNotNull(renderedDoc);
    assertTrue(renderedDoc.contains("Top-level doc comment."));
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

