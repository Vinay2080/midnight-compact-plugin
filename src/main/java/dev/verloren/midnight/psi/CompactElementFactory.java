package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
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
    if (elementType == CompactElementTypes.BINARY_EXPR) {
      return new CompactBinaryExprImpl(node);
    }
    if (elementType == CompactElementTypes.UNARY_EXPR) {
      return new CompactUnaryExprImpl(node);
    }
    if (elementType == CompactElementTypes.PAREN_EXPR) {
      return new CompactParenExprImpl(node);
    }
    if (elementType == CompactElementTypes.REFERENCE_EXPR) {
      return new CompactReferenceExprImpl(node);
    }
    if (elementType == CompactElementTypes.LITERAL_EXPR) {
      return new CompactLiteralExprImpl(node);
    }
    if (elementType == CompactElementTypes.CALL_EXPR) {
      return new CompactCallExprImpl(node);
    }
    if (elementType == CompactElementTypes.TYPE_REFERENCE) {
      return new CompactTypeReferenceImpl(node);
    }
    if (elementType == CompactElementTypes.BUILTIN_TYPE) {
      return new CompactBuiltinTypeImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_LITERAL_EXPR) {
      return new CompactStructLiteralExprImpl(node);
    }
    if (elementType == CompactElementTypes.MEMBER_EXPR) {
      return new CompactMemberExprImpl(node);
    }
    if (elementType == CompactElementTypes.IMPORT_ELEMENT) {
      return new CompactImportElementImpl(node);
    }
    if (elementType == CompactElementTypes.TYPED_ID) {
      return new CompactParameterImpl(node);
    }
    if (elementType == CompactElementTypes.TYPED_PATTERN) {
      return new CompactTypedPatternImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_FIELD) {
      return new CompactStructFieldImpl(node);
    }
    if (elementType == CompactElementTypes.ENUM_MEMBER) {
      return new CompactEnumMemberImpl(node);
    }
    if (elementType == CompactElementTypes.CONST_BINDING) {
      return new CompactConstBindingImpl(node);
    }
    if (elementType == CompactElementTypes.GENERIC_PARAMETER) {
      return new CompactGenericParameterImpl(node);
    }
    if (elementType == CompactElementTypes.PATTERN) {
      return new CompactPatternImpl(node);
    }

    return new CompactPsiElement(node);
  }

  public static @NotNull PsiElement createIdentifierLeaf(@NotNull Project project, @NotNull String text) {
    CompactFile file = (CompactFile)PsiFileFactory.getInstance(project)
            .createFileFromText("rename.compact", CompactFileType.INSTANCE, "type " + text + " = Field;");
    CompactTypeDefinition typeDefinition = PsiTreeUtil.findChildOfType(file, CompactTypeDefinition.class);
    if (typeDefinition == null || typeDefinition.getNameIdentifier() == null) {
      throw new IllegalArgumentException("Invalid Compact identifier: " + text);
    }
    return typeDefinition.getNameIdentifier();
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
            || elementType == CompactElementTypes.BINARY_EXPR
            || elementType == CompactElementTypes.UNARY_EXPR
            || elementType == CompactElementTypes.PAREN_EXPR
            || elementType == CompactElementTypes.REFERENCE_EXPR
            || elementType == CompactElementTypes.LITERAL_EXPR
            || elementType == CompactElementTypes.CALL_EXPR
            || elementType == CompactElementTypes.TYPE_REFERENCE
            || elementType == CompactElementTypes.BUILTIN_TYPE
            || elementType == CompactElementTypes.STRUCT_LITERAL_EXPR
            || elementType == CompactElementTypes.MEMBER_EXPR
            || elementType == CompactElementTypes.IMPORT_ELEMENT
            || elementType == CompactElementTypes.TYPED_ID
            || elementType == CompactElementTypes.TYPED_PATTERN
            || elementType == CompactElementTypes.STRUCT_FIELD
            || elementType == CompactElementTypes.ENUM_MEMBER
            || elementType == CompactElementTypes.CONST_BINDING
            || elementType == CompactElementTypes.GENERIC_PARAMETER
            || elementType == CompactElementTypes.PATTERN;
  }

  private CompactElementFactory() {
  }
}