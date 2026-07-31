// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static dev.verloren.midnight.psi.CompactTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import dev.verloren.midnight.psi.*;

public class CompactProgramElementImpl extends ASTWrapperPsiElement implements CompactProgramElement {

  public CompactProgramElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitProgramElement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CompactCircuitDefinition getCircuitDefinition() {
    return findChildByClass(CompactCircuitDefinition.class);
  }

  @Override
  @Nullable
  public CompactConstructorDefinition getConstructorDefinition() {
    return findChildByClass(CompactConstructorDefinition.class);
  }

  @Override
  @Nullable
  public CompactContractDeclaration getContractDeclaration() {
    return findChildByClass(CompactContractDeclaration.class);
  }

  @Override
  @Nullable
  public CompactEnumDeclaration getEnumDeclaration() {
    return findChildByClass(CompactEnumDeclaration.class);
  }

  @Override
  @Nullable
  public CompactExportForm getExportForm() {
    return findChildByClass(CompactExportForm.class);
  }

  @Override
  @Nullable
  public CompactExternalDeclaration getExternalDeclaration() {
    return findChildByClass(CompactExternalDeclaration.class);
  }

  @Override
  @Nullable
  public CompactImplementsDeclaration getImplementsDeclaration() {
    return findChildByClass(CompactImplementsDeclaration.class);
  }

  @Override
  @Nullable
  public CompactImportForm getImportForm() {
    return findChildByClass(CompactImportForm.class);
  }

  @Override
  @Nullable
  public CompactIncludeForm getIncludeForm() {
    return findChildByClass(CompactIncludeForm.class);
  }

  @Override
  @Nullable
  public CompactLedgerDeclaration getLedgerDeclaration() {
    return findChildByClass(CompactLedgerDeclaration.class);
  }

  @Override
  @Nullable
  public CompactModuleDefinition getModuleDefinition() {
    return findChildByClass(CompactModuleDefinition.class);
  }

  @Override
  @Nullable
  public CompactPragmaForm getPragmaForm() {
    return findChildByClass(CompactPragmaForm.class);
  }

  @Override
  @Nullable
  public CompactStructDeclaration getStructDeclaration() {
    return findChildByClass(CompactStructDeclaration.class);
  }

  @Override
  @Nullable
  public CompactTypeAliasDeclaration getTypeAliasDeclaration() {
    return findChildByClass(CompactTypeAliasDeclaration.class);
  }

  @Override
  @Nullable
  public CompactWitnessDeclaration getWitnessDeclaration() {
    return findChildByClass(CompactWitnessDeclaration.class);
  }

}
