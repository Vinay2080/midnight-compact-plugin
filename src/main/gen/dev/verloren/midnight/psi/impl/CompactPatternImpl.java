// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactPattern;
import dev.verloren.midnight.psi.CompactPatternStructElt;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactPatternImpl extends ASTWrapperPsiElement implements CompactPattern {

  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitPattern(this);
  }

  @Override
  @NotNull
  public List<CompactPattern> getPatternList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactPattern.class);
  }

  @Override
  @NotNull
  public List<CompactPatternStructElt> getPatternStructEltList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactPatternStructElt.class);
  }

}
