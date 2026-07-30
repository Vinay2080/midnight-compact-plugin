// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactCbinding;
import dev.verloren.midnight.psi.CompactExpr;
import dev.verloren.midnight.psi.CompactOptionallyTypedPattern;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactCbindingImpl extends ASTWrapperPsiElement implements CompactCbinding {

  public CompactCbindingImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCbinding(this);
  }

  @Override
  @NotNull
  public CompactExpr getExpr() {
    return findNotNullChildByClass(CompactExpr.class);
  }

  @Override
  @NotNull
  public CompactOptionallyTypedPattern getOptionallyTypedPattern() {
    return findNotNullChildByClass(CompactOptionallyTypedPattern.class);
  }

}
