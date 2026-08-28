package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

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
    PsiReference ref = getCalleeReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      if (resolved instanceof CompactTypeElement) {
        return ((CompactTypeElement) resolved).getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  public @Nullable PsiReference getCalleeReference() {
    CompactReferenceExprImpl refExpr = com.intellij.psi.util.PsiTreeUtil.getChildOfType(this, CompactReferenceExprImpl.class);
    if (refExpr != null) {
      return refExpr.getReference();
    }
    return getReference();
  }

  public @Nullable PsiElement resolveCallee() {
    PsiReference ref = getCalleeReference();
    return ref != null ? ref.resolve() : null;
  }


  @Override
  public @Nullable PsiReference getReference() {
    return CompactPsiUtil.createIdentifierValueReference(this);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
