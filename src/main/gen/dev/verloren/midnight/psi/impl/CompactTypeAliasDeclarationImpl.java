// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactGparams;
import dev.verloren.midnight.psi.CompactTypeAliasDeclaration;
import dev.verloren.midnight.psi.CompactTypeExpression;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactTypeAliasDeclarationImpl extends ASTWrapperPsiElement implements CompactTypeAliasDeclaration {

  public CompactTypeAliasDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTypeAliasDeclaration(this);
  }

  @Override
  @Nullable
  public CompactGparams getGparams() {
    return findChildByClass(CompactGparams.class);
  }

  @Override
  @Nullable
  public CompactTypeExpression getTypeExpression() {
    return findChildByClass(CompactTypeExpression.class);
  }

}
