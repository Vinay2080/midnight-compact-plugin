package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactConstBindingImpl extends CompactNamedElementImpl {
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> CONST_BINDING_TYPE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactConstBindingType");

  public CompactConstBindingImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactType type = CONST_BINDING_TYPE_GUARD.doPreventingRecursion(this, false, () -> {
      CompactExpression expr = getInitializer();
      if (expr != null) {
        return expr.getType();
      }
      return CompactPrimitiveType.UNKNOWN;
    });
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  public CompactExpression getInitializer() {
    return PsiTreeUtil.findChildOfType(this, CompactExpression.class);
  }
}
