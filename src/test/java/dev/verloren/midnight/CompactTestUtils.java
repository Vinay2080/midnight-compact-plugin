package dev.verloren.midnight;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import dev.verloren.midnight.psi.CompactNamedElement;
import org.jetbrains.annotations.NotNull;

import static org.junit.Assert.*;

/**
 * Test utilities and DSL helpers for Compact language tests.
 */
public final class CompactTestUtils {
  private CompactTestUtils() {
  }

  /**
   * Configures a code snippet containing marker `/*caret*\/` (reference site) and `/*def*\/` (expected definition site)
   * and verifies that the reference at caret resolves precisely to the definition site element.
   *
   * @param fixture test fixture
   * @param code    source code containing `/*def*\/` and `/*caret*\/` markers
   */
  public static void doCheckResolve(@NotNull CodeInsightTestFixture fixture, @NotNull String code) {
    int defMarkerIndex = code.indexOf("/*def*/");
    int caretMarkerIndex = code.indexOf("/*caret*/");

    assertTrue("Code must contain /*caret*/ marker", caretMarkerIndex >= 0);
    assertTrue("Code must contain /*def*/ marker", defMarkerIndex >= 0);

    // Calculate actual offsets after removing markers
    int expectedDefOffset;
    int actualCaretOffset;

    if (defMarkerIndex < caretMarkerIndex) {
      expectedDefOffset = defMarkerIndex;
      actualCaretOffset = caretMarkerIndex - "/*def*/".length();
    } else {
      actualCaretOffset = caretMarkerIndex;
      expectedDefOffset = defMarkerIndex - "/*caret*/".length();
    }

    String cleanCode = code.replace("/*def*/", "").replace("/*caret*/", "");
    fixture.configureByText(CompactFileType.INSTANCE, cleanCode);
    fixture.getEditor().getCaretModel().moveToOffset(actualCaretOffset);

    PsiReference ref = fixture.getReferenceAtCaretPosition();
    if (ref == null) {
      PsiElement elementAtCaret = fixture.getFile().findElementAt(actualCaretOffset);
      if (elementAtCaret != null) {
        ref = elementAtCaret.getReference();
      }
    }

    assertNotNull("Reference at /*caret*/ must not be null", ref);
    PsiElement resolved = ref.resolve();
    assertNotNull("Reference at /*caret*/ must resolve to a non-null target", resolved);

    PsiElement expectedElement = fixture.getFile().findElementAt(expectedDefOffset);
    assertNotNull("Element at /*def*/ position must exist", expectedElement);

    // Verify resolved element matches expected definition
    boolean matches = resolved.getTextRange().contains(expectedDefOffset)
        || resolved.getTextRange().equals(expectedElement.getTextRange())
        || (resolved instanceof CompactNamedElement && ((CompactNamedElement) resolved).getNameIdentifier() != null
        && ((CompactNamedElement) resolved).getNameIdentifier().getTextRange().contains(expectedDefOffset));

    assertTrue(
        String.format("Resolved element '%s' at %s does not match expected definition at offset %d (element: '%s' at %s)",
            resolved.getText(), resolved.getTextRange(), expectedDefOffset, expectedElement.getText(), expectedElement.getTextRange()),
        matches
    );
  }

  /**
   * Configures a code snippet containing marker `/*caret*\/` and asserts that the reference does not resolve.
   *
   * @param fixture test fixture
   * @param code    source code containing `/*caret*\/` marker
   */
  public static void doCheckNoResolve(@NotNull CodeInsightTestFixture fixture, @NotNull String code) {
    int caretMarkerIndex = code.indexOf("/*caret*/");
    assertTrue("Code must contain /*caret*/ marker", caretMarkerIndex >= 0);

    String cleanCode = code.replace("/*caret*/", "");
    fixture.configureByText(CompactFileType.INSTANCE, cleanCode);
    fixture.getEditor().getCaretModel().moveToOffset(caretMarkerIndex);

    PsiReference ref = fixture.getReferenceAtCaretPosition();
    if (ref == null) {
      PsiElement elementAtCaret = fixture.getFile().findElementAt(caretMarkerIndex);
      if (elementAtCaret != null) {
        ref = elementAtCaret.getReference();
      }
    }

    if (ref != null) {
      PsiElement resolved = ref.resolve();
      assertNull(String.format("Reference at /*caret*/ should not resolve but resolved to '%s'", resolved != null ? resolved.getText() : null), resolved);
    }
  }
}
