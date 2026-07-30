// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompactExpr9Impl extends ASTWrapperPsiElement implements CompactExpr9 {

  public CompactExpr9Impl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr9(this);
  }

  @Override
  @NotNull
  public List<CompactExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExpr.class);
  }

  @Override
  @Nullable
  public CompactExpr10 getExpr10() {
    return findChildByClass(CompactExpr10.class);
  }

  @Override
  @Nullable
  public CompactFun getFun() {
    return findChildByClass(CompactFun.class);
  }

}
