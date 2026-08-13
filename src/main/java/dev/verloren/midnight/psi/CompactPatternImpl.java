package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactPatternImpl extends CompactNamedElementImpl {
  private static final Key<Boolean> RESOLVING_TYPE = Key.create("COMPACT_PATTERN_RESOLVING_TYPE");

  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    if (getUserData(RESOLVING_TYPE) != null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    putUserData(RESOLVING_TYPE, true);
    try {
      return getTypeInner();
    } finally {
      putUserData(RESOLVING_TYPE, null);
    }
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
