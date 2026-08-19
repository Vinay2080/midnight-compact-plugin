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

/**
 * Abstract base class for all references in Compact PSI.
 *
 * <p>Extends IntelliJ's {@link PsiPolyVariantReferenceBase} and integrates with
 * {@link ResolveCache} to cache resolution results across IDE query passes.</p>
 *
 * <p><b>Lifecycle & Resolution:</b>
 * <ol>
 *   <li>IntelliJ encounters an identifier or navigation target and calls {@link #resolve()} or {@link #multiResolve(boolean)}.</li>
 *   <li>{@link #multiResolve(boolean)} queries IntelliJ's {@link ResolveCache} using {@link #RESOLVER}.</li>
 *   <li>The cache calls {@link #resolveInner()} if the reference is not yet cached.</li>
 *   <li>{@link #resolveInner()} queries {@link dev.verloren.midnight.resolve.CompactResolveUtil} to find matching declaration PSI nodes.</li>
 *   <li>{@link #handleElementRename(String)} handles identifier renaming when refactoring.</li>
 * </ol>
 * </p>
 */
public abstract class CompactReferenceBase extends PsiPolyVariantReferenceBase<PsiElement> {
  private static final ResolveCache.PolyVariantResolver<CompactReferenceBase> RESOLVER = (reference, incompleteCode) -> reference.resolveInner();

  protected CompactReferenceBase(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement, false);
  }

  @Override
  public @Nullable PsiElement resolve() {
    ResolveResult[] results = multiResolve(false);
    return results.length == 1 ? results[0].getElement() : null;
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    return ResolveCache.getInstance(getElement().getProject()).resolveWithCaching(this, RESOLVER, false, incompleteCode);
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

  private @Nullable PsiElement getReferencedIdentifier() {
    return getElement().findElementAt(getRangeInElement().getStartOffset());
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactReferenceBase that = (CompactReferenceBase) o;
    return getElement().equals(that.getElement()) && getRangeInElement().equals(that.getRangeInElement());
  }

  @Override
  public int hashCode() {
    return 31 * getElement().hashCode() + getRangeInElement().hashCode();
  }
}