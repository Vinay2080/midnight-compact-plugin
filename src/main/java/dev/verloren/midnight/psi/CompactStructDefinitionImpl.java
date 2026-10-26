package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI implementation of a Compact struct definition wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#STRUCT_DECLARATION}.
 * Fulfills {@link CompactStructDefinition}:
 * <ul>
 *   <li><b>Field Enumeration:</b> Extracts all {@link CompactStructFieldImpl} children via
 *       {@link #getFields()} using {@link PsiTreeUtil#findChildrenOfType}.</li>
 *   <li><b>Semantic Type Provider:</b> Overrides {@link #getType()} to return a {@link CompactPrimitiveType}
 *       representing the struct's nominal type name.</li>
 *   <li><b>Presentation:</b> Inherits from {@link CompactNamedElementImpl}, rendering a Record icon in Structure View.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitStructDefinition(CompactStructDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getFields()} or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactStructDefinition
 * @see CompactStructFieldImpl
 * @see CompactNamedElementImpl
 */
public class CompactStructDefinitionImpl extends CompactNamedElementImpl implements CompactStructDefinition {

  /**
   * Constructs a struct definition PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the struct declaration
   */
  public CompactStructDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Finds and returns all field declarations contained inside this struct.
   *
   * @return list of child {@link CompactStructFieldImpl} instances; never {@code null}
   */
  @Override
  public @NotNull List<CompactStructFieldImpl> getFields() {
    return new ArrayList<>(PsiTreeUtil.findChildrenOfType(this, CompactStructFieldImpl.class));
  }

  /**
   * Returns the nominal semantic type corresponding to this struct's declared name.
   *
   * @return semantic type representing this struct; never {@code null}
   */
  @Override
  public @NotNull CompactType getType() {
    return new CompactPrimitiveType(getName() != null ? getName() : "struct");
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitStructDefinition(CompactStructDefinition)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitStructDefinition(this);
  }
}