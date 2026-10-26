package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactTypeReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a struct literal instantiation expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#STRUCT_LITERAL_EXPR}.
 * Represents named struct instantiations (e.g. {@code Point { x: 1, y: 2 }} or generic {@code Box<Field> { value: 42 }}).
 * Binds a {@link CompactTypeReference} to the struct type identifier and resolves the instantiated type.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing and struct field validation.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to check field initializers against struct definition.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()}, {@link #getReference()}, and {@link #getReferences()} requires
 *       an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactExpression
 * @see CompactTypeReference
 * @see CompactStructDefinition
 */
public class CompactStructLiteralExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a struct literal expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the struct literal
   */
  public CompactStructLiteralExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} instantiated by this struct literal.
   *
   * <p>If generic type arguments are appended to the identifier, combines them into a parameterized type;
   * otherwise resolves the target struct definition or returns the nominal identifier type.</p>
   *
   * @return the instantiated struct {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    ASTNode genArgs = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.GENERIC_ARGUMENT_LIST);
    if (identifier != null) {
      if (genArgs != null) {
        return new CompactPrimitiveType(identifier.getText() + genArgs.getText());
      }
      PsiReference ref = getReference();
      if (ref != null) {
        PsiElement resolved = ref.resolve();
        if (resolved instanceof CompactTypeElement typeElement) {
          return typeElement.getType();
        }
      }
      return new CompactPrimitiveType(identifier.getText());
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Creates a reference to the struct definition denoted by the identifier.
   *
   * @return a {@link CompactTypeReference} pointing to the struct declaration, or {@code null} if no identifier exists
   */
  @Override
  public @Nullable PsiReference getReference() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    if (identifier == null) {
      return null;
    }
    PsiElement psi = identifier.getPsi();
    int start = psi.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactTypeReference(this, TextRange.from(start, psi.getTextLength()));
  }

  /**
   * Returns all PSI references attached to this struct literal expression.
   *
   * @return an array containing the struct type reference, or {@link PsiReference#EMPTY_ARRAY}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
