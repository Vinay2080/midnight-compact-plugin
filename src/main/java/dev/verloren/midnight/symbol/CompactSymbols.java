package dev.verloren.midnight.symbol;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Factory and static registry for {@link CompactSymbol} instances.
 *
 * <p>Contains predefined instances for Compact's builtin primitive types and
 * maps {@link dev.verloren.midnight.psi.CompactNamedElement} declarations to typed {@link CompactSymbol} models.</p>
 */
public final class CompactSymbols {
  private static final List<CompactBuiltinTypeSymbol> BUILTIN_TYPES = List.of(
          new CompactBuiltinTypeSymbol("Boolean", CompactPrimitiveType.BOOLEAN),
          new CompactBuiltinTypeSymbol("Field", CompactPrimitiveType.FIELD),
          new CompactBuiltinTypeSymbol("Bytes", new CompactPrimitiveType("Bytes")),
          new CompactBuiltinTypeSymbol("Opaque", new CompactPrimitiveType("Opaque")),
          new CompactBuiltinTypeSymbol("Uint", new CompactPrimitiveType("Uint")),
          new CompactBuiltinTypeSymbol("Vector", new CompactPrimitiveType("Vector")),
          new CompactBuiltinTypeSymbol("JubjubScalar", new CompactPrimitiveType("JubjubScalar")),
          new CompactBuiltinTypeSymbol("Secp256k1Base", new CompactPrimitiveType("Secp256k1Base")),
          new CompactBuiltinTypeSymbol("Secp256k1Scalar", new CompactPrimitiveType("Secp256k1Scalar"))
  );

  private CompactSymbols() {
  }

  public static @NotNull List<CompactBuiltinTypeSymbol> builtinTypes() {
    return BUILTIN_TYPES;
  }

  public static @Nullable CompactBuiltinTypeSymbol builtinType(@Nullable String name) {
    if (name == null) {
      return null;
    }
    for (CompactBuiltinTypeSymbol symbol : BUILTIN_TYPES) {
      if (name.equals(symbol.getName())) {
        return symbol;
      }
    }
    return null;
  }

  public static @Nullable CompactSymbol from(@Nullable CompactNamedElement declaration) {
    switch (declaration) {
      case null -> {
        return null;
      }
      case CompactImportElementImpl compactImportElement -> {
        return importAlias(compactImportElement);
      }
      case CompactModuleDefinition compactModuleDefinition -> {
        return new CompactPsiSymbol.Module(declaration);
      }
      default -> {
      }
    }

    CompactSymbolKind kind = kindOf(declaration);
    CompactSymbolNamespace namespace = namespaceOf(declaration);
    return switch (namespace) {
      case VALUE -> new CompactPsiSymbol.Value(declaration, kind);
      case TYPE -> new CompactPsiSymbol.Type(declaration, kind);
      case MODULE -> new CompactPsiSymbol.Module(declaration);
      case UNKNOWN -> new CompactPsiSymbol.Unknown(declaration);
    };
  }

  public static @NotNull CompactSymbolKind kindOf(@NotNull CompactNamedElement declaration) {
    if (declaration instanceof CompactCircuitDefinition) return CompactSymbolKind.CIRCUIT;
    if (declaration instanceof CompactWitnessDeclaration) return CompactSymbolKind.WITNESS;
    if (declaration instanceof CompactLedgerDeclaration) return CompactSymbolKind.LEDGER;
    if (declaration instanceof CompactParameterImpl || isPatternParameter(declaration))
      return CompactSymbolKind.PARAMETER;
    if (declaration instanceof CompactConstBindingImpl || declaration instanceof CompactPatternImpl)
      return CompactSymbolKind.LOCAL_BINDING;
    if (declaration instanceof CompactStructFieldImpl) return CompactSymbolKind.STRUCT_FIELD;
    if (declaration instanceof CompactEnumMemberImpl) return CompactSymbolKind.ENUM_MEMBER;
    if (declaration instanceof CompactStructDefinition) return CompactSymbolKind.STRUCT;
    if (declaration instanceof CompactEnumDefinition) return CompactSymbolKind.ENUM;
    if (declaration instanceof CompactTypeDefinition) return CompactSymbolKind.TYPE_ALIAS;
    if (declaration instanceof CompactExternalContractDeclaration) return CompactSymbolKind.EXTERNAL_CONTRACT;
    if (declaration instanceof CompactGenericParameterImpl) return CompactSymbolKind.GENERIC_PARAMETER;
    if (declaration instanceof CompactModuleDefinition) return CompactSymbolKind.MODULE;
    if (declaration instanceof CompactImportElementImpl) return CompactSymbolKind.IMPORT_ALIAS;
    return CompactSymbolKind.UNKNOWN;
  }

  public static @NotNull CompactSymbolNamespace namespaceOf(@NotNull CompactNamedElement declaration) {
    if (declaration instanceof CompactImportElementImpl) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) declaration);
      return target == null ? CompactSymbolNamespace.UNKNOWN : namespaceOf(target);
    }
    if (declaration instanceof CompactModuleDefinition) {
      return CompactSymbolNamespace.MODULE;
    }
    if (declaration instanceof CompactTypeDefinition
            || declaration instanceof CompactStructDefinition
            || declaration instanceof CompactEnumDefinition
            || declaration instanceof CompactExternalContractDeclaration
            || declaration instanceof CompactGenericParameterImpl) {
      return CompactSymbolNamespace.TYPE;
    }
    return switch (kindOf(declaration)) {
      case CIRCUIT, WITNESS, LEDGER, PARAMETER, LOCAL_BINDING, STRUCT_FIELD, ENUM_MEMBER ->
              CompactSymbolNamespace.VALUE;
      default -> CompactSymbolNamespace.UNKNOWN;
    };
  }

  public static @NotNull CompactVisibility visibilityOf(@NotNull CompactNamedElement declaration) {
    if (hasExportTokenBeforeName(declaration) || isListedInNearestExportForm(declaration)) {
      return CompactVisibility.EXPORTED;
    }
    if (declaration instanceof CompactParameterImpl
            || declaration instanceof CompactConstBindingImpl
            || declaration instanceof CompactPatternImpl
            || declaration instanceof CompactStructFieldImpl
            || declaration instanceof CompactEnumMemberImpl
            || declaration instanceof CompactGenericParameterImpl) {
      return CompactVisibility.LOCAL;
    }
    if (declaration instanceof CompactImportElementImpl) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) declaration);
      return target == null ? CompactVisibility.UNKNOWN : visibilityOf(target);
    }
    return CompactVisibility.MODULE;
  }

  private static @NotNull CompactSymbol importAlias(@NotNull CompactImportElementImpl declaration) {
    CompactNamedElement target = CompactResolveUtil.resolveImportElementSource(declaration);
    CompactSymbolNamespace namespace = target == null ? CompactSymbolNamespace.UNKNOWN : namespaceOf(target);
    return switch (namespace) {
      case VALUE -> new CompactPsiSymbol.Value(declaration, CompactSymbolKind.IMPORT_ALIAS);
      case TYPE -> new CompactPsiSymbol.Type(declaration, CompactSymbolKind.IMPORT_ALIAS);
      case MODULE -> new CompactPsiSymbol.Module(declaration);
      case UNKNOWN -> new CompactPsiSymbol.Unknown(declaration);
    };
  }

  private static boolean isPatternParameter(@NotNull CompactNamedElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
            || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (isToken(parent, type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasExportTokenBeforeName(@NotNull CompactNamedElement declaration) {
    PsiElement nameIdentifier = declaration.getNameIdentifier();
    int nameOffset = nameIdentifier == null ? declaration.getTextRange().getStartOffset() : nameIdentifier.getTextRange().getStartOffset();
    for (PsiElement child = declaration.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getTextRange().getStartOffset() >= nameOffset) {
        return false;
      }
      if (isToken(child, CompactTokenTypes.EXPORT)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isListedInNearestExportForm(@NotNull CompactNamedElement declaration) {
    String name = declaration.getName();
    if (name == null) {
      return false;
    }
    CompactModuleDefinition module = PsiTreeUtil.getParentOfType(declaration, CompactModuleDefinition.class);
    PsiElement boundary = module == null ? declaration.getContainingFile() : module;
    if (boundary == null) {
      return false;
    }
    for (CompactExportDeclaration exportDeclaration : PsiTreeUtil.findChildrenOfType(boundary, CompactExportDeclaration.class)) {
      if (module != null && PsiTreeUtil.getParentOfType(exportDeclaration, CompactModuleDefinition.class) != module) {
        continue;
      }
      if (module == null && PsiTreeUtil.getParentOfType(exportDeclaration, CompactModuleDefinition.class) != null) {
        continue;
      }
      for (PsiElement child : exportDeclaration.getChildren()) {
        if (isToken(child, CompactTokenTypes.IDENTIFIER) && name.equals(child.getText())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isToken(@NotNull PsiElement element, @NotNull IElementType tokenType) {
    return element.getNode() != null && element.getNode().getElementType() == tokenType;
  }
}
