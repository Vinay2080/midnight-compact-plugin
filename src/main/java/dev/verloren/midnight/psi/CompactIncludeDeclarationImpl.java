package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.openapi.util.TextRange;

import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactIncludeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;




/**
 * PSI representation of a Compact {@code include "filename.compact";} statement.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#INCLUDE_FORM}.
 * Resolves the referenced file relative to the containing directory or project content roots:
 * <ul>
 *   <li><b>Reference Binding:</b> Creates a {@link CompactIncludeReference} bound to the string literal token
 *       via {@link #getReference()}, supporting Ctrl+Click file navigation.</li>
 *   <li><b>Target File Resolution:</b> Computes the target {@link CompactFile} via {@link #resolveIncludedFile()}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitIncludeDeclaration(CompactIncludeDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#collectIncludedFiles} when resolving transitive symbols.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} during file navigation.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Reference retrieval and file resolution require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Deletion or text changes require a Write Action on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Cache Invalidation</h3>
 * <ul>
 *   <li><b>Cached File Resolution:</b> Target file resolution is wrapped in {@link CachedValuesManager#getCachedValue}
 *       keyed to {@link PsiModificationTracker#MODIFICATION_COUNT}. This avoids repeated filesystem and index searches
 *       during high-frequency typing while ensuring immediate cache invalidation when any project file is edited.</li>
 *   <li><b>Memory Leak Prevention:</b> The cached target {@link CompactFile} is managed by IntelliJ's soft-reference cache
 *       infrastructure and will be garbage collected under memory pressure.</li>
 * </ul>
 *
 * @see CompactIncludeDeclaration
 * @see dev.verloren.midnight.reference.CompactIncludeReference
 * @see CompactPsiUtil#resolveRelativeCompactFile
 */
public class CompactIncludeDeclarationImpl extends CompactPsiElement implements CompactIncludeDeclaration {

  /**
   * Constructs an include declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the include statement
   */
  public CompactIncludeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitIncludeDeclaration(CompactIncludeDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitIncludeDeclaration(this);
  }

  /**
   * Creates a {@link CompactIncludeReference} spanning the string literal child token.
   *
   * @return include reference for file navigation, or {@code null} if no string literal child exists
   */
  @Override
  public @Nullable PsiReference getReference() {
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode == null) {
      return null;
    }
    int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
    return new CompactIncludeReference(this, TextRange.from(start, stringNode.getTextLength()));
  }

  /**
   * Extracts the unquoted relative file path string from the string literal token.
   *
   * @return relative file path, or {@code null} if missing
   */
  public @Nullable String getIncludePath() {
    return CompactPsiUtil.extractStringLiteralValue(this);
  }

  /**
   * Resolves the target {@link CompactFile} with cached memoization.
   *
   * <p>Invalidates automatically whenever any PSI modification occurs in the project.</p>
   *
   * @return resolved {@link CompactFile}, or {@code null} if target file cannot be found
   */
  public @Nullable CompactFile resolveIncludedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveIncludedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  /**
   * Performs the uncached resolution of the include path against local directories and content roots.
   *
   * @return resolved {@link CompactFile}, or {@code null} if unresolved
   */
  private @Nullable CompactFile doResolveIncludedFile() {
    // Step 1: Extract the sanitized path from the string literal
    String path = getIncludePath();
    if (path == null || path.isBlank()) {
      return null;
    }
    // Step 2: Delegate to multi-tiered resolution in CompactPsiUtil
    return CompactPsiUtil.resolveRelativeCompactFile(this, path);
  }
}