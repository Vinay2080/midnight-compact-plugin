package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * PSI implementation of a Compact module definition wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#MODULE_DEFINITION}.
 * Provides concrete implementation for {@link CompactModuleDefinition}:
 * <ul>
 *   <li><b>Named Declaration Contract:</b> Inherits name resolution, renaming, and presentation
 *       from {@link CompactNamedElementImpl}, rendering a module node icon in Structure View.</li>
 *   <li><b>Member Resolution:</b> Collects all nested {@link CompactNamedElement} children via
 *       {@link #getMembers()} for qualified name resolution.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitModuleDefinition(CompactModuleDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Member retrieval and AST traversal strictly require a platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action wrapped in a
 *       {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * @see CompactModuleDefinition
 * @see CompactNamedElementImpl
 */
public class CompactModuleDefinitionImpl extends CompactNamedElementImpl implements CompactModuleDefinition {

  /**
   * Constructs a module definition PSI node wrapping the AST composite node.
   *
   * @param node the AST composite node representing the module
   */
  public CompactModuleDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitModuleDefinition(CompactModuleDefinition)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitModuleDefinition(this);
  }

  /**
   * Finds and returns all named declarations situated within this module's body.
   *
   * @return collection of child {@link CompactNamedElement} declarations; never {@code null}
   */
  @Override
  public @NotNull Collection<CompactNamedElement> getMembers() {
    return PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class);
  }
}