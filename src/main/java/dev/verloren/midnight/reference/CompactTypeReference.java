package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

public class CompactTypeReference extends CompactReferenceBase {
  public CompactTypeReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    return toResults(CompactResolveUtil.resolveType(getValue(), getElement()));
  }
}