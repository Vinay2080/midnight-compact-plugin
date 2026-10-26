package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a type cast expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CAST_EXPR}.
 * Represents explicit type casting operations (e.g. {@code expr as Field}) in Midnight Compact.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check cast legality.</li>
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
 * @see CompactTypeElement
 */
public class CompactCastExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a cast expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the cast expression
   */
  public CompactCastExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the target static {@link CompactType} to which the operand is cast.
   *
   * <p>Scans children in reverse order to find the target {@link CompactTypeElement} and returns its type.</p>
   *
   * @return the target {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    PsiElement[] children = getChildren();
    for (int i = children.length - 1; i >= 0; i--) {
      if (children[i] instanceof CompactTypeElement typeElem) {
        return typeElem.getType();
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
