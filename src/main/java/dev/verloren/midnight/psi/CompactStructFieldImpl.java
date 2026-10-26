package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a struct field declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#STRUCT_FIELD}.
 * Represents an individual named field within a {@link CompactStructDefinition}:
 * <ul>
 *   <li><b>Resilient Identifier Resolution:</b> {@link #getNameIdentifier()} checks multiple AST representations
 *       (direct identifier child, nested {@link CompactParameterImpl}, or low-level leaf token) to ensure reliable
 *       naming even during incomplete syntax states.</li>
 *   <li><b>Field Type Extraction:</b> {@link #getType()} queries the nested {@link CompactTypeElement} to return
 *       the field's declared semantic type.</li>
 *   <li><b>Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering a Field node icon.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.reference.CompactStructFieldReference} when resolving dot-access expressions (e.g. {@code user.name}).</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactMemberCompletionProvider} when populating available struct fields.</li>
 *   <li>{@link dev.verloren.midnight.documentation.CompactDocumentationProvider} when generating field quick documentation.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getNameIdentifier()} or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactStructDefinition
 * @see CompactNamedElementImpl
 * @see dev.verloren.midnight.reference.CompactStructFieldReference
 */
public class CompactStructFieldImpl extends CompactNamedElementImpl {

  /**
   * Constructs a struct field PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the struct field
   */
  public CompactStructFieldImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resiliently finds the identifier leaf element of this struct field across varying AST structures.
   *
   * @return the identifier PSI leaf, or {@code null} if missing
   */
  @Override
  public @Nullable com.intellij.psi.PsiElement getNameIdentifier() {
    // Step 1: Attempt standard identifier resolution from CompactNamedElementImpl
    com.intellij.psi.PsiElement direct = super.getNameIdentifier();
    if (direct != null) {
      return direct;
    }
    // Step 2: Fall back to nested CompactParameterImpl (typed identifier wrapper)
    CompactParameterImpl typedId = PsiTreeUtil.findChildOfType(this, CompactParameterImpl.class);
    if (typedId != null) {
      return typedId.getNameIdentifier();
    }
    // Step 3: Fall back to raw IDENTIFIER token search in immediate AST children
    ASTNode idNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.IDENTIFIER);
    return idNode != null ? idNode.getPsi() : null;
  }

  /**
   * Returns the declared semantic type of this struct field.
   *
   * @return the field's declared {@link CompactType}, or {@link dev.verloren.midnight.type.CompactPrimitiveType#UNKNOWN} if unspecified
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