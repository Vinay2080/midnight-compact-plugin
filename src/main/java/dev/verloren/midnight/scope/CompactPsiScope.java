package dev.verloren.midnight.scope;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * PSI-backed record implementation of {@link CompactScope}.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Binds an AST element to its semantic scope model:
 * <ul>
 *   <li><b>Owner Encapsulation:</b> Wraps an owner {@link PsiElement} along with its classified {@link CompactScopeKind}.</li>
 *   <li><b>Resolution Bridge:</b> Routes resolution queries to {@link CompactResolveUtil} for value and type symbols,
 *       and to {@link CompactScopes#resolveModuleSymbols} for module symbols.</li>
 *   <li><b>Parent Navigation:</b> Walks up the PSI tree via {@code owner.getParent()} and evaluates
 *       {@link CompactScopes#nearest(PsiElement)} to discover parent scopes dynamically.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Methods querying the PSI tree require an active Read Action ({@code @RequiresReadLock}).</li>
 * </ul>
 *
 * @param owner the underlying AST element defining this scope container
 * @param kind  the semantic classification of this scope boundary
 *
 * @see dev.verloren.midnight.scope.CompactScope
 * @see dev.verloren.midnight.scope.CompactScopes
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public record CompactPsiScope(PsiElement owner, CompactScopeKind kind) implements CompactScope {

  public CompactPsiScope(@NotNull PsiElement owner, @NotNull CompactScopeKind kind) {
    this.owner = owner;
    this.kind = kind;
  }

  @Override
  public @NotNull CompactScopeKind kind() {
    return kind;
  }

  @Override
  public @NotNull PsiElement owner() {
    return owner;
  }

  /**
   * Discovers the nearest parent scope by walking up from this scope's owner.
   *
   * @return enclosing parent {@link CompactScope}, or {@code null} if this is the root file
   */
  @Override
  public @Nullable CompactScope getParentScope() {
    PsiElement parent = owner.getParent();
    return parent == null ? null : CompactScopes.nearest(parent);
  }

  /**
   * Collects all symbols visible from this scope for the specified namespace.
   *
   * @param namespace target namespace
   * @return collection of visible symbols
   */
  @Override
  public @NotNull Collection<CompactSymbol> getSymbols(@NotNull CompactSymbolNamespace namespace) {
    return CompactScopes.visibleSymbols(owner, namespace);
  }

  /**
   * Resolves a named identifier in the target namespace.
   *
   * @param name      identifier name
   * @param namespace target namespace
   * @return list of matching symbols
   */
  @Override
  public @NotNull List<CompactSymbol> resolve(@NotNull String name, @NotNull CompactSymbolNamespace namespace) {
    return switch (namespace) {
      case VALUE -> CompactResolveUtil.resolveValueSymbols(name, owner);
      case TYPE -> CompactResolveUtil.resolveTypeSymbols(name, owner);
      case MODULE -> CompactScopes.resolveModuleSymbols(name, owner);
      case UNKNOWN -> List.of();
    };
  }
}
