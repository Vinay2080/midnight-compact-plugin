package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactPatternImpl extends CompactNamedElementImpl {
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> PATTERN_TYPE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactPatternType");

  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactType type = PATTERN_TYPE_GUARD.doPreventingRecursion(this, false, this::getTypeInner);
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  private @NotNull CompactType getTypeInner() {
    PsiElement parent = getParent();
    if (parent instanceof CompactTypedPatternImpl) {
      return ((CompactTypedPatternImpl) parent).getType();
    }
    if (parent != null && parent.getNode().getElementType() == CompactElementTypes.OPTIONALLY_TYPED_PATTERN) {
      for (PsiElement child : parent.getChildren()) {
        if (child instanceof CompactTypeElement && child != this) {
          return ((CompactTypeElement) child).getType();
        }
      }
    }

    CompactConstBindingImpl constBinding = PsiTreeUtil.getParentOfType(this, CompactConstBindingImpl.class);
    if (constBinding != null) {
      CompactExpression initializer = constBinding.getInitializer();
      if (initializer != null && !PsiTreeUtil.isAncestor(this, initializer, false)) {
        return initializer.getType();
      }
    }

    return super.getType();
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }
}
