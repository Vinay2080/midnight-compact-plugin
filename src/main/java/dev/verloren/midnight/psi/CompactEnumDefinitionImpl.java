package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI implementation of a Compact enum definition wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#ENUM_DECLARATION}.
 * Fulfills {@link CompactEnumDefinition}:
 * <ul>
 *   <li><b>Member Extraction:</b> Collects all {@link CompactEnumMemberImpl} child nodes via
 *       {@link #getMembers()} using {@link PsiTreeUtil#findChildrenOfType}.</li>
 *   <li><b>Semantic Type Provider:</b> Overrides {@link #getType()} to return a {@link CompactPrimitiveType}
 *       reflecting the enum's nominal type name.</li>
 *   <li><b>Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering an Enum node icon.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitEnumDefinition(CompactEnumDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getMembers()} or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactEnumDefinition
 * @see CompactEnumMemberImpl
 * @see CompactNamedElementImpl
 */
public class CompactEnumDefinitionImpl extends CompactNamedElementImpl implements CompactEnumDefinition {

  /**
   * Constructs an enum definition PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the enum declaration
   */
  public CompactEnumDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Finds and returns all member variants declared within this enum.
   *
   * @return list of child {@link CompactEnumMemberImpl} instances; never {@code null}
   */
  @Override
  public @NotNull List<CompactEnumMemberImpl> getMembers() {
    return new ArrayList<>(PsiTreeUtil.findChildrenOfType(this, CompactEnumMemberImpl.class));
  }

  /**
   * Returns the nominal semantic type corresponding to this enum's declared name.
   *
   * @return semantic type representing this enum; never {@code null}
   */
  @Override
  public @NotNull CompactType getType() {
    return new CompactPrimitiveType(getName() != null ? getName() : "enum");
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitEnumDefinition(CompactEnumDefinition)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitEnumDefinition(this);
  }
}