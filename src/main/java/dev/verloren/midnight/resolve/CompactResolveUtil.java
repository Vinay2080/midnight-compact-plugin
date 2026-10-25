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
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * This utility class serves as the central static resolution backbone connecting the IntelliJ
 * reference subsystem, code completion providers, semantic annotators, and refactoring utilities:
 * <ul>
 *   <li><b>Reference Resolution:</b> Invoked by reference implementations
 *       ({@link dev.verloren.midnight.reference.CompactReferenceBase},
 *       {@link dev.verloren.midnight.reference.CompactTypeReference},
 *       {@link dev.verloren.midnight.reference.CompactValueReference},
 *       {@link dev.verloren.midnight.reference.CompactStructFieldReference},
 *       {@link dev.verloren.midnight.reference.CompactEnumMemberReference},
 *       {@link dev.verloren.midnight.reference.CompactImportReference}, and
 *       {@link dev.verloren.midnight.reference.CompactIncludeReference}) during "Go to Declaration",
 *       "Find Usages", and parameter hint computation.</li>
 *   <li><b>Code Completion:</b> Called by completion providers
 *       ({@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider},
 *       {@link dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider},
 *       {@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider})
 *       to collect all visible symbols and types at a specific caret offset.</li>
 *   <li><b>Semantic Code Analysis & Inspections:</b> Queried by inspections (such as unresolved
 *       reference warnings) and annotators to validate identifiers.</li>
 *   <li><b>Refactoring & Rename:</b> Used by {@code CompactRefactoringSupportProvider} and
 *       rename refactoring to find binding targets and verify name collision boundaries.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> All methods require an active Read Action
 *       ({@code @RequiresReadLock}). Direct tree traversal via {@link PsiTreeUtil} and AST
 *       node inspection will throw {@link com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments}
 *       if invoked without a read lock.</li>
 *   <li><b>Background Execution:</b> Reference resolution and completion providers execute
 *       asynchronously on pooled background threads (spawned by {@code ProgressIndicatorProvider}
 *       or IntelliJ daemon workers). Resolution must never trigger EDT write actions or block
 *       the UI thread.</li>
 *   <li><b>Cancellation Support:</b> Operates cooperatively within IntelliJ's non-blocking
 *       read actions. Subsystem callers (daemon code analyzer or completion threads) check
 *       {@code ProgressManager.checkCanceled()} between operations.</li>
 * </ul>
 *
 * <h3>Resolution Architecture & Invariants</h3>
 * <p>
 * Symbol lookup follows a layered lexical scope model with strict shadowing and namespace isolation:
 * <ol>
 *   <li><b>Namespace Isolation:</b> Strictly separates {@link Namespace#VALUE} (variables, parameters,
 *       circuits, witnesses, ledger state, enum constructors) from {@link Namespace#TYPE}
 *       (structs, enums, type aliases, generic type parameters, built-in primitive types).
 *       A value and a type with the same identifier never collide or shadow each other.</li>
 *   <li><b>Innermost Lexical Shadowing:</b> Scope layers are visited from innermost block scopes outward
 *       to module, file, imports, includes, and stdlib. The first matching declaration shadows
 *       identically named outer declarations.</li>
 *   <li><b>Forward-Reference Safety in Local Scopes:</b> Within local block scopes (functions, circuits,
 *       lambdas, loops), only declarations whose text offset strictly precedes the reference site are
 *       visible. Top-level declarations (files, modules, includes) are order-independent and allow
 *       forward references.</li>
 *   <li><b>Recursive Include Resolution with Cycle Detection:</b> Resolves symbols declared across
 *       {@code include} graphs recursively, maintaining a {@code Set<CompactFile> visited} barrier to
 *       prevent infinite loops and stack overflows on circular includes.</li>
 *   <li><b>Module Visibility:</b> Symbols declared inside a {@link CompactModuleDefinition} are private
 *       to that module unless explicitly tagged with the {@code export} modifier or declared within an
 *       {@code export { ... }} block.</li>
 *   <li><b>Completion Lookahead Resilience:</b> Handles partially typed or incomplete syntax tree nodes
 *       preceding the caret at file scope (e.g., during active typing in completion).</li>
 * </ol>
 * </p>
 *
 * <h3>Scope Layering Visual Diagram</h3>
 * <pre>
 *   [Reference Site / Caret (place)]
 *              │
 *              ▼
 *   ┌──────────────────────────────────────────────────────────────────┐
 *   │ 1. Local Scopes (Blocks, For Loops, Lambdas) [Offset-checked]    │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 2. Callable / Definition Scope (Parameters, Generics, Self)      │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 3. Enclosing Module Scope (Direct module members)                │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 4. File-Level Top Declarations (Order-independent)              │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 5. Selective Explicit Imports (import { X } from ...)            │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 6. Included Files (include "..." recursive, cycle-checked)       │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 7. Standard Library Declarations (CompactStdlibService)          │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │ 8. Prefixed / Module-Qualified Imports (prefix + name matching)  │
 *   └──────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Design Decisions & Memory Guards</h3>
 * <ul>
 *   <li><b>Stateless Utility:</b> Class contains only static methods and a private constructor.
 *       No instance state is maintained.</li>
 *   <li><b>Memory Leak Prevention:</b> No {@link com.intellij.openapi.project.Project},
 *       {@link PsiElement}, or {@link PsiFile} references are cached or stored in static fields.
 *       All PSI objects are localized to method call stacks, allowing garbage collection when
 *       files or projects are closed.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.completion.providers.CompactValueCompletionProvider
 * @see dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider
 * @see dev.verloren.midnight.scope.CompactScopes
 * @see dev.verloren.midnight.stdlib.CompactStdlibService
 */
public final class CompactResolveUtil {

  /**
   * Private constructor preventing external instantiation of this stateless utility class.
   */
  private CompactResolveUtil() {
  }

  /**
   * Resolves an identifier in the {@link Namespace#VALUE} namespace visible from the specified PSI site.
   *
   * <p>Searches local variables, parameters, pattern bindings, circuit/witness definitions,
   * ledger declarations, file-level values, imports, includes, and stdlib definitions.</p>
   *
   * @param name  identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching declarations (first match represents the innermost shadowing declaration)
   */
  public static @NotNull List<CompactNamedElement> resolveValue(@NotNull String name, @NotNull PsiElement place) {
    return resolve(name, place, Namespace.VALUE);
  }

  /**
   * Resolves an identifier in the {@link Namespace#TYPE} namespace visible from the specified PSI site.
   *
   * <p>Searches struct definitions, enum definitions, type aliases, generic type parameters,
   * external contract definitions, imported types, included types, and stdlib types.</p>
   *
   * @param name  type identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching type declarations (first match represents the innermost shadowing declaration)
   */
  public static @NotNull List<CompactNamedElement> resolveType(@NotNull String name, @NotNull PsiElement place) {
    return resolve(name, place, Namespace.TYPE);
  }

  /**
   * Resolves an identifier in the {@link Namespace#VALUE} namespace and maps all matching declarations
   * to high-level {@link CompactSymbol} representations.
   *
   * @param name  value identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching value symbols
   */
  public static @NotNull List<CompactSymbol> resolveValueSymbols(@NotNull String name, @NotNull PsiElement place) {
    return toSymbols(resolveValue(name, place));
  }

  /**
   * Resolves an identifier in the {@link Namespace#TYPE} namespace and maps matching declarations
   * to high-level {@link CompactSymbol} representations, including built-in types (e.g. {@code Field},
   * {@code Boolean}, {@code Uint<... >}).
   *
   * @param name  type identifier text to resolve
   * @param place the PSI context where the reference occurs
   * @return list of matching type symbols, prepended with any matching builtin type symbol
   */
  public static @NotNull List<CompactSymbol> resolveTypeSymbols(@NotNull String name, @NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>();
    // Step 1: Check if the identifier matches a known builtin type
    CompactSymbol builtin = CompactSymbols.builtinType(name);
    if (builtin != null) {
      result.add(builtin);
    }
    // Step 2: Resolve user-defined types in lexical scope and append
    result.addAll(toSymbols(resolveType(name, place)));
    return result;
  }

  /**
   * Collects all unique declarations in the {@link Namespace#VALUE} namespace visible from the given place.
   *
   * <p>Used primarily by code completion to populate candidate lookup elements.
   * Employs a {@link LinkedHashMap} to preserve innermost shadowing bindings.</p>
   *
   * @param place the PSI context where completion or lookup is requested
   * @return collection of visible value declarations
   */
  public static @NotNull Collection<CompactNamedElement> collectValueDeclarations(@NotNull PsiElement place) {
    return collectDeclarations(place, Namespace.VALUE);
  }

  /**
   * Collects all unique declarations in the {@link Namespace#TYPE} namespace visible from the given place.
   *
   * <p>Used by code completion to populate candidate type lookup elements.</p>
   *
   * @param place the PSI context where completion or lookup is requested
   * @return collection of visible type declarations
   */
  public static @NotNull Collection<CompactNamedElement> collectTypeDeclarations(@NotNull PsiElement place) {
    return collectDeclarations(place, Namespace.TYPE);
  }

  /**
   * Collects all unique value symbols visible from the given place, mapped to {@link CompactSymbol}.
   *
   * @param place the PSI context where lookup occurs
   * @return collection of visible value symbols
   */
  public static @NotNull Collection<CompactSymbol> collectValueSymbols(@NotNull PsiElement place) {
    return toSymbols(collectValueDeclarations(place));
  }

  /**
   * Collects all unique type symbols visible from the given place, including all language-builtin
   * types and user-defined types in scope.
   *
   * @param place the PSI context where lookup occurs
   * @return collection of visible type symbols with builtins included
   */
  public static @NotNull Collection<CompactSymbol> collectTypeSymbols(@NotNull PsiElement place) {
    List<CompactSymbol> result = new ArrayList<>(CompactSymbols.builtinTypes());
    result.addAll(toSymbols(collectTypeDeclarations(place)));
    return result;
  }

  /**
   * Computes the nearest lexical scope enclosing the specified PSI element.
   *
   * @param place the PSI element to inspect
   * @return the nearest enclosing {@link CompactScope}, or {@code null} if outside any known scope
   */
  public static @Nullable CompactScope scopeFor(@NotNull PsiElement place) {
    return CompactScopes.nearest(place);
  }

  /**
   * Finds a module definition by name accessible from the given PSI context.
   *
   * <p>Searches:
   * <ol>
   *   <li>Top-level module definitions in the current file.</li>
   *   <li>Module definitions declared in transitively included files (with cycle detection).</li>
   * </ol>
   * </p>
   *
   * @param place the PSI element initiating the lookup
   * @param name  module name to locate, or {@code null}
   * @return the matching {@link CompactModuleDefinition}, or {@code null} if not found
   */
  public static @Nullable CompactModuleDefinition findModule(@NotNull PsiElement place, @Nullable String name) {
    if (name == null) {
      return null;
    }
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return null;
    }
    // 1. Search current file for module definition
    for (CompactModuleDefinition module : PsiTreeUtil.findChildrenOfType(file, CompactModuleDefinition.class)) {
      if (name.equals(module.getName())) {
        return module;
      }
    }
    // 2. Search included files recursively
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

  /**
   * Extracts all public declarations exported from the specified module.
   *
   * <p>A member is considered exported if:
   * <ul>
   *   <li>It has an ancestor node containing the {@link CompactTokenTypes#EXPORT} token
   *       (e.g., {@code export circuit foo() { ... }}), OR</li>
   *   <li>Its identifier is explicitly listed in a module-level export statement
   *       (e.g., {@code export { foo, bar }}).</li>
   * </ul>
   * </p>
   *
   * @param module the module definition to inspect
   * @return list of exported member declarations
   */
  public static @NotNull List<CompactNamedElement> moduleExports(@NotNull CompactModuleDefinition module) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement member : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
      // Step 1: Ensure the member is a direct top-level declaration of this module (not deeply nested in a block)
      if (!isDirectModuleDeclaration(member, module)) {
        continue;
      }
      // Step 2: Check for explicit 'export' keyword token or presence in an 'export { ... }' block
      if (CompactPsiUtil.hasAncestorWithToken(member, CompactTokenTypes.EXPORT, module) || isListedInExportForm(member, module)) {
        result.add(member);
      }
    }
    return result;
  }

  /**
   * Generates prefixed symbol names for all declarations exported by modules or files
   * imported with a prefix alias (e.g. {@code import * as Prefix from "./module"}).
   *
   * @param place     the PSI context where completion is invoked
   * @param namespace target namespace (VALUE or TYPE)
   * @return list of fully prefixed candidate names (e.g. {@code "Prefix.foo"})
   */
  public static @NotNull List<String> prefixedImportNames(@NotNull PsiElement place, Namespace namespace) {
    List<String> result = new ArrayList<>();
    CompactFile file = getCompactFile(place);
    if (file == null) {
      return result;
    }
    // Inspect each import declaration in the file looking for prefix modifiers
    for (CompactImportDeclarationImpl importDeclaration : PsiTreeUtil.findChildrenOfType(file, CompactImportDeclarationImpl.class)) {
      String prefix = importDeclaration.getPrefix();
      if (prefix == null) {
        continue;
      }
      // Check imported file exports
      CompactFile importedFile = importDeclaration.resolveImportedFile();
      if (importedFile != null) {
        for (CompactNamedElement decl : PsiTreeUtil.findChildrenOfType(importedFile, CompactNamedElement.class)) {
          String name = decl.getName();
          if (isTopLevelFileDeclaration(decl) && name != null && isInNamespace(decl, namespace)) {
            result.add(prefix + name);
          }
        }
        for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(importedFile, CompactModuleDefinition.class)) {
          for (CompactNamedElement exported : moduleExports(mod)) {
            String name = exported.getName();
            if (name != null && isInNamespace(exported, namespace)) {
              result.add(prefix + name);
            }
          }
        }
      }
      // Check named module exports
      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module != null) {
        for (CompactNamedElement exported : moduleExports(module)) {
          String name = exported.getName();
          if (name != null && isInNamespace(exported, namespace)) {
            result.add(prefix + name);
          }
        }
      }
    }
    return result;
  }

  /**
   * Resolves an individual import element inside a selective import statement
   * (e.g. {@code GameState} in {@code import { GameState } from "./GameState"}) to its original definition.
   *
   * @param importElement the import element PSI node to resolve
   * @return the source declaration in the target file or module, or {@code null} if unresolved
   */
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

  /**
   * Core resolution algorithm evaluating layered lexical scopes in order from innermost to outermost.
   *
   * <p>Short-circuits upon the first layer yielding matching declarations to implement lexical shadowing.
   * Self-resolution is prevented unless the declaration is an enclosing recursive circuit.</p>
   *
   * @param name      identifier text to resolve
   * @param place     reference site
   * @param namespace target namespace (VALUE or TYPE)
   * @return list of matching declarations in the winning scope layer, or empty list
   */
  private static @NotNull List<CompactNamedElement> resolve(@NotNull String name, @NotNull PsiElement place, @NotNull Namespace namespace) {
    // Step 1: Iterate through declaration layers from innermost outward
    for (List<CompactNamedElement> layer : collectDeclarationLayers(place, namespace)) {
      List<CompactNamedElement> matches = new ArrayList<>();
      for (CompactNamedElement declaration : layer) {
        // Guard against self-reference (a declaration cannot resolve to itself) unless it is a recursive circuit
        if (name.equals(declaration.getName()) && (!PsiTreeUtil.isAncestor(declaration, place, false) || declaration instanceof CompactCircuitDefinition)) {
          matches.add(declaration);
        }
      }

      // Step 2: First non-empty layer wins (lexical shadowing)
      if (!matches.isEmpty()) {
        return matches;
      }
    }

    // Step 3: Fall back to prefixed / aliased imports if no un-prefixed declaration matched
    List<CompactNamedElement> prefixed = resolvePrefixedImport(name, place, namespace);
    return prefixed.isEmpty() ? List.of() : prefixed;
  }

  /**
   * Collects all visible declarations across all scope layers into a map keyed by name.
   *
   * <p>Uses {@link Map#putIfAbsent(Object, Object)} so that inner declarations are registered
   * first and prevent outer declarations of the same name from replacing them.</p>
   *
   * @param place     reference site
   * @param namespace target namespace (VALUE or TYPE)
   * @return collection of unique visible declarations
   */
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

  /**
   * Constructs an ordered sequence of declaration layers by traversing the PSI tree upward
   * from {@code place.getParent()} to the root {@link CompactFile}.
   *
   * <p>Each AST node that introduces a lexical scope creates a distinct layer:</p>
   * <ol>
   *   <li>Local blocks, loops, lambdas (forward-offset checked).</li>
   *   <li>Enclosing callables/types (parameters, type parameters).</li>
   *   <li>Enclosing module definitions.</li>
   *   <li>Incomplete declarations preceding {@code place} at file root (completion fallback).</li>
   *   <li>File-level top declarations.</li>
   *   <li>Selective imports.</li>
   *   <li>Included files (transitive).</li>
   *   <li>Standard library definitions.</li>
   * </ol>
   *
   * @param place     reference site
   * @param namespace target namespace
   * @return list of declaration layers from innermost to outermost
   */
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
        layers.add(collectModuleDeclarations((CompactModuleDefinition) scope, namespace));
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
        layers.add(collectFileDeclarations((CompactFile) scope, namespace));
        layers.add(collectSelectionImports((CompactFile) scope, namespace));
        layers.add(collectIncludedDeclarations((CompactFile) scope, namespace));
        layers.add(collectStandardLibraryDeclarations(place.getProject(), namespace));
        break;
      }
    }
    return layers;
  }

  /**
   * Retrieves declarations from the Compact standard library via {@link dev.verloren.midnight.stdlib.CompactStdlibService}.
   *
   * @param project   the active project context
   * @param namespace target namespace
   * @return list of matching standard library declarations
   */
  private static @NotNull List<CompactNamedElement> collectStandardLibraryDeclarations(
      @NotNull com.intellij.openapi.project.Project project,
      @NotNull Namespace namespace
  ) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactFile stdFile : dev.verloren.midnight.stdlib.CompactStdlibService.getInstance(project).getStandardLibraryFiles()) {
      for (CompactNamedElement decl : stdFile.getTopLevelDeclarations()) {
        if (isInNamespace(decl, namespace)) {
          result.add(decl);
        }
      }
    }
    return result;
  }

  /**
   * Recursively traverses all files included via {@code include "..."} directives, accumulating
   * them into {@code out} while using {@code visited} to guard against circular include recursion.
   *
   * @param file    the file to inspect for include directives
   * @param visited set of already traversed files (cycle barrier)
   * @param out     list accumulating transitively included files in discovery order
   */
  private static void collectIncludedFiles(
      @NotNull CompactFile file,
      @NotNull Set<CompactFile> visited,
      @NotNull List<CompactFile> out
  ) {
    for (CompactIncludeDeclarationImpl includeDecl : PsiTreeUtil.findChildrenOfType(file, CompactIncludeDeclarationImpl.class)) {
      CompactFile includedFile = includeDecl.resolveIncludedFile();
      // visited.add returns true only if the file was not already present in the set
      if (includedFile != null && visited.add(includedFile)) {
        out.add(includedFile);
        collectIncludedFiles(includedFile, visited, out);
      }
    }
  }

  /**
   * Gathers all top-level declarations from transitively included files matching the given namespace.
   *
   * @param file      root file containing include directives
   * @param namespace target namespace
   * @return list of matching declarations from included files
   */
  private static @NotNull List<CompactNamedElement> collectIncludedDeclarations(
      @NotNull CompactFile file,
      @NotNull Namespace namespace
  ) {
    List<CompactNamedElement> result = new ArrayList<>();
    Set<CompactFile> visited = new HashSet<>();
    visited.add(file);
    List<CompactFile> includedFiles = new ArrayList<>();
    collectIncludedFiles(file, visited, includedFiles);

    for (CompactFile incFile : includedFiles) {
      for (CompactNamedElement declaration : incFile.getTopLevelDeclarations()) {
        if (isInNamespace(declaration, namespace)) {
          result.add(declaration);
        }
      }
    }
    return result;
  }

  /**
   * Tests whether an AST element establishes a local lexical scope.
   *
   * <p>Recognized local scope elements:
   * <ul>
   *   <li>Code blocks ({@link CompactBlock})</li>
   *   <li>Circuit definitions ({@link CompactCircuitDefinition})</li>
   *   <li>Witness declarations ({@link CompactWitnessDeclaration})</li>
   *   <li>Constructor declarations ({@link CompactConstructorDeclaration})</li>
   *   <li>Type definitions ({@link CompactTypeDefinition})</li>
   *   <li>Struct definitions ({@link CompactStructDefinition})</li>
   *   <li>Lambda expressions ({@code LAMBDA_EXPR})</li>
   *   <li>For-loop statements ({@code FOR_STATEMENT})</li>
   * </ul>
   * </p>
   *
   * @param element the AST element to test
   * @return {@code true} if element represents a local lexical scope
   */
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

  /**
   * Determines whether a declaration belongs directly to the specified local scope container,
   * preventing parameters from leaking into block scopes and ensuring proper generic ownership.
   *
   * @param declaration candidate declaration
   * @param scope       the scope container being populated
   * @return {@code true} if the declaration belongs directly to {@code scope}
   */
  private static boolean belongsToLocalScope(@NotNull CompactNamedElement declaration, @NotNull PsiElement scope) {
    // Case 1: Block scope - parameters and pattern params do not belong to block scope directly
    if (scope instanceof CompactBlock) {
      if (declaration instanceof CompactParameterImpl || CompactPsiUtil.isPatternParameter(declaration)) {
        return false;
      }
      CompactBlock parentBlock = PsiTreeUtil.getParentOfType(declaration, CompactBlock.class);
      return parentBlock == scope;
    }
    // Case 2: Callable or Type definition scopes (parameters, generics, or self)
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
    // Case 3: Lambda expressions
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
    // Case 4: For loop statements
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

  /**
   * Collects declarations in a local scope whose text offset strictly precedes the reference site.
   *
   * <p>Enforces the language requirement that local variables and bindings cannot be referenced
   * before their declaration line.</p>
   *
   * @param scope     the local scope element to search
   * @param place     the reference site providing the cutoff offset
   * @param namespace target namespace
   * @return list of preceding named declarations
   */
  private static @NotNull List<CompactNamedElement> collectNamedBefore(@NotNull PsiElement scope, @NotNull PsiElement place, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    int placeOffset = place.getTextRange().getStartOffset();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(scope, CompactNamedElement.class)) {
      if (declaration.getTextRange().getStartOffset() < placeOffset
          && isInNamespace(declaration, namespace)
          && belongsToLocalScope(declaration, scope)) {
        result.add(declaration);
      }
    }
    return result;
  }

  /**
   * Collects all top-level declarations directly owned by a module.
   *
   * @param module    the module definition to inspect
   * @param namespace target namespace
   * @return list of direct module declarations in namespace
   */
  private static @NotNull List<CompactNamedElement> collectModuleDeclarations(@NotNull CompactModuleDefinition module, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
      if (isDirectModuleDeclaration(declaration, module) && isInNamespace(declaration, namespace)) {
        result.add(declaration);
      }
    }
    return result;
  }

  /**
   * Collects all top-level declarations in the given file matching the namespace.
   *
   * @param file      the file to inspect
   * @param namespace target namespace
   * @return list of file-level declarations in namespace
   */
  private static @NotNull List<CompactNamedElement> collectFileDeclarations(@NotNull CompactFile file, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactNamedElement declaration : file.getTopLevelDeclarations()) {
      if (isInNamespace(declaration, namespace)) {
        result.add(declaration);
      }
    }
    return result;
  }

  /**
   * Checks whether a declaration is a top-level file declaration (not enclosed in any module).
   *
   * @param declaration candidate declaration
   * @return {@code true} if declaration is at top-level file scope
   */
  public static boolean isTopLevelFileDeclaration(@NotNull CompactNamedElement declaration) {
    if (nearestModule(declaration) != null) {
      return false;
    }
    return getDeclaration(declaration);
  }

  /**
   * Helper filter distinguishing top-level statements/definitions from nested parameters,
   * struct fields, or local block bindings.
   *
   * @param declaration candidate declaration
   * @return {@code true} if declaration is a valid standalone definition
   */
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

  /**
   * Checks whether a declaration is a direct child of the specified module (not nested within a block or inner type).
   *
   * @param declaration candidate declaration
   * @param module      target module definition
   * @return {@code true} if directly declared at the module level
   */
  public static boolean isDirectModuleDeclaration(@NotNull CompactNamedElement declaration, @NotNull CompactModuleDefinition module) {
    if (declaration == module || nearestModule(declaration) != module) {
      return false;
    }
    return getDeclaration(declaration);
  }

  /**
   * Collects all selectively imported elements ({@code import { X, Y } from ...}) in a file.
   *
   * @param file      file containing import statements
   * @param namespace target namespace
   * @return list of imported elements matching the namespace
   */
  private static @NotNull List<CompactNamedElement> collectSelectionImports(@NotNull CompactFile file, @NotNull Namespace namespace) {
    List<CompactNamedElement> result = new ArrayList<>();
    for (CompactImportElementImpl importElement : PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class)) {
      if (isInNamespace(importElement, namespace)) {
        result.add(importElement);
      }
    }
    return result;
  }

  /**
   * Resolves an identifier with a prefix alias (e.g. {@code Prefix.name} or {@code Prefixname})
   * against imported files and modules.
   *
   * @param name      composite or prefixed name
   * @param place     reference site
   * @param namespace target namespace
   * @return list of resolved matching declarations
   */
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

      // 1. Check exports of imported file
      CompactFile importedFile = importDeclaration.resolveImportedFile();
      if (importedFile != null) {
        for (CompactNamedElement decl : PsiTreeUtil.findChildrenOfType(importedFile, CompactNamedElement.class)) {
          if (isTopLevelFileDeclaration(decl) && exportedName.equals(decl.getName()) && isInNamespace(decl, namespace)) {
            result.add(decl);
          }
        }
        for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(importedFile, CompactModuleDefinition.class)) {
          for (CompactNamedElement exported : moduleExports(mod)) {
            if (exportedName.equals(exported.getName()) && isInNamespace(exported, namespace)) {
              result.add(exported);
            }
          }
        }
      }

      // 2. Check exports of named module
      CompactModuleDefinition module = findModule(place, importDeclaration.getModuleName());
      if (module != null) {
        for (CompactNamedElement exported : moduleExports(module)) {
          if (exportedName.equals(exported.getName()) && isInNamespace(exported, namespace)) {
            result.add(exported);
          }
        }
      }
    }
    return result;
  }

  /**
   * Classifies a declaration into the {@link Namespace#TYPE} or {@link Namespace#VALUE} namespace.
   *
   * <h3>Classification Rules</h3>
   * <ul>
   *   <li><b>Import Elements:</b> Transparently resolves to the underlying source element and re-evaluates.</li>
   *   <li><b>TYPE Namespace:</b> {@link CompactTypeDefinition}, {@link CompactStructDefinition},
   *       {@link CompactEnumDefinition}, {@link CompactExternalContractDeclaration}, and
   *       {@link CompactGenericParameterImpl}.</li>
   *   <li><b>VALUE Namespace:</b> {@link CompactLedgerDeclaration}, {@link CompactWitnessDeclaration},
   *       {@link CompactCircuitDefinition}, {@link CompactPatternImpl}, {@link CompactConstBindingImpl},
   *       {@link CompactParameterImpl} (excluding struct fields), and {@link CompactEnumDefinition} (as constructor).</li>
   * </ul>
   *
   * @param declaration the declaration element to classify
   * @param namespace   target namespace to test against
   * @return {@code true} if declaration belongs to the specified namespace
   */
  private static boolean isInNamespace(@NotNull CompactNamedElement declaration, @NotNull Namespace namespace) {
    if (declaration instanceof CompactImportElementImpl) {
      CompactNamedElement target = resolveImportElementSource((CompactImportElementImpl) declaration);
      return target != null && isInNamespace(target, namespace);
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

  /**
   * Checks whether a module member is explicitly listed in a module's {@code export { ... }} block.
   *
   * @param member candidate member element
   * @param module enclosing module
   * @return {@code true} if member's name appears in an export form inside the module
   */
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

  /**
   * Locates the nearest enclosing {@link CompactModuleDefinition} ancestor of an element.
   *
   * @param element the element to inspect
   * @return nearest enclosing module, or {@code null} if at file top-level
   */
  private static @Nullable CompactModuleDefinition nearestModule(@NotNull PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, CompactModuleDefinition.class);
  }

  /**
   * Casts or unwraps the containing file of an element to a typed {@link CompactFile}.
   *
   * @param element the element to inspect
   * @return the containing {@link CompactFile}, or {@code null} if not a Compact file
   */
  private static @Nullable CompactFile getCompactFile(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file instanceof CompactFile ? (CompactFile) file : null;
  }

  /**
   * Converts a collection of PSI named elements into high-level {@link CompactSymbol} instances.
   *
   * @param declarations collection of declaration elements
   * @return list of non-null symbols converted via {@link CompactSymbols#from(CompactNamedElement)}
   */
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

  /**
   * Symbol namespace discriminator distinguishing value identifiers from type identifiers.
   *
   * <p>Enforces strict namespace separation during resolution and completion so that types and
   * values never clash or inappropriately shadow each other.</p>
   */
  public enum Namespace {
    /**
     * Value namespace: variables, parameters, circuits, witnesses, ledger state, and const bindings.
     */
    VALUE,

    /**
     * Type namespace: structs, enums, type aliases, generic type parameters, and builtins.
     */
    TYPE
  }
}
