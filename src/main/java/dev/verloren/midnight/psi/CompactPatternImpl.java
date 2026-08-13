package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactPatternImpl extends CompactNamedElementImpl {
  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull dev.verloren.midnight.type.CompactType getType() {
    PsiElement parent = getParent();
    if (parent instanceof CompactTypedPatternImpl) {
      return ((CompactTypedPatternImpl) parent).getType();
    }
    if (parent != null && parent.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.OPTIONALLY_TYPED_PATTERN) {
       CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(parent, CompactTypeElement.class);
       if (typeElement != null) {
         return typeElement.getType();
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