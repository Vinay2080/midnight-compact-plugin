// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactContractDeclaration;
import dev.verloren.midnight.psi.CompactExternalContractCircuit;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactContractDeclarationImpl extends ASTWrapperPsiElement implements CompactContractDeclaration {

  public CompactContractDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitContractDeclaration(this);
  }

  @Override
  @NotNull
  public List<CompactExternalContractCircuit> getExternalContractCircuitList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExternalContractCircuit.class);
  }

}
