package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a pad expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#PAD_EXPR}.
 * Represents byte-padding expressions of the form {@code pad(size, expr)} in Midnight Compact,
 * which pads byte arrays or values to a fixed length {@code Bytes<N>}.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check size argument validity.</li>
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
public class CompactPadExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a pad expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the pad expression
   */
  public CompactPadExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this padding expression.
   *
   * <p>Parses the integer literal size argument if present to construct {@code Bytes<N>};
   * otherwise defaults to untyped {@link CompactPrimitiveType#BYTES}.</p>
   *
   * @return the resolved {@code Bytes<N>} type or {@link CompactPrimitiveType#BYTES}
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] args = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    if (args != null && args.length > 0) {
      String text = args[0].getText().trim();
      try {
        int size = Integer.parseInt(text);
        return new CompactPrimitiveType("Bytes<" + size + ">");
      } catch (NumberFormatException ignored) {
      }
    }
    return CompactPrimitiveType.BYTES;
  }
}
