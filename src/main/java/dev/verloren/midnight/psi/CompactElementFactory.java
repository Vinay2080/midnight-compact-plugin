package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.parser.CompactElementTypes;
import org.jetbrains.annotations.NotNull;

public final class CompactElementFactory {
  public static @NotNull PsiElement createElement(@NotNull ASTNode node) {
    IElementType elementType = node.getElementType();
    if (elementType == CompactElementTypes.PRAGMA_FORM) {
      return new CompactPragmaFormImpl(node);
    }
    if (elementType == CompactElementTypes.INCLUDE_FORM) {
      return new CompactIncludeDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.IMPORT_FORM) {
      return new CompactImportDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.EXPORT_FORM) {
      return new CompactExportDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.MODULE_DEFINITION) {
      return new CompactModuleDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_DECLARATION) {
      return new CompactStructDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.ENUM_DECLARATION) {
      return new CompactEnumDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.CONTRACT_DECLARATION) {
      return new CompactExternalContractDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.IMPLEMENTS_DECLARATION) {
      return new CompactContractImplementsDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.TYPE_ALIAS_DECLARATION) {
      return new CompactTypeDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.LEDGER_DECLARATION) {
      return new CompactLedgerDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.WITNESS_DECLARATION) {
      return new CompactWitnessDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.CONSTRUCTOR_DEFINITION) {
      return new CompactConstructorDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.CIRCUIT_DEFINITION) {
      return new CompactCircuitDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.BLOCK) {
      return new CompactBlock(node);
    }
    if (elementType == CompactElementTypes.REFERENCE_EXPR) {
      return new CompactReferenceExprImpl(node);
    }

    return new CompactPsiElement(node);
  }

  public static boolean hasDedicatedElement(@NotNull IElementType elementType) {
    return elementType == CompactElementTypes.PRAGMA_FORM
            || elementType == CompactElementTypes.INCLUDE_FORM
            || elementType == CompactElementTypes.IMPORT_FORM
            || elementType == CompactElementTypes.EXPORT_FORM
            || elementType == CompactElementTypes.MODULE_DEFINITION
            || elementType == CompactElementTypes.STRUCT_DECLARATION
            || elementType == CompactElementTypes.ENUM_DECLARATION
            || elementType == CompactElementTypes.CONTRACT_DECLARATION
            || elementType == CompactElementTypes.IMPLEMENTS_DECLARATION
            || elementType == CompactElementTypes.TYPE_ALIAS_DECLARATION
            || elementType == CompactElementTypes.LEDGER_DECLARATION
            || elementType == CompactElementTypes.WITNESS_DECLARATION
            || elementType == CompactElementTypes.CONSTRUCTOR_DEFINITION
            || elementType == CompactElementTypes.CIRCUIT_DEFINITION
            || elementType == CompactElementTypes.BLOCK
            || elementType == CompactElementTypes.REFERENCE_EXPR;
  }

  private CompactElementFactory() {
  }
}