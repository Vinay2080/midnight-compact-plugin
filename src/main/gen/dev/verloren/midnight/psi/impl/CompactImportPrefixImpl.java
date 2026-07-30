// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactImportPrefix;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactImportPrefixImpl extends ASTWrapperPsiElement implements CompactImportPrefix {

  public CompactImportPrefixImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportPrefix(this);
  }

}
