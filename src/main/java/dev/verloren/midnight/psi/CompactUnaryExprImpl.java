package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a unary expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#UNARY_EXPR}.
 * Represents prefix unary operations (e.g. arithmetic negation {@code -}, bitwise inversion {@code ~},
 * logical negation {@code !}) applied to a single operand expression.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil#inferUnaryExprType} to compute operator-specific result types.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check operand validity.</li>
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
public class CompactUnaryExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a unary expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the unary expression
   */
  public CompactUnaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this unary expression.
   *
   * <p>Locates the single operand child expression and the operator token belonging to
   * {@link dev.verloren.midnight.lexer.CompactTokenSets#OPERATORS}, delegating evaluation to
   * {@link CompactTypeInferenceUtil#inferUnaryExprType}.</p>
   *
   * @return the computed result {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if operand/operator is missing
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression operand = null;
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        operand = (CompactExpression) child;
        break;
      }
    }
    if (operand == null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    ASTNode operatorNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
    if (operatorNode == null) {
      return CompactPrimitiveType.UNKNOWN;
    }

    return CompactTypeInferenceUtil.inferUnaryExprType(operatorNode.getElementType(), operand);
  }
}
