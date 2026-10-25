package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactTypeElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * In-editor intention action to remove an explicit type annotation from a {@code const} variable binding.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Available anywhere on the {@code const} statement line
 *       or directly when the caret is placed on an explicit type annotation (e.g. {@code : Field} in
 *       {@code const x: Field = 1;}).</li>
 *   <li><b>Whitespace & Colon Cleanup:</b> Locates the leading {@link CompactTokenTypes#COLON} token,
 *       consumes preceding spaces, and safely deletes or replaces the span with a single space before
 *       the {@code =} assignment operator to ensure clean code formatting without syntax errors.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under a Read Action ({@code @RequiresReadLock})
 *       on the EDT.</li>
 *   <li><b>Write Action for Invocation:</b> {@link #invoke} mutates the editor {@link Document} on the EDT
 *       within the platform's default write action.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.intention.CompactSpecifyTypeExplicitlyIntention
 * @see dev.verloren.midnight.psi.CompactConstBindingImpl
 */
public class CompactRemoveRedundantTypeIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact type annotation";
  }

  /**
   * Checks whether the intention is applicable at the current editor position.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    // Step 1: Locate enclosing const binding
    CompactConstBindingImpl binding = CompactSpecifyTypeExplicitlyIntention.findConstBinding(element);
    if (binding == null) {
      return false;
    }

    // Step 2: Ensure an explicit type annotation is present
    CompactTypeElement typeElem = CompactSpecifyTypeExplicitlyIntention.getDeclaredTypeElement(binding);
    if (typeElem == null) {
      return false;
    }

    setText("Remove type annotation");
    return true;
  }

  /**
   * Removes the type annotation and cleans up surrounding whitespace and colon tokens.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactConstBindingImpl binding = CompactSpecifyTypeExplicitlyIntention.findConstBinding(element);
    if (binding == null) {
      return;
    }

    CompactTypeElement typeElem = CompactSpecifyTypeExplicitlyIntention.getDeclaredTypeElement(binding);
    if (typeElem == null) {
      return;
    }

    // Step 1: Find the colon token before the type element
    PsiElement optionallyTypedPattern = typeElem.getParent();
    ASTNode colonNode = optionallyTypedPattern != null
        ? optionallyTypedPattern.getNode().findChildByType(CompactTokenTypes.COLON)
        : null;

    int startOffset = colonNode != null
        ? colonNode.getTextRange().getStartOffset()
        : typeElem.getTextRange().getStartOffset();
    int endOffset = typeElem.getTextRange().getEndOffset();

    Document document = editor.getDocument();
    CharSequence chars = document.getCharsSequence();

    // Step 2: Consume any space preceding the colon
    while (startOffset > 0 && chars.charAt(startOffset - 1) == ' ') {
      startOffset--;
    }

    // Step 3: Ensure spacing before '=' is preserved cleanly
    boolean hasTrailingSpace = endOffset < chars.length() && chars.charAt(endOffset) == ' ';
    if (!hasTrailingSpace && endOffset < chars.length() && chars.charAt(endOffset) == '=') {
      document.replaceString(startOffset, endOffset, " ");
    } else {
      document.deleteString(startOffset, endOffset);
    }
  }
}
