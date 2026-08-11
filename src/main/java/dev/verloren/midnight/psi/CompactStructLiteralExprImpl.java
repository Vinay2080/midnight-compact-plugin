package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactStructLiteralExprImpl extends CompactPsiElement {
  public CompactStructLiteralExprImpl(@NotNull ASTNode node) {
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
    return new CompactTypeReference(this, TextRange.from(start, psi.getTextLength()));
  }
}