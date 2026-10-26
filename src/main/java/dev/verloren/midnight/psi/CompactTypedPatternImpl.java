package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a typed pattern binding ({@code pattern: Type}) wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TYPED_PATTERN}.
 * Connects a destructuring pattern to an explicit type annotation:
 * <ul>
 *   <li><b>Type Annotation Association:</b> Binds pattern variables (represented by child {@link CompactPatternImpl} nodes)
 *       to their explicit declared {@link CompactTypeElement}.</li>
 *   <li><b>Type Provider:</b> Implements {@link CompactTypeElement#getType()} by locating the non-pattern type element child
 *       and returning its evaluated semantic type.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link CompactPatternImpl#getType()} when evaluating the type of pattern variables.</li>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during parameter type resolution.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactPatternImpl
 * @see CompactTypeElement
 * @see CompactPsiElement
 */
public class CompactTypedPatternImpl extends CompactPsiElement implements CompactTypeElement {
  /**
   * Constructs a typed pattern PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the typed pattern
   */
  public CompactTypedPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Evaluates the declared semantic type of this pattern from its associated type annotation.
   *
   * @return the annotated {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if missing
   */
  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement[] typeElements = PsiTreeUtil.getChildrenOfType(this, CompactTypeElement.class);
    if (typeElements != null) {
      for (CompactTypeElement typeElement : typeElements) {
        if (!(typeElement instanceof CompactPatternImpl)) {
          return typeElement.getType();
        }
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
