package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of an individual enum member variant wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#ENUM_MEMBER}.
 * Represents a specific named variant within an enum (e.g., {@code Red} in {@code enum Color { Red, Green }}):
 * <ul>
 *   <li><b>Member Resolution Target:</b> Target of {@link dev.verloren.midnight.reference.CompactEnumMemberReference}
 *       in qualified member expressions like {@code Color.Red}.</li>
 *   <li><b>Type Inheritance:</b> {@link #getType()} dynamically resolves the enclosing {@link CompactEnumDefinition}
 *       and returns its nominal enum type.</li>
 *   <li><b>Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering a Field node icon.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.reference.CompactEnumMemberReference} during variant navigation and resolution.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider} when populating enum variants.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} traverses the AST parent chain and requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactEnumDefinition
 * @see CompactNamedElementImpl
 * @see dev.verloren.midnight.reference.CompactEnumMemberReference
 */
public class CompactEnumMemberImpl extends CompactNamedElementImpl {

  /**
   * Constructs an enum member PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the enum variant
   */
  public CompactEnumMemberImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Returns the semantic {@link CompactType} of this enum member, which is the type of the parent enum.
   *
   * @return the parent enum's semantic type, or {@link CompactPrimitiveType#UNKNOWN} if unparented
   */
  @Override
  public @NotNull CompactType getType() {
    CompactEnumDefinition parent = PsiTreeUtil.getParentOfType(this, CompactEnumDefinition.class);
    if (parent != null) {
      return parent.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}