package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a binary expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#BINARY_EXPR}.
 * Represents infix binary operations (e.g. arithmetic {@code +}, {@code -}, bitwise {@code &amp;},
 * logical {@code &amp;&amp;}, relational {@code &lt;}, {@code ==}) between two operand expressions.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil#inferBinaryExprType} to compute operator-specific result types.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check operand type compatibility.</li>
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
 * @see CompactTypeInferenceUtil
 */
public class CompactBinaryExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a binary expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the binary expression
   */
  public CompactBinaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this binary expression.
   *
   * <p>Locates the left and right operand expressions and the operator token belonging to
   * {@link dev.verloren.midnight.lexer.CompactTokenSets#OPERATORS}, delegating evaluation to
   * {@link CompactTypeInferenceUtil#inferBinaryExprType}.</p>
   *
   * @return the computed result {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if operands/operator are missing
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] children = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    if (children == null || children.length < 2) {
      return CompactPrimitiveType.UNKNOWN;
    }

    ASTNode operatorNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
    if (operatorNode == null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    return CompactTypeInferenceUtil.inferBinaryExprType(children[0], operatorNode.getElementType(), children[1]);
  }
}
