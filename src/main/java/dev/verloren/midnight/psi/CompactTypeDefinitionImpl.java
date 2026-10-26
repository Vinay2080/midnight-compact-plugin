package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a Compact type alias declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TYPE_ALIAS_DECLARATION}.
 * Fulfills {@link CompactTypeDefinition}:
 * <ul>
 *   <li><b>Target Type Element Extraction:</b> Locates the RHS {@link CompactTypeElement} via
 *       {@link #getTargetTypeElement()}.</li>
 *   <li><b>Semantic Type Evaluation:</b> Overrides {@link #getType()} to return the semantic type of the aliased
 *       type reference child, allowing callers to transparently inspect the underlying type.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitTypeDefinition(CompactTypeDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} or {@link #getTargetTypeElement()} requires an active
 *       platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactTypeDefinition
 * @see CompactNamedElementImpl
 * @see CompactTypeReferenceImpl
 */
public class CompactTypeDefinitionImpl extends CompactNamedElementImpl implements CompactTypeDefinition {

  /**
   * Constructs a type alias declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the type alias
   */
  public CompactTypeDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Locates the RHS type element child specifying the aliased type.
   *
   * @return the target {@link CompactTypeElement}, or {@code null} if missing
   */
  @Override
  public @Nullable CompactTypeElement getTargetTypeElement() {
    return PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
  }

  /**
   * Computes the semantic {@link CompactType} by evaluating the underlying type reference child.
   *
   * @return resolved semantic type, or {@link CompactPrimitiveType#UNKNOWN} if unresolvable
   */
  @Override
  public @NotNull CompactType getType() {
    CompactTypeReferenceImpl typeRef = PsiTreeUtil.findChildOfType(this, CompactTypeReferenceImpl.class);
    if (typeRef != null) {
      return typeRef.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitTypeDefinition(CompactTypeDefinition)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTypeDefinition(this);
  }
}