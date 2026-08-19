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
 * Factory and query utilities for discovering scopes and collecting visible symbols.
 *
 * <p>Finds the nearest {@link CompactScope} for any given {@link PsiElement} and aggregates
 * visible symbols across value, type, and module namespaces.</p>
 */
public final class CompactScopes {
  private CompactScopes() {
  }

  public static @Nullable CompactScope nearest(@NotNull PsiElement place) {
    for (PsiElement element = place; element != null; element = element.getParent()) {
      CompactScopeKind kind = kindOf(element);
      if (kind != CompactScopeKind.UNKNOWN) {
        return new CompactPsiScope(element, kind);
      }
    }
    return null;
  }

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

  private static boolean nodeType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    return element.getNode() != null && element.getNode().getElementType() == type;
  }

  public static @NotNull Collection<CompactSymbol> visibleSymbols(@NotNull PsiElement place,
                                                                  @NotNull CompactSymbolNamespace namespace) {
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

  private static void addAll(@NotNull Map<String, CompactSymbol> result,
                             @NotNull Collection<? extends CompactSymbol> symbols) {
    for (CompactSymbol symbol : symbols) {
      String name = symbol.getName();
      if (name != null) {
        result.putIfAbsent(name, symbol);
      }
    }
  }

  public static @NotNull Collection<CompactSymbol> collectModuleSymbols(@NotNull PsiElement place) {
    Map<String, CompactSymbol> result = new LinkedHashMap<>();
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return result.values();
    }
    for (CompactModuleDefinition module : PsiTreeUtil.findChildrenOfType(file, CompactModuleDefinition.class)) {
      CompactSymbol symbol = CompactSymbols.from(module);
      if (symbol != null && symbol.getName() != null) {
        result.putIfAbsent(symbol.getName(), symbol);
      }
    }
    return result.values();
  }

  private static @Nullable CompactFile getCompactFile(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file instanceof CompactFile ? (CompactFile) file : null;
  }

  public static @NotNull List<CompactSymbol> resolveModuleSymbols(@NotNull String name, @NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>();
    for (CompactSymbol symbol : collectModuleSymbols(place)) {
      if (name.equals(symbol.getName())) {
        result.add(symbol);
      }
    }
    return result;
  }
}
