package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of an external circuit declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#EXTERNAL_CIRCUIT}.
 * Fulfills {@link CompactExternalCircuit}:
 * <ul>
 *   <li><b>Purity Flag:</b> Inspects child AST tokens for {@link CompactTokenTypes#PURE} via {@link #isPure()}.</li>
 *   <li><b>Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering a Method icon in Structure View.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitExternalCircuit(CompactExternalCircuit)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #isPure()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactExternalCircuit
 * @see CompactExternalContractDeclaration
 * @see CompactNamedElementImpl
 */
public class CompactExternalCircuitImpl extends CompactNamedElementImpl implements CompactExternalCircuit {

  /**
   * Constructs an external circuit PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the external circuit signature
   */
  public CompactExternalCircuitImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Checks whether this circuit signature has the {@code pure} keyword token.
   *
   * @return {@code true} if marked pure; {@code false} otherwise
   */
  @Override
  public boolean isPure() {
    return getNode().findChildByType(CompactTokenTypes.PURE) != null;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitExternalCircuit(CompactExternalCircuit)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalCircuit(this);
  }
}
