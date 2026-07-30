// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactImportFormImpl extends ASTWrapperPsiElement implements CompactImportForm {

  public CompactImportFormImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportForm(this);
  }

  @Override
  @Nullable
  public CompactGargs getGargs() {
    return findChildByClass(CompactGargs.class);
  }

  @Override
  @Nullable
  public CompactImportName getImportName() {
    return findChildByClass(CompactImportName.class);
  }

  @Override
  @Nullable
  public CompactImportPrefix getImportPrefix() {
    return findChildByClass(CompactImportPrefix.class);
  }

  @Override
  @Nullable
  public CompactImportSelection getImportSelection() {
    return findChildByClass(CompactImportSelection.class);
  }

}
