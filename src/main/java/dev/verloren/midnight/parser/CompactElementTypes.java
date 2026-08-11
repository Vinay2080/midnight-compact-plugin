package dev.verloren.midnight.parser;

import com.intellij.psi.tree.IElementType;

public final class CompactElementTypes {
  public static final IElementType PRAGMA_FORM = new CompactElementType("PRAGMA_FORM");

  public static final IElementType INCLUDE_FORM = new CompactElementType("INCLUDE_FORM");
  public static final IElementType IMPORT_FORM = new CompactElementType("IMPORT_FORM");
  public static final IElementType IMPORT_SELECTION = new CompactElementType("IMPORT_SELECTION");
  public static final IElementType IMPORT_ELEMENT = new CompactElementType("IMPORT_ELEMENT");
  public static final IElementType IMPORT_PREFIX = new CompactElementType("IMPORT_PREFIX");
  public static final IElementType EXPORT_FORM = new CompactElementType("EXPORT_FORM");
  public static final IElementType MODULE_DEFINITION = new CompactElementType("MODULE_DEFINITION");
  public static final IElementType STRUCT_DECLARATION = new CompactElementType("STRUCT_DECLARATION");
  public static final IElementType STRUCT_FIELD = new CompactElementType("STRUCT_FIELD");
  public static final IElementType ENUM_DECLARATION = new CompactElementType("ENUM_DECLARATION");
  public static final IElementType ENUM_MEMBER = new CompactElementType("ENUM_MEMBER");
  public static final IElementType CONTRACT_DECLARATION = new CompactElementType("CONTRACT_DECLARATION");
  public static final IElementType EXTERNAL_CIRCUIT = new CompactElementType("EXTERNAL_CIRCUIT");
  public static final IElementType IMPLEMENTS_DECLARATION = new CompactElementType("IMPLEMENTS_DECLARATION");
  public static final IElementType TYPE_ALIAS_DECLARATION = new CompactElementType("TYPE_ALIAS_DECLARATION");
  public static final IElementType LEDGER_DECLARATION = new CompactElementType("LEDGER_DECLARATION");
  public static final IElementType WITNESS_DECLARATION = new CompactElementType("WITNESS_DECLARATION");
  public static final IElementType CONSTRUCTOR_DEFINITION = new CompactElementType("CONSTRUCTOR_DEFINITION");
  public static final IElementType CIRCUIT_DEFINITION = new CompactElementType("CIRCUIT_DEFINITION");

  public static final IElementType GENERIC_PARAMETER_LIST = new CompactElementType("GENERIC_PARAMETER_LIST");
  public static final IElementType GENERIC_PARAMETER = new CompactElementType("GENERIC_PARAMETER");
  public static final IElementType GENERIC_ARGUMENT_LIST = new CompactElementType("GENERIC_ARGUMENT_LIST");
  public static final IElementType GENERIC_ARGUMENT = new CompactElementType("GENERIC_ARGUMENT");
  public static final IElementType SIMPLE_PARAMETER_LIST = new CompactElementType("SIMPLE_PARAMETER_LIST");
  public static final IElementType PATTERN_PARAMETER_LIST = new CompactElementType("PATTERN_PARAMETER_LIST");
  public static final IElementType ARROW_PARAMETER_LIST = new CompactElementType("ARROW_PARAMETER_LIST");
  public static final IElementType TYPED_ID = new CompactElementType("TYPED_ID");
  public static final IElementType TYPED_PATTERN = new CompactElementType("TYPED_PATTERN");
  public static final IElementType OPTIONALLY_TYPED_PATTERN = new CompactElementType("OPTIONALLY_TYPED_PATTERN");
  public static final IElementType PATTERN = new CompactElementType("PATTERN");
  public static final IElementType PATTERN_STRUCT_ELEMENT = new CompactElementType("PATTERN_STRUCT_ELEMENT");
  public static final IElementType RETURN_TYPE = new CompactElementType("RETURN_TYPE");

  public static final IElementType TYPE_REFERENCE = new CompactElementType("TYPE_REFERENCE");
  public static final IElementType BUILTIN_TYPE = new CompactElementType("BUILTIN_TYPE");
  public static final IElementType TUPLE_TYPE = new CompactElementType("TUPLE_TYPE");
  public static final IElementType TYPE_SIZE = new CompactElementType("TYPE_SIZE");

  public static final IElementType BLOCK = new CompactElementType("BLOCK");
  public static final IElementType IF_STATEMENT = new CompactElementType("IF_STATEMENT");
  public static final IElementType FOR_STATEMENT = new CompactElementType("FOR_STATEMENT");
  public static final IElementType CONST_STATEMENT = new CompactElementType("CONST_STATEMENT");
  public static final IElementType CONST_BINDING = new CompactElementType("CONST_BINDING");
  public static final IElementType RETURN_STATEMENT = new CompactElementType("RETURN_STATEMENT");
  public static final IElementType EXPR_STATEMENT = new CompactElementType("EXPR_STATEMENT");
  public static final IElementType EXPRESSION_SEQUENCE = new CompactElementType("EXPRESSION_SEQUENCE");

  public static final IElementType BINARY_EXPR = new CompactElementType("BINARY_EXPR");
  public static final IElementType CAST_EXPR = new CompactElementType("CAST_EXPR");
  public static final IElementType TERNARY_EXPR = new CompactElementType("TERNARY_EXPR");
  public static final IElementType ASSIGN_EXPR = new CompactElementType("ASSIGN_EXPR");
  public static final IElementType UNARY_EXPR = new CompactElementType("UNARY_EXPR");
  public static final IElementType INDEX_EXPR = new CompactElementType("INDEX_EXPR");
  public static final IElementType MEMBER_EXPR = new CompactElementType("MEMBER_EXPR");
  public static final IElementType CALL_EXPR = new CompactElementType("CALL_EXPR");
  public static final IElementType MAP_EXPR = new CompactElementType("MAP_EXPR");
  public static final IElementType FOLD_EXPR = new CompactElementType("FOLD_EXPR");
  public static final IElementType SLICE_EXPR = new CompactElementType("SLICE_EXPR");
  public static final IElementType TUPLE_EXPR = new CompactElementType("TUPLE_EXPR");
  public static final IElementType BYTES_EXPR = new CompactElementType("BYTES_EXPR");
  public static final IElementType STRUCT_LITERAL_EXPR = new CompactElementType("STRUCT_LITERAL_EXPR");
  public static final IElementType ASSERT_EXPR = new CompactElementType("ASSERT_EXPR");
  public static final IElementType EMIT_EXPR = new CompactElementType("EMIT_EXPR");
  public static final IElementType DISCLOSE_EXPR = new CompactElementType("DISCLOSE_EXPR");
  public static final IElementType REFERENCE_EXPR = new CompactElementType("REFERENCE_EXPR");
  public static final IElementType LITERAL_EXPR = new CompactElementType("LITERAL_EXPR");
  public static final IElementType PAD_EXPR = new CompactElementType("PAD_EXPR");
  public static final IElementType DEFAULT_EXPR = new CompactElementType("DEFAULT_EXPR");
  public static final IElementType PAREN_EXPR = new CompactElementType("PAREN_EXPR");
  public static final IElementType LAMBDA_EXPR = new CompactElementType("LAMBDA_EXPR");
  public static final IElementType TUPLE_ARG = new CompactElementType("TUPLE_ARG");
  public static final IElementType STRUCT_ARG = new CompactElementType("STRUCT_ARG");

  private CompactElementTypes() {
  }
}
