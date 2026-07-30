// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactPattern;
import dev.verloren.midnight.psi.CompactTypeExpression;
import dev.verloren.midnight.psi.CompactTypedPattern;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactTypedPatternImpl extends ASTWrapperPsiElement implements CompactTypedPattern {

  public CompactTypedPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTypedPattern(this);
  }

  @Override
  @NotNull
  public CompactPattern getPattern() {
    return findNotNullChildByClass(CompactPattern.class);
  }

  @Override
  @NotNull
  public CompactTypeExpression getTypeExpression() {
    return findNotNullChildByClass(CompactTypeExpression.class);
  }

}
