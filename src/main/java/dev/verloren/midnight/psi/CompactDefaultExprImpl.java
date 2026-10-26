package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a default value expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#DEFAULT_EXPR}.
 * Represents default initializer expressions of the form {@code default<Type>} in Midnight Compact,
 * producing the canonical zero-initialization or default representation for the given type.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check assignment compatibility.</li>
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
public class CompactDefaultExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a default expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the default expression
   */
  public CompactDefaultExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of this default expression.
   *
   * <p>Finds the child {@link CompactTypeElement} specifying the target type and returns its resolved
   * type. If absent or invalid, returns {@link CompactPrimitiveType#UNKNOWN}.</p>
   *
   * @return the resolved target {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement typeElem = PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
    return typeElem != null ? typeElem.getType() : CompactPrimitiveType.UNKNOWN;
  }
}
