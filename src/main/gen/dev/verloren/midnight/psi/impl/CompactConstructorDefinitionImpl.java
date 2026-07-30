// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactConstructorDefinition;
import dev.verloren.midnight.psi.CompactPatternParameterList;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactConstructorDefinitionImpl extends ASTWrapperPsiElement implements CompactConstructorDefinition {

  public CompactConstructorDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitConstructorDefinition(this);
  }

  @Override
  @Nullable
  public CompactBlock getBlock() {
    return findChildByClass(CompactBlock.class);
  }

  @Override
  @Nullable
  public CompactPatternParameterList getPatternParameterList() {
    return findChildByClass(CompactPatternParameterList.class);
  }

}
