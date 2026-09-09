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
 * In-editor intention to remove an explicit type annotation from a {@code const} binding.
 *
 * <p>Available anywhere on the {@code const} statement line (general suggestion) as well as directly
 * on the type annotation itself.</p>
 */
public class CompactRemoveRedundantTypeIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact type annotation";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactConstBindingImpl binding = CompactSpecifyTypeExplicitlyIntention.findConstBinding(element);
    if (binding == null) {
      return false;
    }

    CompactTypeElement typeElem = CompactSpecifyTypeExplicitlyIntention.getDeclaredTypeElement(binding);
    if (typeElem == null) {
      return false;
    }

    setText("Remove type annotation");
    return true;
  }

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

    // Find colon token before typeElem
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

    // Consume any space before colon if present
    while (startOffset > 0 && chars.charAt(startOffset - 1) == ' ') {
      startOffset--;
    }

    // Ensure that if there's no space before '=', we insert one, or if there is already a space, keep one.
    boolean hasTrailingSpace = endOffset < chars.length() && chars.charAt(endOffset) == ' ';
    if (!hasTrailingSpace && endOffset < chars.length() && chars.charAt(endOffset) == '=') {
      document.replaceString(startOffset, endOffset, " ");
    } else {
      document.deleteString(startOffset, endOffset);
    }
  }
}
