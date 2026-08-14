package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactStructFieldReference;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactMemberExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactMemberExprImpl(@NotNull ASTNode node) {
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
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @Nullable PsiReference getReference() {
    PsiElement member = getMemberIdentifier();
    if (member == null) {
      return null;
    }

    int start = member.getStartOffsetInParent();
    PsiElement baseIdentifier = getBaseIdentifier();
    if (baseIdentifier != null) {
      for (CompactNamedElement target : CompactResolveUtil.resolveType(baseIdentifier.getText(), this)) {
        if (target instanceof CompactEnumDefinition) {
          return new CompactEnumMemberReference(this, TextRange.from(start, member.getTextLength()));
        }
      }
    }

    if (getBaseExpression() != null) {
      return new CompactStructFieldReference(this, TextRange.from(start, member.getTextLength()));
    }

    return null;
  }

  public @Nullable PsiElement getMemberIdentifier() {
    ASTNode[] nodes = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    PsiElement[] identifiers = new PsiElement[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      identifiers[i] = nodes[i].getPsi();
    }
    return identifiers.length == 0 ? null : identifiers[identifiers.length - 1];
  }

  private @Nullable PsiElement getBaseIdentifier() {
    CompactExpression base = getBaseExpression();
    if (base == null) {
      return null;
    }
    ASTNode identifier = base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  public @Nullable CompactExpression getBaseExpression() {
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return (CompactExpression) child;
      }
    }
    return null;
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
