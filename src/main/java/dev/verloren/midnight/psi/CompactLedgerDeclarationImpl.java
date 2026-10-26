package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a contract ledger state declaration wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#LEDGER_DECLARATION}.
 * Fulfills {@link CompactLedgerDeclaration}:
 * <ul>
 *   <li><b>Type Extraction:</b> Resolves the attached {@link CompactTypeElement} via {@link #getTypeElement()}.</li>
 *   <li><b>Semantic Type Provider:</b> Overrides {@link #getType()} to return the declared ledger variable type.</li>
 *   <li><b>Modifier Flags:</b> Queries child tokens for {@link dev.verloren.midnight.lexer.CompactTokenTypes#SEALED}
 *       and {@link dev.verloren.midnight.lexer.CompactTokenTypes#EXPORT}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitLedgerDeclaration(CompactLedgerDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getTypeElement()}, {@link #isSealed()}, or {@link #getType()}
 *       requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactLedgerDeclaration
 * @see CompactNamedElementImpl
 */
public class CompactLedgerDeclarationImpl extends CompactNamedElementImpl implements CompactLedgerDeclaration {

  /**
   * Constructs a ledger declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the ledger state declaration
   */
  public CompactLedgerDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Finds the child type element specifying the type of this ledger variable.
   *
   * @return the {@link CompactTypeElement}, or {@code null} if missing
   */
  @Override
  public @Nullable CompactTypeElement getTypeElement() {
    return PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
  }

  /**
   * Evaluates the declared semantic type of this ledger variable.
   *
   * @return the declared {@link dev.verloren.midnight.type.CompactType}
   */
  @Override
  public @NotNull dev.verloren.midnight.type.CompactType getType() {
    CompactTypeElement typeElement = getTypeElement();
    if (typeElement != null) {
      return typeElement.getType();
    }
    return super.getType();
  }

  /**
   * Checks whether this ledger state variable is declared with the {@code sealed} keyword.
   *
   * @return {@code true} if sealed; {@code false} otherwise
   */
  @Override
  public boolean isSealed() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.SEALED) != null;
  }

  /**
   * Checks whether this ledger variable is declared with the {@code export} keyword.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  @Override
  public boolean isExported() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.EXPORT) != null;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitLedgerDeclaration(CompactLedgerDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitLedgerDeclaration(this);
  }
}
