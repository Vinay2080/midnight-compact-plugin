// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactExpr7;
import dev.verloren.midnight.psi.CompactExpr8;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactExpr7Impl extends ASTWrapperPsiElement implements CompactExpr7 {

  public CompactExpr7Impl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr7(this);
  }

  @Override
  @Nullable
  public CompactExpr7 getExpr7() {
    return findChildByClass(CompactExpr7.class);
  }

  @Override
  @Nullable
  public CompactExpr8 getExpr8() {
    return findChildByClass(CompactExpr8.class);
  }

}
