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
 * In-editor intention action to wrap any expression with a {@code disclose(...)} boundary.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Available in the Alt+Enter context actions popup on expressions
 *       located within circuit or constructor code blocks.</li>
 *   <li><b>Security & Privacy Boundaries:</b> Helps developers manually mark private data for public disclosure
 *       when bridging private witness computations to public ledger state.</li>
 *   <li><b>Caret Repositioning:</b> After wrapping the expression in {@code disclose(...)}, automatically
 *       repositions the editor caret immediately after the closing parenthesis.</li>
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
 * @see dev.verloren.midnight.inspection.CompactUndisclosedWitnessInspection
 * @see dev.verloren.midnight.parser.CompactElementTypes#DISCLOSE_EXPR
 */
public class CompactSurroundWithDiscloseIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact privacy & disclosure";
  }

  /**
   * Identifies an expression under the caret eligible for wrapping in disclose(...).
   *
   * @param element the PSI element at the caret
   * @return eligible {@link CompactExpression}, or {@code null}
   */
  @Nullable
  private CompactExpression findSurroundableExpression(@NotNull PsiElement element) {
    // Step 1: Must be within a circuit or constructor block
    PsiElement block = PsiTreeUtil.getParentOfType(element, CompactBlock.class);
    if (block == null) {
      return null;
    }

    // Step 2: Locate enclosing expression
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

  /**
   * Replaces the expression with disclose(expr) and repositions the caret.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactExpression expr = findSurroundableExpression(element);
    if (expr == null) {
      return;
    }

    Document document = editor.getDocument();
    TextRange range = expr.getTextRange();
    String exprText = expr.getText();

    // Step 1: Replace expression with disclose(expr)
    document.replaceString(range.getStartOffset(), range.getEndOffset(), "disclose(" + exprText + ")");

    // Step 2: Reposition caret directly after the closing parenthesis
    editor.getCaretModel().moveToOffset(range.getStartOffset() + "disclose(".length() + exprText.length() + 1);
  }
}
