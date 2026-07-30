// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface CompactProgramElement extends PsiElement {

  @Nullable
  CompactCircuitDefinition getCircuitDefinition();

  @Nullable
  CompactConstructorDefinition getConstructorDefinition();

  @Nullable
  CompactContractDeclaration getContractDeclaration();

  @Nullable
  CompactEnumDeclaration getEnumDeclaration();

  @Nullable
  CompactExportForm getExportForm();

  @Nullable
  CompactExternalDeclaration getExternalDeclaration();

  @Nullable
  CompactImplementsDeclaration getImplementsDeclaration();

  @Nullable
  CompactImportForm getImportForm();

  @Nullable
  CompactIncludeForm getIncludeForm();

  @Nullable
  CompactLedgerDeclaration getLedgerDeclaration();

  @Nullable
  CompactModuleDefinition getModuleDefinition();

  @Nullable
  CompactPragmaForm getPragmaForm();

  @Nullable
  CompactStructDeclaration getStructDeclaration();

  @Nullable
  CompactTypeAliasDeclaration getTypeAliasDeclaration();

  @Nullable
  CompactWitnessDeclaration getWitnessDeclaration();

}
