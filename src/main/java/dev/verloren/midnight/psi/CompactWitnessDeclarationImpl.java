package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PSI implementation of a witness function declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#WITNESS_DECLARATION}.
 * Fulfills {@link CompactWitnessDeclaration}:
 * <ul>
 *   <li><b>Parameter Extraction:</b> Delegates parameter discovery to {@link CompactPsiUtil#getParameters(PsiElement)}.</li>
 *   <li><b>Return Type Resolution:</b> Resolves the return type syntax element by locating the trailing {@link CompactTypeElement} child.</li>
 *   <li><b>Semantic Type Provider:</b> Overrides {@link #getType()} to return the semantic return type of the witness.</li>
 *   <li><b>Export Visibility:</b> Queries child tokens for {@link dev.verloren.midnight.lexer.CompactTokenTypes#EXPORT}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitWitnessDeclaration(CompactWitnessDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Querying parameters, type, and visibility requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactWitnessDeclaration
 * @see CompactNamedElementImpl
 * @see CompactPsiUtil#getParameters
 */
public class CompactWitnessDeclarationImpl extends CompactNamedElementImpl implements CompactWitnessDeclaration {

  /**
   * Constructs a witness declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the witness declaration
   */
  public CompactWitnessDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Collects all parameters and pattern-bound arguments of this witness declaration.
   *
   * @return list of parameter named elements; never {@code null}
   */
  @Override
  public @NotNull List<CompactNamedElement> getParameters() {
    return CompactPsiUtil.getParameters(this);
  }

  /**
   * Returns the explicit return type element child.
   *
   * <p>When multiple type elements are present in the signature (e.g., parameter types),
   * the return type is the final type element following the parameter list.</p>
   *
   * @return trailing {@link CompactTypeElement}, or {@code null} if omitted
   */
  @Override
  public @Nullable CompactTypeElement getReturnTypeElement() {
    CompactTypeElement[] typeElements = PsiTreeUtil.getChildrenOfType(this, CompactTypeElement.class);
    if (typeElements != null && typeElements.length > 0) {
      return typeElements[typeElements.length - 1];
    }
    return PsiTreeUtil.getChildOfType(this, CompactTypeElement.class);
  }

  /**
   * Evaluates the semantic return type of this witness function.
   *
   * @return the witness return {@link dev.verloren.midnight.type.CompactType}
   */
  @Override
  public @NotNull dev.verloren.midnight.type.CompactType getType() {
    CompactTypeElement returnType = getReturnTypeElement();
    if (returnType != null) {
      return returnType.getType();
    }
    return super.getType();
  }

  /**
   * Checks whether this witness is declared with the {@code export} modifier.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  @Override
  public boolean isExported() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.EXPORT) != null;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitWitnessDeclaration(CompactWitnessDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitWitnessDeclaration(this);
  }
}

