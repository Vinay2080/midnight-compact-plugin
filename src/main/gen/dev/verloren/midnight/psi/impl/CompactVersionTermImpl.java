// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactVersionAtom;
import dev.verloren.midnight.psi.CompactVersionTerm;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompactVersionTermImpl extends ASTWrapperPsiElement implements CompactVersionTerm {

  public CompactVersionTermImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitVersionTerm(this);
  }

  @Override
  @Nullable
  public CompactVersionAtom getVersionAtom() {
    return findChildByClass(CompactVersionAtom.class);
  }

  @Override
  @NotNull
  public List<CompactVersionTerm> getVersionTermList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactVersionTerm.class);
  }

}
