package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * Common base PSI element for all Compact language abstract syntax tree (AST) wrapper nodes.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the root concrete PSI class in the Midnight Compact plugin PSI hierarchy.
 * Instantiated reflectively or via {@link CompactElementFactory#createElement(ASTNode)} by
 * {@link dev.verloren.midnight.parser.CompactParserDefinition#createElement(ASTNode)} whenever
 * the IntelliJ parser converts raw lexer tokens and AST composite nodes into high-level PSI elements.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core platform pipelines:
 * <ul>
 *   <li><b>Tree Construction:</b> {@link com.intellij.psi.impl.source.tree.CompositeElement} during incremental reparsing.</li>
 *   <li><b>Highlighting &amp; Analysis:</b> {@link com.intellij.codeInsight.daemon.DaemonCodeAnalyzer} driving annotators,
 *       inspections, and syntax highlighters.</li>
 *   <li><b>Visitor Dispatch:</b> {@link com.intellij.psi.PsiElementVisitor} and {@link CompactVisitor} during AST inspections.</li>
 *   <li><b>Reference Resolution:</b> {@link com.intellij.psi.PsiReferenceService} and reference search pipelines.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Instantiated and traversed predominantly on pooled background threads (syntax inspection,
 *       indexing, daemon analysis) or the Event Dispatch Thread (EDT) during quick-fixes, navigation, and refactoring actions.</li>
 *   <li><b>Read Action:</b> All query methods (including {@link #getReferences()} and {@link #accept(PsiElementVisitor)})
 *       strictly require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications (e.g. node replacement, deletion) require a Write Action or
 *       {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>ASTWrapperPsiElement Foundation:</b> Inheriting from {@link ASTWrapperPsiElement} provides a lightweight,
 *       GC-friendly wrapper around an underlying native {@link ASTNode}. The PSI wrapper itself holds no heavy state,
 *       preventing memory leaks when files are closed or discarded.</li>
 *   <li><b>Standardized Reference Array:</b> Delegates {@link #getReferences()} to single-reference {@link #getReference()}
 *       if available, eliminating redundant array allocations on elements without custom reference logic.</li>
 * </ul>
 *
 * <h3>Compact PSI Class Hierarchy (ASCII)</h3>
 * <pre>
 *         com.intellij.extapi.psi.ASTWrapperPsiElement
 *                             │
 *                             ▼
 *                     CompactPsiElement
 *                       │           │
 *        ┌──────────────┴───┐       └─────────────────────┐
 *        ▼                  ▼                             ▼
 * CompactNamedElementImpl  CompactBlock          Expression &amp; Statement Impls
 *   (Declarations)        (Scopes)        (BinaryExprImpl, CallExprImpl, etc.)
 * </pre>
 *
 * @see ASTWrapperPsiElement
 * @see CompactVisitor
 * @see CompactElementFactory
 * @see dev.verloren.midnight.parser.CompactParserDefinition
 */
public class CompactPsiElement extends ASTWrapperPsiElement {

  /**
   * Constructs a typed PSI node wrapping the specified AST composite node.
   *
   * @param node the underlying low-level AST node produced by Grammar-Kit / parser
   */
  public CompactPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Retrieves all references originating from this PSI element.
   *
   * <p>Subclasses commonly implement {@link #getReference()} for single-target expressions
   * (e.g. identifiers, member accesses). This method wraps any non-null single reference
   * into a single-element array, falling back to {@link ASTWrapperPsiElement#getReferences()}
   * when no primary reference is defined.</p>
   *
   * @return an array of references associated with this element; never {@code null}
   */
  @Override
  public com.intellij.psi.PsiReference @NotNull [] getReferences() {
    com.intellij.psi.PsiReference ref = getReference();
    return ref == null ? super.getReferences() : new com.intellij.psi.PsiReference[]{ref};
  }

  /**
   * Double-dispatch entry point for standard IntelliJ {@link PsiElementVisitor}.
   *
   * <p>If the visitor is an instance of {@link CompactVisitor}, dispatches to the specialized
   * {@link #accept(CompactVisitor)} method; otherwise delegates to {@link ASTWrapperPsiElement#accept(PsiElementVisitor)}.</p>
   *
   * @param visitor the visitor inspecting this element
   */
  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) {
      accept((CompactVisitor) visitor);
    } else {
      super.accept(visitor);
    }
  }

  /**
   * Double-dispatch entry point for specialized {@link CompactVisitor}.
   *
   * <p>Subclasses override this method to invoke their corresponding {@code visit*()} method
   * on the visitor, enabling type-safe traversal without cascading {@code instanceof} checks.</p>
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCompactElement(this);
  }
}
