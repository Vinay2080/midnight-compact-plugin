package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element representing a delimited lexical code block (`{ ... }`) in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Encloses a sequence of statements, local declarations, and expressions.
 * Serves as the primary lexical scope boundary in the Compact AST:
 * <ul>
 *   <li><b>Lexical Scope Delimitation:</b> Defines the boundary for local variable bindings
 *       ({@link CompactConstBindingImpl}, {@link CompactPatternImpl}). Symbols declared inside a block
 *       are isolated from enclosing or sibling scopes.</li>
 *   <li><b>Search Scope Determination:</b> Used by {@link CompactNamedElementImpl#getUseScope()}
 *       to distinguish block-scoped constants from file-level global constants.</li>
 *   <li><b>Code Formatting &amp; Folding:</b> Anchors brace matching, indentation blocks, and code folding regions
 *       in {@link dev.verloren.midnight.formatting.CompactBlock}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when walking up the PSI tree
 *       to collect local declarations in scope before a reference point.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} to find
 *       in-scope variables during auto-completion.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Scope traversal and child inspection strictly require a platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Block content modifications require a Write Action on the Event Dispatch Thread (EDT).</li>
 * </ul>
 *
 * @see CompactPsiElement
 * @see CompactConstBindingImpl
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public class CompactBlock extends CompactPsiElement {

  /**
   * Constructs a block PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the enclosed block
   */
  public CompactBlock(@NotNull ASTNode node) {
    super(node);
  }
}