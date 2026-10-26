package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a parenthesized expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#PAREN_EXPR}.
 * Serves as a transparent syntactic wrapper around an inner expression (e.g. {@code (a + b)}),
 * controlling precedence without introducing a distinct semantic type.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} when evaluating grouped expressions.</li>
 *   <li>Inspection and intention passes that unwrap parentheses or evaluate inner terms.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactExpression
 */
public class CompactParenExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a parenthesized expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the parenthesized expression
   */
  public CompactParenExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this expression by unwrapping the enclosed inner expression.
   *
   * @return the {@link CompactType} of the enclosed child expression, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return ((CompactExpression) child).getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
