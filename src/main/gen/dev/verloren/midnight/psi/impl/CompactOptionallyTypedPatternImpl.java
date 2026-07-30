// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactOptionallyTypedPattern;
import dev.verloren.midnight.psi.CompactPattern;
import dev.verloren.midnight.psi.CompactTypedPattern;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactOptionallyTypedPatternImpl extends ASTWrapperPsiElement implements CompactOptionallyTypedPattern {

  public CompactOptionallyTypedPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitOptionallyTypedPattern(this);
  }

  @Override
  @Nullable
  public CompactPattern getPattern() {
    return findChildByClass(CompactPattern.class);
  }

  @Override
  @Nullable
  public CompactTypedPattern getTypedPattern() {
    return findChildByClass(CompactTypedPattern.class);
  }

}
