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
 * <p>Bypasses value bindings and navigates directly to the underlying struct, enum,
 * type alias, or contract declaration defining the type of a variable, parameter,
 * ledger state, or expression.</p>
 */
public class CompactTypeDeclarationProvider implements TypeDeclarationProvider {
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  @Override
  public PsiElement @Nullable [] getSymbolTypeDeclarations(@NotNull PsiElement symbol) {
    if (!symbol.isValid() || symbol.getLanguage() != CompactLanguage.INSTANCE) {
      return null;
    }

    Set<PsiElement> results = new LinkedHashSet<>();

    // 1. If the symbol is inside an explicit type reference (e.g. `p: Point`), resolve directly
    CompactTypeReferenceImpl typeRef = PsiTreeUtil.getParentOfType(symbol, CompactTypeReferenceImpl.class, false);
    if (typeRef != null) {
      addResolvedTypeReference(typeRef, results);
      if (!results.isEmpty()) {
        return toResultArray(results);
      }
    }

    // 2. Resolve references directly on or around the symbol (e.g. parameter/variable usage)
    resolveAndCollect(symbol, results);
    if (!results.isEmpty()) {
      return toResultArray(results);
    }

    // 3. Identify the candidate target semantic element (declaration or expression)
    PsiElement target = findSemanticTarget(symbol);
    if (target != null) {
      collectTypeDeclarations(target, results);
      if (!results.isEmpty()) {
        return toResultArray(results);
      }
    }

    return null;
  }

  private void collectTypeDeclarations(@NotNull PsiElement target, @NotNull Set<PsiElement> results) {
    // A. Explicit type annotations on declarations
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

    // B. Semantic return type for callables
    if (target instanceof CompactCircuitDefinition || target instanceof CompactWitnessDeclaration) {
      CompactType retType = CompactPsiUtil.getCallableReturnType(target);
      resolveTypeByName(retType.name(), target, results);
      if (!results.isEmpty()) return;
    }

    // C. Inferred semantic type of expressions / bindings
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

  private void resolveAndCollect(@NotNull PsiElement symbol, @NotNull Set<PsiElement> results) {
    // 1. Direct references on the symbol
    for (PsiReference ref : symbol.getReferences()) {
      PsiElement target = ref.resolve();
      if (target != null) {
        collectTypeDeclarations(target, results);
        if (!results.isEmpty()) return;
      }
    }

    // 2. Direct references on the symbol's parent (e.g. identifier inside CompactReferenceExpr)
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

  private static boolean isTypeDeclaration(@NotNull PsiElement elem) {
    return elem instanceof CompactStructDefinition
        || elem instanceof CompactEnumDefinition
        || elem instanceof CompactTypeDefinition
        || elem instanceof CompactExternalContractDeclaration;
  }

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

  private static boolean isBuiltinType(@NotNull String name) {
    return "Boolean".equals(name) || "Field".equals(name) || "Void".equals(name)
        || "Uint".equals(name) || "Bytes".equals(name) || "Vector".equals(name)
        || "Opaque".equals(name) || "Cell".equals(name) || "Map".equals(name)
        || "Unknown".equals(name) || "struct".equals(name) || "enum".equals(name);
  }

  private static PsiElement @Nullable [] toResultArray(@NotNull Set<PsiElement> results) {
    return results.isEmpty() ? null : results.toArray(PsiElement.EMPTY_ARRAY);
  }
}
