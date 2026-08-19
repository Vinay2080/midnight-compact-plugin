package dev.verloren.midnight.scope;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Interface representing a lexical or structural scope container in Compact.
 *
 * <p>Provides symbol enumeration and scoped name lookup across different {@link CompactSymbolNamespace}s.</p>
 */
public interface CompactScope {
  @NotNull CompactScopeKind getKind();

  @NotNull PsiElement getOwner();

  @Nullable CompactScope getParentScope();

  @NotNull Collection<CompactSymbol> getSymbols(@NotNull CompactSymbolNamespace namespace);

  @NotNull List<CompactSymbol> resolve(@NotNull String name, @NotNull CompactSymbolNamespace namespace);
}
