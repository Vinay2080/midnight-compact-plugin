package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.psi.CompactElementFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Abstract poly-variant reference base class for the Compact PSI language model.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Extends IntelliJ's {@link PsiPolyVariantReferenceBase} and serves as the unifying foundation
 * for all symbol references in the Compact plugin:
 * <ul>
 *   <li><b>Navigation & Resolution:</b> Triggered when users invoke "Go to Declaration" (Ctrl+B / Cmd+B / Ctrl+Click),
 *       parameter info tooltips, or hover documentation. IntelliJ calls {@link #resolve()} or
 *       {@link #multiResolve(boolean)}.</li>
 *   <li><b>Resolve Caching:</b> Integrates directly with IntelliJ's platform {@link ResolveCache}.
 *       Resolutions are calculated once via {@link #resolveInner()} and cached per PSI modification count
 *       ({@link com.intellij.psi.util.PsiModificationTracker}), avoiding expensive repetitive AST traversals
 *       during syntax highlighting and daemon passes.</li>
 *   <li><b>Refactoring & Rename:</b> Participates in IntelliJ's rename refactoring subsystem via
 *       {@link #handleElementRename(String)}, ensuring referenced identifiers update correctly when target
 *       definitions change.</li>
 *   <li><b>Find Usages:</b> Queried by {@link com.intellij.find.findUsages.FindUsagesHandler} to match
 *       source references against target declaration PSI elements.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> All resolution operations ({@link #resolve()}, {@link #multiResolve(boolean)},
 *       {@link #resolveInner()}) require an active Read Action ({@code @RequiresReadLock}). AST inspection and
 *       tree walking without a read lock will trigger an IntelliJ assertion failure.</li>
 *   <li><b>Thread Environment:</b> Typically executed on pooled background daemon threads (e.g. during
 *       code inspections or background reference validation), but may also run on the Event Dispatch Thread (EDT)
 *       during synchronous user-driven navigation actions (Ctrl+Click).</li>
 *   <li><b>Write Action for Rename:</b> {@link #handleElementRename(String)} modifies the PSI/Document model
 *       and must be executed inside a Write Action on the Event Dispatch Thread (EDT) within a
 *       {@link com.intellij.openapi.command.WriteCommandAction}.</li>
 * </ul>
 *
 * <h3>Resolution Lifecycle Diagram</h3>
 * <pre>
 *   [User Action / Daemon / Rename]
 *                 │
 *                 ▼
 *    [CompactReferenceBase.resolve()]
 *                 │
 *                 ▼
 *   [CompactReferenceBase.multiResolve()]
 *                 │
 *                 ▼
 *    [ResolveCache.resolveWithCaching()]
 *          │                     │
 *     (Cache Hit)           (Cache Miss)
 *          │                     │
 *          ▼                     ▼
 *   [Cached Results]    [reference.resolveInner()]
 *                                │
 *                                ▼
 *                       [CompactResolveUtil]
 * </pre>
 *
 * <h3>Edge Case Mitigations & Renaming Logic</h3>
 * <ul>
 *   <li><b>Ledger State Variable Sigil Preservation:</b> In Compact syntax, ledger state variables are
 *       prefixed with a {@code $} sigil (e.g., {@code $counter}). When {@link #handleElementRename(String)}
 *       is invoked during rename refactoring, it inspects the existing identifier text. If the old name
 *       starts with {@code $} and the user supplied a new name without {@code $}, the prefix is
 *       automatically preserved ({@code "$" + newElementName}) to prevent breaking ledger variable grammar.</li>
 *   <li><b>Poly-Variant Fallback:</b> {@link #resolve()} safely unwraps {@link #multiResolve(boolean)}: if
 *       exactly one unambiguous target is resolved, it returns that {@link PsiElement}; if ambiguous or empty,
 *       it gracefully returns {@code null}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactTypeReference
 * @see dev.verloren.midnight.reference.CompactValueReference
 * @see dev.verloren.midnight.reference.CompactStructFieldReference
 * @see dev.verloren.midnight.reference.CompactEnumMemberReference
 * @see dev.verloren.midnight.reference.CompactImportReference
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see com.intellij.psi.impl.source.resolve.ResolveCache
 */
public abstract class CompactReferenceBase extends PsiPolyVariantReferenceBase<PsiElement> {

  /**
   * Cached resolver lambda for {@link ResolveCache#resolveWithCaching}.
   *
   * <p>Stateless and reused across all reference instances to eliminate per-call allocation overhead.</p>
   */
  private static final ResolveCache.PolyVariantResolver<CompactReferenceBase> RESOLVER = (reference, _) -> reference.resolveInner();

  /**
   * Constructs a reference anchored to a specific PSI element within a given sub-range.
   *
   * @param element        the PSI element hosting this reference (e.g. leaf identifier token or expression)
   * @param rangeInElement the sub-range within the host element where the reference text is located
   */
  protected CompactReferenceBase(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement, false);
  }

  /**
   * Resolves the reference to a single target declaration element.
   *
   * <p>Delegates to {@link #multiResolve(boolean)} with {@code incompleteCode = false}.
   * If resolution produces exactly one result, returns that element; otherwise returns {@code null}.</p>
   *
   * @return the unique resolved target element, or {@code null} if ambiguous or unresolved
   */
  @Override
  public @Nullable PsiElement resolve() {
    ResolveResult[] results = multiResolve(false);
    return results.length == 1 ? results[0].getElement() : null;
  }

  /**
   * Resolves this reference against the platform {@link ResolveCache}.
   *
   * <p>Caches results across repeated queries. The cache is automatically cleared when
   * any PSI change occurs in the project.</p>
   *
   * @param incompleteCode {@code true} if code completion or incomplete typing is in progress
   * @return array of resolve results (maybe empty or contain multiple candidates)
   */
  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    return ResolveCache.getInstance(getElement().getProject()).resolveWithCaching(this, RESOLVER, true, incompleteCode);
  }

  /**
   * Autocompletion variants hook for this reference.
   *
   * <p>Returns {@link #EMPTY_ARRAY} because code completion in this plugin is driven by
   * dedicated {@link dev.verloren.midnight.completion.CompactCompletionContributor} and
   * {@link com.intellij.codeInsight.completion.CompletionProvider} implementations.</p>
   *
   * @return empty array
   */
  @Override
  public Object @NotNull [] getVariants() {
    return EMPTY_ARRAY;
  }

  /**
   * Handles identifier renaming during IntelliJ rename refactoring.
   *
   * <p>Replaces the underlying leaf identifier token with a newly created identifier token.
   * Special case: preserves the {@code $} sigil on Compact ledger state variables if the user omitted it.</p>
   *
   * @param newElementName the new name provided by the user in the Rename dialogue
   * @return the modified host PSI element
   * @throws IncorrectOperationException if the referenced identifier cannot be located or modified
   */
  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    // Step 1: Locate the specific leaf identifier token within the host element's relative range
    PsiElement identifier = getReferencedIdentifier();
    if (identifier == null) {
      throw new IncorrectOperationException("Cannot rename Compact reference without an identifier");
    }

    // Step 2: Handle ledger state variable sigil preservation ($)
    String replacementName = identifier.getText().startsWith("$") && !newElementName.startsWith("$")
            ? "$" + newElementName
            : newElementName;

    // Step 3: Replace old identifier leaf with newly generated token
    identifier.replace(CompactElementFactory.createIdentifierLeaf(getElement().getProject(), replacementName));
    return getElement();
  }

  /**
   * Helper to locate the leaf token at the start of the reference's text range.
   *
   * @return the leaf {@link PsiElement} corresponding to the referenced identifier, or {@code null}
   */
  private @Nullable PsiElement getReferencedIdentifier() {
    return getElement().findElementAt(getRangeInElement().getStartOffset());
  }

  /**
   * Converts a collection of target PSI elements into an array of {@link ResolveResult} objects.
   *
   * @param elements target elements found during resolution
   * @return array of {@link PsiElementResolveResult} wrappers
   */
  protected ResolveResult @NotNull [] toResults(@NotNull Collection<? extends PsiElement> elements) {
    ResolveResult[] results = new ResolveResult[elements.size()];
    int i = 0;
    for (PsiElement element : elements) {
      results[i++] = new PsiElementResolveResult(element);
    }
    return results;
  }

  /**
   * Performs the actual reference resolution calculation.
   *
   * <p>Invoked by the cached {@link #RESOLVER} only when results are not already cached in
   * {@link ResolveCache}. Concrete subclasses must implement their specific symbol lookup here.</p>
   *
   * @return array of resolve results (empty if unresolved)
   */
  protected abstract ResolveResult @NotNull [] resolveInner();

  /**
   * Structural equality comparison based on host element and text range.
   *
   * <p>Essential for correct lookup key matching inside {@link ResolveCache}.</p>
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactReferenceBase that = (CompactReferenceBase) o;
    return getElement().equals(that.getElement()) && getRangeInElement().equals(that.getRangeInElement());
  }

  /**
   * Computes hash code combining host element and reference text range for {@link ResolveCache} keys.
   */
  @Override
  public int hashCode() {
    return 31 * getElement().hashCode() + getRangeInElement().hashCode();
  }
}