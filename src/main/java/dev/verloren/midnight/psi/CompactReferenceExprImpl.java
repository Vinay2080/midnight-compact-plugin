package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a symbol reference expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#REFERENCE_EXPR}.
 * Implements {@link CompactReferenceExpr} and binds symbol usage to its target declaration:
 * <ul>
 *   <li><b>Reference Binding:</b> Creates a {@link dev.verloren.midnight.reference.CompactValueReference}
 *       via {@link #getReference()}, delegated to {@link CompactPsiUtil#createIdentifierValueReference(PsiElement)}.</li>
 *   <li><b>Target Resolution:</b> Implements {@link #resolve()} by querying the underlying {@link PsiReference#resolve()}.</li>
 *   <li><b>Type Inference &amp; Recursion Safety:</b> Evaluates the semantic type of the referenced declaration
 *       via {@link #getType()}, wrapped in a {@link com.intellij.openapi.util.RecursionGuard} ({@link #TYPE_INFERENCE_GUARD})
 *       to protect against mutual recursion or cyclical definitions.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing and unification.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} during declaration navigation.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection} when flagging unknown symbols.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()}, {@link #getReference()}, or {@link #resolve()}
 *       strictly requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via parent AST mutations.</li>
 * </ul>
 *
 * @see CompactReferenceExpr
 * @see dev.verloren.midnight.reference.CompactValueReference
 * @see CompactPsiUtil#createIdentifierValueReference
 */
public class CompactReferenceExprImpl extends CompactPsiElement implements CompactReferenceExpr {
  /**
   * Thread-safe recursion guard preventing infinite loop stack overflows during reference type inference.
   */
  private static final com.intellij.openapi.util.RecursionGuard<PsiElement> TYPE_INFERENCE_GUARD =
      com.intellij.openapi.util.RecursionManager.createGuard("CompactReferenceExprType");

  /**
   * Constructs a reference expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the reference expression
   */
  public CompactReferenceExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Evaluates the semantic type of the referenced target declaration, guarded against cyclic references.
   *
   * @return the resolved symbol's semantic {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if unresolvable
   */
  @Override
  public @NotNull CompactType getType() {
    CompactType type = TYPE_INFERENCE_GUARD.doPreventingRecursion(this, false, () -> {
      PsiReference ref = getReference();
      if (ref != null) {
        PsiElement resolved = ref.resolve();
        if (resolved instanceof CompactTypeElement typeElement) {
          return typeElement.getType();
        } else if (resolved instanceof CompactExpression expr) {
          return expr.getType();
        }
      }
      return CompactPrimitiveType.UNKNOWN;
    });
    return type != null ? type : CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Creates a value reference bound to the child identifier token of this reference expression.
   *
   * @return the {@link dev.verloren.midnight.reference.CompactValueReference}, or {@code null} if missing identifier
   */
  @Override
  public @Nullable PsiReference getReference() {
    return CompactPsiUtil.createIdentifierValueReference(this);
  }

  /**
   * Directly resolves the target declaration of this reference.
   *
   * @return the resolved {@link PsiElement}, or {@code null} if unresolved
   */
  @Override
  public @Nullable PsiElement resolve() {
    PsiReference ref = getReference();
    return ref != null ? ref.resolve() : null;
  }

  /**
   * Returns an array containing the single reference or empty array.
   *
   * @return array of references; never {@code null}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
