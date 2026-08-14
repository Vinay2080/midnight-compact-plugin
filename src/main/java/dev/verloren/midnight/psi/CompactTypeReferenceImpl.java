package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactTypeReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactTypeReferenceImpl extends CompactPsiElement implements CompactTypeElement {
  public CompactTypeReferenceImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      if (resolved instanceof CompactTypeElement) {
        return ((CompactTypeElement) resolved).getType();
      }
    }
    // Handle builtin types by text if resolution fails
    String text = getText();
    if ("Boolean".equals(text)) return CompactPrimitiveType.BOOLEAN;
    if ("Field".equals(text)) return CompactPrimitiveType.FIELD;
    if (text.startsWith("Uint") || text.startsWith("Bytes") || text.startsWith("Vector")) {
      return new CompactPrimitiveType(text);
    }

    return CompactPrimitiveType.UNKNOWN;
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

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}