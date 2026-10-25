package dev.verloren.midnight.scope;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import dev.verloren.midnight.symbol.CompactSymbols;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Factory and query utilities for discovering lexical scopes and collecting visible symbols.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Central entry point for scope-based queries in the Compact plugin:
 * <ul>
 *   <li><b>Nearest Scope Search:</b> {@link #nearest(PsiElement)} walks upward from any reference,
 *       caret offset, or syntax node to locate the immediate enclosing {@link CompactScope}.</li>
 *   <li><b>Visible Symbol Aggregation:</b> {@link #visibleSymbols(PsiElement, CompactSymbolNamespace)}
 *       aggregates all symbols visible from a given site across value, type, and module namespaces,
 *       using a {@link LinkedHashMap} to preserve innermost shadowing order.</li>
 *   <li><b>Module Symbol Discovery:</b> {@link #collectModuleSymbols(PsiElement)} and
 *       {@link #resolveModuleSymbols(String, PsiElement)} search top-level modules declared in the containing file.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> All discovery and symbol aggregation routines inspect the PSI
 *       tree and require an active Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Memory Safety:</b> Completely static and stateless. Does not cache PSI nodes in static fields.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.scope.CompactScope
 * @see dev.verloren.midnight.scope.CompactPsiScope
 * @see dev.verloren.midnight.scope.CompactScopeKind
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public final class CompactScopes {

  /**
   * Private constructor preventing external instantiation of this stateless utility class.
   */
  private CompactScopes() {
  }

  /**
   * Discovers the nearest enclosing {@link CompactScope} for a given PSI element.
   *
   * <p>Walks upward through parent nodes until a node matching a known {@link CompactScopeKind} is found.</p>
   *
   * @param place the starting PSI element
   * @return nearest {@link CompactScope}, or {@code null} if outside any known scope container
   */
  public static @Nullable CompactScope nearest(@NotNull PsiElement place) {
    for (PsiElement element = place; element != null; element = element.getParent()) {
      CompactScopeKind kind = kindOf(element);
      if (kind != CompactScopeKind.UNKNOWN) {
        return new CompactPsiScope(element, kind);
      }
    }
    return null;
  }

  /**
   * Classifies an AST element into its corresponding {@link CompactScopeKind}.
   *
   * @param element the PSI element to test
   * @return matching scope kind, or {@link CompactScopeKind#UNKNOWN} if not a scope container
   */
  private static @NotNull CompactScopeKind kindOf(@NotNull PsiElement element) {
    if (element instanceof CompactFile) return CompactScopeKind.FILE;
    if (element instanceof CompactModuleDefinition) return CompactScopeKind.MODULE;
    if (nodeType(element, CompactElementTypes.BLOCK)) return CompactScopeKind.BLOCK;
    if (nodeType(element, CompactElementTypes.CIRCUIT_DEFINITION)
            || nodeType(element, CompactElementTypes.WITNESS_DECLARATION)) {
      return CompactScopeKind.CALLABLE;
    }
    if (nodeType(element, CompactElementTypes.CONSTRUCTOR_DEFINITION)) return CompactScopeKind.CONSTRUCTOR;
    if (nodeType(element, CompactElementTypes.LAMBDA_EXPR)) return CompactScopeKind.LAMBDA;
    if (nodeType(element, CompactElementTypes.FOR_STATEMENT)) return CompactScopeKind.FOR;
    if (nodeType(element, CompactElementTypes.TYPE_ALIAS_DECLARATION)) return CompactScopeKind.TYPE_DECLARATION;
    if (nodeType(element, CompactElementTypes.STRUCT_DECLARATION)) return CompactScopeKind.STRUCT;
    if (nodeType(element, CompactElementTypes.ENUM_DECLARATION)) return CompactScopeKind.ENUM;
    if (nodeType(element, CompactElementTypes.CONTRACT_DECLARATION)) return CompactScopeKind.CONTRACT;
    return CompactScopeKind.UNKNOWN;
  }

  /**
   * Helper checking the AST element type of a PSI element.
   */
  private static boolean nodeType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    return element.getNode() != null && element.getNode().getElementType() == type;
  }

  /**
   * Collects all symbols visible from the specified site in the given namespace.
   *
   * <p>Preserves innermost shadowing order by populating a {@link LinkedHashMap} with {@code putIfAbsent}.</p>
   *
   * @param place     reference or caret site
   * @param namespace target namespace (VALUE, TYPE, or MODULE)
   * @return collection of visible symbols
   */
  public static @NotNull Collection<CompactSymbol> visibleSymbols(
      @NotNull PsiElement place,
      @NotNull CompactSymbolNamespace namespace
  ) {
    Map<String, CompactSymbol> result = new LinkedHashMap<>();
    if (namespace == CompactSymbolNamespace.VALUE) {
      addAll(result, CompactResolveUtil.collectValueSymbols(place));
    } else if (namespace == CompactSymbolNamespace.TYPE) {
      addAll(result, CompactSymbols.builtinTypes());
      addAll(result, CompactResolveUtil.collectTypeSymbols(place));
    } else if (namespace == CompactSymbolNamespace.MODULE) {
      addAll(result, collectModuleSymbols(place));
    }
    return result.values();
  }

  /**
   * Helper adding symbols to a map while preserving earlier (innermost) bindings.
   */
  private static void addAll(
      @NotNull Map<String, CompactSymbol> result,
      @NotNull Collection<? extends CompactSymbol> symbols
  ) {
    for (CompactSymbol symbol : symbols) {
      String name = symbol.name();
      if (name != null) {
        result.putIfAbsent(name, symbol);
      }
    }
  }

  /**
   * Collects all module definitions declared at file scope in the containing file.
   *
   * @param place the context PSI element
   * @return collection of module symbols
   */
  public static @NotNull Collection<CompactSymbol> collectModuleSymbols(@NotNull PsiElement place) {
    Map<String, CompactSymbol> result = new LinkedHashMap<>();
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return result.values();
    }
    for (CompactModuleDefinition module : PsiTreeUtil.findChildrenOfType(file, CompactModuleDefinition.class)) {
      CompactSymbol symbol = CompactSymbols.from(module);
      if (symbol != null && symbol.name() != null) {
        result.putIfAbsent(symbol.name(), symbol);
      }
    }
    return result.values();
  }

  /**
   * Helper extracting the typed {@link CompactFile} of an element.
   */
  private static @Nullable CompactFile getCompactFile(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file instanceof CompactFile ? (CompactFile) file : null;
  }

  /**
   * Resolves module symbols matching a specific identifier name.
   *
   * @param name  module name to match
   * @param place context PSI element
   * @return list of matching module symbols
   */
  public static @NotNull List<CompactSymbol> resolveModuleSymbols(@NotNull String name, @NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>();
    for (CompactSymbol symbol : collectModuleSymbols(place)) {
      if (name.equals(symbol.name())) {
        result.add(symbol);
      }
    }
    return result;
  }
}
