package dev.verloren.midnight.type;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Static evaluator for inferring the semantic types of Compact expressions.
 *
 * <p>Determines resulting types for binary expressions (logical, comparison, arithmetic)
 * and unary expressions (logical NOT, negation).</p>
 */
public class CompactTypeInferenceUtil {
  /**
   * Infers the result type of a binary expression {@code left op right}.
   *
   * @param left     left operand expression
   * @param operator binary operator token type
   * @param right    right operand expression
   * @return inferred result type (e.g. {@link CompactPrimitiveType#BOOLEAN} for comparisons)
   */
  public static @NotNull CompactType inferBinaryExprType(@NotNull CompactExpression left, @NotNull IElementType operator, @NotNull CompactExpression right) {
    CompactType leftType = left.getType();

    // Logical operators return Boolean
    if (operator == CompactTokenTypes.ANDAND || operator == CompactTokenTypes.OROR) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // Comparison operators return Boolean
    if (operator == CompactTokenTypes.EQEQ || operator == CompactTokenTypes.NEQ ||
            operator == CompactTokenTypes.LT || operator == CompactTokenTypes.LTE ||
            operator == CompactTokenTypes.GT || operator == CompactTokenTypes.GTE) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // Arithmetic operators return the type of the operands (assuming they match for now)
    if (operator == CompactTokenTypes.PLUS || operator == CompactTokenTypes.MINUS ||
            operator == CompactTokenTypes.STAR || operator == CompactTokenTypes.SLASH ||
            operator == CompactTokenTypes.PERCENT) {
      CompactType rightType = right.getType();
      if (leftType instanceof CompactNumericLiteralType && !(rightType instanceof CompactNumericLiteralType) && !CompactPrimitiveType.UNKNOWN.equals(rightType)) {
        return rightType;
      }
      return leftType;
    }

    return CompactPrimitiveType.UNKNOWN;
  }

  public static @NotNull CompactType inferUnaryExprType(@NotNull IElementType operator, @NotNull CompactExpression operand) {
    if (operator == CompactTokenTypes.NOT) {
      return CompactPrimitiveType.BOOLEAN;
    }
    if (operator == CompactTokenTypes.MINUS) {
      return operand.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
