package dev.verloren.midnight.editor;

import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;

import java.util.ArrayList;
import java.util.List;

public class CompactFoldingTest extends BasePlatformTestCase {

  public void testContractAndCircuitFolding() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export contract TestContract {
              ledger {
                counter: Counter;
              }
            
              constructor() {
                // init
              }
            
              export circuit testMethod(): Void {
                assert true, "ok";
              }
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    Document doc = myFixture.getEditor().getDocument();

    CompactFoldingBuilder builder = new CompactFoldingBuilder();
    List<FoldingDescriptor> descriptors = new ArrayList<>();
    builder.buildLanguageFoldRegions(descriptors, file, doc, false);

    assertFalse("Should find folding descriptors", descriptors.isEmpty());

    // Verify placeholder text
    for (FoldingDescriptor descriptor : descriptors) {
      String placeholder = builder.getLanguagePlaceholderText(descriptor.getElement(), descriptor.getRange());
      assertNotNull(placeholder);
      assertEquals("{...}", placeholder);
    }
  }

  public void testBlockCommentAndDocCommentFolding() {
    String code = """
            pragma language_version >= 0.20.0;
            
            /*
             * Multi-line
             * block comment
             */
            export contract CommentTest {
              /// Doc comment line 1
              /// Doc comment line 2
              export circuit foo(): Void {
              }
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    Document doc = myFixture.getEditor().getDocument();

    CompactFoldingBuilder builder = new CompactFoldingBuilder();
    List<FoldingDescriptor> descriptors = new ArrayList<>();
    builder.buildLanguageFoldRegions(descriptors, file, doc, false);

    boolean foundCommentFolding = false;
    for (FoldingDescriptor descriptor : descriptors) {
      String placeholder = builder.getLanguagePlaceholderText(descriptor.getElement(), descriptor.getRange());
      if ("/*...*/".equals(placeholder) || "///...".equals(placeholder)) {
        foundCommentFolding = true;
      }
    }
    assertTrue("Should find comment folding descriptor", foundCommentFolding);
  }

  public void testIncludeGroupFolding() {
    String code = """
            pragma language_version >= 0.20.0;
            
            include "./a.compact";
            include "./b.compact";
            include "./c.compact";
            
            export contract IncTest {
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    Document doc = myFixture.getEditor().getDocument();

    CompactFoldingBuilder builder = new CompactFoldingBuilder();
    List<FoldingDescriptor> descriptors = new ArrayList<>();
    builder.buildLanguageFoldRegions(descriptors, file, doc, false);

    boolean foundIncludeGroup = false;
    for (FoldingDescriptor descriptor : descriptors) {
      String placeholder = builder.getLanguagePlaceholderText(descriptor.getElement(), descriptor.getRange());
      if ("include ...".equals(placeholder)) {
        foundIncludeGroup = true;
      }
    }
    assertTrue("Should fold multiple consecutive includes", foundIncludeGroup);
  }

  public void testStructAndEnumFolding() {
    String code = """
            pragma language_version >= 0.20.0;
            
            struct Point {
              x: Field,
              y: Field
            }
            
            enum State {
              A,
              B
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    Document doc = myFixture.getEditor().getDocument();

    CompactFoldingBuilder builder = new CompactFoldingBuilder();
    List<FoldingDescriptor> descriptors = new ArrayList<>();
    builder.buildLanguageFoldRegions(descriptors, file, doc, false);

    assertTrue("Should find folding descriptors for struct and enum", descriptors.size() >= 2);
  }
}
