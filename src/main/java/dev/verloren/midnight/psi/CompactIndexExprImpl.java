package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of an index access expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#INDEX_EXPR}.
 * Represents indexing operations (e.g. {@code vector[index]}) on composite vector types.</p>
 *
 * <h3>Type Extraction</h3>
 * <p>Extracts the base expression type:
 * <ul>
 *   <li>If the base type matches {@code Vector<N, T>}, extracts and returns {@code T}.</li>
 *   <li>Otherwise returns {@link CompactPrimitiveType#UNKNOWN}.</li>
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
public class CompactIndexExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs an index expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the index expression
   */
  public CompactIndexExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the element {@link CompactType} obtained from indexing into the base expression.
   *
   * @return the unwrapped vector element type, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression[] exprs = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
    if (exprs != null && exprs.length > 0) {
      CompactType baseType = exprs[0].getType();
      String name = baseType.name();
      if (name.startsWith("Vector<") && name.endsWith(">")) {
        int commaIdx = name.indexOf(',');
        if (commaIdx != -1) {
          String elemTypeName = name.substring(commaIdx + 1, name.length() - 1).trim();
          return new CompactPrimitiveType(elemTypeName);
        }
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
