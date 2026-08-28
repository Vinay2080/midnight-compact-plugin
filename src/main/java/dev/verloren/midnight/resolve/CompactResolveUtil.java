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

/**
 * Core symbol resolution engine and lexical scope walker for the Compact language plugin.
 *
 * <p>Implements symbol lookup for Go to Declaration, Code Completion, Rename Refactoring,
 * Find Usages, and semantic code inspections.</p>
 *
 * <p><b>Resolution Architecture & Invariants:</b>
 * <ol>
 *   <li><b>Namespace Isolation:</b> Strictly separates {@link Namespace#VALUE} (variables, parameters, circuits, witnesses, ledger state) from {@link Namespace#TYPE} (structs, enums, aliases, generic parameters, builtins).</li>
 *   <li><b>Innermost Lexical Shadowing:</b> Scope layers are visited from innermost block scopes outward to module, file, imports, includes, and prefixes. A matching local declaration shadows identically named outer declarations.</li>
 *   <li><b>Forward-Reference Safety in Blocks:</b> Within block scopes (functions, circuits, loops), only declarations whose offset precedes the reference site are visible.</li>
 *   <li><b>Recursive Include Resolution:</b> Recursively resolves included files with cycle detection to prevent stack overflows on circular includes.</li>
 *   <li><b>Module Visibility:</b> Symbols inside modules are only visible externally if exported via {@code export} or listed in an {@code export { ... }} form.</li>
 * </ol>
 * </p>
 */
public final class CompactResolveUtil {
  private CompactResolveUtil() {
  }

  /**
   * Resolves an identifier in the VALUE namespace visible from the specified PSI site.
   *
   * @param name  identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching declarations (first match represents the innermost shadowing declaration)
   */
  public static @NotNull List<CompactNamedElement> resolveValue(@NotNull String name, @NotNull PsiElement place) {
    return resolve(name, place, Namespace.VALUE);
  }

  /**
   * Resolves an identifier in the TYPE namespace visible from the specified PSI site.
   *
   * @param name  type identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching type declarations
   */
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
      if (!isDirectModuleDeclaration(member, module)) {
        continue;
      }
      if (CompactPsiUtil.hasAncestorWithToken(member, CompactTokenTypes.EXPORT, module) || isListedInExportForm(member, module)) {
        result.add(member);
      }
    }
    return result;
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
      CompactFile importedFile = importDeclaration.resolveImportedFile();
      if (importedFile != null) {
        for (CompactNamedElement decl : PsiTreeUtil.findChildrenOfType(importedFile, CompactNamedElement.class)) {
          String name = decl.getName();
          if (isTopLevelFileDeclaration(decl) && name != null && isInNamespace(decl, namespace, importDeclaration)) {
            result.add(prefix + name);
          }
        }
        for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(importedFile, CompactModuleDefinition.class)) {
          for (CompactNamedElement exported : moduleExports(mod)) {
            String name = exported.getName();
            if (name != null && isInNamespace(exported, namespace, importDeclaration)) {
              result.add(prefix + name);
            }
          }
        }
      }
      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module != null) {
        for (CompactNamedElement exported : moduleExports(module)) {
          String name = exported.getName();
          if (name != null && isInNamespace(exported, namespace, importDeclaration)) {
            result.add(prefix + name);
          }
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
    String name = source.getText();
    CompactImportDeclarationImpl importDeclaration = PsiTreeUtil.getParentOfType(importElement, CompactImportDeclarationImpl.class);
    if (importDeclaration == null) {
      return null;
    }

    // 1. Check imported file (e.g., import { GameState } from './GameState')
    CompactFile importedFile = importDeclaration.resolveImportedFile();
    if (importedFile != null) {
      for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(importedFile, CompactNamedElement.class)) {
        if (isTopLevelFileDeclaration(declaration) && name.equals(declaration.getName())) {
          return declaration;
        }
      }
      for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(importedFile, CompactModuleDefinition.class)) {
        for (CompactNamedElement exported : moduleExports(mod)) {
          if (name.equals(exported.getName())) {
            return exported;
          }
        }
      }
    }

    // 2. Check module (e.g., import { square } from Math)
    CompactModuleDefinition module = findModule(importDeclaration, importDeclaration.getModuleName());
    if (module != null) {
      for (CompactNamedElement exported : moduleExports(module)) {
        if (name.equals(exported.getName())) {
          return exported;
        }
      }
    }
    return null;
  }

  private static @NotNull List<CompactNamedElement> resolve(@NotNull String name, @NotNull PsiElement place, @NotNull Namespace namespace) {
    for (List<CompactNamedElement> layer : collectDeclarationLayers(place, namespace)) {
      List<CompactNamedElement> matches = new ArrayList<>();
      for (CompactNamedElement declaration : layer) {
        if (name.equals(declaration.getName()) && (!PsiTreeUtil.isAncestor(declaration, place, false) || declaration instanceof CompactCircuitDefinition)) {
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
    // Walk outward from the direct parent to the root file.
    // Each scope level forms a distinct layer to enforce innermost lexical shadowing.
    for (PsiElement scope = place.getParent(); scope != null; scope = scope.getParent()) {
      if (isLocalScope(scope)) {
        // Only consider local declarations placed BEFORE the reference to enforce forward declaration rules
        layers.add(collectNamedBefore(scope, place, namespace));
      }
      if (scope instanceof CompactModuleDefinition) {
        // Enclosing module members (all top-level declarations in module available regardless of order)
        layers.add(collectModuleDeclarations((CompactModuleDefinition) scope, namespace, place));
      }
      if (scope instanceof CompactFile) {
        // Handle incomplete declarations directly preceding place at file level (e.g., during completion)
        PsiElement prev = PsiTreeUtil.getPrevSiblingOfType(place, CompactTypeDefinition.class);
        if (prev == null) {
          prev = PsiTreeUtil.getPrevSiblingOfType(place, CompactStructDefinition.class);
        }
        if (prev == null) {
          prev = PsiTreeUtil.getPrevSiblingOfType(place, CompactCircuitDefinition.class);
        }
        if (prev != null && isLocalScope(prev)) {
          layers.add(collectNamedBefore(prev, place, namespace));
        }

        // File-level top declarations (all top-level declarations in file available regardless of order),
        // then selective imports, then included files, then standard library
        layers.add(collectFileDeclarations((CompactFile) scope, namespace, place));
        layers.add(collectSelectionImports((CompactFile) scope, namespace));
        layers.add(collectIncludedDeclarations((CompactFile) scope, namespace, place));
        layers.add(collectStandardLibraryDeclarations(place.getProject(), namespace, place));
        break;
      }
    }
    return layers;
  }

  private static @NotNull List<CompactNamedElement> collectStandardLibraryDeclarations(
      @NotNull com.intellij.openapi.project.Project project,
      @NotNull Namespace namespace,
      @NotNull PsiElement place
  ) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactFile stdFile : dev.verloren.midnight.stdlib.CompactStandardLibraryProvider.getStandardLibraryFiles(project)) {
      for (CompactNamedElement decl : PsiTreeUtil.findChildrenOfType(stdFile, CompactNamedElement.class)) {
        if (isTopLevelFileDeclaration(decl) && isInNamespace(decl, namespace, place)) {
          result.add(decl);
        }
      }
    }
    return result;
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
        if (isTopLevelFileDeclaration(declaration) && isInNamespace(declaration, namespace, place)) {
          result.add(declaration);
        }
      }
    }
    return result;
  }

  private static boolean isLocalScope(@NotNull PsiElement element) {
    return element instanceof CompactBlock
            || element instanceof CompactCircuitDefinition
            || element instanceof CompactWitnessDeclaration
            || element instanceof CompactConstructorDeclaration
            || element instanceof CompactTypeDefinition
            || element instanceof CompactStructDefinition
            || (element.getNode() != null && (element.getNode().getElementType() == CompactElementTypes.LAMBDA_EXPR
                || element.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT));
  }

  private static boolean belongsToLocalScope(@NotNull CompactNamedElement declaration, @NotNull PsiElement scope) {
    if (scope instanceof CompactBlock) {
      if (declaration instanceof CompactParameterImpl || CompactPsiUtil.isPatternParameter(declaration)) {
        return false;
      }
      CompactBlock parentBlock = PsiTreeUtil.getParentOfType(declaration, CompactBlock.class);
      return parentBlock == scope;
    }
    if (scope instanceof CompactCircuitDefinition
        || scope instanceof CompactWitnessDeclaration
        || scope instanceof CompactConstructorDeclaration
        || scope instanceof CompactTypeDefinition
        || scope instanceof CompactStructDefinition) {
      if (declaration == scope) {
        return true;
      }
      if (declaration instanceof CompactParameterImpl || CompactPsiUtil.isPatternParameter(declaration)) {
        PsiElement callable = PsiTreeUtil.getParentOfType(declaration,
            CompactCircuitDefinition.class,
            CompactWitnessDeclaration.class,
            CompactConstructorDeclaration.class);
        return callable == scope;
      }
      if (declaration instanceof CompactGenericParameterImpl) {
        PsiElement owner = PsiTreeUtil.getParentOfType(declaration,
            CompactTypeDefinition.class,
            CompactStructDefinition.class,
            CompactCircuitDefinition.class,
            CompactWitnessDeclaration.class);
        return owner == scope;
      }
      return false;
    }
    if (scope.getNode() != null && scope.getNode().getElementType() == CompactElementTypes.LAMBDA_EXPR) {
      if (declaration instanceof CompactParameterImpl || CompactPsiUtil.isPatternParameter(declaration)) {
        for (PsiElement p = declaration.getParent(); p != null; p = p.getParent()) {
          if (p.getNode() != null && p.getNode().getElementType() == CompactElementTypes.LAMBDA_EXPR) {
            return p == scope;
          }
          if (p instanceof CompactBlock || p instanceof CompactFile) {
            break;
          }
        }
      }
      return false;
    }
    if (scope.getNode() != null && scope.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT) {
      for (PsiElement p = declaration.getParent(); p != null; p = p.getParent()) {
        if (p.getNode() != null && p.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT) {
          return p == scope;
        }
        if (p instanceof CompactBlock) {
          break;
        }
      }
      return false;
    }
    return true;
  }

  private static @NotNull List<CompactNamedElement> collectNamedBefore(@NotNull PsiElement scope, @NotNull PsiElement place, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    int placeOffset = place.getTextRange().getStartOffset();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(scope, CompactNamedElement.class)) {
      if (declaration.getTextRange().getStartOffset() < placeOffset
          && isInNamespace(declaration, namespace, place)
          && belongsToLocalScope(declaration, scope)) {
        result.add(declaration);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> collectModuleDeclarations(@NotNull CompactModuleDefinition module, @NotNull Namespace namespace, @NotNull PsiElement place) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
      if (isDirectModuleDeclaration(declaration, module) && isInNamespace(declaration, namespace, place)) {
        result.add(declaration);
      }
    }
    return result;
  }

  private static @NotNull List<CompactNamedElement> collectFileDeclarations(@NotNull CompactFile file, @NotNull Namespace namespace, @NotNull PsiElement place) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(file, CompactNamedElement.class)) {
      if (isTopLevelFileDeclaration(declaration) && isInNamespace(declaration, namespace, place)) {
        result.add(declaration);
      }
    }
    return result;
  }

  public static boolean isTopLevelFileDeclaration(@NotNull CompactNamedElement declaration) {
    if (nearestModule(declaration) != null) {
      return false;
    }
    return getDeclaration(declaration);
  }

  private static boolean getDeclaration(@NotNull CompactNamedElement declaration) {
    if (declaration instanceof CompactParameterImpl || CompactPsiUtil.isPatternParameter(declaration)) {
      return false;
    }
    if (declaration instanceof CompactStructFieldImpl || declaration instanceof CompactGenericParameterImpl) {
      return false;
    }
    if (declaration instanceof CompactConstBindingImpl || declaration instanceof CompactPatternImpl) {
      if (PsiTreeUtil.getParentOfType(declaration, CompactBlock.class) != null) {
        return false;
      }
      return PsiTreeUtil.getParentOfType(declaration,
              CompactCircuitDefinition.class,
              CompactWitnessDeclaration.class,
              CompactConstructorDeclaration.class) == null;
    }
    return true;
  }

  public static boolean isDirectModuleDeclaration(@NotNull CompactNamedElement declaration, @NotNull CompactModuleDefinition module) {
    if (declaration == module || nearestModule(declaration) != module) {
      return false;
    }
    return getDeclaration(declaration);
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
      String exportedName = name.substring(prefix.length());

      CompactFile importedFile = importDeclaration.resolveImportedFile();
      if (importedFile != null) {
        for (CompactNamedElement decl : PsiTreeUtil.findChildrenOfType(importedFile, CompactNamedElement.class)) {
          if (isTopLevelFileDeclaration(decl) && exportedName.equals(decl.getName()) && isInNamespace(decl, namespace, place)) {
            result.add(decl);
          }
        }
        for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(importedFile, CompactModuleDefinition.class)) {
          for (CompactNamedElement exported : moduleExports(mod)) {
            if (exportedName.equals(exported.getName()) && isInNamespace(exported, namespace, place)) {
              result.add(exported);
            }
          }
        }
      }

      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module != null) {
        for (CompactNamedElement exported : moduleExports(module)) {
          if (exportedName.equals(exported.getName()) && isInNamespace(exported, namespace, place)) {
            result.add(exported);
          }
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
      return !CompactPsiUtil.hasAncestorOfType(declaration, CompactElementTypes.STRUCT_FIELD);
    }
    return declaration instanceof CompactLedgerDeclaration
            || declaration instanceof CompactWitnessDeclaration
            || declaration instanceof CompactCircuitDefinition
            || declaration instanceof CompactPatternImpl
            || declaration instanceof CompactConstBindingImpl
            || declaration instanceof CompactEnumDefinition;
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
