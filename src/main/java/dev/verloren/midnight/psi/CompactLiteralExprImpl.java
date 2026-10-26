package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactNumericLiteralType;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a constant literal expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#LITERAL_EXPR}.
 * Represents compile-time literal values:
 * <ul>
 *   <li><b>Boolean Literals:</b> Maps {@link CompactTokenTypes#TRUE} and {@link CompactTokenTypes#FALSE}
 *       to {@link CompactPrimitiveType#BOOLEAN}.</li>
 *   <li><b>Polymorphic Numeric Literals:</b> Maps numeric tokens (decimal, hex, binary in {@link CompactTokenSets#NAT_LITERALS})
 *       to {@link CompactNumericLiteralType}, enabling bidirectional unification with {@code Uint<N>} or {@code Field}.</li>
 *   <li><b>String Literals:</b> Maps {@link CompactTokenTypes#STRING_LITERAL} to {@link CompactPrimitiveType#BYTES}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during literal type checking and constant folding.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} during operand validation.</li>
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
 * @see CompactNumericLiteralType
 * @see CompactPrimitiveType
 */
public class CompactLiteralExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a literal expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the literal
   */
  public CompactLiteralExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this literal expression based on its underlying token.
   *
   * <p>Resolution rules:
   * <ul>
   *   <li>{@code true} / {@code false} &rarr; {@link CompactPrimitiveType#BOOLEAN}</li>
   *   <li>Numeric literals (decimal, hexadecimal, binary) &rarr; {@link CompactNumericLiteralType} preserving source text</li>
   *   <li>String literals &rarr; {@link CompactPrimitiveType#BYTES}</li>
   *   <li>Unrecognized or empty AST nodes &rarr; {@link CompactPrimitiveType#UNKNOWN}</li>
   * </ul>
   * </p>
   *
   * @return the inferred {@link CompactType}, never {@code null}
   */
  @Override
  public @NotNull CompactType getType() {
    ASTNode first = getNode().getFirstChildNode();
    if (first == null) return CompactPrimitiveType.UNKNOWN;

    IElementType type = first.getElementType();
    if (type == CompactTokenTypes.TRUE || type == CompactTokenTypes.FALSE) {
      return CompactPrimitiveType.BOOLEAN;
    }
    if (CompactTokenSets.NAT_LITERALS.contains(type)) {
      return new CompactNumericLiteralType(getText());
    }
    if (type == CompactTokenTypes.STRING_LITERAL) {
      return CompactPrimitiveType.BYTES;
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
