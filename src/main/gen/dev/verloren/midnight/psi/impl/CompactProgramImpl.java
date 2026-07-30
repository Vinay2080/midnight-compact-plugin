// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactProgram;
import dev.verloren.midnight.psi.CompactProgramElement;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactProgramImpl extends ASTWrapperPsiElement implements CompactProgram {

  public CompactProgramImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitProgram(this);
  }

  @Override
  @NotNull
  public List<CompactProgramElement> getProgramElementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactProgramElement.class);
  }

}
