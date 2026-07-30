// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactExpr;
import dev.verloren.midnight.psi.CompactExpr3;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactExprImpl extends ASTWrapperPsiElement implements CompactExpr {

  public CompactExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr(this);
  }

  @Override
  @NotNull
  public List<CompactExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExpr.class);
  }

  @Override
  @NotNull
  public List<CompactExpr3> getExpr3List() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExpr3.class);
  }

}
