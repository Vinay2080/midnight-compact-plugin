package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactParenExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactParenExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return ((CompactExpression) child).getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
