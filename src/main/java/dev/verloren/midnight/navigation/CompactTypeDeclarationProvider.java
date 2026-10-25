package dev.verloren.midnight.navigation;

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles "Go to Type Declaration" (Ctrl+Shift+B / Cmd+Shift+B) in Compact smart contract files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.typeDeclarationProvider} extension point.
 * Enables developers to jump directly past variable bindings, parameters, and expressions to the underlying
 * data structure declaration defining their type (e.g. struct, enum, contract interface, or type alias):
 * <ul>
 *   <li><b>Explicit Type References:</b> When triggered on a type annotation (e.g. {@code p: Point}), jumps
 *       directly to {@code struct Point} or {@code type Point}.</li>
 *   <li><b>Value Bindings &amp; Parameters:</b> When invoked on a variable (e.g. {@code const x = p;}), discovers
 *       the underlying type through AST inspection or type inference and navigates to its type declaration.</li>
 *   <li><b>Ledger State:</b> Resolves the declared type of ledger declarations (e.g. {@code ledger state: State}).</li>
 *   <li><b>Enum Members:</b> Jumps from an enum case (e.g. {@code Color.Red}) directly to the parent {@code enum Color}.</li>
 *   <li><b>Circuits &amp; Witnesses:</b> Inspects the return type of a circuit/witness and navigates to its type definition.</li>
 *   <li><b>Built-in Primitives:</b> Safely filters out built-in types (e.g. {@code Field}, {@code Boolean}, {@code Uint})
 *       since they have no source declaration target.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction} when the user
 * triggers "Go to Type Declaration" at the caret position. The platform passes the leaf {@link PsiElement}
 * at the caret. If a non-null, non-empty array of target declarations is returned, the editor navigates to the target.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Invoked on the Event Dispatch Thread (EDT) or pooled background threads.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while traversing PSI
 *       trees, type elements, and resolving references.</li>
 *   <li><b>Write Actions:</b> Pure query provider; never executes write actions.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline Diagram</h3>
 * <pre>
 *   [User presses Ctrl+Shift+B / "Go to Type Declaration"]
 *                             │
 *                             ▼
 *              [CompactTypeDeclarationProvider]
 *                             │
 *         ┌───────────────────┼───────────────────┐
 *         ▼                   ▼                   ▼
 *   Tier 1: Inside explicit  Tier 2: Direct or    Tier 3: Semantic target
 *   CompactTypeReference     parent reference     (CompactNamedElement or
 *   (e.g. `x: Point`)        target's type        CompactExpression)
 *         │                   │                   │
 *         └───────────────────┼───────────────────┘
 *                             ▼
 *                  [collectTypeDeclarations]
 *                  ├── Explicit Type Annotation (Parameter, Const, Ledger, Field)
 *                  ├── Enum Member ──► Enclosing Enum Definition
 *                  ├── Callable ──────► Return Type Definition
 *                  └── Expression ────► Inferred Type ──► Struct / Enum / Type Alias
 * </pre>
 *
 * @see dev.verloren.midnight.navigation.CompactGotoDeclarationHandler
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
 */
public class CompactTypeDeclarationProvider implements TypeDeclarationProvider {

  /**
   * Regular expression matching standard Compact identifier tokens within composite type signatures.
   */
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  /**
   * Resolves the underlying type declaration targets for the specified symbol.
   *
   * @param symbol the PSI element under the caret
   * @return array of target type declarations (struct, enum, type alias, contract), or null if none
   */
  @Override
  public PsiElement @Nullable [] getSymbolTypeDeclarations(@NotNull PsiElement symbol) {
    if (!symbol.isValid() || symbol.getLanguage() != CompactLanguage.INSTANCE) {
      return null;
    }

    Set<PsiElement> results = new LinkedHashSet<>();

    // Step 1: If the symbol is inside an explicit type reference (e.g. `p: Point`), resolve directly
    CompactTypeReferenceImpl typeRef = PsiTreeUtil.getParentOfType(symbol, CompactTypeReferenceImpl.class, false);
    if (typeRef != null) {
      addResolvedTypeReference(typeRef, results);
      if (!results.isEmpty()) {
        return toResultArray(results);
      }
    }

    // Step 2: Resolve references directly on or around the symbol (e.g. parameter/variable usage)
    resolveAndCollect(symbol, results);
    if (!results.isEmpty()) {
      return toResultArray(results);
    }

    // Step 3: Identify the candidate target semantic element (declaration or expression)
    PsiElement target = findSemanticTarget(symbol);
    if (target != null) {
      collectTypeDeclarations(target, results);
      if (!results.isEmpty()) {
        return toResultArray(results);
      }
    }

    return null;
  }

  /**
   * Collects type declarations from a resolved semantic target (declaration, binding, or expression).
   *
   * @param target  the semantic target element to inspect
   * @param results accumulator set collecting resolved type definitions
   */
  private void collectTypeDeclarations(@NotNull PsiElement target, @NotNull Set<PsiElement> results) {
    // Step A: Explicit type annotations on declarations
    if (target instanceof CompactParameterImpl || target instanceof CompactConstBindingImpl) {
      if (extractFromContainer(target, target, results)) {
        return;
      }
    } else if (target instanceof CompactPatternImpl) {
      PsiElement parent = target.getParent();
      if (parent != null && extractFromContainer(parent, target, results)) {
        return;
      }
    } else if (target instanceof CompactLedgerDeclarationImpl ledger) {
      CompactTypeElement te = ledger.getTypeElement();
      if (te != null) {
        addDeclarationsFromTypeElement(te, results);
        if (!results.isEmpty()) return;
      }
    } else if (target instanceof CompactStructFieldImpl) {
      CompactTypeElement te = PsiTreeUtil.findChildOfType(target, CompactTypeElement.class);
      if (te != null) {
        addDeclarationsFromTypeElement(te, results);
        if (!results.isEmpty()) return;
      }
    } else if (target instanceof CompactTypeDefinition typeDef) {
      CompactTypeElement te = typeDef.getTargetTypeElement();
      if (te != null) {
        addDeclarationsFromTypeElement(te, results);
        if (!results.isEmpty()) return;
      }
    } else if (target instanceof CompactEnumMemberImpl enumMember) {
      CompactEnumDefinition enumDef = PsiTreeUtil.getParentOfType(enumMember, CompactEnumDefinition.class);
      if (enumDef != null) {
        results.add(enumDef);
        return;
      }
    }

    // Step B: Semantic return type for callables (circuits and witnesses)
    if (target instanceof CompactCircuitDefinition || target instanceof CompactWitnessDeclaration) {
      CompactType retType = CompactPsiUtil.getCallableReturnType(target);
      resolveTypeByName(retType.name(), target, results);
      if (!results.isEmpty()) return;
    }

    // Step C: Inferred semantic type of expressions / bindings
    CompactType type = null;
    switch (target) {
      case CompactExpression expr -> type = expr.getType();
      case CompactConstBindingImpl constBinding -> type = constBinding.getType();
      case CompactNamedElement named -> type = named.getType();
      default -> {
      }
    }

    if (type != null && !CompactPrimitiveType.UNKNOWN.equals(type)) {
      resolveTypeByName(type.name(), target, results);
    }
  }

  /**
   * Traverses child type elements within a declaration container to extract and resolve type declarations.
   *
   * @param container the enclosing declaration AST node
   * @param excluded  optional child element to exclude from inspection
   * @param results   accumulator set collecting resolved type definitions
   * @return true if at least one type declaration was resolved; false otherwise
   */
  private boolean extractFromContainer(
      @NotNull PsiElement container,
      @Nullable PsiElement excluded,
      @NotNull Set<PsiElement> results
  ) {
    CompactTypeReferenceImpl ref = PsiTreeUtil.findChildOfType(container, CompactTypeReferenceImpl.class);
    if (ref != null) {
      addDeclarationsFromTypeElement(ref, results);
      if (!results.isEmpty()) {
        return true;
      }
    }
    for (CompactTypeElement child : PsiTreeUtil.findChildrenOfType(container, CompactTypeElement.class)) {
      if (child != excluded && !(child instanceof CompactPatternImpl)) {
        addDeclarationsFromTypeElement(child, results);
        if (!results.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Recursively traverses a Compact type element to resolve all referenced custom type declarations.
   *
   * @param te      the type element AST node
   * @param results accumulator set collecting resolved type definitions
   */
  private void addDeclarationsFromTypeElement(@NotNull CompactTypeElement te, @NotNull Set<PsiElement> results) {
    if (te instanceof CompactTypeReferenceImpl refImpl) {
      addResolvedTypeReference(refImpl, results);
    }
    for (CompactTypeReferenceImpl childRef : PsiTreeUtil.findChildrenOfType(te, CompactTypeReferenceImpl.class)) {
      addResolvedTypeReference(childRef, results);
    }
    if (results.isEmpty()) {
      CompactType type = te.getType();
      if (!CompactPrimitiveType.UNKNOWN.equals(type)) {
        resolveTypeByName(type.name(), te, results);
      }
    }
  }

  /**
   * Resolves a single {@link CompactTypeReferenceImpl} by reference or by identifier lookup.
   *
   * @param typeRef the type reference AST node
   * @param results accumulator set collecting resolved type definitions
   */
  private void addResolvedTypeReference(@NotNull CompactTypeReferenceImpl typeRef, @NotNull Set<PsiElement> results) {
    PsiReference ref = typeRef.getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      if (resolved instanceof CompactImportElementImpl importElement) {
        resolved = CompactResolveUtil.resolveImportElementSource(importElement);
      }
      if (resolved != null && isTypeDeclaration(resolved)) {
        results.add(resolved);
      }
    }
    if (results.isEmpty()) {
      resolveTypeByName(typeRef.getText(), typeRef, results);
    }
  }

  /**
   * Parses candidate type names from a raw type signature and resolves them in the context scope.
   *
   * @param rawName the raw type string (e.g. {@code "Map<Counter, Balance>"})
   * @param context the context PSI element for lexical lookup
   * @param results accumulator set collecting resolved type definitions
   */
  private void resolveTypeByName(@NotNull String rawName, @NotNull PsiElement context, @NotNull Set<PsiElement> results) {
    List<String> candidateNames = extractTypeIdentifiers(rawName);
    for (String name : candidateNames) {
      if (isBuiltinType(name)) {
        continue;
      }
      List<CompactNamedElement> resolved = CompactResolveUtil.resolveType(name, context);
      for (CompactNamedElement elem : resolved) {
        if (elem instanceof CompactImportElementImpl importElement) {
          CompactNamedElement unwrapped = CompactResolveUtil.resolveImportElementSource(importElement);
          if (unwrapped != null) {
            results.add(unwrapped);
            continue;
          }
        }
        results.add(elem);
      }
    }
  }

  /**
   * Inspects references directly attached to the symbol or its parent to resolve target type declarations.
   *
   * @param symbol  the source symbol element
   * @param results accumulator set collecting resolved type definitions
   */
  private void resolveAndCollect(@NotNull PsiElement symbol, @NotNull Set<PsiElement> results) {
    // Step 1: Check direct references on the symbol
    for (PsiReference ref : symbol.getReferences()) {
      PsiElement target = ref.resolve();
      if (target != null) {
        collectTypeDeclarations(target, results);
        if (!results.isEmpty()) return;
      }
    }

    // Step 2: Check direct references on the symbol's parent (e.g. identifier inside CompactReferenceExpr)
    PsiElement parent = symbol.getParent();
    if (parent != null) {
      for (PsiReference ref : parent.getReferences()) {
        PsiElement target = ref.resolve();
        if (target != null) {
          collectTypeDeclarations(target, results);
          if (!results.isEmpty()) return;
        }
      }
    }
  }

  /**
   * Walks up the PSI tree from a leaf symbol to find the enclosing named declaration or expression.
   *
   * @param symbol the leaf symbol to start from
   * @return the enclosing {@link CompactNamedElement} or {@link CompactExpression}, or null
   */
  private @Nullable PsiElement findSemanticTarget(@NotNull PsiElement symbol) {
    PsiElement curr = symbol;
    while (curr != null && !(curr instanceof CompactFile)) {
      if (curr instanceof CompactNamedElement || curr instanceof CompactExpression) {
        return curr;
      }
      curr = curr.getParent();
    }
    return null;
  }

  /**
   * Tests whether the specified PSI element represents a user-defined type declaration.
   *
   * @param elem the PSI element to test
   * @return true if the element is a struct, enum, type alias, or external contract; false otherwise
   */
  private static boolean isTypeDeclaration(@NotNull PsiElement elem) {
    return elem instanceof CompactStructDefinition
        || elem instanceof CompactEnumDefinition
        || elem instanceof CompactTypeDefinition
        || elem instanceof CompactExternalContractDeclaration;
  }

  /**
   * Extracts distinct identifier substrings from a complex type name string.
   *
   * @param rawName the raw type signature
   * @return list of identifier candidate names
   */
  private static List<String> extractTypeIdentifiers(@NotNull String rawName) {
    List<String> names = new ArrayList<>();
    Matcher matcher = IDENTIFIER_PATTERN.matcher(rawName);
    while (matcher.find()) {
      String id = matcher.group();
      if (!isBuiltinType(id) && !names.contains(id)) {
        names.add(id);
      }
    }
    return names;
  }

  /**
   * Checks whether the specified name represents a built-in primitive or core compiler type.
   *
   * @param name the type name to check
   * @return true if built-in; false if potential user-defined type
   */
  private static boolean isBuiltinType(@NotNull String name) {
    return "Boolean".equals(name) || "Field".equals(name) || "Void".equals(name)
        || "Uint".equals(name) || "Bytes".equals(name) || "Vector".equals(name)
        || "Opaque".equals(name) || "Cell".equals(name) || "Map".equals(name)
        || "Unknown".equals(name) || "struct".equals(name) || "enum".equals(name);
  }

  /**
   * Converts a result set of declarations into a nullable array suitable for IntelliJ navigation actions.
   *
   * @param results set of target declarations
   * @return non-empty array of elements, or null if empty
   */
  private static PsiElement @Nullable [] toResultArray(@NotNull Set<PsiElement> results) {
    return results.isEmpty() ? null : results.toArray(PsiElement.EMPTY_ARRAY);
  }
}
