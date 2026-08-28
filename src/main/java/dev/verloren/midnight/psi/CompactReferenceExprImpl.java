package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactReferenceExprImpl extends CompactPsiElement implements CompactReferenceExpr {
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> TYPE_INFERENCE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactReferenceExprType");

  public CompactReferenceExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactType type = TYPE_INFERENCE_GUARD.doPreventingRecursion(this, false, () -> {
      PsiReference ref = getReference();
      if (ref != null) {
        PsiElement resolved = ref.resolve();
        if (resolved instanceof CompactTypeElement) {
          return ((CompactTypeElement) resolved).getType();
        }
      }
      return CompactPrimitiveType.UNKNOWN;
    });
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @Nullable PsiReference getReference() {
    return CompactPsiUtil.createIdentifierValueReference(this);
  }

  @Override
  public @Nullable PsiElement resolve() {
    PsiReference ref = getReference();
    return ref != null ? ref.resolve() : null;
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
