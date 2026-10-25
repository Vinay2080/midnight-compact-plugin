package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;

import dev.verloren.midnight.psi.CompactIncludeDeclarationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the string literal path in an {@code include "path.compact";} statement to the target {@link dev.verloren.midnight.psi.CompactFile}.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Binds directly to the relative file path literal in an include declaration:
 * <ul>
 *   <li><b>Ctrl+Click Navigation:</b> Enables seamless navigation from an include statement directly
 *       to the target Compact file in the IDE editor.</li>
 *   <li><b>File Refactoring:</b> When included files are moved or renamed via Project View refactoring,
 *       IntelliJ uses this reference to update the string literal path.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link #resolve()} must be executed inside a Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Thread Environment:</b> Operates on background threads during file indexing or on the EDT
 *       during interactive navigation.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline</h3>
 * <pre>
 *   [include "helper.compact";]
 *                 │
 *                 ▼
 *    [CompactIncludeDeclarationImpl]
 *                 │
 *                 ▼
 *       [resolveIncludedFile()]
 *                 │
 *                 ▼
 *        [Target CompactFile]
 * </pre>
 *
 * @see dev.verloren.midnight.psi.CompactIncludeDeclarationImpl
 * @see dev.verloren.midnight.psi.CompactFile
 */
public class CompactIncludeReference extends PsiReferenceBase<CompactIncludeDeclarationImpl> {

  /**
   * Constructs an include file reference.
   *
   * @param element the enclosing include declaration element
   * @param range   relative text range of the file path string literal (excluding quotes)
   */
  public CompactIncludeReference(@NotNull CompactIncludeDeclarationImpl element, @NotNull TextRange range) {
    super(element, range);
  }

  /**
   * Resolves the include statement to the target {@link dev.verloren.midnight.psi.CompactFile}.
   *
   * <p>Delegates to {@link CompactIncludeDeclarationImpl#resolveIncludedFile()}, which locates
   * the virtual file relative to the containing directory and parses it via {@link com.intellij.psi.PsiManager}.</p>
   *
   * @return the resolved {@link dev.verloren.midnight.psi.CompactFile}, or {@code null} if file not found
   */
  @Override
  public @Nullable PsiElement resolve() {
    return myElement.resolveIncludedFile();
  }

  /**
   * Completion variants hook for include path references.
   *
   * @return empty array (file path completion is handled by file path completion contributors)
   */
  @Override
  public Object @NotNull [] getVariants() {
    return EMPTY_ARRAY;
  }
}
