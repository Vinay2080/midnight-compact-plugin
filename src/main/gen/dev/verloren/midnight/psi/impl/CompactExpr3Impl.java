// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactExpr3;
import dev.verloren.midnight.psi.CompactExpr7;
import dev.verloren.midnight.psi.CompactTypeExpression;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactExpr3Impl extends ASTWrapperPsiElement implements CompactExpr3 {

  public CompactExpr3Impl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr3(this);
  }

  @Override
  @NotNull
  public List<CompactExpr7> getExpr7List() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExpr7.class);
  }

  @Override
  @NotNull
  public List<CompactTypeExpression> getTypeExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactTypeExpression.class);
  }

}
