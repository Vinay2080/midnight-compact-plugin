package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI implementation of a contract constructor declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CONSTRUCTOR_DEFINITION}.
 * Fulfills {@link CompactConstructorDeclaration}:
 * <ul>
 *   <li><b>Parameter Collection:</b> Collects constructor parameters and pattern variables via {@link #getParameters()}.</li>
 *   <li><b>Body Block:</b> Locates the initialization block via {@link #getBody()}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitConstructorDeclaration(CompactConstructorDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getParameters()} or {@link #getBody()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactConstructorDeclaration
 * @see CompactPsiElement
 * @see CompactPsiUtil#hasAncestorOfType
 */
public class CompactConstructorDeclarationImpl extends CompactPsiElement implements CompactConstructorDeclaration {

  /**
   * Constructs a constructor declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the constructor
   */
  public CompactConstructorDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Collects all parameters and destructuring pattern variables declared for this constructor.
   *
   * @return list of parameter named elements; never {@code null}
   */
  @Override
  public @NotNull List<CompactNamedElement> getParameters() {
    List<CompactNamedElement> params = new ArrayList<>();
    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class)) {
      if (named instanceof CompactParameterImpl || isPatternParameter(named)) {
        params.add(named);
      }
    }
    return params;
  }

  /**
   * Checks whether a named pattern is part of a parameter list.
   *
   * @param declaration the candidate pattern element
   * @return {@code true} if nested within a parameter list; {@code false} otherwise
   */
  private static boolean isPatternParameter(@NotNull CompactNamedElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
            || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  /**
   * Checks whether an element has an ancestor matching the given AST element type.
   *
   * @param element the starting element
   * @param type the ancestor AST element type
   * @return {@code true} if matching ancestor exists; {@code false} otherwise
   */
  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    return CompactPsiUtil.hasAncestorOfType(element, type);
  }

  /**
   * Returns the constructor body block.
   *
   * @return the {@link CompactBlock} body, or {@code null} if omitted
   */
  @Override
  public @Nullable CompactBlock getBody() {
    return PsiTreeUtil.findChildOfType(this, CompactBlock.class);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitConstructorDeclaration(CompactConstructorDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitConstructorDeclaration(this);
  }
}