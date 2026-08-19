package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves Compact identifiers in the {@link CompactResolveUtil.Namespace#VALUE} namespace.
 *
 * <p>Used by variable references, parameters, local constants, circuits, witnesses,
 * and ledger state declarations.</p>
 */
public class CompactValueReference extends CompactReferenceBase {
  public CompactValueReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    return toResults(CompactResolveUtil.resolveValue(getValue(), getElement()));
  }
}