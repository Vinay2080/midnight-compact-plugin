package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactTupleExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactTupleExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] items = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    int count = items != null ? items.length : 0;
    if (count > 0) {
      CompactType itemType = items[0].getType();
      if (!CompactPrimitiveType.UNKNOWN.equals(itemType)) {
        return new CompactPrimitiveType("Vector<" + count + ", " + itemType.name() + ">");
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
