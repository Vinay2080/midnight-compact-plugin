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

public class CompactExpr3Impl extends ASTWrapperPsiElement implements CompactExpr3 {

  public CompactExpr3Impl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExpr3(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor)visitor);
    else super.accept(visitor);
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
