package dev.verloren.midnight.inspection.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix to remove the {@code pure} modifier from a Compact circuit declaration.
 */
public class CompactRemovePureModifierFix implements LocalQuickFix {

  @Override
  public @NotNull String getName() {
    return "Remove 'pure' modifier";
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Remove modifier";
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit != null) {
      ASTNode pureNode = circuit.getNode().findChildByType(CompactTokenTypes.PURE);
      if (pureNode != null) {
        ASTNode next = pureNode.getTreeNext();
        if (next != null && next.getElementType() == com.intellij.psi.TokenType.WHITE_SPACE) {
          circuit.getNode().removeChild(next);
        }
        circuit.getNode().removeChild(pureNode);
      }
    }
  }
}

