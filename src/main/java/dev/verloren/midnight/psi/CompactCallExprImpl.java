package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactValueReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactCallExprImpl extends CompactPsiElement {
  public CompactCallExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiReference getReference() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    if (identifier == null) {
      return null;
    }
    PsiElement psi = identifier.getPsi();
    int start = psi.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactValueReference(this, TextRange.from(start, psi.getTextLength()));
  }
}
