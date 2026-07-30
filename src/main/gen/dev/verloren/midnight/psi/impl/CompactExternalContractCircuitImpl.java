// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactExternalContractCircuit;
import dev.verloren.midnight.psi.CompactSimpleParameterList;
import dev.verloren.midnight.psi.CompactTypeExpression;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactExternalContractCircuitImpl extends ASTWrapperPsiElement implements CompactExternalContractCircuit {

  public CompactExternalContractCircuitImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalContractCircuit(this);
  }

  @Override
  @Nullable
  public CompactSimpleParameterList getSimpleParameterList() {
    return findChildByClass(CompactSimpleParameterList.class);
  }

  @Override
  @Nullable
  public CompactTypeExpression getTypeExpression() {
    return findChildByClass(CompactTypeExpression.class);
  }

}
