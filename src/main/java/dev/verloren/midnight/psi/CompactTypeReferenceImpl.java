package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactTypeReferenceImpl extends CompactPsiElement {
  public CompactTypeReferenceImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiReference getReference() {
    PsiElement identifier = findIdentifierChild();
    if (identifier == null) {
      return null;
    }
    int start = identifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactTypeReference(this, TextRange.from(start, identifier.getTextLength()));
  }

  private @Nullable PsiElement findIdentifierChild() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }
}