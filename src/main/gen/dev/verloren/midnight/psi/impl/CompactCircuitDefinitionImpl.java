// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactCircuitDefinitionImpl extends ASTWrapperPsiElement implements CompactCircuitDefinition {

  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCircuitDefinition(this);
  }

  @Override
  @Nullable
  public CompactBlock getBlock() {
    return findChildByClass(CompactBlock.class);
  }

  @Override
  @Nullable
  public CompactGparams getGparams() {
    return findChildByClass(CompactGparams.class);
  }

  @Override
  @Nullable
  public CompactPatternParameterList getPatternParameterList() {
    return findChildByClass(CompactPatternParameterList.class);
  }

  @Override
  @Nullable
  public CompactTypeExpression getTypeExpression() {
    return findChildByClass(CompactTypeExpression.class);
  }

}
