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

public class CompactExpr10Impl extends ASTWrapperPsiElement implements CompactExpr10 {

  public CompactExpr10Impl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr10(this);
  }

  @Override
  @NotNull
  public List<CompactExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExpr.class);
  }

  @Override
  @Nullable
  public CompactFun getFun() {
    return findChildByClass(CompactFun.class);
  }

  @Override
  @NotNull
  public List<CompactStructArg> getStructArgList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactStructArg.class);
  }

  @Override
  @Nullable
  public CompactTerm getTerm() {
    return findChildByClass(CompactTerm.class);
  }

  @Override
  @Nullable
  public CompactTref getTref() {
    return findChildByClass(CompactTref.class);
  }

  @Override
  @Nullable
  public CompactTsize getTsize() {
    return findChildByClass(CompactTsize.class);
  }

  @Override
  @NotNull
  public List<CompactTupleArg> getTupleArgList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactTupleArg.class);
  }

}
