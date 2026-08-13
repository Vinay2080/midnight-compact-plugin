package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CompactVisitor extends PsiElementVisitor {
  public void visitPragmaForm(@NotNull CompactPragmaForm pragmaForm) {
    visitElement(pragmaForm);
  }

  @Override
  public void visitElement(@NotNull PsiElement element) {
    super.visitElement(element);
  }

  public void visitIncludeDeclaration(@NotNull CompactIncludeDeclaration includeDeclaration) {
    visitElement(includeDeclaration);
  }

  public void visitImportDeclaration(@NotNull CompactImportDeclaration importDeclaration) {
    visitElement(importDeclaration);
  }

  public void visitExportDeclaration(@NotNull CompactExportDeclaration exportDeclaration) {
    visitElement(exportDeclaration);
  }

  public void visitModuleDefinition(@NotNull CompactModuleDefinition moduleDefinition) {
    visitElement(moduleDefinition);
  }

  public void visitStructDefinition(@NotNull CompactStructDefinition structDefinition) {
    visitElement(structDefinition);
  }

  public void visitEnumDefinition(@NotNull CompactEnumDefinition enumDefinition) {
    visitElement(enumDefinition);
  }

  public void visitExternalContractDeclaration(@NotNull CompactExternalContractDeclaration contractDeclaration) {
    visitElement(contractDeclaration);
  }

  public void visitContractImplementsDeclaration(@NotNull CompactContractImplementsDeclaration implementsDeclaration) {
    visitElement(implementsDeclaration);
  }

  public void visitTypeDefinition(@NotNull CompactTypeDefinition typeDefinition) {
    visitElement(typeDefinition);
  }

  public void visitLedgerDeclaration(@NotNull CompactLedgerDeclaration ledgerDeclaration) {
    visitElement(ledgerDeclaration);
  }

  public void visitWitnessDeclaration(@NotNull CompactWitnessDeclaration witnessDeclaration) {
    visitElement(witnessDeclaration);
  }

  public void visitConstructorDeclaration(@NotNull CompactConstructorDeclaration constructorDeclaration) {
    visitElement(constructorDeclaration);
  }

  public void visitCircuitDefinition(@NotNull CompactCircuitDefinition circuitDefinition) {
    visitElement(circuitDefinition);
  }

  public void visitCompactElement(@NotNull CompactPsiElement element) {
    visitElement(element);
  }
}