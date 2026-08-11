package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactGenericParameterImpl extends CompactNamedElementImpl {
  public CompactGenericParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    PsiElement id = findChildByType(CompactTokenTypes.IDENTIFIER);
    return id != null ? id : this;
  }
}