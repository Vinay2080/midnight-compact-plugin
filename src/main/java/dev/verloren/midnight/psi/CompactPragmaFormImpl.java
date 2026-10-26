package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.version.CompactSemVerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a compiler pragma directive wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#PRAGMA_FORM}.
 * Implements {@link CompactPragmaForm}:
 * <ul>
 *   <li><b>Identifier Extraction:</b> Queries child AST nodes for {@link CompactTokenTypes#IDENTIFIER}
 *       via {@link #getPragmaIdentifier()}.</li>
 *   <li><b>Boundary Lookahead:</b> Collects all sibling nodes between the pragma name identifier and
 *       the terminating {@link CompactTokenTypes#SEMICOLON} to extract the constraint text.</li>
 *   <li><b>SemVer Parsing Delegation:</b> Passes the extracted constraint text to {@link CompactSemVerUtil#extractVersion(String)}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitPragmaForm(CompactPragmaForm)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query methods require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only PSI node; modification occurs via AST replacement in write actions.</li>
 * </ul>
 *
 * @see CompactPragmaForm
 * @see dev.verloren.midnight.version.CompactSemVerUtil
 */
public class CompactPragmaFormImpl extends CompactPsiElement implements CompactPragmaForm {

  /**
   * Constructs a pragma form PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the pragma statement
   */
  public CompactPragmaFormImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Finds the leaf AST identifier child identifying the pragma directive name.
   *
   * @return identifier PSI leaf, or {@code null} if missing
   */
  @Override
  public @Nullable PsiElement getPragmaIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  /**
   * Returns the textual name of this pragma directive.
   *
   * @return pragma name string, or {@code null} if unidentifiable
   */
  @Override
  public @Nullable String getPragmaName() {
    PsiElement id = getPragmaIdentifier();
    return id != null ? id.getText() : null;
  }

  /**
   * Scans forward across siblings following the pragma identifier until the terminating semicolon,
   * accumulating the raw constraint text.
   *
   * @return trimmed constraint string, or {@code null} if blank
   */
  @Override
  public @Nullable String getConstraintText() {
    // Step 1: Locate the pragma identifier anchor
    PsiElement id = getPragmaIdentifier();
    if (id == null) {
      return null;
    }
    // Step 2: Traverse sibling nodes until the terminating semicolon
    StringBuilder sb = new StringBuilder();
    PsiElement sibling = id.getNextSibling();
    while (sibling != null) {
      if (sibling.getNode().getElementType() == CompactTokenTypes.SEMICOLON) {
        break;
      }
      sb.append(sibling.getText());
      sibling = sibling.getNextSibling();
    }
    // Step 3: Trim and return non-empty text
    String text = sb.toString().trim();
    return text.isEmpty() ? null : text;
  }

  /**
   * Extracts the semantic version string from the constraint text.
   *
   * @return parsed version string (e.g. {@code "0.20"}), or {@code null} if unparseable
   */
  @Override
  public @Nullable String getRequiredVersion() {
    return CompactSemVerUtil.extractVersion(getConstraintText());
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitPragmaForm(CompactPragmaForm)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitPragmaForm(this);
  }
}
