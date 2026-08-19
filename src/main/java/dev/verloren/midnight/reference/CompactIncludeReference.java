package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;

import dev.verloren.midnight.psi.CompactIncludeDeclarationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the string literal in an {@code include "path.compact";} statement to the target {@link dev.verloren.midnight.psi.CompactFile}.
 *
 * <p>Enables Ctrl+Click navigation directly from the include path string to the target file.</p>
 */
public class CompactIncludeReference extends PsiReferenceBase<CompactIncludeDeclarationImpl> {

  public CompactIncludeReference(@NotNull CompactIncludeDeclarationImpl element, @NotNull TextRange range) {
    super(element, range);
  }

  @Override
  public @Nullable PsiElement resolve() {
    return myElement.resolveIncludedFile();
  }

  @Override
  public Object @NotNull [] getVariants() {
    return EMPTY_ARRAY;
  }
}
