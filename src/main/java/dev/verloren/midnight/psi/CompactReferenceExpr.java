package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;


/**
 * PSI element interface representing a reference expression (symbol identifier usage) in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a variable, parameter, constant, circuit, or witness reference (e.g. {@code balance} or {@code transfer}).
 * Serves as the primary bridge connecting syntax expressions to symbol resolution:
 * <ul>
 *   <li><b>Reference Resolution:</b> Exposes {@link #resolve()} to navigate directly to the target declaration.</li>
 *   <li><b>Type Propagation:</b> Implements {@link CompactExpression#getType()}, propagating the resolved symbol's
 *       type into the enclosing expression or statement.</li>
 *   <li><b>Navigation &amp; Refactoring:</b> Enables "Go to Declaration" (Ctrl+B), Find Usages, and rename refactorings.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} when navigating from references.</li>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression type inference.</li>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} when traversing symbol scopes.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #resolve()} or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via reference targets updates AST leaves under Write Actions on the EDT.</li>
 * </ul>
 *
 * @see CompactReferenceExprImpl
 * @see CompactExpression
 * @see dev.verloren.midnight.reference.CompactValueReference
 */
public interface CompactReferenceExpr extends CompactExpression {

  /**
   * Resolves this reference expression to its target-named declaration.
   *
   * @return the resolved {@link PsiElement} target, or {@code null} if unresolved
   */
  @Nullable PsiElement resolve();
}
