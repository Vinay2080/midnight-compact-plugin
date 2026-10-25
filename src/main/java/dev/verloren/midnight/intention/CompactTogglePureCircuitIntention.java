package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactPsiUtil;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * In-editor intention action to toggle between {@code circuit} and {@code pure circuit}.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Available in the Alt+Enter context actions popup when the caret
 *       is positioned anywhere on the circuit signature/header line before the body block.</li>
 *   <li><b>Pure Circuit Semantics:</b> Pure circuits in Compact cannot access ledger state, invoke witnesses,
 *       emit events, or call non-pure circuits. This intention allows fast toggling between pure and impure modes.</li>
 *   <li><b>Whitespace Cleanup:</b> Uses {@link CompactPsiUtil#deleteNodeWithTrailingWhitespace} when removing
 *       the {@code pure} keyword to avoid orphaned double spaces.</li>
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
 * @see dev.verloren.midnight.psi.CompactCircuitDefinition#isPure()
 * @see dev.verloren.midnight.inspection.CompactPureCircuitInspection
 */
public class CompactTogglePureCircuitIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact circuit modifier";
  }

  /**
   * Checks whether the caret is on the signature header of a circuit definition.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit == null) {
      return false;
    }

    // Step 1: Bound availability to the header line before the body block
    CompactBlock body = circuit.getBody();
    if (body != null) {
      Document doc = editor.getDocument();
      int caretLine = doc.getLineNumber(element.getTextOffset());
      int bodyStartLine = doc.getLineNumber(body.getTextOffset());
      if (caretLine > bodyStartLine) {
        return false;
      }
    }

    // Step 2: Dynamically toggle text based on whether the circuit is currently pure
    if (circuit.isPure()) {
      setText("Make circuit non-pure");
    } else {
      setText("Make circuit pure");
    }
    return true;
  }

  /**
   * Inserts or removes the 'pure' modifier in the document.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit == null) {
      return;
    }

    Document document = editor.getDocument();
    ASTNode pureNode = circuit.getNode().findChildByType(CompactTokenTypes.PURE);

    if (pureNode != null) {
      // Step 3: Remove 'pure' token and trailing whitespace
      CompactPsiUtil.deleteNodeWithTrailingWhitespace(document, pureNode);
    } else {
      // Step 4: Insert 'pure ' before the 'circuit' keyword token
      ASTNode circuitNode = circuit.getNode().findChildByType(CompactTokenTypes.CIRCUIT);
      if (circuitNode != null) {
        document.insertString(circuitNode.getTextRange().getStartOffset(), "pure ");
      }
    }
  }
}
