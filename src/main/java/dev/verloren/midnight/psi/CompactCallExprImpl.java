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

public class CompactCallExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactCallExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      if (resolved instanceof CompactTypeElement) {
        // For now, assume it's a function/circuit and we need its return type
        // In a full implementation, we'd handle Function types here
        return ((CompactTypeElement) resolved).getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
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
