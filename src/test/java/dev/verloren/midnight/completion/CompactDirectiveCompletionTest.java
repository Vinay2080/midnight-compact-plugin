package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactDirectiveCompletionTest extends CompactCompletionTestBase {

  public void testAssertCompletionInsertsParenthesesAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          <caret>
        }
        """
    );
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull("Lookup elements should not be null", elements);
    LookupElement assertEl = null;
    for (LookupElement el : elements) {
      if ("assert".equals(el.getLookupString())) {
        assertEl = el;
        break;
      }
    }
    assertNotNull("Should find 'assert' lookup element", assertEl);
    myFixture.getLookup().setCurrentItem(assertEl);
    myFixture.type('\n');
    myFixture.checkResult(
        """
        circuit test(): Void {
          assert(<caret>)
        }
        """
    );
  }

  public void testAssertCompletionWithExistingParenthesesDoesNotDuplicate() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          <caret>(true, "msg");
        }
        """
    );
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull("Lookup elements should not be null", elements);
    LookupElement assertEl = null;
    for (LookupElement el : elements) {
      if ("assert".equals(el.getLookupString())) {
        assertEl = el;
        break;
      }
    }
    assertNotNull("Should find 'assert' lookup element", assertEl);
    myFixture.getLookup().setCurrentItem(assertEl);
    myFixture.type('\n');
    myFixture.checkResult(
        """
        circuit test(): Void {
          assert(<caret>true, "msg");
        }
        """
    );
  }

  public void testAssertCompletionPrefixAutoInsert() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          ass<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    if (elements == null) {
      myFixture.checkResult(
          """
          circuit test(): Void {
            assert(<caret>)
          }
          """
      );
    } else {
      LookupElement assertEl = null;
      for (LookupElement el : elements) {
        if ("assert".equals(el.getLookupString())) {
          assertEl = el;
          break;
        }
      }
      assertNotNull(assertEl);
      myFixture.getLookup().setCurrentItem(assertEl);
      myFixture.type('\n');
      myFixture.checkResult(
          """
          circuit test(): Void {
            assert(<caret>)
          }
          """
      );
    }
  }

  public void testAfterPragmaContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_PRAGMA, CompactCompletionContext.classify(pos));
  }

  public void testPragmaCompletionDirectivesSuggested() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'language_version'", lookupStrings.contains("language_version"));
    assertTrue("Should suggest 'compiler_version'", lookupStrings.contains("compiler_version"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'import'", lookupStrings.contains("import"));
  }

  public void testPragmaCompletionFilteringAndInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma lang<caret>
        """
    );
    myFixture.completeBasic();
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'pragma language_version ' but was:\n" + text,
        text.contains("pragma language_version "));
  }

  public void testPragmaCompilerVersionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma comp<caret>
        """
    );
    myFixture.completeBasic();
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'pragma compiler_version ' but was:\n" + text,
        text.contains("pragma compiler_version "));
  }

  public void testPragmaContextWithPrefixInsideForm() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma langu<caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_PRAGMA, CompactCompletionContext.classify(pos));
  }
}
