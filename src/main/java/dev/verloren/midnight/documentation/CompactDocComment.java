package dev.verloren.midnight.documentation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * PSI wrapper representing a Compact documentation comment ({@code /** ... *&#47;} or {@code ///}).
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Extends {@link FakePsiElement} and implements {@link PsiDocCommentBase}. Acts as a lightweight
 * decorator around a physical {@link PsiComment} AST node. By implementing {@link PsiDocCommentBase}, it
 * seamlessly enables IntelliJ's built-in documentation rendering infrastructure:
 * <ul>
 *   <li><b>In-Editor Rendered Documentation:</b> Powers the "Toggle Rendered View" gutter icons (Ctrl+Alt+Q / Cmd+Alt+Q)
 *       that replace raw doc comment syntax with beautifully formatted inline HTML previews.</li>
 *   <li><b>Reader Mode:</b> Automatically renders doc comments in a clean, book-like reading typography
 *       when viewing read-only library files or standard library contracts.</li>
 *   <li><b>Owner Association:</b> Implements {@link #getOwner()} by linking the comment to its succeeding
 *       declaration (circuit, witness, struct, enum, pragma) via {@link CompactDocumentationProvider#findTargetDeclarationForComment}.</li>
 * </ul>
 * </p>
 *
 * <h3>Design Rationale</h3>
 * <p>Rather than complicating the core Grammar-Kit BNF grammar with dedicated doc-comment AST rules, the Compact
 * plugin keeps the lexer/parser simple by treating doc comments as standard comment tokens and decorating them
 * dynamically with {@link CompactDocComment} during documentation passes. This avoids AST bloat while providing
 * 100% feature parity with first-class IDE doc comments.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Instantiated and queried during background highlighting passes
 *       ({@code DocRenderPassFactory}), Quick Documentation popups, or on the Event Dispatch Thread (EDT).</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while querying parent,
 *       siblings, owner, and text ranges.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.documentation.CompactDocumentationProvider
 * @see com.intellij.psi.PsiDocCommentBase
 * @see com.intellij.psi.impl.FakePsiElement
 */
public class CompactDocComment extends FakePsiElement implements PsiDocCommentBase {

  /**
   * The underlying physical {@link PsiComment} token in the Compact PSI tree.
   */
  private final PsiComment delegate;

  /**
   * Constructs a documentation comment adapter around the given physical comment.
   *
   * @param delegate the physical PSI comment node
   */
  public CompactDocComment(@NotNull PsiComment delegate) {
    this.delegate = delegate;
  }

  @Override
  public @Nullable PsiElement getOwner() {
    return CompactDocumentationProvider.findTargetDeclarationForComment(delegate);
  }

  @Override
  public PsiElement getParent() {
    return delegate.getParent();
  }

  @Override
  public PsiElement getNextSibling() {
    return delegate.getNextSibling();
  }

  @Override
  public PsiElement getPrevSibling() {
    return delegate.getPrevSibling();
  }

  @Override
  public PsiFile getContainingFile() {
    return delegate.getContainingFile();
  }

  @Override
  public TextRange getTextRange() {
    return delegate.getTextRange();
  }

  @Override
  public int getTextOffset() {
    return delegate.getTextOffset();
  }

  @Override
  public String getText() {
    return delegate.getText();
  }

  @Override
  public @NonNull IElementType getTokenType() {
    return delegate.getTokenType();
  }

  @Override
  public @NonNull Project getProject() {
    return delegate.getProject();
  }

  @Override
  public boolean isValid() {
    return delegate.isValid();
  }

  @Override
  public @NonNull PsiElement getNavigationElement() {
    return delegate;
  }

  public @NotNull PsiComment getDelegate() {
    return delegate;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof CompactDocComment other) {
      return delegate.equals(other.delegate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
