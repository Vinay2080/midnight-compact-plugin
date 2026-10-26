package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a Compact {@code export ...} statement wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#EXPORT_FORM}.
 * Implements {@link CompactExportDeclaration}:
 * <ul>
 *   <li><b>Export Wrapper:</b> Encapsulates exported declarations or export identifier lists.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitExportDeclaration(CompactExportDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query operations require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactExportDeclaration
 * @see CompactPsiElement
 */
public class CompactExportDeclarationImpl extends CompactPsiElement implements CompactExportDeclaration {

  /**
   * Constructs an export declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the export statement
   */
  public CompactExportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitExportDeclaration(CompactExportDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExportDeclaration(this);
  }
}