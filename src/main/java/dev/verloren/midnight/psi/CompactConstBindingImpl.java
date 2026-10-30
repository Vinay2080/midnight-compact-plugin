package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a constant binding declaration ({@code const x = expr;}) wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CONST_BINDING}.
 * Represents either a file-level constant or a local block-scoped constant binding:
 * <ul>
 *   <li><b>Context-Aware Search Scoping:</b> {@link CompactNamedElementImpl#getUseScope()} inspects
 *       whether this constant is enclosed in a {@link CompactBlock}; if so, it scopes usages strictly to
 *       {@link com.intellij.psi.search.LocalSearchScope}, skipping global project indexes during Find Usages.</li>
 *   <li><b>Type Inference from Initializer:</b> {@link #getType()} infers the constant's semantic type from
 *       its RHS initializer expression via {@link #getInitializer()}.</li>
 *   <li><b>Recursion Safety Guard:</b> Employs an IntelliJ {@link com.intellij.openapi.util.RecursionGuard}
 *       ({@link #CONST_BINDING_TYPE_GUARD}) to prevent stack overflow errors during cyclic or recursive definitions
 *       (such as {@code const x = x;}).</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when resolving references to variables/constants.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} when suggesting constants in scope.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} or {@link #getInitializer()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactNamedElementImpl
 * @see CompactExpression
 * @see CompactBlock
 */
public class CompactConstBindingImpl extends CompactNamedElementImpl {

  /**
   * Thread-safe recursion guard preventing infinite loop stack overflows during cyclic type inference.
   */
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> CONST_BINDING_TYPE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactConstBindingType");

  /**
   * Constructs a constant binding PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the const binding
   */
  public CompactConstBindingImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Evaluates the semantic type of this constant by analyzing its initializer expression, guarded against cyclic recursion.
   *
   * @return the inferred {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if cycle detected or initializer missing
   */
  @Override
  public @NotNull CompactType getType() {
    CompactType type = CONST_BINDING_TYPE_GUARD.doPreventingRecursion(this, false, () -> {
      CompactExpression expr = getInitializer();
      if (expr != null) {
        return expr.getType();
      }
      return CompactPrimitiveType.UNKNOWN;
    });
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Returns the RHS initializer expression assigned to this constant.
   *
   * @return the {@link CompactExpression} initializer, or {@code null} if missing (incomplete syntax)
   */
  public CompactExpression getInitializer() {
    return PsiTreeUtil.findChildOfType(this, CompactExpression.class);
  }
}
