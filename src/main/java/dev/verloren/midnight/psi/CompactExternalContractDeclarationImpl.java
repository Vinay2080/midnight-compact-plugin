package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PSI implementation of an external contract declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CONTRACT_DECLARATION}.
 * Fulfills {@link CompactExternalContractDeclaration}:
 * <ul>
 *   <li><b>Child Circuit Extraction:</b> Discovers all {@link CompactExternalCircuit} children via
 *       {@link #getCircuits()} using {@link PsiTreeUtil#getChildrenOfTypeAsList}.</li>
 *   <li><b>Icon &amp; Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering a class icon
 *       in navigation popups and Structure View.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitExternalContractDeclaration(CompactExternalContractDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getCircuits()} or querying declarations strictly requires
 *       an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications or renaming require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactExternalContractDeclaration
 * @see CompactExternalCircuit
 * @see CompactNamedElementImpl
 */
public class CompactExternalContractDeclarationImpl extends CompactNamedElementImpl implements CompactExternalContractDeclaration {

  /**
   * Constructs an external contract declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the external contract declaration
   */
  public CompactExternalContractDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Returns all circuit declarations defined directly inside this contract interface.
   *
   * @return list of external circuit children; never {@code null}
   */
  @Override
  public @NotNull List<CompactExternalCircuit> getCircuits() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExternalCircuit.class);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitExternalContractDeclaration(CompactExternalContractDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalContractDeclaration(this);
  }
}
