package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a tuple/vector literal expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TUPLE_EXPR}.
 * Represents comma-separated collections enclosed in brackets (e.g. {@code [a, b, c]}).
 * Infers fixed-size vector types formatted as {@code Vector<N, T>}, where {@code N} is the literal item
 * count and {@code T} is the type of the elements.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during vector and array expression checking.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to validate element compatibility.</li>
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
 * @see CompactPrimitiveType
 */
public class CompactTupleExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a tuple expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the tuple expression
   */
  public CompactTupleExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this tuple/vector expression.
   *
   * <p>Finds all child {@link CompactExpression} elements. If elements are present and the first item
   * resolves to a valid type, returns a {@link CompactPrimitiveType} representing {@code Vector<count, itemType>}.
   * If empty or untyped, returns {@link CompactPrimitiveType#UNKNOWN}.</p>
   *
   * @return the inferred {@code Vector<N, T>} type or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] items = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    int count = items != null ? items.length : 0;
    if (count > 0) {
      CompactType itemType = items[0].getType();
      if (!CompactPrimitiveType.UNKNOWN.equals(itemType)) {
        return new CompactPrimitiveType("Vector<" + count + ", " + itemType.name() + ">");
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
