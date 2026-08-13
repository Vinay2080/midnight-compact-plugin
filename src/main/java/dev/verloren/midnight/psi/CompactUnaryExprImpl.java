package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

public class CompactUnaryExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactUnaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactExpression operand = null;
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        operand = (CompactExpression) child;
        break;
      }
    }
    if (operand == null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    ASTNode operatorNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
    if (operatorNode == null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    return CompactTypeInferenceUtil.inferUnaryExprType(operatorNode.getElementType(), operand);
  }
}
