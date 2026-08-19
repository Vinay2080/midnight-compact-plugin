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
 * PSI-backed implementation of {@link CompactScope}.
 *
 * <p>Wraps an owner {@link PsiElement} and delegates symbol resolution and visibility
 * queries to {@link dev.verloren.midnight.resolve.CompactResolveUtil} and {@link CompactScopes}.</p>
 */
public final class CompactPsiScope implements CompactScope {
  private final PsiElement owner;
  private final CompactScopeKind kind;

  public CompactPsiScope(@NotNull PsiElement owner, @NotNull CompactScopeKind kind) {
    this.owner = owner;
    this.kind = kind;
  }

  @Override
  public @NotNull CompactScopeKind getKind() {
    return kind;
  }

  @Override
  public @NotNull PsiElement getOwner() {
    return owner;
  }

  @Override
  public @Nullable CompactScope getParentScope() {
    PsiElement parent = owner.getParent();
    return parent == null ? null : CompactScopes.nearest(parent);
  }

  @Override
  public @NotNull Collection<CompactSymbol> getSymbols(@NotNull CompactSymbolNamespace namespace) {
    return CompactScopes.visibleSymbols(owner, namespace);
  }

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
