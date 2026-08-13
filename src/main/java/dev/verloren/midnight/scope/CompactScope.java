package dev.verloren.midnight.scope;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface CompactScope {
  @NotNull CompactScopeKind getKind();

  @NotNull PsiElement getOwner();

  @Nullable CompactScope getParentScope();

  @NotNull Collection<CompactSymbol> getSymbols(@NotNull CompactSymbolNamespace namespace);

  @NotNull List<CompactSymbol> resolve(@NotNull String name, @NotNull CompactSymbolNamespace namespace);
}
