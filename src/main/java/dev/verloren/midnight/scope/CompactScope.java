package dev.verloren.midnight.scope;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Interface representing a lexical or structural scope container in the Compact language model.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Unifies lexical scope discovery and symbol enumeration:
 * <ul>
 *   <li><b>Scope Walkers:</b> Created by {@link CompactScopes#nearest(PsiElement)} and navigated via
 *       {@link #getParentScope()} to walk outward from inner blocks to files during reference resolution.</li>
 *   <li><b>Symbol Enumeration:</b> {@link #getSymbols(CompactSymbolNamespace)} provides the list of all
 *       symbols visible at this scope level for a specific namespace, consumed during code completion.</li>
 *   <li><b>Scoped Resolution:</b> {@link #resolve(String, CompactSymbolNamespace)} attempts to find
 *       a symbol matching a specific identifier name within this scope layer.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Scope navigation and symbol resolution inspect the underlying
 *       PSI tree and therefore require an active IntelliJ Read Action ({@code @RequiresReadLock}).</li>
 * </ul>
 *
 * <h3>Lexical Hierarchy Visual Diagram</h3>
 * <pre>
 *   [CompactFile: FILE]
 *           ▲
 *           │ getParentScope()
 *   [CompactCircuitDefinition: CALLABLE]
 *           ▲
 *           │ getParentScope()
 *   [CompactBlock: BLOCK]
 *           ▲
 *           │ getParentScope()
 *   [Reference Site / Caret]
 * </pre>
 *
 * @see dev.verloren.midnight.scope.CompactScopeKind
 * @see dev.verloren.midnight.scope.CompactPsiScope
 * @see dev.verloren.midnight.scope.CompactScopes
 * @see dev.verloren.midnight.symbol.CompactSymbolNamespace
 */
@SuppressWarnings("unused")
public interface CompactScope {

  /**
   * The structural classification of this scope boundary.
   *
   * @return scope kind enum value
   */
  @NotNull CompactScopeKind kind();

  /**
   * The host PSI element defining this lexical scope boundary (e.g. {@code CompactBlock}, {@code CompactFile}).
   *
   * @return the owning PSI element
   */
  @NotNull PsiElement owner();

  /**
   * Returns the immediate enclosing parent scope container in the lexical hierarchy.
   *
   * @return parent {@link CompactScope}, or {@code null} if this is the root file scope
   */
  @Nullable CompactScope getParentScope();

  /**
   * Collects all unique symbols in the given namespace visible from this scope level.
   *
   * @param namespace target namespace (VALUE, TYPE, or MODULE)
   * @return collection of visible {@link CompactSymbol} instances
   */
  @NotNull Collection<CompactSymbol> getSymbols(@NotNull CompactSymbolNamespace namespace);

  /**
   * Resolves a named identifier within this scope and its enclosing parent scopes.
   *
   * @param name      identifier text to resolve
   * @param namespace target namespace
   * @return list of matching symbols (innermost match first)
   */
  @NotNull List<CompactSymbol> resolve(@NotNull String name, @NotNull CompactSymbolNamespace namespace);
}
