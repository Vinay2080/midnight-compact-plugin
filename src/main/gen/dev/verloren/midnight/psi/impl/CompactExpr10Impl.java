// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static dev.verloren.midnight.psi.CompactTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import dev.verloren.midnight.psi.*;

public class CompactExpr10Impl extends ASTWrapperPsiElement implements CompactExpr10 {

  public CompactExpr10Impl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr10(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor)visitor);
    else super.accept(visitor);
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
