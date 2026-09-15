package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactPadExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactPadExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] args = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    if (args != null && args.length > 0) {
      String text = args[0].getText().trim();
      try {
        int size = Integer.parseInt(text);
        return new CompactPrimitiveType("Bytes<" + size + ">");
      } catch (NumberFormatException ignored) {
      }
    }
    return CompactPrimitiveType.BYTES;
  }
}
