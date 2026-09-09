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
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * In-editor intention to toggle between {@code circuit} and {@code pure circuit}.
 *
 * <p>Available anywhere on the circuit signature/header line as a general suggestion.</p>
 */
public class CompactTogglePureCircuitIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact circuit modifier";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit == null) {
      return false;
    }

    CompactBlock body = circuit.getBody();
    if (body != null) {
      Document doc = editor.getDocument();
      int caretLine = doc.getLineNumber(element.getTextOffset());
      int bodyStartLine = doc.getLineNumber(body.getTextOffset());
      // Available anywhere on the circuit header line(s) before descending into body statements
      if (caretLine > bodyStartLine) {
        return false;
      }
    }

    if (circuit.isPure()) {
      setText("Make circuit non-pure");
    } else {
      setText("Make circuit pure");
    }
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit == null) {
      return;
    }

    Document document = editor.getDocument();
    ASTNode pureNode = circuit.getNode().findChildByType(CompactTokenTypes.PURE);

    if (pureNode != null) {
      // Remove 'pure '
      int start = pureNode.getTextRange().getStartOffset();
      int end = pureNode.getTextRange().getEndOffset();
      CharSequence chars = document.getCharsSequence();
      while (end < chars.length() && Character.isWhitespace(chars.charAt(end)) && chars.charAt(end) != '\n') {
        end++;
      }
      document.deleteString(start, end);
    } else {
      // Insert 'pure ' before 'circuit'
      ASTNode circuitNode = circuit.getNode().findChildByType(CompactTokenTypes.CIRCUIT);
      if (circuitNode != null) {
        document.insertString(circuitNode.getTextRange().getStartOffset(), "pure ");
      }
    }
  }
}
