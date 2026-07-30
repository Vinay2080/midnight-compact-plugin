// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactFunImpl extends ASTWrapperPsiElement implements CompactFun {

  public CompactFunImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitFun(this);
  }

  @Override
  @Nullable
  public CompactArrowParameterList getArrowParameterList() {
    return findChildByClass(CompactArrowParameterList.class);
  }

  @Override
  @Nullable
  public CompactBlock getBlock() {
    return findChildByClass(CompactBlock.class);
  }

  @Override
  @Nullable
  public CompactExpr getExpr() {
    return findChildByClass(CompactExpr.class);
  }

  @Override
  @Nullable
  public CompactFun getFun() {
    return findChildByClass(CompactFun.class);
  }

  @Override
  @Nullable
  public CompactGargs getGargs() {
    return findChildByClass(CompactGargs.class);
  }

  @Override
  @Nullable
  public CompactReturnType getReturnType() {
    return findChildByClass(CompactReturnType.class);
  }

}
