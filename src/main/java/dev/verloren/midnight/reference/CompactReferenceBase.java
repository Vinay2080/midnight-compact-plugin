package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.psi.CompactElementFactory;
import dev.verloren.midnight.psi.CompactNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class CompactReferenceBase extends PsiPolyVariantReferenceBase<PsiElement> {
  private static final ResolveCache.PolyVariantResolver<CompactReferenceBase> RESOLVER = (reference, incompleteCode) -> reference.resolveInner();

  protected CompactReferenceBase(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement, false);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    return ResolveCache.getInstance(getElement().getProject()).resolveWithCaching(this, RESOLVER, false, incompleteCode);
  }

  @Override
  public @Nullable PsiElement resolve() {
    ResolveResult[] results = multiResolve(false);
    return results.length == 1 ? results[0].getElement() : null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return EMPTY_ARRAY;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    PsiElement identifier = getReferencedIdentifier();
    if (identifier == null) {
      throw new IncorrectOperationException("Cannot rename Compact reference without an identifier");
    }
    String replacementName = identifier.getText().startsWith("$") && !newElementName.startsWith("$")
            ? "$" + newElementName
            : newElementName;
    identifier.replace(CompactElementFactory.createIdentifierLeaf(getElement().getProject(), replacementName));
    return getElement();
  }

  protected ResolveResult @NotNull [] toResults(@NotNull Collection<? extends CompactNamedElement> elements) {
    ResolveResult[] results = new ResolveResult[elements.size()];
    int i = 0;
    for (CompactNamedElement element : elements) {
      results[i++] = new PsiElementResolveResult(element);
    }
    return results;
  }

  protected abstract ResolveResult @NotNull [] resolveInner();

  private @Nullable PsiElement getReferencedIdentifier() {
    return getElement().findElementAt(getRangeInElement().getStartOffset());
  }
}