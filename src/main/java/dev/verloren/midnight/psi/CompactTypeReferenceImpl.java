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
 * PSI implementation of a type reference element wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TYPE_REFERENCE}.
 * Represents references to user-defined types (structs, enums, type aliases) or generic type parameters
 * occurring in type signatures, parameter annotations, and return type specifications.</p>
 *
 * <h3>Type Resolution Pipeline</h3>
 * <p>When evaluating {@link #getType()}:
 * <ol>
 *   <li>If parameterized with generic brackets (e.g. {@code Box<Field>}), treats as a parameterized type literal.</li>
 *   <li>Resolves the type identifier via {@link #getReference()} and delegates to the target {@link CompactTypeElement}.</li>
 *   <li>Falls back to recognizing built-in type names ({@code Boolean}, {@code Field}, {@code Uint...}, etc.) if unresolved.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()}, {@link #getReference()}, {@link #findIdentifierChild()},
 *       and {@link #getReferences()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only element; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactTypeElement
 * @see CompactTypeReference
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public class CompactTypeReferenceImpl extends CompactPsiElement implements CompactTypeElement {
  /**
   * Constructs a type reference PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the type reference
   */
  public CompactTypeReferenceImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} denoted by this type reference.
   *
   * <p>Attempts reference resolution to a defined type, with fallbacks for parameterized types
   * and built-in type names.</p>
   *
   * @return the resolved {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    String text = getText();
    if (text.contains("<")) {
      return new CompactPrimitiveType(text);
    }
    PsiReference ref = getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      if (resolved instanceof CompactTypeElement) {
        return ((CompactTypeElement) resolved).getType();
      }
    }
    // Handle builtin types by text if resolution fails
    if ("Boolean".equals(text)) return CompactPrimitiveType.BOOLEAN;
    if ("Field".equals(text)) return CompactPrimitiveType.FIELD;
    if (text.startsWith("Uint") || text.startsWith("Bytes") || text.startsWith("Vector")
        || text.startsWith("Either") || text.startsWith("Maybe")) {
      return new CompactPrimitiveType(text);
    }

    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Creates a reference to the type declaration matching this reference element's identifier.
   *
   * @return a {@link CompactTypeReference} pointing to the target type, or {@code null} if no identifier is present
   */
  @Override
  public @Nullable PsiReference getReference() {
    PsiElement identifier = findIdentifierChild();
    if (identifier == null) {
      return null;
    }
    int start = identifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactTypeReference(this, TextRange.from(start, identifier.getTextLength()));
  }

  /**
   * Locates the underlying AST identifier leaf node representing the type name.
   *
   * @return the identifier {@link PsiElement}, or {@code null} if missing
   */
  public @Nullable PsiElement findIdentifierChild() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  /**
   * Returns all PSI references attached to this type reference element.
   *
   * @return an array containing the type reference, or {@link PsiReference#EMPTY_ARRAY}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}