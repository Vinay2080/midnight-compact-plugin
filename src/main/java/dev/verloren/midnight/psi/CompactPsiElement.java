package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactPsiElement extends ASTWrapperPsiElement {
  public CompactPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCompactElement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) {
      accept((CompactVisitor)visitor);
    } else {
      super.accept(visitor);
    }
  }
}
