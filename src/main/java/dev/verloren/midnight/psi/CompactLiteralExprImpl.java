package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactNumericLiteralType;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactLiteralExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactLiteralExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    ASTNode first = getNode().getFirstChildNode();
    if (first == null) return CompactPrimitiveType.UNKNOWN;

    IElementType type = first.getElementType();
    if (type == CompactTokenTypes.TRUE || type == CompactTokenTypes.FALSE) {
      return CompactPrimitiveType.BOOLEAN;
    }
    if (CompactTokenSets.NAT_LITERALS.contains(type)) {
      return new CompactNumericLiteralType(getText());
    }
    if (type == CompactTokenTypes.STRING_LITERAL) {
      return CompactPrimitiveType.BYTES;
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
