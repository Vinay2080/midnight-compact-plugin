package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a typed parameter declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TYPED_ID}.
 * Represents a formal parameter in circuits, witnesses, constructors, or arrow functions (e.g. {@code to: Address}):
 * <ul>
 *   <li><b>Local Scoping:</b> Scoped to {@link com.intellij.psi.search.LocalSearchScope} of the containing file
 *       via {@link CompactNamedElementImpl#getUseScope()}, optimizing Find Usages and refactorings.</li>
 *   <li><b>Type Provider:</b> Extracts the parameter's semantic type via {@link #getType()} by inspecting
 *       the child {@link CompactTypeElement}.</li>
 *   <li><b>Renaming &amp; Navigation:</b> Fulfills {@link CompactNamedElement}, enabling in-place parameter renaming.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during parameter scope resolution.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} when suggesting in-scope parameters.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactNamedElementImpl
 * @see CompactTypeElement
 * @see CompactCircuitDefinition
 */
public class CompactParameterImpl extends CompactNamedElementImpl {

  /**
   * Constructs a parameter PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the typed parameter
   */
  public CompactParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Computes the semantic {@link CompactType} of this parameter by inspecting its type annotation child.
   *
   * @return the parameter's declared type, or {@link dev.verloren.midnight.type.CompactPrimitiveType#UNKNOWN} if omitted
   */
  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
    if (typeElement != null) {
      return typeElement.getType();
    }
    return super.getType();
  }
}