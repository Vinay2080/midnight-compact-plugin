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
 * <p>Implements {@link PsiDocCommentBase} so that IntelliJ's documentation subsystem, Reader Mode,
 * and in-editor "Render Documentation Comments" gutter icons recognize and render Compact doc comments.</p>
 */
public class CompactDocComment extends FakePsiElement implements PsiDocCommentBase {

  private final PsiComment delegate;

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
