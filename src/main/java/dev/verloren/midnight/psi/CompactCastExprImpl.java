package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactCastExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactCastExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    PsiElement[] children = getChildren();
    for (int i = children.length - 1; i >= 0; i--) {
      if (children[i] instanceof CompactTypeElement typeElem) {
        return typeElem.getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
