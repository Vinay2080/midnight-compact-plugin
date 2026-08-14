package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactCastExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactCastExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactTypeReferenceImpl typeRef = PsiTreeUtil.findChildOfType(this, CompactTypeReferenceImpl.class);
    if (typeRef != null) {
      return typeRef.getType();
    }
    CompactBuiltinTypeImpl builtinType = PsiTreeUtil.findChildOfType(this, CompactBuiltinTypeImpl.class);
    if (builtinType != null) {
      return builtinType.getType();
    }
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return ((CompactExpression) child).getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
