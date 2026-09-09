package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-editor intention to surround an expression with {@code disclose(...)}.
 */
public class CompactSurroundWithDiscloseIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact privacy & disclosure";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Nullable
  private CompactExpression findSurroundableExpression(@NotNull PsiElement element) {
    // Must be within a circuit or constructor block
    PsiElement block = PsiTreeUtil.getParentOfType(element, CompactBlock.class);
    if (block == null) {
      return null;
    }

    CompactExpression expr = PsiTreeUtil.getParentOfType(element, CompactExpression.class, false);
    while (expr != null) {
      // Don't surround statement-level sequence nodes
      if (expr.getNode().getElementType() == CompactElementTypes.EXPRESSION_SEQUENCE) {
        expr = PsiTreeUtil.getParentOfType(expr, CompactExpression.class, true);
        continue;
      }
      // If already wrapped in disclose(...), skip
      if (expr.getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR) {
        return null;
      }
      PsiElement parent = expr.getParent();
      if (parent != null && parent.getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR) {
        return null;
      }
      return expr;
    }
    return null;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactExpression expr = findSurroundableExpression(element);
    if (expr == null) {
      return false;
    }

    setText("Surround with disclose(...)");
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactExpression expr = findSurroundableExpression(element);
    if (expr == null) {
      return;
    }

    Document document = editor.getDocument();
    TextRange range = expr.getTextRange();
    String exprText = expr.getText();

    document.replaceString(range.getStartOffset(), range.getEndOffset(), "disclose(" + exprText + ")");
    editor.getCaretModel().moveToOffset(range.getStartOffset() + "disclose(".length() + exprText.length() + 1);
  }
}
