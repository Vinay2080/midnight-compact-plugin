package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a pattern-bound variable (e.g. in destructuring bindings or pattern matching)
 * wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#PATTERN}.
 * Represents a binding variable introduced via pattern destructuring (e.g. {@code (x, y)} or typed patterns):
 * <ul>
 *   <li><b>Local Scoping:</b> Always scoped to {@link com.intellij.psi.search.LocalSearchScope} of the containing file
 *       via {@link CompactNamedElementImpl#getUseScope()}.</li>
 *   <li><b>Contextual Type Inference:</b> {@link #getType()} evaluates the pattern variable's type across multiple tiers:
 *       <ol>
 *         <li>Direct parent {@link CompactTypedPatternImpl}.</li>
 *         <li>Sibling {@link CompactTypeElement} under an {@code OPTIONALLY_TYPED_PATTERN} container.</li>
 *         <li>Initializer expression of an enclosing {@link CompactConstBindingImpl}.</li>
 *       </ol>
 *   </li>
 *   <li><b>Recursion Protection:</b> Uses {@link com.intellij.openapi.util.RecursionGuard} ({@link #PATTERN_TYPE_GUARD})
 *       to prevent infinite loops when pattern variables reference their own initializers.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when resolving pattern-bound variable references.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} when populating local variables.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} or {@link #getNameIdentifier()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactTypedPatternImpl
 * @see CompactConstBindingImpl
 * @see CompactNamedElementImpl
 */
public class CompactPatternImpl extends CompactNamedElementImpl {
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> PATTERN_TYPE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactPatternType");

  /**
   * Constructs a pattern PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the pattern
   */
  public CompactPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Evaluates the semantic type of this pattern variable with recursion prevention.
   *
   * @return inferred {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if cycle detected or untyped
   */
  @Override
  public @NotNull CompactType getType() {
    CompactType type = PATTERN_TYPE_GUARD.doPreventingRecursion(this, false, this::getTypeInner);
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  private @NotNull CompactType getTypeInner() {
    PsiElement parent = getParent();
    if (parent instanceof CompactTypedPatternImpl) {
      return ((CompactTypedPatternImpl) parent).getType();
    }
    if (parent != null && parent.getNode().getElementType() == CompactElementTypes.OPTIONALLY_TYPED_PATTERN) {
      for (PsiElement child : parent.getChildren()) {
        if (child instanceof CompactTypeElement && child != this) {
          return ((CompactTypeElement) child).getType();
        }
      }
    }

    CompactConstBindingImpl constBinding = PsiTreeUtil.getParentOfType(this, CompactConstBindingImpl.class);
    if (constBinding != null) {
      CompactExpression initializer = constBinding.getInitializer();
      if (initializer != null && !PsiTreeUtil.isAncestor(this, initializer, false)) {
        return initializer.getType();
      }
    }

    return super.getType();
  }

  /**
   * Finds the child identifier leaf token of this pattern.
   *
   * @return identifier PSI leaf, or {@code null} if unparseable
   */
  @Override
  public @Nullable PsiElement getNameIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }
}
