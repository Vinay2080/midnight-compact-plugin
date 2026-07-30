// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.psi.CompactVersionTerm;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactPragmaFormImpl extends ASTWrapperPsiElement implements CompactPragmaForm {

  public CompactPragmaFormImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitPragmaForm(this);
  }

  @Override
  @NotNull
  public List<CompactVersionTerm> getVersionTermList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactVersionTerm.class);
  }

}
