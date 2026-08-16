package dev.verloren.midnight.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.scope.CompactScope;
import dev.verloren.midnight.scope.CompactScopes;
import dev.verloren.midnight.symbol.CompactSymbol;
import dev.verloren.midnight.symbol.CompactSymbols;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CompactResolveUtil {
  private CompactResolveUtil() {
  }

  public static @NotNull List<CompactNamedElement> resolveValue(@NotNull String name, @NotNull PsiElement place) {
    return resolve(name, place, Namespace.VALUE);
  }

  public static @NotNull List<CompactNamedElement> resolveType(@NotNull String name, @NotNull PsiElement place) {
    return resolve(name, place, Namespace.TYPE);
  }

  public static @NotNull List<CompactSymbol> resolveValueSymbols(@NotNull String name, @NotNull PsiElement place) {
    return toSymbols(resolveValue(name, place));
  }

  public static @NotNull List<CompactSymbol> resolveTypeSymbols(@NotNull String name, @NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>();
    CompactSymbol builtin = CompactSymbols.builtinType(name);
    if (builtin != null) {
      result.add(builtin);
    }
    result.addAll(toSymbols(resolveType(name, place)));
    return result;
  }

  public static @NotNull Collection<CompactNamedElement> collectValueDeclarations(@NotNull PsiElement place) {
    return collectDeclarations(place, Namespace.VALUE);
  }

  public static @NotNull Collection<CompactNamedElement> collectTypeDeclarations(@NotNull PsiElement place) {
    return collectDeclarations(place, Namespace.TYPE);
  }

  public static @NotNull Collection<CompactSymbol> collectValueSymbols(@NotNull PsiElement place) {
    return toSymbols(collectValueDeclarations(place));
  }

  public static @NotNull Collection<CompactSymbol> collectTypeSymbols(@NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>(CompactSymbols.builtinTypes());
    result.addAll(toSymbols(collectTypeDeclarations(place)));
    return result;
  }

  public static @Nullable CompactScope scopeFor(@NotNull PsiElement place) {
    return CompactScopes.nearest(place);
  }

  public static @Nullable CompactModuleDefinition findModule(@NotNull PsiElement place, @Nullable String name) {
    if (name == null) {
      return null;
    }
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return null;
    }
    // 1. Search current file
    for (CompactModuleDefinition module : PsiTreeUtil.findChildrenOfType(file, CompactModuleDefinition.class)) {
      if (name.equals(module.getName())) {
        return module;
      }
    }
    // 2. Search included files
    Set<CompactFile> visited = new HashSet<>();
    visited.add(file);
    List<CompactFile> includedFiles = new ArrayList<>();
    collectIncludedFiles(file, visited, includedFiles);
    for (CompactFile incFile : includedFiles) {
      for (CompactModuleDefinition module : PsiTreeUtil.findChildrenOfType(incFile, CompactModuleDefinition.class)) {
        if (name.equals(module.getName())) {
          return module;
        }
      }
    }
    return null;
  }

  public static @NotNull List<CompactNamedElement> moduleExports(@NotNull CompactModuleDefinition module) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement member : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
      if (member == module || nearestModule(member) != module) {
        continue;
      }
      if (hasAncestorWithToken(member, CompactTokenTypes.EXPORT, module) || isListedInExportForm(member, module)) {
        result.add(member);
      }
    }
    return result;
  }

  private static boolean hasAncestorWithToken(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType tokenType, @NotNull CompactModuleDefinition module) {
    for (PsiElement p = element; p != null && p != module; p = p.getParent()) {
      if (hasToken(p, tokenType)) {
        return true;
      }
    }
    return false;
  }

  public static @NotNull List<String> prefixedImportNames(@NotNull PsiElement place, Namespace namespace) {
    List<String> result = new ArrayList<>();
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return result;
    }
    for (CompactImportDeclarationImpl importDeclaration : PsiTreeUtil.findChildrenOfType(file, CompactImportDeclarationImpl.class)) {
      String prefix = importDeclaration.getPrefix();
      if (prefix == null) {
        continue;
      }
      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module == null) {
        continue;
      }
      for (CompactNamedElement exported : moduleExports(module)) {
        String name = exported.getName();
        if (name != null && isInNamespace(exported, namespace, importDeclaration)) {
          result.add(prefix + name);
        }
      }
    }
    return result;
  }

  public static @Nullable CompactNamedElement resolveImportElementSource(@NotNull CompactImportElementImpl importElement) {
    PsiElement source = importElement.getSourceIdentifier();
    if (source == null) {
      return null;
    }
    CompactImportDeclarationImpl importDeclaration = PsiTreeUtil.getParentOfType(importElement, CompactImportDeclarationImpl.class);
    if (importDeclaration == null) {
      return null;
    }
    CompactModuleDefinition module = findModule(importDeclaration, importDeclaration.getModuleName());
    if (module == null) {
      return null;
    }
    for (CompactNamedElement exported : moduleExports(module)) {
      if (source.getText().equals(exported.getName())) {
        return exported;
      }
    }
    return null;
  }

  private static @NotNull List<CompactNamedElement> resolve(@NotNull String name, @NotNull PsiElement place, @NotNull Namespace namespace) {
    for (List<CompactNamedElement> layer : collectDeclarationLayers(place, namespace)) {
      List<CompactNamedElement> matches = new ArrayList<>();
      for (CompactNamedElement declaration : layer) {
        if (name.equals(declaration.getName()) && !PsiTreeUtil.isAncestor(declaration, place, false)) {
          matches.add(declaration);
        }
      }
      if (!matches.isEmpty()) {
        return matches;
      }
    }

    List<CompactNamedElement> prefixed = resolvePrefixedImport(name, place, namespace);
    return prefixed.isEmpty() ? List.of() : prefixed;
  }

  private static @NotNull Collection<CompactNamedElement> collectDeclarations(@NotNull PsiElement place, @NotNull Namespace namespace) {
    Map<String, CompactNamedElement> result = new LinkedHashMap<>();
    for (List<CompactNamedElement> layer : collectDeclarationLayers(place, namespace)) {
      for (CompactNamedElement declaration : layer) {
        String name = declaration.getName();
        if (name != null && !PsiTreeUtil.isAncestor(declaration, place, false)) {
          result.putIfAbsent(name, declaration);
        }
      }
    }
    return result.values();
  }

  private static @NotNull List<List<CompactNamedElement>> collectDeclarationLayers(@NotNull PsiElement place, @NotNull Namespace namespace) {
    List<List<CompactNamedElement>> layers = new ArrayList<>();
    for (PsiElement scope = place.getParent(); scope != null; scope = scope.getParent()) {
      if (isLocalScope(scope)) {
        layers.add(collectNamedBefore(scope, place, namespace));
      }
      if (scope instanceof CompactModuleDefinition) {
        layers.add(collectModuleDeclarations((CompactModuleDefinition) scope, namespace, place));
      }
      if (scope instanceof CompactFile) {
        layers.add(collectFileDeclarations((CompactFile) scope, namespace, place));
        layers.add(collectSelectionImports((CompactFile) scope, namespace));
        layers.add(collectIncludedDeclarations((CompactFile) scope, namespace, place));
        break;
      }
    }
    return layers;
  }

  private static void collectIncludedFiles(
      @NotNull CompactFile file,
      @NotNull Set<CompactFile> visited,
      @NotNull List<CompactFile> out
  ) {
    for (CompactIncludeDeclarationImpl includeDecl : PsiTreeUtil.findChildrenOfType(file, CompactIncludeDeclarationImpl.class)) {
      CompactFile includedFile = includeDecl.resolveIncludedFile();
      if (includedFile != null && visited.add(includedFile)) {
        out.add(includedFile);
        collectIncludedFiles(includedFile, visited, out);
      }
    }
  }

  private static @NotNull List<CompactNamedElement> collectIncludedDeclarations(
      @NotNull CompactFile file,
      @NotNull Namespace namespace,
      @NotNull PsiElement place
  ) {
    List<CompactNamedElement> result = new ArrayList<>();
    Set<CompactFile> visited = new HashSet<>();
    visited.add(file);
    List<CompactFile> includedFiles = new ArrayList<>();
    collectIncludedFiles(file, visited, includedFiles);

    for (CompactFile incFile : includedFiles) {
      for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(incFile, CompactNamedElement.class)) {
        if (nearestModule(declaration) == null && isInNamespace(declaration, namespace, place)) {
          result.add(declaration);
        }
      }
    }
    return result;
  }

  private static boolean isLocalScope(@NotNull PsiElement element) {
    return element instanceof CompactBlock
            || element.getNode().getElementType() == CompactElementTypes.CIRCUIT_DEFINITION
            || element.getNode().getElementType() == CompactElementTypes.WITNESS_DECLARATION
            || element.getNode().getElementType() == CompactElementTypes.CONSTRUCTOR_DEFINITION
            || element.getNode().getElementType() == CompactElementTypes.LAMBDA_EXPR
            || element.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT
            || element.getNode().getElementType() == CompactElementTypes.TYPE_ALIAS_DECLARATION
            || element.getNode().getElementType() == CompactElementTypes.STRUCT_DECLARATION
            || element.getNode().getElementType() == CompactElementTypes.CONTRACT_DECLARATION;
  }

  private static @NotNull List<CompactNamedElement> collectNamedBefore(@NotNull PsiElement scope, @NotNull PsiElement place, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    int placeOffset = place.getTextRange().getStartOffset();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(scope, CompactNamedElement.class)) {
      if (declaration.getTextRange().getStartOffset() < placeOffset && isInNamespace(declaration, namespace, place)) {
        result.add(declaration);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> collectModuleDeclarations(@NotNull CompactModuleDefinition module, @NotNull Namespace namespace, @NotNull PsiElement place) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
      if (declaration != module && nearestModule(declaration) == module && isInNamespace(declaration, namespace, place)) {
        result.add(declaration);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> collectFileDeclarations(@NotNull CompactFile file, @NotNull Namespace namespace, @NotNull PsiElement place) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(file, CompactNamedElement.class)) {
      if (nearestModule(declaration) == null && isInNamespace(declaration, namespace, place)) {
        result.add(declaration);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> collectSelectionImports(@NotNull CompactFile file, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactImportElementImpl importElement : PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class)) {
      if (isInNamespace(importElement, namespace, importElement)) {
        result.add(importElement);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> resolvePrefixedImport(@NotNull String name, @NotNull PsiElement place, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return result;
    }
    for (CompactImportDeclarationImpl importDeclaration : PsiTreeUtil.findChildrenOfType(file, CompactImportDeclarationImpl.class)) {
      String prefix = importDeclaration.getPrefix();
      if (prefix == null || !name.startsWith(prefix)) {
        continue;
      }
      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module == null) {
        continue;
      }
      String exportedName = name.substring(prefix.length());
      if (exportedName.startsWith("_")) {
        exportedName = exportedName.substring(1);
      }
      for (CompactNamedElement exported : moduleExports(module)) {
        if (exportedName.equals(exported.getName()) && isInNamespace(exported, namespace, place)) {
          result.add(exported);
        }
      }
    }
    return result;
  }

  private static boolean isInNamespace(@NotNull CompactNamedElement declaration, @NotNull Namespace namespace, @NotNull PsiElement place) {
    if (declaration instanceof CompactImportElementImpl) {
      CompactNamedElement target = resolveImportElementSource((CompactImportElementImpl) declaration);
      return target != null && isInNamespace(target, namespace, place);
    }
    if (namespace == Namespace.TYPE) {
      return declaration instanceof CompactTypeDefinition
              || declaration instanceof CompactStructDefinition
              || declaration instanceof CompactEnumDefinition
              || declaration instanceof CompactExternalContractDeclaration
              || declaration instanceof CompactGenericParameterImpl;
    }
    if (declaration instanceof CompactParameterImpl) {
      return !hasAncestorOfType(declaration, CompactElementTypes.STRUCT_FIELD);
    }
    return declaration instanceof CompactLedgerDeclaration
            || declaration instanceof CompactWitnessDeclaration
            || declaration instanceof CompactCircuitDefinition
            || declaration instanceof CompactPatternImpl
            || declaration instanceof CompactEnumDefinition;
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent.getNode() != null && parent.getNode().getElementType() == type) {
        return true;
      }
    }
    return false;
  }

  private static boolean isListedInExportForm(@NotNull CompactNamedElement member, @NotNull CompactModuleDefinition module) {
    String name = member.getName();
    if (name == null) {
      return false;
    }
    for (CompactExportDeclaration exportDeclaration : PsiTreeUtil.findChildrenOfType(module, CompactExportDeclaration.class)) {
      if (nearestModule(exportDeclaration) != module) {
        continue;
      }
      PsiElement[] children = exportDeclaration.getChildren();
      for (PsiElement child : children) {
        if (child.getNode() != null && child.getNode().getElementType() == CompactTokenTypes.IDENTIFIER && name.equals(child.getText())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hasToken(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType tokenType) {
    return element.getNode().findChildByType(tokenType) != null;
  }

  private static @Nullable CompactModuleDefinition nearestModule(@NotNull PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, CompactModuleDefinition.class);
  }

  private static @Nullable CompactFile getCompactFile(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file instanceof CompactFile ? (CompactFile) file : null;
  }

  private static @NotNull List<CompactSymbol> toSymbols(@NotNull Collection<? extends CompactNamedElement> declarations) {
    List<CompactSymbol> result = new ArrayList<>();
    for (CompactNamedElement declaration : declarations) {
      CompactSymbol symbol = CompactSymbols.from(declaration);
      if (symbol != null) {
        result.add(symbol);
      }
    }
    return result;
  }

  public enum Namespace {
    VALUE,
    TYPE
  }
}
