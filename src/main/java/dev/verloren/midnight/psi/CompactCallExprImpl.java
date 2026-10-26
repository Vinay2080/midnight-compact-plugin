package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a call/invocation expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#CALL_EXPR}.
 * Represents invocations of circuits, witnesses, functions, or type constructors (e.g. {@code transfer(to, amount)}
 * or {@code genericCirc<T>(arg)}). Dispatches callee symbol resolution and performs generic type argument substitution.</p>
 *
 * <h3>Generic Substitution Pipeline</h3>
 * <p>When evaluating {@link #getType()}:
 * <ol>
 *   <li>Resolves the callee reference through {@link #getCalleeReference()}.</li>
 *   <li>Determines the raw return/declared type of the resolved target (e.g. {@link CompactCircuitDefinition},
 *       {@link CompactWitnessDeclaration}, or {@link CompactTypeElement}).</li>
 *   <li>If type arguments are supplied via {@link dev.verloren.midnight.parser.CompactElementTypes#GENERIC_ARGUMENT_LIST},
 *       constructs a parameter-to-argument map via {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#buildGenericSubstitution}
 *       and specializes the return type via {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#substituteGenerics}.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()}, {@link #getCalleeReference()}, {@link #resolveCallee()},
 *       and reference methods requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactExpression
 * @see CompactReferenceExprImpl
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public class CompactCallExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a call expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the call expression
   */
  public CompactCallExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static return {@link CompactType} produced by invoking this call expression.
   *
   * <p>Resolves the callee via {@link #getCalleeReference()}, determines its declared type
   * across circuit, witness, or named declarations, and applies generic substitutions if generic
   * arguments are present.</p>
   *
   * @return the resolved or specialized {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getCalleeReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      CompactType returnType = null;
      if (resolved instanceof CompactCircuitDefinition circuit) {
        returnType = circuit.getType();
      } else if (resolved instanceof CompactWitnessDeclaration witness) {
        returnType = witness.getType();
      } else if (resolved instanceof CompactTypeElement typeElement) {
        returnType = typeElement.getType();
      } else if (resolved instanceof CompactNamedElement named) {
        returnType = named.getType();
      }

      if (returnType != null) {
        ASTNode genArgsNode = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.GENERIC_ARGUMENT_LIST);
        if (genArgsNode != null && resolved != null) {
          java.util.List<String> genericArgs = dev.verloren.midnight.type.CompactTypeInferenceUtil.parseGenericArgs(genArgsNode.getText());
          java.util.Map<String, String> substitution = dev.verloren.midnight.type.CompactTypeInferenceUtil.buildGenericSubstitution(resolved, genericArgs);
          String substitutedName = dev.verloren.midnight.type.CompactTypeInferenceUtil.substituteGenerics(returnType.name(), substitution);
          return new CompactPrimitiveType(substitutedName);
        }
        return returnType;
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Returns the PSI reference pointing to the called target element.
   *
   * <p>Checks if this call contains a child {@link CompactReferenceExprImpl} and uses its reference;
   * otherwise falls back to the reference defined directly on this node.</p>
   *
   * @return the callee {@link PsiReference}, or {@code null} if unresolvable
   */
  public @Nullable PsiReference getCalleeReference() {
    CompactReferenceExprImpl refExpr = com.intellij.psi.util.PsiTreeUtil.getChildOfType(this, CompactReferenceExprImpl.class);
    if (refExpr != null) {
      return refExpr.getReference();
    }
    return getReference();
  }

  /**
   * Resolves the target callee declaration directly.
   *
   * @return the resolved {@link PsiElement} declaration, or {@code null} if unresolved
   */
  public @Nullable PsiElement resolveCallee() {
    PsiReference ref = getCalleeReference();
    return ref != null ? ref.resolve() : null;
  }

  /**
   * Creates an identifier-based value reference bound to this call expression.
   *
   * @return a {@link PsiReference} created via {@link CompactPsiUtil#createIdentifierValueReference}, or {@code null}
   */
  @Override
  public @Nullable PsiReference getReference() {
    return CompactPsiUtil.createIdentifierValueReference(this);
  }

  /**
   * Returns all PSI references attached to this call expression.
   *
   * @return an array containing the primary reference, or {@link PsiReference#EMPTY_ARRAY}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
