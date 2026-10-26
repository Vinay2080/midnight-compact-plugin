package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * Double-dispatch visitor pattern base class for traversing and analyzing Compact PSI elements.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Extends IntelliJ's {@link PsiElementVisitor} to provide specialized visit callbacks for all major
 * Compact syntax constructs. Subclassed by code inspections, annotators, and refactoring analyzers:
 * <ul>
 *   <li><b>Code Inspections:</b> {@link com.intellij.codeInspection.LocalInspectionTool#buildVisitor}
 *       instantiates anonymous or specialized subclasses of {@link CompactVisitor} to collect warnings and errors.</li>
 *   <li><b>Type-Safe Dispatch:</b> Eliminates fragile, cascading {@code instanceof} blocks by relying
 *       on polymorphism via {@link CompactPsiElement#accept(CompactVisitor)}.</li>
 *   <li><b>Hierarchical Fallback:</b> All specialized {@code visit*()} methods default to invoking
 *       {@link #visitElement(PsiElement)}, ensuring unhandled constructs gracefully fall back to generic handling.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across code analysis pipelines:
 * <ul>
 *   <li>{@link com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl} driving background inspections.</li>
 *   <li>{@link com.intellij.psi.PsiRecursiveElementVisitor} and related tree walkers when traversing PSI trees.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Predominantly executed on pooled background daemon threads during file inspections.</li>
 *   <li><b>Read Action:</b> Must always run within an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Cancellation Support:</b> Heavy AST traversals must periodically call {@link com.intellij.openapi.progress.ProgressManager#checkCanceled()}
 *       to respect user typing interrupts.</li>
 * </ul>
 *
 * @see PsiElementVisitor
 * @see CompactPsiElement#accept(CompactVisitor)
 * @see com.intellij.codeInspection.LocalInspectionTool
 */
public class CompactVisitor extends PsiElementVisitor {

  /**
   * Visits a compiler pragma declaration (e.g., {@code pragma language_version >= 0.20;}).
   *
   * @param pragmaForm the pragma form element
   */
  public void visitPragmaForm(@NotNull CompactPragmaForm pragmaForm) {
    visitElement(pragmaForm);
  }

  /**
   * Fallback handler for generic PSI elements.
   *
   * @param element the generic element
   */
  @Override
  public void visitElement(@NotNull PsiElement element) {
    super.visitElement(element);
  }

  /**
   * Visits an include directive declaration (e.g., {@code include "other.compact";}).
   *
   * @param includeDeclaration the include declaration element
   */
  public void visitIncludeDeclaration(@NotNull CompactIncludeDeclaration includeDeclaration) {
    visitElement(includeDeclaration);
  }

  /**
   * Visits an import declaration statement (e.g., {@code import { Foo, Bar } from "mod";}).
   *
   * @param importDeclaration the import declaration element
   */
  public void visitImportDeclaration(@NotNull CompactImportDeclaration importDeclaration) {
    visitElement(importDeclaration);
  }

  /**
   * Visits an export declaration statement (e.g., {@code export { MyType };}).
   *
   * @param exportDeclaration the export declaration element
   */
  public void visitExportDeclaration(@NotNull CompactExportDeclaration exportDeclaration) {
    visitElement(exportDeclaration);
  }

  /**
   * Visits a module declaration (e.g., {@code module MyModule { ... }}).
   *
   * @param moduleDefinition the module definition element
   */
  public void visitModuleDefinition(@NotNull CompactModuleDefinition moduleDefinition) {
    visitElement(moduleDefinition);
  }

  /**
   * Visits a struct declaration (e.g., {@code struct Point { x: Field, y: Field }}).
   *
   * @param structDefinition the struct definition element
   */
  public void visitStructDefinition(@NotNull CompactStructDefinition structDefinition) {
    visitElement(structDefinition);
  }

  /**
   * Visits an enum declaration (e.g., {@code enum Color { Red, Green, Blue }}).
   *
   * @param enumDefinition the enum definition element
   */
  public void visitEnumDefinition(@NotNull CompactEnumDefinition enumDefinition) {
    visitElement(enumDefinition);
  }

  /**
   * Visits an external contract declaration (e.g., {@code contract Token { ... }}).
   *
   * @param contractDeclaration the external contract declaration element
   */
  public void visitExternalContractDeclaration(@NotNull CompactExternalContractDeclaration contractDeclaration) {
    visitElement(contractDeclaration);
  }

  /**
   * Visits a contract implements declaration (e.g., {@code contract Token implements Interface { ... }}).
   *
   * @param implementsDeclaration the implements declaration element
   */
  public void visitContractImplementsDeclaration(@NotNull CompactContractImplementsDeclaration implementsDeclaration) {
    visitElement(implementsDeclaration);
  }

  /**
   * Visits an external circuit declaration inside an external contract.
   *
   * @param externalCircuit the external circuit declaration element
   */
  public void visitExternalCircuit(@NotNull CompactExternalCircuit externalCircuit) {
    visitElement(externalCircuit);
  }

  /**
   * Visits a type alias declaration (e.g., {@code type Hash = Bytes<32>;}).
   *
   * @param typeDefinition the type alias definition element
   */
  public void visitTypeDefinition(@NotNull CompactTypeDefinition typeDefinition) {
    visitElement(typeDefinition);
  }

  /**
   * Visits a ledger state variable declaration (e.g., {@code ledger balance: Cell<Uint<64>>;}).
   *
   * @param ledgerDeclaration the ledger declaration element
   */
  public void visitLedgerDeclaration(@NotNull CompactLedgerDeclaration ledgerDeclaration) {
    visitElement(ledgerDeclaration);
  }

  /**
   * Visits a witness function declaration (e.g., {@code witness getSecret(): Bytes<32>;}).
   *
   * @param witnessDeclaration the witness declaration element
   */
  public void visitWitnessDeclaration(@NotNull CompactWitnessDeclaration witnessDeclaration) {
    visitElement(witnessDeclaration);
  }

  /**
   * Visits a contract constructor declaration (e.g., {@code constructor(initialSupply: Uint<64>) { ... }}).
   *
   * @param constructorDeclaration the constructor declaration element
   */
  public void visitConstructorDeclaration(@NotNull CompactConstructorDeclaration constructorDeclaration) {
    visitElement(constructorDeclaration);
  }

  /**
   * Visits a circuit function definition (e.g., {@code circuit transfer(to: Address, amount: Uint<64>): Void { ... }}).
   *
   * @param circuitDefinition the circuit definition element
   */
  public void visitCircuitDefinition(@NotNull CompactCircuitDefinition circuitDefinition) {
    visitElement(circuitDefinition);
  }

  /**
   * Visits a base Compact PSI element when no more specific visit callback matches.
   *
   * @param element the base Compact PSI element
   */
  public void visitCompactElement(@NotNull CompactPsiElement element) {
    visitElement(element);
  }
}
