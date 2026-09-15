package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactIndexExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactIndexExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] exprs = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    if (exprs != null && exprs.length > 0) {
      CompactType baseType = exprs[0].getType();
      String name = baseType.name();
      if (name.startsWith("Vector<") && name.endsWith(">")) {
        int commaIdx = name.indexOf(',');
        if (commaIdx != -1) {
          String elemTypeName = name.substring(commaIdx + 1, name.length() - 1).trim();
          return new CompactPrimitiveType(elemTypeName);
        }
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
