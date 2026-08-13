package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactPsiElement extends ASTWrapperPsiElement {
  public CompactPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public com.intellij.psi.PsiReference @NotNull [] getReferences() {
    com.intellij.psi.PsiReference ref = getReference();
    return ref == null ? super.getReferences() : new com.intellij.psi.PsiReference[]{ref};
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) {
      accept((CompactVisitor) visitor);
    } else {
      super.accept(visitor);
    }
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCompactElement(this);
  }
}
