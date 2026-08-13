package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactValueReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactReferenceExprImpl extends CompactPsiElement implements CompactReferenceExpr {
  private static final com.intellij.openapi.util.Key<Boolean> RESOLVING = com.intellij.openapi.util.Key.create("COMPACT_RESOLVING");

  public CompactReferenceExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    if (getUserData(RESOLVING) != null) {
      return CompactPrimitiveType.UNKNOWN;
    }
    putUserData(RESOLVING, true);
    try {
      PsiReference ref = getReference();
      if (ref != null) {
        PsiElement resolved = ref.resolve();
        if (resolved instanceof CompactTypeElement) {
          return ((CompactTypeElement) resolved).getType();
        }
      }
      return CompactPrimitiveType.UNKNOWN;
    } finally {
      putUserData(RESOLVING, null);
    }
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