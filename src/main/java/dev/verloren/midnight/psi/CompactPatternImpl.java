package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactPatternImpl extends CompactNamedElementImpl {
  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }
}