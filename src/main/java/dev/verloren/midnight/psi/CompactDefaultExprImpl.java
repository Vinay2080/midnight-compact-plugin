package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactDefaultExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactDefaultExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement typeElem = PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
    return typeElem != null ? typeElem.getType() : CompactPrimitiveType.UNKNOWN;
  }
}
