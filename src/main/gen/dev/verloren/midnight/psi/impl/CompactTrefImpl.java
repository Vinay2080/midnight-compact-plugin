// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactGargs;
import dev.verloren.midnight.psi.CompactTref;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactTrefImpl extends ASTWrapperPsiElement implements CompactTref {

  public CompactTrefImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTref(this);
  }

  @Override
  @Nullable
  public CompactGargs getGargs() {
    return findChildByClass(CompactGargs.class);
  }

}
