package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.List;

/**
 * PSI implementation of a circuit definition wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CIRCUIT_DEFINITION}.
 * Fulfills {@link CompactCircuitDefinition}:
 * <ul>
 *   <li><b>Parameter Extraction:</b> Delegates parameter discovery to {@link CompactPsiUtil#getParameters(PsiElement)}.</li>
 *   <li><b>Return Type Resolution:</b> Resolves the return type syntax element by locating the trailing {@link CompactTypeElement} child.</li>
 *   <li><b>Semantic Type Provider:</b> Overrides {@link #getType()} to return the semantic return type of the circuit.</li>
 *   <li><b>Modifier Flags:</b> Queries child AST tokens for {@link dev.verloren.midnight.lexer.CompactTokenTypes#PURE}
 *       and {@link dev.verloren.midnight.lexer.CompactTokenTypes#EXPORT}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitCircuitDefinition(CompactCircuitDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Parameter queries, type extraction, and body retrieval require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactCircuitDefinition
 * @see CompactNamedElementImpl
 * @see CompactPsiUtil#getParameters
 */
public class CompactCircuitDefinitionImpl extends CompactNamedElementImpl implements CompactCircuitDefinition {

  /**
   * Constructs a circuit definition PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the circuit definition
   */
  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Collects all parameters and destructuring pattern variables declared for this circuit.
   *
   * @return list of parameter named elements; never {@code null}
   */
  @Override
  public @NotNull List<CompactNamedElement> getParameters() {
    return CompactPsiUtil.getParameters(this);
  }

  /**
   * Retrieves the body block of this circuit.
   *
   * @return the {@link CompactBlock} body, or {@code null} if omitted or incomplete
   */
  @Override
  public @Nullable CompactBlock getBody() {
    return PsiTreeUtil.findChildOfType(this, CompactBlock.class);
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
   * Evaluates the semantic return type of this circuit.
   *
   * @return the circuit's return {@link dev.verloren.midnight.type.CompactType}
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
   * Checks whether this circuit is marked with the {@code pure} keyword.
   *
   * @return {@code true} if marked pure; {@code false} otherwise
   */
  @Override
  public boolean isPure() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.PURE) != null;
  }

  /**
   * Checks whether this circuit is marked with the {@code export} keyword.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  @Override
  public boolean isExported() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.EXPORT) != null;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitCircuitDefinition(CompactCircuitDefinition)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCircuitDefinition(this);
  }
}

