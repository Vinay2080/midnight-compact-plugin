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

public class CompactCircuitDefinitionImpl extends ASTWrapperPsiElement implements CompactCircuitDefinition {

  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCircuitDefinition(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public CompactBlock getBlock() {
    return findNotNullChildByClass(CompactBlock.class);
  }

  @Override
  @Nullable
  public CompactGparams getGparams() {
    return findChildByClass(CompactGparams.class);
  }

  @Override
  @NotNull
  public CompactPatternParameterList getPatternParameterList() {
    return findNotNullChildByClass(CompactPatternParameterList.class);
  }

  @Override
  @NotNull
  public CompactTypeExpression getTypeExpression() {
    return findNotNullChildByClass(CompactTypeExpression.class);
  }

}
