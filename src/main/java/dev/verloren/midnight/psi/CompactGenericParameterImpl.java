package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a generic type parameter declaration (e.g. {@code T} in {@code struct List<T> { ... }}).
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#GENERIC_PARAMETER}.
 * Represents a type variable placeholder:
 * <ul>
 *   <li><b>Local Scoping:</b> Restricted to the containing file's {@link com.intellij.psi.search.LocalSearchScope}
 *       via {@link CompactNamedElementImpl#getUseScope()}.</li>
 *   <li><b>Fallback Identifier:</b> Overrides {@link #getNameIdentifier()} to safely fall back to {@code this}
 *       if the leaf identifier node cannot be cleanly separated from the parameter composite node.</li>
 *   <li><b>Generic Substitution:</b> Used by generic type resolution algorithms in
 *       {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#buildGenericSubstitution}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getNameIdentifier()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactNamedElementImpl
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public class CompactGenericParameterImpl extends CompactNamedElementImpl {

  /**
   * Constructs a generic parameter PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the generic type parameter
   */
  public CompactGenericParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Returns the child identifier token or falls back to this element if no separate child token is found.
   *
   * @return the identifier PSI element; never {@code null}
   */
  @Override
  public @Nullable PsiElement getNameIdentifier() {
    PsiElement id = findChildByType(CompactTokenTypes.IDENTIFIER);
    return id != null ? id : this;
  }
}